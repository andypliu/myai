package com.example.myai.data.remote

import android.content.Context
import android.util.Log
import com.example.myai.data.config.ApiConfig
import com.example.myai.domain.model.OllamaModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class NvidiaModelsService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun getModels(): Result<List<OllamaModel>> = withContext(Dispatchers.IO) {
        try {
            Log.d("NvidiaModelsService", "Scraping models from catalog: ${ApiConfig.NVIDIA_CATALOG_URL}")
            
            val request = Request.Builder()
                .url(ApiConfig.NVIDIA_CATALOG_URL)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e("NvidiaModelsService", "Catalog scrape error: ${response.code}")
                return@withContext Result.failure(Exception("Catalog scrape error: ${response.code}"))
            }

            val html = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
            val document = Jsoup.parse(html)
            
            // Extract model links from the catalog
            // Links are in the format: /[publisher]/[model-slug]
            val modelElements = document.select("a[href^=/]")
            
            val models = modelElements.mapNotNull { element ->
                val href = element.attr("href").trim('/')
                val parts = href.split('/')
                
                // A valid model ID should have exactly 2 parts: publisher/model-slug
                if (parts.size >= 2) {
                    val publisher = parts[0]
                    val slug = parts[1]
                    
                    // Filter out reserved words and generic paths
                    val reserved = listOf("docs", "search", "explore", "models", "owners", "about", "privacy", "terms", "blog", "support", "solutions")
                    if (publisher in reserved || slug in reserved) return@mapNotNull null
                    
                    // Basic validation: publisher shouldn't contain dots or special chars
                    if (publisher.contains('.') || publisher.contains('?') || publisher.contains('#')) return@mapNotNull null
                    
                    val id = "$publisher/$slug"
                    Log.d("NvidiaModelsService", "Found potential model: $id")
                    
                    OllamaModel(
                        name = id,
                        modifiedAt = "",
                        size = 0L
                    )
                } else {
                    null
                }
            }.distinctBy { it.name }.sortedBy { it.name }

            if (models.isEmpty()) {
                Log.w("NvidiaModelsService", "No models found, check selectors.")
            }

            Log.i("NvidiaModelsService", "Successfully scraped ${models.size} models")
            Result.success(models)
        } catch (e: Exception) {
            Log.e("NvidiaModelsService", "Error scraping Nvidia catalog", e)
            Result.failure(e)
        }
    }
}
