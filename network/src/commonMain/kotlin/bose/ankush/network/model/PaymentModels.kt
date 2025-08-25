package bose.ankush.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class CreateOrderRequest(
    val amount: Long,
    val currency: String,
    val receipt: String? = null,
    @SerialName("partial_payment") val partialPayment: Boolean? = null,
    @SerialName("first_payment_min_amount") val firstPaymentMinAmount: Long? = null,
    val notes: Map<String, String>? = null
)

@Serializable
data class CreateOrderData(
    val orderId: String,
    val amount: Long,
    val currency: String,
    val receipt: String? = null,
    val status: String? = null,
    val createdAt: Long? = null
)

@Serializable
data class CreateOrderResponse(
    val message: String? = null,
    val data: JsonElement? = null,
    val status: JsonElement? = null
) {
    /**
     * Safely extract CreateOrderData when the backend returns the expected object in `data`.
     * Returns null if fields are missing or types are invalid.
     */
    fun extractData(): CreateOrderData? {
        val obj = data as? JsonObject ?: return null
        val orderId = obj["orderId"]?.jsonPrimitive?.contentOrNull
            ?: obj["order_id"]?.jsonPrimitive?.contentOrNull ?: ""
        val amountStr = obj["amount"]?.jsonPrimitive?.content
        val amount = amountStr?.toLongOrNull() ?: 0L
        val currency = obj["currency"]?.jsonPrimitive?.contentOrNull ?: ""
        val receipt = obj["receipt"]?.jsonPrimitive?.contentOrNull
        val statusStr = obj["status"]?.jsonPrimitive?.contentOrNull
        val createdAt = obj["createdAt"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: obj["created_at"]?.jsonPrimitive?.content?.toLongOrNull()
        return if (orderId.isNotBlank() && amount > 0 && currency.isNotBlank()) {
            CreateOrderData(
                orderId = orderId,
                amount = amount,
                currency = currency,
                receipt = receipt,
                status = statusStr,
                createdAt = createdAt
            )
        } else null
    }
}

@Serializable
data class VerifyPaymentRequest(
    @SerialName("razorpay_order_id") val razorpayOrderId: String,
    @SerialName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerialName("razorpay_signature") val razorpaySignature: String
)

@Serializable
data class VerifyPaymentData(
    val verified: Boolean = false
)

@Serializable
data class VerifyPaymentResponse(
    @SerialName("status") val success: Boolean = false,
    val message: String? = null,
    val data: VerifyPaymentData? = null
)