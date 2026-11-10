import UIKit
import UserNotifications
import FirebaseMessaging
import ComposeApp

/// Push plumbing, the iOS side of Android's WeatherifyMessagingService: APNs registration, the FCM
/// token, and incoming pushes. Swizzling is off (FirebaseAppDelegateProxyEnabled), so every
/// hand-off to Firebase happens here. What a push means is decided in the shared AccountPushes.
final class AppDelegate: NSObject, UIApplicationDelegate {
    private static let weatherAlertsTopic = "weather_alerts"

    // Created after iOSApp.init has started Koin.
    private lazy var pushes = AccountPushes()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        // Silent pushes need only this. Visible ones also need the user's permission, which Home
        // and Settings ask for.
        application.registerForRemoteNotifications()
        pushes.start()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        // FCM can only issue its token once it has this one.
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("APNs registration failed: \(error.localizedDescription)")
    }

    /// Silent pushes (content-available), in the foreground or woken in the background.
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        Messaging.messaging().appDidReceiveMessage(userInfo)
        let type = userInfo["type"] as? String
        Task { @MainActor in
            let fetched = (try? await pushes.handleSilentPush(type: type))?.boolValue ?? false
            completionHandler(fetched ? .newData : .noData)
        }
    }
}

extension AppDelegate: MessagingDelegate {
    /// On every launch once the token is ready, and whenever FCM rotates it.
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else {
            return
        }
        // Topic subscriptions belong to the token; repeating one that exists is a no-op.
        messaging.subscribe(toTopic: Self.weatherAlertsTopic)
        pushes.tokenRefreshed(token: fcmToken)
    }
}

extension AppDelegate: UNUserNotificationCenterDelegate {
    /// Shows pushes while the app is open too, as Android does.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        Messaging.messaging().appDidReceiveMessage(notification.request.content.userInfo)
        completionHandler([.banner, .list, .sound])
    }

    /// A tap just opens the app; recording it lets FCM report the open.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        Messaging.messaging().appDidReceiveMessage(response.notification.request.content.userInfo)
        completionHandler()
    }
}

/// The FCM token for the shared push registration (NativePushTokens in Kotlin).
final class FirebasePushTokens: NativePushTokens {
    func fetchToken(completion: @escaping (String?) -> Void) {
        // Fails (nil) until APNs has answered; the delegate above registers the token when it lands.
        Messaging.messaging().token { token, _ in
            completion(token)
        }
    }

    func deleteToken() {
        Messaging.messaging().deleteToken { error in
            if let error {
                print("Couldn't rotate the FCM token after account deletion: \(error.localizedDescription)")
            }
        }
    }
}
