package com.myperfectoutfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.myperfectoutfit.ui.screens.MainScreen
import com.myperfectoutfit.ui.theme.MyPerfectOutfitTheme
import com.myperfectoutfit.worker.TripNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleTripNotifications()
        setContent {
            MyPerfectOutfitTheme {
                var showIntro by remember { mutableStateOf(true) }
                
                if (showIntro) {
                    com.myperfectoutfit.ui.screens.IntroSplashScreen(onFinished = { showIntro = false })
                } else {
                    MainScreen()
                }
            }
        }
    }

    private fun scheduleTripNotifications() {
        val workManager = WorkManager.getInstance(applicationContext)

        // 1. Ejecución periódica (cada 12 horas)
        val periodicRequest = PeriodicWorkRequestBuilder<TripNotificationWorker>(
            12, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "TripEndCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        // 2. Ejecución única inmediata (para pruebas y refresco al abrir)
        val oneTimeRequest = OneTimeWorkRequestBuilder<TripNotificationWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS) // Un pequeño delay para no saturar el inicio
            .build()
        
        workManager.enqueue(oneTimeRequest)
    }
}