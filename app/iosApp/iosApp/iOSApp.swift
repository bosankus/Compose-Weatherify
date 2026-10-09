import SwiftUI
import FirebaseCore
import ComposeApp

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        KoinKt.startWeatherifyKoin(nativeAi: FoundationModelsBridge())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
