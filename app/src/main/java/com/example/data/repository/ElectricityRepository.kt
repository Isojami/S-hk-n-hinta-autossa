package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.ElectricityDatabase
import com.example.data.local.ElectricityPriceEntity
import com.example.data.model.DayPriceSummary
import com.example.data.model.DayType
import com.example.data.model.PricePoint
import com.example.data.network.NetworkClient
import com.example.data.network.PorssisahkoApiService
import com.example.data.network.SpotHintaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ElectricityUiState(
    val yesterday: DayPriceSummary,
    val today: DayPriceSummary,
    val tomorrow: DayPriceSummary,
    val currentPricePoint: PricePoint? = null,
    val selectedDay: DayType = DayType.TODAY,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastUpdatedMillis: Long = 0L
) {
    val activeSummary: DayPriceSummary
        get() = when (selectedDay) {
            DayType.YESTERDAY -> yesterday
            DayType.TODAY -> today
            DayType.TOMORROW -> tomorrow
        }
}

class ElectricityRepository(
    context: Context,
    private val porssisahkoApi: PorssisahkoApiService = NetworkClient.porssisahkoApi,
    private val spotHintaApi: SpotHintaApiService = NetworkClient.spotHintaApi
) {
    private val db = ElectricityDatabase.getInstance(context)
    private val dao = db.electricityPriceDao()
    private val helsinkiZone = ZoneId.of("Europe/Helsinki")

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<ElectricityUiState> = _uiState.asStateFlow()

    private fun createInitialState(): ElectricityUiState {
        val now = ZonedDateTime.now(helsinkiZone)
        val todayDate = now.toLocalDate()
        val yesterdayDate = todayDate.minusDays(1)
        val tomorrowDate = todayDate.plusDays(1)

        val formatter = DateTimeFormatter.ofPattern("d.M. (EEE)", Locale("fi"))

        return ElectricityUiState(
            yesterday = DayPriceSummary(
                dayType = DayType.YESTERDAY,
                dateString = yesterdayDate.toString(),
                formattedDate = yesterdayDate.format(formatter)
            ),
            today = DayPriceSummary(
                dayType = DayType.TODAY,
                dateString = todayDate.toString(),
                formattedDate = todayDate.format(formatter)
            ),
            tomorrow = DayPriceSummary(
                dayType = DayType.TOMORROW,
                dateString = tomorrowDate.toString(),
                formattedDate = tomorrowDate.format(formatter),
                unavailableMessage = "Huomisen hinnat julkaistaan yleensä klo 13:45–14:00."
            ),
            isLoading = true
        )
    }

    suspend fun refreshPrices(force: Boolean = false) {
        withContext(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val now = ZonedDateTime.now(helsinkiZone)
            val todayDate = now.toLocalDate()
            val yesterdayDate = todayDate.minusDays(1)
            val tomorrowDate = todayDate.plusDays(1)
            val currentHour = now.hour

            val fiFormatter = DateTimeFormatter.ofPattern("d.M. (EEE)", Locale("fi"))

            try {
                // 1. Fetch latest 48h prices from porssisahko.net
                val latestResponse = try {
                    porssisahkoApi.getLatestPrices()
                } catch (e: Exception) {
                    Log.w("ElectricityRepo", "Failed to fetch porssisahko latest prices: ${e.message}")
                    null
                }

                val entitiesToInsert = mutableListOf<ElectricityPriceEntity>()

                latestResponse?.prices?.forEach { item ->
                    try {
                        val startInstant = Instant.parse(item.startDate)
                        val zdt = startInstant.atZone(helsinkiZone)
                        val dateStr = zdt.toLocalDate().toString()
                        val hour = zdt.hour
                        val id = "${dateStr}T%02d".format(hour)
                        entitiesToInsert.add(
                            ElectricityPriceEntity(
                                id = id,
                                dateString = dateStr,
                                hour = hour,
                                priceCentPerKwh = item.price,
                                timestampMillis = startInstant.toEpochMilli()
                            )
                        )
                    } catch (e: Exception) {
                        Log.w("ElectricityRepo", "Parse error for item ${item.startDate}: ${e.message}")
                    }
                }

                if (entitiesToInsert.isNotEmpty()) {
                    dao.insertPrices(entitiesToInsert)
                }

                // 2. Check if yesterday is missing in database
                val yesterdayExisting = dao.getPricesForDateDirect(yesterdayDate.toString())
                if (yesterdayExisting.size < 20) {
                    // Query missing hours from porssisahko single-hour endpoint in parallel
                    fetchMissingHoursForDate(yesterdayDate.toString())
                }

                // 3. Check if today or tomorrow need fallback from spot-hinta
                val todayExisting = dao.getPricesForDateDirect(todayDate.toString())
                if (todayExisting.isEmpty()) {
                    fetchSpotHintaFallback()
                }

                // 4. Load from DB to build complete day summaries
                val yesterdayPrices = loadPricesForDay(yesterdayDate.toString(), DayType.YESTERDAY)
                val todayPrices = loadPricesForDay(todayDate.toString(), DayType.TODAY)
                val tomorrowPrices = loadPricesForDay(tomorrowDate.toString(), DayType.TOMORROW)

                // Current price point
                val currentPrice = todayPrices.firstOrNull { it.hour == currentHour }
                    ?: todayPrices.lastOrNull()

                val yesterdaySummary = buildDaySummary(
                    DayType.YESTERDAY,
                    yesterdayDate.toString(),
                    yesterdayDate.format(fiFormatter),
                    yesterdayPrices,
                    currentPrice = null
                )

                val todaySummary = buildDaySummary(
                    DayType.TODAY,
                    todayDate.toString(),
                    todayDate.format(fiFormatter),
                    todayPrices,
                    currentPrice = currentPrice
                )

                val tomorrowSummary = buildDaySummary(
                    DayType.TOMORROW,
                    tomorrowDate.toString(),
                    tomorrowDate.format(fiFormatter),
                    tomorrowPrices,
                    currentPrice = null,
                    unavailableMsg = if (tomorrowPrices.isEmpty()) "Huomisen hinnat vahvistetaan n. klo 13:45–14:00." else null
                )

                _uiState.value = _uiState.value.copy(
                    yesterday = yesterdaySummary,
                    today = todaySummary,
                    tomorrow = tomorrowSummary,
                    currentPricePoint = currentPrice,
                    isLoading = false,
                    lastUpdatedMillis = System.currentTimeMillis()
                )

            } catch (e: Exception) {
                Log.e("ElectricityRepo", "Error refreshing prices", e)
                // Fallback to whatever is in DB
                loadCachedStateOrFallback(yesterdayDate, todayDate, tomorrowDate, fiFormatter, currentHour, e.message)
            }
        }
    }

    private suspend fun fetchMissingHoursForDate(dateStr: String) = coroutineScope {
        try {
            val tasks = (0..23).map { h ->
                async {
                    try {
                        val res = porssisahkoApi.getPriceForHour(dateStr, h)
                        ElectricityPriceEntity(
                            id = "${dateStr}T%02d".format(h),
                            dateString = dateStr,
                            hour = h,
                            priceCentPerKwh = res.price,
                            timestampMillis = System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            val fetched = tasks.awaitAll().filterNotNull()
            if (fetched.isNotEmpty()) {
                dao.insertPrices(fetched)
            }
        } catch (e: Exception) {
            Log.w("ElectricityRepo", "Failed fetching missing hours for $dateStr: ${e.message}")
        }
    }

    private suspend fun fetchSpotHintaFallback() {
        try {
            val spotItems = spotHintaApi.getTodayAndDayForward()
            val entities = spotItems.mapNotNull { item ->
                try {
                    val zdt = ZonedDateTime.parse(item.dateTime)
                    val dateStr = zdt.toLocalDate().toString()
                    val hour = zdt.hour
                    val priceCent = item.priceWithTax * 100.0 // EUR -> cents
                    ElectricityPriceEntity(
                        id = "${dateStr}T%02d".format(hour),
                        dateString = dateStr,
                        hour = hour,
                        priceCentPerKwh = priceCent,
                        timestampMillis = zdt.toInstant().toEpochMilli()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (entities.isNotEmpty()) {
                dao.insertPrices(entities)
            }
        } catch (e: Exception) {
            Log.w("ElectricityRepo", "SpotHinta fallback failed: ${e.message}")
        }
    }

    private suspend fun loadPricesForDay(dateStr: String, dayType: DayType): List<PricePoint> {
        val entities = dao.getPricesForDateDirect(dateStr)
        // Group by hour in case 15min items were stored, taking mean or single entry
        return entities.groupBy { it.hour }.map { (hour, list) ->
            val avgPrice = list.map { it.priceCentPerKwh }.average()
            val first = list.first()
            PricePoint(
                id = first.id,
                dateString = dateStr,
                hour = hour,
                priceCentPerKwh = avgPrice,
                timestampMillis = first.timestampMillis,
                dayType = dayType
            )
        }.sortedBy { it.hour }
    }

    private fun buildDaySummary(
        dayType: DayType,
        dateString: String,
        formattedDate: String,
        prices: List<PricePoint>,
        currentPrice: PricePoint?,
        unavailableMsg: String? = null
    ): DayPriceSummary {
        if (prices.isEmpty()) {
            return DayPriceSummary(
                dayType = dayType,
                dateString = dateString,
                formattedDate = formattedDate,
                prices = emptyList(),
                isAvailable = false,
                unavailableMessage = unavailableMsg ?: "Tietoja ei saatavilla tälle päivälle."
            )
        }

        val min = prices.minByOrNull { it.priceCentPerKwh }
        val max = prices.maxByOrNull { it.priceCentPerKwh }
        val avg = prices.map { it.priceCentPerKwh }.average()

        // Find 3 consecutive cheapest hours (best EV charging window)
        val bestWindow = if (prices.size >= 3) {
            prices.windowed(3, 1).minByOrNull { window ->
                window.sumOf { it.priceCentPerKwh }
            } ?: emptyList()
        } else {
            emptyList()
        }

        return DayPriceSummary(
            dayType = dayType,
            dateString = dateString,
            formattedDate = formattedDate,
            prices = prices,
            currentPrice = currentPrice,
            minPrice = min,
            maxPrice = max,
            avgPrice = avg,
            bestChargingWindow = bestWindow,
            isAvailable = true
        )
    }

    private suspend fun loadCachedStateOrFallback(
        yesterdayDate: LocalDate,
        todayDate: LocalDate,
        tomorrowDate: LocalDate,
        formatter: DateTimeFormatter,
        currentHour: Int,
        errMsg: String?
    ) {
        val yesterdayPrices = loadPricesForDay(yesterdayDate.toString(), DayType.YESTERDAY)
        val todayPrices = loadPricesForDay(todayDate.toString(), DayType.TODAY)
        val tomorrowPrices = loadPricesForDay(tomorrowDate.toString(), DayType.TOMORROW)

        val currentPrice = todayPrices.firstOrNull { it.hour == currentHour }

        _uiState.value = _uiState.value.copy(
            yesterday = buildDaySummary(DayType.YESTERDAY, yesterdayDate.toString(), yesterdayDate.format(formatter), yesterdayPrices, null),
            today = buildDaySummary(DayType.TODAY, todayDate.toString(), todayDate.format(formatter), todayPrices, currentPrice),
            tomorrow = buildDaySummary(DayType.TOMORROW, tomorrowDate.toString(), tomorrowDate.format(formatter), tomorrowPrices, null),
            currentPricePoint = currentPrice,
            isLoading = false,
            errorMessage = if (todayPrices.isEmpty()) (errMsg ?: "Ei verkkoyhteyttä") else null
        )
    }

    fun selectDay(dayType: DayType) {
        _uiState.value = _uiState.value.copy(selectedDay = dayType)
    }

    companion object {
        @Volatile
        private var INSTANCE: ElectricityRepository? = null

        fun getInstance(context: Context): ElectricityRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ElectricityRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
