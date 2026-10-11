import CoreLocation
import UIKit
import UserNotifications

/// Location and notification permission prompts for the home screen, in native form.
@MainActor
final class HomePermissions: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var pending: CheckedContinuation<CLAuthorizationStatus, Never>?

    override init() {
        super.init()
        manager.delegate = self
    }

    var locationStatus: CLAuthorizationStatus {
        manager.authorizationStatus
    }

    var hasLocation: Bool {
        locationStatus == .authorizedWhenInUse || locationStatus == .authorizedAlways
    }

    /// Shows the system prompt when the user hasn't answered yet; returns whether location is allowed.
    func requestLocation() async -> Bool {
        guard locationStatus == .notDetermined else {
            return hasLocation
        }
        let status = await withCheckedContinuation { continuation in
            pending = continuation
            manager.requestWhenInUseAuthorization()
        }
        return status == .authorizedWhenInUse || status == .authorizedAlways
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        guard status != .notDetermined else {
            return
        }
        Task { @MainActor in
            pending?.resume(returning: status)
            pending = nil
        }
    }

    /// Denied for good: iOS will not show its prompt again.
    func notificationsDenied() async -> Bool {
        await UNUserNotificationCenter.current().notificationSettings().authorizationStatus == .denied
    }

    func notificationsGranted() async -> Bool {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        return settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
    }

    /// Asks for notifications. Once denied, iOS never asks again, so a denial is permanent.
    /// `wasAlreadyDenied` means no system prompt was shown at all.
    func requestNotifications() async -> (granted: Bool, permanentlyDeclined: Bool, wasAlreadyDenied: Bool) {
        let center = UNUserNotificationCenter.current()
        let before = await center.notificationSettings().authorizationStatus
        if before == .denied {
            return (false, true, true)
        }
        let granted = (try? await center.requestAuthorization(options: [.alert, .badge, .sound])) ?? false
        return (granted, !granted, false)
    }

    func openAppSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else {
            return
        }
        UIApplication.shared.open(url)
    }

    /// Straight to this app's Notifications page, where "Allow Notifications" lives.
    func openNotificationSettings() {
        guard let url = URL(string: UIApplication.openNotificationSettingsURLString) else {
            return
        }
        UIApplication.shared.open(url)
    }
}
