import SwiftUI
import ComposeApp

/// The three pages of the home shell, in swipe order.
enum HomePage: Int, Hashable, CaseIterable {
    case weather
    case places
    case hub
}

/// SwiftUI side of the shared HomeScreenController: publishes each frame of HomeScreenUi and
/// carries out the controller's one-off events (permission prompts, settings, page jumps).
@MainActor
final class HomeModel: ObservableObject {
    let controller = HomeScreenController()
    let permissions = HomePermissions()

    @Published private(set) var ui: HomeScreenUi?
    @Published var page: HomePage = .weather {
        didSet {
            if page != oldValue {
                controller.placeActions.pageChanged(placesPage: page == .places)
            }
        }
    }
    @Published var openAlert: AlertUi?
    @Published var showAiSummary = false
    /// Whether the weather summary card shows at all: the model is usable here and there is a forecast.
    @Published private(set) var aiAvailable = false
    /// What the summary sheet draws, straight from the shared view model.
    @Published private(set) var aiSummary = AiSummaryUiState(phase: .loading, text: "", isStreaming: false, tips: [], place: nil)
    @Published var showLocationDenied = false
    private var lastLocationAllowed: Bool?

    deinit {
        let controller = controller
        DispatchQueue.main.async {
            controller.close()
        }
    }

    /// Runs for as long as the home screen is on screen.
    func run() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { @MainActor in
                for await frame in self.controller.state {
                    self.ui = frame
                }
            }
            group.addTask { @MainActor in
                for await event in self.controller.events {
                    await self.handle(event)
                }
            }
            group.addTask { @MainActor in
                for await value in self.controller.aiSummaryAvailable {
                    self.aiAvailable = value.boolValue
                }
            }
            group.addTask { @MainActor in
                for await value in self.controller.aiSummaryState {
                    self.aiSummary = value
                }
            }
            group.addTask { @MainActor in
                await self.syncPermissions(askLocation: true)
            }
        }
    }

    /// Back in the foreground: permissions may have changed in Settings, and the photo URL is short-lived.
    func didBecomeActive() {
        controller.onAppear()
        Task {
            await syncPermissions(askLocation: false)
        }
    }

    func refresh() async {
        try? await controller.refresh()
    }

    private func syncPermissions(askLocation: Bool) async {
        controller.notificationPermissionChanged(granted: await permissions.notificationsGranted())
        // The shared ViewModel already fetches on start when location is allowed; only a fresh
        // grant (first prompt, or flipped on in Settings) needs another fetch.
        let wasAllowed = lastLocationAllowed ?? permissions.hasLocation
        let allowed = askLocation ? await permissions.requestLocation() : permissions.hasLocation
        lastLocationAllowed = allowed
        if allowed && !wasAllowed {
            controller.locationPermissionGranted()
        } else if askLocation && permissions.locationStatus == .denied {
            // Let the screen settle (and the sign-in handoff finish) before asking for anything.
            try? await Task.sleep(nanoseconds: 1_800_000_000)
            showLocationDenied = true
        }
    }

    private func handle(_ event: HomeScreenEvent) async {
        switch onEnum(of: event) {
        case .requestLocationPermission:
            if await permissions.requestLocation() {
                controller.locationPermissionGranted()
            } else {
                showLocationDenied = true
            }
        case .requestNotificationPermission:
            let result = await permissions.requestNotifications()
            controller.notificationPermissionResult(
                granted: result.granted,
                permanentlyDeclined: result.permanentlyDeclined
            )
            // Denied before this tap: iOS showed nothing, so take the user to the switch itself.
            if result.wasAlreadyDenied {
                permissions.openNotificationSettings()
            }
        case .openNotificationSettings:
            permissions.openNotificationSettings()
        case .openLocationSettings:
            permissions.openAppSettings()
        case .showWeather:
            withAnimation(.spring(response: 0.38, dampingFraction: 0.88)) {
                page = .weather
            }
        case .showPlaces:
            withAnimation(.spring(response: 0.38, dampingFraction: 0.88)) {
                page = .places
            }
        }
    }
}
