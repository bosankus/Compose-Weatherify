package bose.ankush.payment.data

import bose.ankush.network.model.CreateOrderResponse
import bose.ankush.payment.domain.model.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun CreateOrderResponse.toOrder(): Order? {
    val obj = data as? JsonObject ?: return null
    val orderId =
        obj["orderId"]?.jsonPrimitive?.contentOrNull
            ?: obj["order_id"]?.jsonPrimitive?.contentOrNull ?: ""
    val amount = obj["amount"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
    val currency = obj["currency"]?.jsonPrimitive?.contentOrNull ?: ""
    if (orderId.isBlank() || amount <= 0L || currency.isBlank()) return null
    return Order(orderId = orderId, amount = amount, currency = currency)
}
