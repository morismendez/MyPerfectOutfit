package com.myperfectoutfit.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.myperfectoutfit.data.local.enums.LaundryState
import com.myperfectoutfit.data.repository.PlanningRepository
import com.myperfectoutfit.data.repository.SuitcaseRepository
import com.myperfectoutfit.data.repository.WardrobeRepository
import com.myperfectoutfit.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class TripNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val suitcaseRepository: SuitcaseRepository,
    private val planningRepository: PlanningRepository,
    private val repository: WardrobeRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        android.util.Log.d("TripNotificationWorker", "--- INICIANDO TRABAJADOR ---")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        android.util.Log.d("TripNotificationWorker", "Fecha local detectada: $today")

        // Buscamos viajes que terminaron ayer (o antes) y no están completados
        // Para simplificar, obtenemos todas las maletas y filtramos
        // En una app real, el DAO debería tener una query específica.
        
        try {
            val userFlow = repository.getPrimaryUser()
            val user = userFlow.first() 
            
            if (user == null) {
                android.util.Log.w("TripNotificationWorker", "Usuario no encontrado, abortando.")
                return Result.success()
            }
            
            val suitcases = suitcaseRepository.getAllSuitcases(user.id).first()
            
            val tripsToNotifyEnd = suitcases.filter { 
                !it.isCompleted && it.endDate < today 
            }

            android.util.Log.d("TripNotificationWorker", "Viajes finalizados detectados: ${tripsToNotifyEnd.size}")
            
            val notificationHelper = NotificationHelper(applicationContext)

            if (tripsToNotifyEnd.isNotEmpty()) {
                tripsToNotifyEnd.forEach { trip ->
                    android.util.Log.d("TripNotificationWorker", "Mostrando notificación de fin para: ${trip.tripName}")
                    notificationHelper.showTripEndNotification(trip.tripName)
                }
            }

            // --- Recordatorio de Empaque (48h antes) ---
            val sdfFull = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val nowTime = Date().time
            val fortyEightHoursInMillis = 48 * 60 * 60 * 1000L

            val tripsToNotifyPacking = suitcases.filter { trip ->
                !trip.isCompleted && trip.startDate >= today && try {
                    val tripStartTime = sdfFull.parse(trip.startDate)?.time ?: 0L
                    val diff = tripStartTime - nowTime
                    diff in 0..fortyEightHoursInMillis
                } catch (_: Exception) { false }
            }

            if (tripsToNotifyPacking.isNotEmpty()) {
                tripsToNotifyPacking.forEach { trip ->
                    notificationHelper.showPackingReminderNotification(trip.tripName)
                }
            }

            // --- Recordatorio de Outfit (Mañana: 5 AM a 7 AM) ---
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            android.util.Log.d("TripNotificationWorker", "Hora actual para recordatorio: $hour")

            if (hour in 5..7) {
                val todayOutfit = planningRepository.getPlannedOutfitByDate(user.id, today).first()
                android.util.Log.d("TripNotificationWorker", "Outfit planeado hoy: ${todayOutfit != null}")

                if (todayOutfit != null) {
                    notificationHelper.showPlannedOutfitNotification()
                } else {
                    android.util.Log.d("TripNotificationWorker", "Lanzando notificación de NO outfit")
                    notificationHelper.showNoOutfitPlannedNotification()
                }
            }

            // --- Alerta de Preparación Nocturna (19h a 21h) ---
            if (hour in 19..21) {
                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tomorrowCal.time)
                
                val tomorrowOutfit = planningRepository.getPlannedOutfitByDate(user.id, tomorrowStr).first()
                if (tomorrowOutfit != null) {
                    val laundryGarments = mutableListOf<String>()
                    
                    fun check(name: String, state: LaundryState?) {
                        if (state == LaundryState.IN_LAUNDRY) laundryGarments.add(name)
                    }

                    check("Camisa", tomorrowOutfit.shirt?.laundryState)
                    check("Pantalón", tomorrowOutfit.pant?.laundryState)
                    check("Chaqueta", tomorrowOutfit.jacket?.laundryState)
                    check("Corbata", tomorrowOutfit.tie?.laundryState)
                    check("Vestido", tomorrowOutfit.dress?.laundryState)
                    check("Falda", tomorrowOutfit.skirt?.laundryState)

                    val summary = if (laundryGarments.isNotEmpty()) laundryGarments.joinToString(", ") else null
                    notificationHelper.showNightPreparationNotification(summary)
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("TripNotificationWorker", "Error en el trabajador: ${e.message}", e)
            return Result.retry()
        }
    }
}
