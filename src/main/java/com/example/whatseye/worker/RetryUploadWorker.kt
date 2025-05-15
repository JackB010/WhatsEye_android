package com.example.whatseye.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.whatseye.utils.retryFailedUploads

class RetryUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Call your retry logic here
        retryFailedUploads(applicationContext)
        return Result.success()
    }
}