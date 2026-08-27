package com.myperfectoutfit.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
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
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        // 1. La Imagen debajo
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.wrapContentSize()
                            )
                        }

                        // 2. El Marco Guía encima (Overlay)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val circleRadius = size.minDimension / 2.5f
                            val center = Offset(size.width / 2, size.height / 2)

                            // Recorte del círculo para oscurecer el exterior
                            val path = Path().apply {
                                addOval(androidx.compose.ui.geometry.Rect(center, circleRadius))
                            }

                            clipPath(path, clipOp = ClipOp.Difference) {
                                drawRect(color = Color.Black.copy(alpha = 0.7f))
                            }

                            // Dibujar el borde blanco de la guía
                            drawCircle(
                                color = Color.White,
                                radius = circleRadius,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // Instrucciones rápidas
                Text(
                    text = "Usa dos dedos para ampliar y desliza para centrar",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            bitmap?.let {
                                val framedBitmap = cropCircle(it, scale, offset)
                                val framedUri = saveFramedImage(context, framedBitmap)
                                if (framedUri != null) onImageFramed(framedUri)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}

private fun cropCircle(bitmap: Bitmap, scale: Float, offset: Offset): Bitmap {
    val outputSize = 500
    val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }

    // El resultado final siempre será un círculo sobre fondo transparente (o color de fondo de app)
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawCircle(outputSize / 2f, outputSize / 2f, outputSize / 2f, paint)

    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)

    // Calculamos cómo dibujar el bitmap para que coincida con lo que el usuario vio en la pantalla
    // Esto requiere mapear el offset y la escala de la UI (basada en el tamaño del Box) 
    // al tamaño real del bitmap.
    
    val matrix = android.graphics.Matrix()
    
    // 1. Centrar el bitmap en el origen
    matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
    
    // 2. Aplicar la escala
    // Nota: El factor de escala en la UI es relativo al tamaño de visualización.
    // Para simplificar, asumimos que el encuadre en UI es una representación proporcional.
    matrix.postScale(scale, scale)
    
    // 3. Aplicar el desplazamiento (offset) y mover al centro del output
    // El offset de Compose está en pixeles de pantalla, aquí lo aplicamos directamente 
    // ajustado al centro de nuestro lienzo de 500x500.
    matrix.postTranslate(outputSize / 2f + offset.x, outputSize / 2f + offset.y)

    canvas.drawBitmap(bitmap, matrix, paint)

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
