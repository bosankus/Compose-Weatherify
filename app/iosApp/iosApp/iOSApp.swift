import SwiftUI
import FirebaseCore
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        FirebaseApp.configure()
        KoinKt.startWeatherifyKoin(nativeAi: FoundationModelsBridge(), pushTokens: FirebasePushTokens())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
