package bose.ankush.weatherify.base.config

/**
 * Platform-agnostic interface for build/environment configuration values.
 * Replaces direct BuildConfig references in shared/common code to enable KMP compatibility.
 */
interface AppConfig {
    val razorpayKey: String
}
