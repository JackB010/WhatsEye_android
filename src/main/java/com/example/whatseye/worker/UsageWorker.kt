package com.example.whatseye.worker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.whatseye.api.RetrofitClient
import com.example.whatseye.dataType.data.UsageData
import com.example.whatseye.dataType.db.UsageDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        RetrofitClient.initialize(applicationContext)

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val now = Calendar.getInstance()
            val currentDate = dateFormat.format(now.time)
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentUsageSeconds = getHourUsage(now.timeInMillis)

            sendDataToServer(UsageData(currentDate,currentHour,currentUsageSeconds))
            dbHelper.insertOrUpdateUsageData(currentDate, currentHour , currentUsageSeconds, 1 )

            Log.d("sendDataToServer", "sendDataToServer")

            val previous = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -1) }
            val previousHour  = previous.get(Calendar.HOUR_OF_DAY)
            val previousDate  = dateFormat.format(previous.time)
            val previousUsageSeconds  = getHourUsage(previous.timeInMillis)

            // Save locally with (date, hour)
            dbHelper.insertOrUpdateUsageData(previousDate, previousHour , previousUsageSeconds, 0 )

            if (isInternetAvailable(applicationContext)) {
                val unsent = dbHelper.getUnsent()
                for (data in unsent) {
                    delay(500)
                    if (sendDataToServer(data)) {
                        dbHelper.insertOrUpdateUsageData(data.date, data.hour , data.usage_seconds, 1)
                    }
                }
            }
            dbHelper.close()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun getHourUsage(referenceTime: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = referenceTime

        // Start of hour
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        // End of hour
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis

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
        return usageMillis / 1000 // seconds
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun sendDataToServer(data: UsageData): Boolean = withContext(Dispatchers.IO) {
        val call = RetrofitClient.controlApi.sendUsageData(data)
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
