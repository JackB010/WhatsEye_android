package com.example.whatseye.worker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.whatseye.api.RetrofitClient
import com.example.whatseye.dataType.UsageData
import com.example.whatseye.dataType.db.UsageDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class UsageWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val dbHelper = UsageDatabase(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.HOUR_OF_DAY, -1)
            val startTime = calendar.timeInMillis
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(calendar.time)

            val usageSeconds = calculateWhatsAppUsage(startTime, endTime)

            // Save locally with (date, hour)
            dbHelper.insertUsageData(currentDate, hour, usageSeconds)

            if (isInternetAvailable(applicationContext)) {
                val unsent = dbHelper.getUnsent()
                for (data in unsent) {
                    if (sendDataToServer(data)) {
                        dbHelper.deleteUsageData(data.date, data.hour)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun calculateWhatsAppUsage(startTime: Long, endTime: Long): Long {
        val events = usageStatsManager.queryEvents(startTime, endTime)
        var usageMillis = 0L
        var lastForegroundTime = 0L
        var lastPackage: String? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != "com.whatsapp") continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastForegroundTime = event.timeStamp
                    lastPackage = event.packageName
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (lastForegroundTime > 0 && lastPackage == "com.whatsapp") {
                        usageMillis += event.timeStamp - lastForegroundTime
                        lastForegroundTime = 0L
                        lastPackage = null
                    }
                }
            }
        }

        return usageMillis / 1000
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun sendDataToServer(data: UsageData): Boolean = withContext(Dispatchers.IO) {
        val userId = "40e98f0a-6905-4aef-80ec-4682b96d1e08"
        val call = RetrofitClient.api.sendUsageData(userId, data)
        val deferred = CompletableDeferred<Boolean>()

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                deferred.complete(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                deferred.complete(false)
            }
        })

        deferred.await()
    }
}
