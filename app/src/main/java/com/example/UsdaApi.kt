package com.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class UsdaSearchResponse(
    val totalHits: Int? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val foods: List<UsdaFoodItem>? = null
)

@Serializable
data class UsdaFoodItem(
    val fdcId: Int,
    val description: String,
    val dataType: String? = null,
    val foodNutrients: List<UsdaFoodNutrient>? = null
) {
    fun getCalories(): Double {
        return foodNutrients?.find { 
            val name = it.nutrientName?.lowercase() ?: ""
            it.unitName?.lowercase() == "kcal" || name.contains("energy") || name.contains("calories")
        }?.value ?: 0.0
    }

    fun getProtein(): Double {
        return foodNutrients?.find { 
            val name = it.nutrientName?.lowercase() ?: ""
            name.contains("protein")
        }?.value ?: 0.0
    }

    fun getCarbs(): Double {
        return foodNutrients?.find { 
            val name = it.nutrientName?.lowercase() ?: ""
            name.contains("carbohydrate")
        }?.value ?: 0.0
    }

    fun getFats(): Double {
        return foodNutrients?.find { 
            val name = it.nutrientName?.lowercase() ?: ""
            name.contains("lipid") || name == "fat" || name.contains("fat, total")
        }?.value ?: 0.0
    }
}

@Serializable
data class UsdaFoodNutrient(
    val nutrientId: Int? = null,
    val nutrientName: String? = null,
    val nutrientNumber: String? = null,
    val unitName: String? = null,
    val value: Double? = null
)

interface UsdaApiService {
    @GET("v1/foods/search")
    suspend fun searchFood(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("pageSize") pageSize: Int = 10,
        @Query("pageNumber") pageNumber: Int = 1
    ): UsdaSearchResponse
}

object UsdaRetrofitClient {
    private const val BASE_URL = "https://api.nal.usda.gov/fdc/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: UsdaApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(UsdaApiService::class.java)
        retrofit
    }
}
