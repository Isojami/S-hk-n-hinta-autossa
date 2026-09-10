package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class LatestPricesResponse(
    @Json(name = "prices") val prices: List<PorssisahkoPriceItem> = emptyList()
)

data class PorssisahkoPriceItem(
    @Json(name = "price") val price: Double,
    @Json(name = "startDate") val startDate: String,
    @Json(name = "endDate") val endDate: String
)

data class SinglePriceResponse(
    @Json(name = "price") val price: Double
)

data class SpotHintaItem(
    @Json(name = "DateTime") val dateTime: String,
    @Json(name = "PriceWithTax") val priceWithTax: Double,
    @Json(name = "Rank") val rank: Int? = null
)

data class SpotHintaJustNow(
    @Json(name = "DateTime") val dateTime: String,
    @Json(name = "PriceWithTax") val priceWithTax: Double
)

interface PorssisahkoApiService {
    @GET("v1/latest-prices.json")
    suspend fun getLatestPrices(): LatestPricesResponse

    @GET("v1/price.json")
    suspend fun getPriceForHour(
        @Query("date") date: String, // YYYY-MM-DD
        @Query("hour") hour: Int     // 0..23
    ): SinglePriceResponse
}

interface SpotHintaApiService {
    @GET("TodayAndDayForward")
    suspend fun getTodayAndDayForward(): List<SpotHintaItem>

    @GET("JustNow")
    suspend fun getJustNow(): SpotHintaJustNow
}

object NetworkClient {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "NordPoolSpotCarApp/1.0 (Android; Kotlin)")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    val porssisahkoApi: PorssisahkoApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.porssisahko.net/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PorssisahkoApiService::class.java)
    }

    val spotHintaApi: SpotHintaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.spot-hinta.fi/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SpotHintaApiService::class.java)
    }
}
