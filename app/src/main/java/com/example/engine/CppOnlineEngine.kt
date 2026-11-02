package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class OnlineExecutionResult(
    val output: String,
    val error: String? = null,
    val exitCode: Int = 0,
    val isSuccess: Boolean = true
)

class CppOnlineEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun executeOnline(sourceCode: String, stdin: String = ""): OnlineExecutionResult {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject()
                json.put("language", "c++")
                json.put("version", "10.2.0")

                val filesArray = JSONArray()
                val fileObj = JSONObject()
                fileObj.put("name", "main.cpp")
                fileObj.put("content", sourceCode)
                filesArray.put(fileObj)

                json.put("files", filesArray)
                json.put("stdin", stdin)

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://emkc.org/api/v2/piston/execute")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext OnlineExecutionResult(
                        output = "",
                        error = "API error (${response.code}): $responseBody",
                        exitCode = 1,
                        isSuccess = false
                    )
                }

                val respJson = JSONObject(responseBody)
                val runObj = respJson.optJSONObject("run")
                val stdout = runObj?.optString("stdout") ?: ""
                val stderr = runObj?.optString("stderr") ?: ""
                val code = runObj?.optInt("code", 0) ?: 0

                val combinedOutput = stdout.ifEmpty { stderr }
                val errorMsg = if (stderr.isNotEmpty() && code != 0) stderr else null

                OnlineExecutionResult(
                    output = combinedOutput,
                    error = errorMsg,
                    exitCode = code,
                    isSuccess = code == 0
                )
            } catch (e: Exception) {
                OnlineExecutionResult(
                    output = "",
                    error = "Failed to reach online compiler: ${e.message}",
                    exitCode = 1,
                    isSuccess = false
                )
            }
        }
    }
}
