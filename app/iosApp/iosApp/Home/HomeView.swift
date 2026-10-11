import SwiftUI
import ComposeApp

/// Native home screen, bound to the shared HomeScreenController and SettingsScreenController.
/// One photo backdrop and floating tab bar over three pages you swipe between: weather (under the
/// header), saved places (under the header), and settings. Sheets, pull to refresh and permission
/// prompts are the iOS ones.
struct HomeView: View {

    @StateObject private var model = HomeModel()
    @StateObject private var settings: SettingsModel
    /// Which way the last page change went, so the pages slide in the right direction.
    @State private var movingForward = true
    @State private var showLanguage = false
    @State private var sample = BackdropSample.dark
    /// The tab bar slides away while reading down the page and returns on the way back up.
    @State private var tabBarHidden = false
    @State private var scrollTracker = ScrollDirectionTracker()
    @Environment(\.scenePhase) private var scenePhase

    init(session: AppSession) {
        _settings = StateObject(wrappedValue: SettingsModel(session: session))
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            BackdropView(
                backdrop: model.ui?.backdrop,
                onSample: { sample = $0 },
                onPhotoShown: { model.controller.backdropPhotoShown() }
            )
            RadialGradient(colors: [sample.cornerGlow, sample.cornerGlow.opacity(0)], center: .topTrailing, startRadius: 0, endRadius: 420)
                .ignoresSafeArea()
                .allowsHitTesting(false)
                .animation(.easeInOut(duration: 0.15), value: sample)
            if let ui = model.ui {
                content(ui)
            }
        }
        .environment(\.colorScheme, .dark)
        .task {
            await model.run()
        }
        .task {
            await settings.run()
        }
        .navigationDestination(isPresented: $showLanguage) {
            LanguagePage {
                // Shared copy is read in the new language from now on; refresh what is on screen.
                model.controller.reloadStrings()
                settings.controller.reloadStrings()
            }
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                model.didBecomeActive()
            }
        }
    }

    @ViewBuilder
    private func content(_ ui: HomeScreenUi) -> some View {
        let ink = sample.ink
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Weather and places share the forecast header; settings has its own profile header.
                if model.page != .hub {
                    HomeHeader(
                        header: ui.header,
                        strings: ui.strings,
                        avatarUrl: ui.avatarUrl,
                        ink: ink,
                        onResetLocation: model.controller.resetLocation,
                        onOpenAccount: { show(.hub) }
                    )
                        .transition(.opacity)
                }
                ZStack(alignment: .top) {
                    switch model.page {
                    case .weather:
                        WeatherPage(ui: ui, ink: ink, model: model, showAiSummary: model.aiAvailable, onOpenPlaces: { show(.places) })
                            .transition(pageTransition)
                    case .places:
                        PlacesPage(
                            places: ui.places,
                            strings: ui.strings.places,
                            retry: ui.strings.retry,
                            ink: ink,
                            controller: model.controller,
                            onUpgrade: { show(.hub) }
                        )
                            .transition(pageTransition)
                    case .hub:
                        if let settingsUi = settings.ui {
                            SettingsPage(
                                ui: settingsUi,
                                ink: ink,
                                model: settings,
                                permissions: model.permissions,
                                onOpenLanguage: { showLanguage = true }
                            )
                                .transition(pageTransition)
                        }
                    }
                }
            }
            .padding(.horizontal, HomeTheme.pageInset)
            .padding(.bottom, HomeTheme.tabBarHeight + 40)
            .background(
                GeometryReader { proxy in
                    Color.clear.preference(
                        key: ScrollOffsetKey.self,
                        value: proxy.frame(in: .named(Self.scrollSpace)).minY
                    )
                }
            )
        }
        .coordinateSpace(name: Self.scrollSpace)
        .onVerticalScroll { offset in
            guard let hide = scrollTracker.update(offset), hide != tabBarHidden else {
                return
            }
            // Rises with a small overshoot so it lands on top of the page; leaves without one.
            let spring: Animation = hide
                ? .spring(response: 0.38, dampingFraction: 0.92)
                : .spring(response: 0.45, dampingFraction: 0.68)
            withAnimation(spring) {
                tabBarHidden = hide
            }
        }
        .scrollIndicators(.hidden)
        .refreshable {
            if model.page == .hub {
                await settings.refresh()
            } else {
                await model.refresh()
            }
        }
        .simultaneousGesture(pageSwipe)
        .overlay(alignment: .bottom) {
            bottomOverlay(ui, ink: ink)
        }
        .overlay(alignment: .top) {
            StatusBarFade()
        }
        .overlay(alignment: .top) {
            OfflineBanner(title: ui.strings.offlineTitle, message: ui.chrome.offlineMessage)
        }
        .overlay(alignment: .top) {
            NoticeBanner(notice: $settings.notice)
        }
        .sheet(isPresented: searchBinding(ui)) {
            PlaceSearchSheet(search: ui.search, strings: ui.strings.places, controller: model.controller)
        }
        .sheet(isPresented: composerBinding(ui)) {
            EventSheet(composer: ui.composer, strings: ui.strings.eventSheet, controller: model.controller)
        }
        .sheet(isPresented: $model.showAiSummary, onDismiss: { model.controller.closeAiSummary() }) {
            AiSummarySheet(
                state: model.aiSummary,
                strings: ui.strings.aiSummary,
                onRetry: { model.controller.retryAiSummary() }
            )
        }
        .sheet(item: $model.openAlert) { alert in
            AlertSheet(alert: alert, strings: ui.strings)
        }
        .alert(ui.strings.notificationMessage, isPresented: notificationBinding(ui)) {
            // Declined for good: iOS won't ask again, so the button says where it goes.
            let declined = ui.chrome.notificationDeclinedForever
            Button(declined ? ui.strings.openSettings : ui.strings.enableNotifications) {
                model.controller.enableNotifications()
            }
            Button(ui.strings.cancel, role: .cancel) {
                model.controller.dismissNotifications()
            }
        } message: {
            if ui.chrome.notificationDeclinedForever {
                Text(ui.strings.notificationDeclined)
            }
        }
        .alert(ui.strings.requestLocation, isPresented: $model.showLocationDenied) {
            Button(ui.strings.openSettings) {
                model.permissions.openAppSettings()
            }
            Button(ui.strings.cancel, role: .cancel) {
            }
        } message: {
            Text(ui.strings.locationDenied)
        }
    }

    private func bottomOverlay(_ ui: HomeScreenUi, ink: Color) -> some View {
        VStack(spacing: 12) {
            HStack {
                RefreshChip(visible: ui.chrome.isBackgroundRefreshing && !ui.chrome.isRefreshing, ink: ink)
                Spacer()
                if ui.places.showAddButton {
                    Button(action: model.controller.placeActions.openSearch) {
                        Image(systemName: "plus")
                            .font(.system(size: 22, weight: .semibold))
                            .foregroundStyle(HomeTheme.onYellow)
                            .frame(width: 56, height: 56)
                            .background(HomeTheme.yellow, in: Circle())
                            .shadow(color: .black.opacity(0.3), radius: 6, y: 3)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(ui.strings.places.add)
                    .transition(.scale(scale: 0.4).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.2), value: ui.places.showAddButton)
            HomeTabBar(selected: selectedTab, strings: ui.strings.tabs, inactiveTint: ink) { tab in
                switch tab {
                case .home: show(.weather)
                case .map: show(.places)
                case .hub: show(.hub)
                }
            }
            .frame(maxWidth: 170)
            .offset(y: tabBarHidden ? HomeTheme.tabBarHeight + 60 : 0)
            .opacity(tabBarHidden ? 0 : 1)
            .scaleEffect(tabBarHidden ? 0.9 : 1, anchor: .bottom)
            .allowsHitTesting(!tabBarHidden)
        }
        .padding(.horizontal, HomeTheme.pageInset)
        .padding(.bottom, 4)
    }

    /// Horizontal swipes move one page along; vertical drags stay with the scroll view.
    private var pageSwipe: some Gesture {
        DragGesture(minimumDistance: 24)
            .onEnded { value in
                let dx = value.translation.width
                guard abs(dx) > abs(value.translation.height) * 1.5, abs(dx) > 60 else {
                    return
                }
                let next = model.page.rawValue + (dx < 0 ? 1 : -1)
                if let page = HomePage(rawValue: next) {
                    show(page)
                }
            }
    }

    private var selectedTab: HomeTab {
        switch model.page {
        case .weather: .home
        case .places: .map
        case .hub: .hub
        }
    }

    /// New page slides in from the side it lies on; the old one leaves the other way.
    private var pageTransition: AnyTransition {
        .asymmetric(
            insertion: .move(edge: movingForward ? .trailing : .leading),
            removal: .move(edge: movingForward ? .leading : .trailing)
        )
            .combined(with: .opacity)
    }

    private func show(_ page: HomePage) {
        guard page != model.page else {
            withAnimation(.spring(response: 0.42, dampingFraction: 0.68)) {
                tabBarHidden = false
            }
            return
        }
        movingForward = page.rawValue > model.page.rawValue
        withAnimation(.spring(response: 0.38, dampingFraction: 0.88)) {
            model.page = page
            tabBarHidden = false
        }
    }

    private static let scrollSpace = "homeScroll"

    private func searchBinding(_ ui: HomeScreenUi) -> Binding<Bool> {
        Binding(get: { ui.search.isOpen }, set: {
            if !$0 {
                model.controller.placeActions.closeSearch()
            }
        })
    }

    private func composerBinding(_ ui: HomeScreenUi) -> Binding<Bool> {
        Binding(get: { ui.composer.isVisible }, set: {
            if !$0 {
                model.controller.nearbyActions.dismissComposer()
            }
        })
    }

    private func notificationBinding(_ ui: HomeScreenUi) -> Binding<Bool> {
        Binding(get: { ui.chrome.showNotificationPrompt }, set: { _ in })
    }
}

/// Top of the scroll content in the scroll view's space: 0 at rest, negative as you read down.
private struct ScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private extension View {
    /// Reports where the scroll content's top is (0 at rest, negative while reading down). Uses
    /// the native scroll geometry on iOS 18+, and the geometry preference before that.
    @ViewBuilder
    func onVerticalScroll(_ action: @escaping (CGFloat) -> Void) -> some View {
        if #available(iOS 18.0, *) {
            onScrollGeometryChange(for: CGFloat.self) { geometry in
                -(geometry.contentOffset.y + geometry.contentInsets.top)
            } action: { _, offset in
                action(offset)
            }
        } else {
            onPreferenceChange(ScrollOffsetKey.self, perform: action)
        }
    }
}

/// Turns scroll offsets into hide/show decisions for the tab bar. Small jitters are ignored:
/// it needs about 12pt of travel in one direction, and near the top the bar always shows.
/// A plain class, so tracking every frame never redraws the screen.
private final class ScrollDirectionTracker {
    private var lastOffset: CGFloat?
    private var travel: CGFloat = 0

    /// True to hide, false to show, nil to leave the bar as it is.
    func update(_ offset: CGFloat) -> Bool? {
        defer {
            lastOffset = offset
        }
        guard let last = lastOffset else {
            return nil
        }
        if offset > -24 {
            travel = 0
            return false
        }
        let delta = offset - last
        if delta == 0 {
            return nil
        }
        if (delta < 0) != (travel < 0) {
            travel = 0
        }
        travel += delta
        if travel < -12 {
            return true
        }
        if travel > 12 {
            return false
        }
        return nil
    }
}

/// Blurs and darkens what scrolls under the status bar so the clock stays readable.
private struct StatusBarFade: View {
    var body: some View {
        GeometryReader { proxy in
            let height = proxy.safeAreaInsets.top + 16
            Rectangle()
                .fill(.ultraThinMaterial)
                .overlay(Color.black.opacity(0.2))
                .mask(LinearGradient(colors: [.black, .black, .clear], startPoint: .top, endPoint: .bottom))
                .frame(height: height)
                .ignoresSafeArea(edges: .top)
        }
        .allowsHitTesting(false)
    }
}

/// "Updating" capsule while a cached forecast refreshes in the background.
private struct RefreshChip: View {
    let visible: Bool
    let ink: Color

    var body: some View {
        HStack(spacing: 8) {
            ProgressView().controlSize(.small).tint(ink)
            Text("Updating").font(.system(size: 13, weight: .medium)).foregroundStyle(ink)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(HomeTheme.cardFill, in: Capsule())
        .opacity(visible ? 1 : 0)
        .animation(.easeInOut(duration: 0.2), value: visible)
        .accessibilityHidden(!visible)
    }
}

/// Offline notice that slides in under the status bar and hides itself after a few seconds.
private struct OfflineBanner: View {
    let title: String
    let message: String?
    @State private var shown: String?

    var body: some View {
        Group {
            if let shown {
                HStack(spacing: 10) {
                    Image(systemName: "wifi.slash").font(.system(size: 15, weight: .semibold))
                    VStack(alignment: .leading, spacing: 1) {
                        Text(title).font(.system(size: 14, weight: .semibold))
                        Text(shown).font(.system(size: 13)).lineLimit(2)
                    }
                }
                .foregroundStyle(HomeTheme.onYellow)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(HomeTheme.yellow, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .shadow(color: .black.opacity(0.25), radius: 8, y: 4)
                .padding(.horizontal, HomeTheme.pageInset)
                .transition(.move(edge: .top).combined(with: .opacity))
                .onTapGesture {
                    self.shown = nil
                }
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: shown)
        .task(id: message) {
            guard let message, !message.isEmpty else {
                shown = nil
                return
            }
            shown = message
            try? await Task.sleep(nanoseconds: 4_000_000_000)
            shown = nil
        }
    }
}
