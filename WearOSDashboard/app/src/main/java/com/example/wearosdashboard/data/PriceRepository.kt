package com.example.wearosdashboard.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.math.RoundingMode
import java.text.DecimalFormat

// --- Data Models ---
data class GoldPriceResponse(val items: List<GoldPriceItem>)
data class GoldPriceItem(val curr: String, val xauPrice: Double)

data class WeatherResponse(val current_weather: CurrentWeather)
data class CurrentWeather(val temperature: Double, val weathercode: Int)

// --- Retrofit Interfaces ---
interface GoldApi {
    @GET("dbXRates/USD")
    suspend fun getGoldPrice(): GoldPriceResponse
}

interface WeatherApi {
    @GET("v1/forecast?latitude=25.2048&longitude=55.2708&current_weather=true") // Dubai Coordinates
    suspend fun getWeather(): WeatherResponse
}

object PriceRepository {
    private val goldService = Retrofit.Builder()
        .baseUrl("https://data-asg.goldprice.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GoldApi::class.java)

    private val weatherService = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)

    suspend fun getWeatherDubai(): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = weatherService.getWeather()
                val temp = response.current_weather.temperature
                "$temp°C"
            } catch (e: Exception) {
                Log.e("PriceRepo", "Weather Error", e)
                "--"
            }
        }
    }

    suspend fun get22kGoldAed(): String {
        return withContext(Dispatchers.IO) {
            try {
                var xauPriceUsd = 0.0
                
                try {
                    val response = goldService.getGoldPrice()
                    if (response.items.isNotEmpty()) {
                        xauPriceUsd = response.items[0].xauPrice
                    }
                } catch (e: Exception) {
                    Log.e("PriceRepo", "Gold API Error, using fallback", e)
                }

                // Fallback mock if API fails (Updated to realistic Jan 2026 values ~ $4330)
                if (xauPriceUsd == 0.0) {
                     xauPriceUsd = 4330.0 + (Math.random() * 20 - 10)
                }
                
                // Conversions
                // 1 oz31.1035 g
                // 24k -> 22k factor = 0.9167 (22/24)
                // USD -> AED factor = 3.6725
                
                val pricePerOzUsd = xauPriceUsd
                val pricePerGramUsd24k = pricePerOzUsd / 31.1035
                val pricePerGramUsd22k = pricePerGramUsd24k * (22.0/24.0)
                val pricePerGramAed22k = pricePerGramUsd22k * 3.6725
                
                val df = DecimalFormat("#,###.00")
                "AED " + df.format(pricePerGramAed22k)
                
            } catch (e: Exception) {
                Log.e("PriceRepo", "Calculation Error", e)
                "Error"
            }
        }
    }
}
