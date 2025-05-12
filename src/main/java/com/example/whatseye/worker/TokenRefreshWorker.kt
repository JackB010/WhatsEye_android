package com.example.whatseye.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.whatseye.api.RetrofitClient
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.dataType.data.RefreshTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.awaitResponse
import java.io.IOException


class TokenRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val tokenManager = JwtTokenManager(context)

    companion object {
        const val KEY_ERROR = "error"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get the refresh token from secure storage
            val refreshToken = tokenManager.getRefreshJwt()
                ?: return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "No refresh token available")
                )

            // Make the refresh token request
            val request = RefreshTokenRequest(refresh = refreshToken)
            val response = RetrofitClient.accountApi.refreshToken(request).awaitResponse()

            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse?.refresh != null) {
                    // Save new tokens
                    tokenManager.saveAccessJwt(tokenResponse.access)
                    tokenManager.saveRefreshJwt(tokenResponse.refresh)
                    Result.success()
                } else {
                    tokenManager.clearAllTokens()
                    Result.failure(workDataOf(KEY_ERROR to "Empty or invalid token response"))
                }
            } else {
                Result.failure(workDataOf(KEY_ERROR to "token_not_valid"))
            }
        } catch (e: IOException) {
            // Handle network-related errors
            Result.retry()
        } catch (e: HttpException) {
            // Handle HTTP errors
            Result.failure(workDataOf(KEY_ERROR to "HTTP error: ${e.code()}"))
        } catch (e: Exception) {
            // Handle unexpected errors
            Result.failure(workDataOf(KEY_ERROR to "Unexpected error: ${e.message}"))
        }
    }
}