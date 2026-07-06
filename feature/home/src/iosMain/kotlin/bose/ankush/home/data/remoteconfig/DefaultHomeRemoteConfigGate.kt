package bose.ankush.home.data.remoteconfig

import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate

/**
 * No remote-config backend is wired up for iOS yet (no KMP Firebase Remote Config dependency in
 * this repo). Falls back to the feature being disabled by default until that follow-up lands.
 */
internal class DefaultHomeRemoteConfigGate : HomeRemoteConfigGate {
    override fun initialize() = Unit

    override fun isNotificationBannerEnabled(): Boolean = false
}
