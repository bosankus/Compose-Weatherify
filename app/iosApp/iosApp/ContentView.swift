import SwiftUI
import ComposeApp

/// Sign-in state from the shared AuthViewModel, observed for the root view.
@MainActor
final class SessionModel: ObservableObject {
    let session = AppSession()
    @Published private(set) var isLoggedIn = false
    @Published private(set) var isReady = false

    func observe() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { @MainActor in
                for await value in self.session.isLoggedIn {
                    self.isLoggedIn = value.boolValue
                }
            }
            group.addTask { @MainActor in
                for await value in self.session.isAuthInitialized {
                    self.isReady = value.boolValue
                }
            }
        }
    }
}

/// Native app shell, all SwiftUI: sign-in, then the home shell (weather, places, settings; the
/// language page pushed on top).
///
/// Launch shows the splash (leaf and name) for at least a moment, then dissolves into Home or
/// sign-in. Signing in hands off calmly instead of cutting: the form steps away, the forest holds
/// a short welcome, then it blurs and drifts past while Home comes into focus.
struct ContentView: View {
    @StateObject private var model = SessionModel()
    @State private var stage = Stage.loading
    @State private var splashShownAt = Date()

    private enum Stage {
        case loading, signIn, welcoming, home
    }

    var body: some View {
        ZStack {
            Color(hex: 0x101418).ignoresSafeArea().zIndex(-2)
            switch stage {
            case .loading:
                SplashView()
                    .transition(.splashDeparture)
                    // Under whatever comes next, which fades in on top: no dark blink between.
                    .zIndex(-1)
            case .signIn, .welcoming:
                LoginView(session: model.session, handingOff: stage == .welcoming)
                    .transition(.loginDeparture)
                    .zIndex(0)
            case .home:
                SignedInRoot(session: model.session)
                    .transition(.homeArrival)
                    // On top, fading in over the forest, so the two blend with no dark gap.
                    .zIndex(1)
            }
        }
        .task {
            await model.observe()
        }
        .onChange(of: model.isReady) { _ in
            route()
        }
        .onChange(of: model.isLoggedIn) { _ in
            route()
        }
    }

    private func route() {
        guard model.isReady else {
            return
        }
        // Keep the splash up long enough to be seen, never just a blink.
        let remaining = Self.minimumSplash - Date().timeIntervalSince(splashShownAt)
        if stage == .loading, remaining > 0 {
            Task {
                try? await Task.sleep(nanoseconds: UInt64(remaining * 1_000_000_000))
                route()
            }
            return
        }
        // Read the live value: the two flows reach Swift on separate tasks, so "ready" can land
        // before "signed in" and would otherwise flash sign-in (and a welcome) on every launch.
        let signedIn = model.session.isLoggedIn.value.boolValue && !Self.previewLogin
        switch (stage, signedIn) {
        case (.signIn, true):
            // Just signed in: let the welcome play, then move on.
            withAnimation(.easeInOut(duration: 0.3)) {
                stage = .welcoming
            }
            Task {
                try? await Task.sleep(nanoseconds: 750_000_000)
                withAnimation(.easeInOut(duration: 0.6)) {
                    stage = .home
                }
            }
        case (.loading, true) where Self.previewHandoff:
            // Debug: start on sign-in, then play the handoff as if the user just signed in.
            withAnimation(.easeInOut(duration: 0.45)) {
                stage = .signIn
            }
            Task {
                try? await Task.sleep(nanoseconds: 2_500_000_000)
                route()
            }
        case (.loading, true):
            withAnimation(.easeInOut(duration: 0.55)) {
                stage = .home
            }
        case (.home, false), (.loading, false), (.welcoming, false):
            withAnimation(.easeInOut(duration: 0.45)) {
                stage = .signIn
            }
        default:
            break
        }
    }

    /// The splash is already on screen the instant the app opens, so this only stops it from
    /// blinking away if sign-in state is known almost at once (it usually is, ~0.2s after launch).
    private static let minimumSplash: TimeInterval = 0.3

    /// Debug builds launched with `-previewHandoff` start on sign-in while signed in, then play the
    /// sign-in-to-Home handoff.
    private static var previewHandoff: Bool {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("-previewHandoff")
        #else
        false
        #endif
    }

    /// Debug builds launched with `-previewLogin` show the sign-in screen while signed in, so it
    /// can be designed without logging out.
    private static var previewLogin: Bool {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("-previewLogin")
        #else
        false
        #endif
    }
}

private struct SignedInRoot: View {
    let session: AppSession
    var body: some View {
        NavigationStack {
            HomeView(session: session)
                .toolbar(.hidden, for: .navigationBar)
        }
    }
}

/// Blur, scale and fade together, for the sign-in handoff.
private struct Dreamy: ViewModifier {
    let blur: CGFloat
    let scale: CGFloat
    let opacity: Double

    func body(content: Content) -> some View {
        content.blur(radius: blur).scaleEffect(scale).opacity(opacity)
    }
}

private extension AnyTransition {
    /// Home comes into focus: from a soft blur, slightly small, to sharp and settled.
    static var homeArrival: AnyTransition {
        .asymmetric(
            insertion: .modifier(active: Dreamy(blur: 14, scale: 0.96, opacity: 0), identity: Dreamy(blur: 0, scale: 1, opacity: 1)),
            removal: .opacity
        )
    }

    /// The splash drifts back: it grows slightly and softens, staying opaque under the next
    /// screen as that fades in on top.
    static var splashDeparture: AnyTransition {
        .asymmetric(
            insertion: .opacity,
            removal: .modifier(active: Dreamy(blur: 8, scale: 1.05, opacity: 1), identity: Dreamy(blur: 0, scale: 1, opacity: 1))
        )
    }

    /// The forest drifts past: it grows a little and blurs, staying opaque under Home as Home
    /// fades in on top, so nothing dark shows between them.
    static var loginDeparture: AnyTransition {
        .asymmetric(
            insertion: .opacity,
            removal: .modifier(active: Dreamy(blur: 18, scale: 1.08, opacity: 1), identity: Dreamy(blur: 0, scale: 1, opacity: 1))
        )
    }
}
