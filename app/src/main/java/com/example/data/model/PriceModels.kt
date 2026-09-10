package com.example.data.model

enum class DayType(val title: String, val shortTitle: String) {
    YESTERDAY("Eilinen", "Eilen"),
    TODAY("Tänään", "Tänään"),
    TOMORROW("Huominen", "Huomenna")
}

data class PricePoint(
    val id: String,
    val dateString: String, // YYYY-MM-DD
    val hour: Int,          // 0..23
    val priceCentPerKwh: Double, // snt / kWh (sis. ALV)
    val timestampMillis: Long,
    val dayType: DayType
) {
    val displayHour: String
        get() = "%02d:00".format(hour)

    val formattedPrice: String
        get() = "%.2f".format(priceCentPerKwh).replace('.', ',')

    val priceCategory: PriceCategory
        get() = when {
            priceCentPerKwh < 5.0 -> PriceCategory.VERY_CHEAP
            priceCentPerKwh < 10.0 -> PriceCategory.CHEAP
            priceCentPerKwh < 15.0 -> PriceCategory.NORMAL
            priceCentPerKwh < 25.0 -> PriceCategory.EXPENSIVE
            else -> PriceCategory.VERY_EXPENSIVE
        }
}

enum class PriceCategory(val label: String) {
    VERY_CHEAP("Erittäin halpa"),
    CHEAP("Edullinen"),
    NORMAL("Kohtuullinen"),
    EXPENSIVE("Kallis"),
    VERY_EXPENSIVE("Erittäin kallis")
}

data class DayPriceSummary(
    val dayType: DayType,
    val dateString: String,
    val formattedDate: String,
    val prices: List<PricePoint> = emptyList(),
    val currentPrice: PricePoint? = null,
    val minPrice: PricePoint? = null,
    val maxPrice: PricePoint? = null,
    val avgPrice: Double = 0.0,
    val bestChargingWindow: List<PricePoint> = emptyList(),
    val isAvailable: Boolean = false,
    val unavailableMessage: String? = null
) {
    val formattedAvgPrice: String
        get() = "%.2f".format(avgPrice).replace('.', ',')
}
