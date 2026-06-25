package bose.ankush.network.model

import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class ServiceListResponse(
    val success: Boolean = true,
    val message: String = "",
    val data: ServiceListData,
)

@Serializable
data class ServiceListData(
    val services: List<ServiceDto> = emptyList(),
    val totalCount: Long = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
)

@Serializable
data class ServiceDto(
    val id: String,
    val serviceCode: String,
    val displayName: String,
    val description: String,
    val pricingTiers: List<PricingTierDto>,
    val features: List<FeatureDto>,
    val status: String,
    val limits: Map<String, ServiceLimitDto> = emptyMap(),
    val availabilityStart: String? = null,
    val availabilityEnd: String? = null,
    val totalPurchases: Long = 0,
    val lowestPrice: Int = 0,
    val currency: String = "INR",
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class PricingTierDto(
    val id: String,
    val amount: Int,
    val currency: String,
    val duration: Int,
    val durationType: String,
    val isDefault: Boolean = false,
    val isFeatured: Boolean = false,
    val displayOrder: Int = 0,
)

@Serializable
data class FeatureDto(
    val id: String,
    val description: String,
    val isHighlighted: Boolean = false,
    val displayOrder: Int = 0,
)

@Serializable
data class ServiceLimitDto(
    val value: Long,
    val type: String,
    val unit: String,
)

data class Service(
    val id: String,
    val serviceCode: String,
    val displayName: String,
    val description: String,
    val pricingTiers: List<PricingTier>,
    val features: List<Feature>,
    val status: ServiceStatus,
    val limits: Map<String, ServiceLimit>,
    val availabilityStart: String? = null,
    val availabilityEnd: String? = null,
    val totalPurchases: Long = 0,
    val lowestPrice: Int = 0,
    val currency: String = "INR",
    val createdAt: String,
    val updatedAt: String,
) {
    val isAvailable: Boolean
        get() = status == ServiceStatus.ACTIVE && isWithinAvailabilityWindow()

    private fun isWithinAvailabilityWindow(): Boolean {
        if (availabilityStart == null && availabilityEnd == null) return true

        val now = Clock.System.now().toEpochMilliseconds()
        val start = availabilityStart?.let { parseIsoDate(it) }
        val end = availabilityEnd?.let { parseIsoDate(it) }

        if (start != null && now < start) return false
        if (end != null && now > end) return false
        return true
    }

    fun getRecommendedTier(): PricingTier? =
        pricingTiers.firstOrNull { it.isFeatured }
            ?: pricingTiers.firstOrNull { it.isDefault }
            ?: pricingTiers.firstOrNull()
}

data class PricingTier(
    val id: String,
    val amount: Int,
    val currency: String,
    val duration: Int,
    val durationType: DurationType,
    val isDefault: Boolean = false,
    val isFeatured: Boolean = false,
    val displayOrder: Int = 0,
) {
    fun getDisplayPrice(): String = "₹$amount"

    fun getAmountInPaise(): Int = amount * 100

    fun getDisplayDuration(): String =
        when (durationType) {
            DurationType.DAYS -> if (duration == 1) "1 day" else "$duration days"
            DurationType.MONTHS -> if (duration == 1) "1 month" else "$duration months"
            DurationType.YEARS -> if (duration == 1) "1 year" else "$duration years"
        }
}

data class Feature(
    val id: String,
    val description: String,
    val isHighlighted: Boolean = false,
    val displayOrder: Int = 0,
)

data class ServiceLimit(
    val value: Long,
    val type: LimitType,
    val unit: String,
)

enum class ServiceStatus {
    ACTIVE,
    INACTIVE,
    ARCHIVED,
}

enum class DurationType {
    DAYS,
    MONTHS,
    YEARS,
}

enum class LimitType {
    HARD,
    SOFT,
}

fun ServiceDto.toDomain(): Service =
    Service(
        id = id,
        serviceCode = serviceCode,
        displayName = displayName,
        description = description,
        pricingTiers = pricingTiers.map { it.toDomain() },
        features = features.map { it.toDomain() }.sortedBy { it.displayOrder },
        status =
            try {
                ServiceStatus.valueOf(status.uppercase())
            } catch (_: Exception) {
                ServiceStatus.ACTIVE
            },
        limits = limits.mapValues { it.value.toDomain() },
        availabilityStart = availabilityStart,
        availabilityEnd = availabilityEnd,
        totalPurchases = totalPurchases,
        lowestPrice = lowestPrice,
        currency = currency,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun PricingTierDto.toDomain(): PricingTier =
    PricingTier(
        id = id,
        amount = amount,
        currency = currency,
        duration = duration,
        durationType =
            try {
                DurationType.valueOf(durationType.uppercase())
            } catch (_: Exception) {
                DurationType.MONTHS
            },
        isDefault = isDefault,
        isFeatured = isFeatured,
        displayOrder = displayOrder,
    )

fun FeatureDto.toDomain(): Feature =
    Feature(
        id = id,
        description = description,
        isHighlighted = isHighlighted,
        displayOrder = displayOrder,
    )

fun ServiceLimitDto.toDomain(): ServiceLimit =
    ServiceLimit(
        value = value,
        type =
            try {
                LimitType.valueOf(type.uppercase())
            } catch (_: Exception) {
                LimitType.HARD
            },
        unit = unit,
    )

fun parseIsoDate(dateString: String): Long? =
    try {
        dateString.replace("Z", "+00:00").let { _ ->
            Clock.System.now().toEpochMilliseconds()
        }
    } catch (_: Exception) {
        null
    }
