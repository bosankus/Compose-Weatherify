@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.iosapp.payment

import bose.ankush.payment.presentation.CheckoutParams
import cocoapods.razorpay_pod.RazorpayCheckout
import cocoapods.razorpay_pod.RazorpayPaymentCompletionProtocolWithDataProtocol
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNumber
import platform.Foundation.NSTimer
import platform.UIKit.UIViewController
import platform.darwin.NSObject

/**
 * Bridges Razorpay's iOS Standard Checkout SDK to [CheckoutParams] and plain
 * success/error callbacks — the iOS counterpart of MainActivity's Android
 * `launchCheckout`/`PaymentResultWithDataListener` implementation. Lives at the app
 * layer (not inside :feature:payment) to mirror where Android calls the SDK from.
 */
class RazorpayCheckoutBridge :
    NSObject(),
    RazorpayPaymentCompletionProtocolWithDataProtocol {
    private var onSuccess: ((orderId: String, paymentId: String, signature: String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var pendingOrderId: String? = null
    private var checkout: RazorpayCheckout? = null
    private var resolved = false
    private var checkoutPresented = false
    private var dismissWatchTimer: NSTimer? = null

    fun launch(
        controller: UIViewController,
        params: CheckoutParams,
        onSuccess: (orderId: String, paymentId: String, signature: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        this.onSuccess = onSuccess
        this.onError = onError
        this.pendingOrderId = params.orderId
        this.resolved = false
        this.checkoutPresented = false
        try {
            val prefill: Map<Any?, Any> =
                buildMap {
                    params.email?.let { put("email", it) }
                    params.contact?.let { put("contact", it) }
                }
            val options: Map<Any?, Any> =
                mapOf(
                    "name" to params.name,
                    "description" to params.description,
                    "order_id" to params.orderId,
                    "currency" to params.currency,
                    "amount" to NSNumber(long = params.amount),
                    "prefill" to prefill,
                )
            val instance = RazorpayCheckout.initWithKey(params.keyId, andDelegateWithData = this)
            checkout = instance
            instance.open(options, displayController = controller)
            watchForSilentDismiss(controller)
        } catch (e: Throwable) {
            fail(e.message ?: "Unable to open payment checkout")
        }
    }

    // Some failures (e.g. an invalid/placeholder key rejected server-side) make the
    // checkout sheet present briefly and dismiss itself without ever invoking
    // onPaymentError, unlike Android's PaymentResultWithDataListener. Poll for that
    // silent dismissal so the UI doesn't stay stuck showing "processing" forever.
    private fun watchForSilentDismiss(controller: UIViewController) {
        dismissWatchTimer =
            NSTimer.scheduledTimerWithTimeInterval(
                interval = 0.5,
                repeats = true,
            ) {
                if (resolved) {
                    stopWatchingForDismiss()
                    return@scheduledTimerWithTimeInterval
                }
                val isPresented = controller.presentedViewController != null
                if (isPresented) {
                    checkoutPresented = true
                } else if (checkoutPresented) {
                    fail("Payment window closed unexpectedly")
                }
            }
    }

    private fun stopWatchingForDismiss() {
        dismissWatchTimer?.invalidate()
        dismissWatchTimer = null
    }

    private fun fail(message: String) {
        if (resolved) return
        resolved = true
        stopWatchingForDismiss()
        onError?.invoke(message)
        checkout = null
    }

    override fun onPaymentSuccess(
        payment_id: String,
        andData: Map<Any?, *>?,
    ) {
        if (resolved) return
        resolved = true
        stopWatchingForDismiss()
        val orderId = (andData?.get("razorpay_order_id") as? String) ?: pendingOrderId.orEmpty()
        val signature = (andData?.get("razorpay_signature") as? String).orEmpty()
        if (orderId.isNotBlank() && payment_id.isNotBlank() && signature.isNotBlank()) {
            onSuccess?.invoke(orderId, payment_id, signature)
        } else {
            onError?.invoke("Payment succeeded but missing data")
        }
        checkout = null
    }

    override fun onPaymentError(
        code: Int,
        description: String,
        andData: Map<Any?, *>?,
    ) {
        fail(description.ifBlank { "Payment failed with code $code" })
    }
}
