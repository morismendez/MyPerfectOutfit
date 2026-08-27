package com.myperfectoutfit.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImageFramingDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onImageFramed: (Uri) -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(imageUri) {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        BitmapFactory.decodeStream(inputStream)
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale *= zoom
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val circleRadius = size.minDimension / 2.5f
                            val center = Offset(size.width / 2, size.height / 2)

                            // Dibujar fondo oscuro semi-transparente
                            drawRect(color = Color.Black.copy(alpha = 0.5f))

                            // Recortar el círculo central
                            val path = androidx.compose.ui.graphics.Path().apply {
                                addOval(androidx.compose.ui.geometry.Rect(center, circleRadius))
                            }

                            clipPath(path, androidx.compose.ui.graphics.ClipOp.Difference) {
                                drawRect(color = Color.Black.copy(alpha = 0.7f))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(300.dp) // Tamaño relativo de la ventana de encuadre
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("Cancelar")
                    }
                    Button(onClick = {
                        val framedBitmap = cropCircle(bitmap!!, scale, offset)
                        val framedUri = saveFramedImage(context, framedBitmap)
                        if (framedUri != null) onImageFramed(framedUri)
                    }) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}

private fun cropCircle(bitmap: Bitmap, scale: Float, offset: Offset): Bitmap {
    val size = 500 // Tamaño final de la foto de perfil
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }

    // Dibujar círculo
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)

    // Calcular dimensiones de dibujo basadas en escala y offset
    // Este es un cálculo simplificado para centrar el bitmap
    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
    
    // Ajustar según el centro
    val scaledWidth = bitmap.width * scale
    val scaledHeight = bitmap.height * scale
    
    val left = (size - scaledWidth) / 2 + offset.x
    val top = (size - scaledHeight) / 2 + offset.y
    val right = left + scaledWidth
    val bottom = top + scaledHeight

    canvas.drawBitmap(bitmap, srcRect, RectF(left, top, right, bottom), paint)

    return output
}

private fun saveFramedImage(context: android.content.Context, bitmap: Bitmap): Uri? {
    val file = File(context.filesDir, "profile_framed_${System.currentTimeMillis()}.jpg")
    return try {
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()
        Uri.fromFile(file)
    } catch (e: Exception) {
        null
    }
}
