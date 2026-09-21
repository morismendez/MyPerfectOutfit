package com.myperfectoutfit.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.myperfectoutfit.MainActivity
import com.myperfectoutfit.R

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "outfit_reminders_v2"
        private const val CHANNEL_NAME = "Asesor de Estilo"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Cambio a HIGH para que aparezca el banner
            ).apply {
                description = "Notificaciones para finalizar viajes y organizar lavandería"
                setShowBadge(true) // Habilita el punto en el icono
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showTripEndNotification(tripName: String) {
        android.util.Log.d("NotificationHelper", "Intentando mostrar notificación de fin: $tripName")
        if (!hasNotificationPermission()) {
            android.util.Log.w("NotificationHelper", "Sin permiso de notificaciones")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Bienvenido a casa!")
            .setContentText("Tu viaje a '$tripName' ha finalizado. ¿Organizamos tu ropa?")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Cambio a HIGH
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(tripName.hashCode(), notification)
    }

    fun showPackingReminderNotification(tripName: String) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Es hora de empacar!")
            .setContentText("Tu viaje a '$tripName' comienza pronto. ¿Armamos tu maleta perfecta?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(tripName.hashCode() + 1, notification)
    }

    fun showPlannedOutfitNotification() {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Buenos días!")
            .setContentText("Tienes un outfit planificado para hoy. ¡Míralo ahora!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(20240911, notification)
    }

    fun showNoOutfitPlannedNotification() {
        android.util.Log.d("NotificationHelper", "Intentando mostrar notificación de NO outfit")
        if (!hasNotificationPermission()) {
            android.util.Log.w("NotificationHelper", "Sin permiso de notificaciones")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 3, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat) // Usando un icono de sistema más común
            .setContentTitle("¿Qué nos ponemos hoy?")
            .setContentText("Aún no tienes un outfit planeado. ¡Deja que la IA te dé una recomendación!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(20240912, notification)
    }

    fun showNightPreparationNotification(garmentsSummary: String?) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 4, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (!garmentsSummary.isNullOrBlank()) {
            "Para mañana agendaste un outfit con prendas pendientes ($garmentsSummary). Recuerda tenerlas listas y planchadas."
        } else {
            "Recuerda dejar listo y planchado tu outfit de mañana. ¡Empecemos bien el día!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Preparación Nocturna")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(20240913, notification)
    }
}
