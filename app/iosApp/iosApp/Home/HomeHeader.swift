import SwiftUI
import ComposeApp

/// Observed time, the saved-place chip, the big temperature, place and condition, and the
/// account photo. Stays above both pages, like the Android header block.
struct HomeHeader: View {
    let header: HeaderUi
    let strings: HomeStrings
    let avatarUrl: String?
    let ink: Color
    let onResetLocation: () -> Void
    let onOpenAccount: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Spacer().frame(height: 28)
            if let name = header.locationOverrideName {
                Button(action: onResetLocation) {
                    HStack(spacing: 6) {
                        Image(systemName: "location.fill").font(.system(size: 14))
                        Text("\(name)  ·  \(strings.resetToGps)")
                            .font(.system(size: 14, weight: .medium))
                    }
                    .foregroundStyle(ink)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(HomeTheme.cardFill, in: Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(header.locationOverrideDescription ?? name)
                .padding(.bottom, 16)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
            if let observed = header.observedLabel {
                Text(observed)
                    .font(.system(size: 13))
                    .foregroundStyle(ink.opacity(0.72))
                    .headerShadow()
                    .padding(.bottom, 4)
            }
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 10) {
                    Text(header.temperature)
                        .font(.system(size: 96, weight: .light))
                        .tracking(-1.5)
                        .monospacedDigit()
                        .numericTransition()
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    Text(header.place)
                        .font(.system(size: 30, weight: .medium))
                        .lineLimit(2)
                    Text(header.conditionLine)
                        .font(.system(size: 18))
                }
                .foregroundStyle(ink)
                .headerShadow()
                .frame(maxWidth: .infinity, alignment: .leading)
                .animation(.easeInOut(duration: 0.5), value: header.temperature)
                AccountMark(photoUrl: avatarUrl, label: strings.account, action: onOpenAccount)
                    .padding(.top, 8)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: header.locationOverrideName)
    }
}

/// Round account photo (or the default avatar on a white disc). Opens the Hub.
struct AccountMark: View {
    let photoUrl: String?
    let label: String
    let action: () -> Void
    @State private var image: UIImage?

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle().fill(Color.white)
                if let image {
                    Image(uiImage: image).resizable().scaledToFill()
                } else {
                    // Same line-art dog as Android, on its white disc so it reads on any photo.
                    Image("DefaultAvatar")
                        .resizable()
                        .scaledToFit()
                        .foregroundStyle(HomeTheme.inkOnLight)
                }
            }
            .frame(width: 40, height: 40)
            .clipShape(Circle())
            .shadow(color: .black.opacity(0.25), radius: 4, y: 1)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        // Signed URLs are short-lived and per request, so never read them from disk.
        .task(id: photoUrl) {
            guard let photoUrl, let url = URL(string: photoUrl) else {
                image = nil
                return
            }
            image = await RemoteImage.load(url, retries: 1, cachePolicy: .reloadIgnoringLocalCacheData)
        }
    }
}

enum HomeTab: CaseIterable {
    case home, map, hub

    var symbol: String {
        switch self {
        case .home: "house.fill"
        case .map: "map.fill"
        case .hub: "square.grid.2x2.fill"
        }
    }

    func label(_ strings: TabStrings) -> String {
        switch self {
        case .home: strings.home
        case .map: strings.map
        case .hub: strings.hub
        }
    }
}

/// Floating bottom tabs on a Liquid Glass capsule (a blurred capsule before iOS 26). A brighter
/// glass pill marks the selected tab: its leading edge springs ahead and the trailing edge
/// follows, so it stretches and settles as it moves (as on Android). Icons bounce and the phone
/// ticks on each selection.
struct HomeTabBar: View {
    let selected: HomeTab
    let strings: TabStrings
    let inactiveTint: Color
    let onSelect: (HomeTab) -> Void

    @State private var left: CGFloat = 0
    @State private var right: CGFloat = 1

    /// Gap between the bar's edge and the selection pill.
    private let inset: CGFloat = 3

    var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width - inset * 2
            let tabWidth = width / CGFloat(HomeTab.allCases.count)
            let stretch = max(0, min(1, ((right - left) * width - tabWidth) / tabWidth))
            let pillHeight = (proxy.size.height - inset * 2) * (1 - 0.1 * stretch)
            ZStack(alignment: .leading) {
                SelectionPill()
                    .frame(width: max(0, (right - left) * width), height: pillHeight)
                    .offset(x: inset + left * width)
                HStack(spacing: 0) {
                    ForEach(HomeTab.allCases, id: \.self) { tab in
                        TabIcon(tab: tab, isSelected: tab == selected, inactiveTint: inactiveTint) {
                            onSelect(tab)
                        }
                        .accessibilityLabel(tab.label(strings))
                        .accessibilityAddTraits(tab == selected ? [.isSelected, .isButton] : .isButton)
                    }
                }
                .padding(inset)
            }
            .frame(maxHeight: .infinity)
        }
        .frame(height: HomeTheme.tabBarHeight)
        .tabBarSurface()
        .selectionHaptic(trigger: selected)
        .onAppear {
            place(selected, animated: false)
        }
        .onChange(of: selected) { tab in
            place(tab, animated: true)
        }
    }

    private func place(_ tab: HomeTab, animated: Bool) {
        let count = CGFloat(HomeTab.allCases.count)
        let index = CGFloat(HomeTab.allCases.firstIndex(of: tab) ?? 0)
        let newLeft = index / count
        let newRight = (index + 1) / count
        guard animated else {
            left = newLeft
            right = newRight
            return
        }
        let movingRight = newLeft >= left
        let lead = Animation.spring(response: 0.3, dampingFraction: 0.7)
        let trail = Animation.spring(response: 0.46, dampingFraction: 0.72)
        withAnimation(movingRight ? trail : lead) {
            left = newLeft
        }
        withAnimation(movingRight ? lead : trail) {
            right = newRight
        }
    }
}

/// One tab: the icon shrinks a little while pressed and bounces when it becomes selected.
private struct TabIcon: View {
    let tab: HomeTab
    let isSelected: Bool
    let inactiveTint: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: tab.symbol)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(isSelected ? HomeTheme.tabSelectedIcon : inactiveTint.opacity(0.8))
                .bounceOnSelect(isSelected)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .contentShape(Rectangle())
                .animation(.easeInOut(duration: 0.2), value: isSelected)
        }
        .buttonStyle(PressScaleStyle())
    }
}

/// The highlight behind the selected tab: tinted glass on iOS 26, a soft white capsule before.
private struct SelectionPill: View {
    var body: some View {
        if #available(iOS 26.0, *) {
            Capsule()
                .fill(Color.white.opacity(0.55))
                .glassEffect(.regular.tint(.white.opacity(0.6)), in: .capsule)
        } else {
            Capsule()
                .fill(Color.white.opacity(0.92))
                .shadow(color: .black.opacity(0.15), radius: 3, y: 1)
        }
    }
}

/// Native press feedback: a quick spring down to 88% while the finger is on the tab.
private struct PressScaleStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.88 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

private extension View {
    /// Liquid Glass on iOS 26 and later, the dark blur otherwise; either way lifted off the page
    /// with a light rim along the top edge and two shadows (a soft one for height, a tight one
    /// for contact), so the bar reads as floating above the content.
    @ViewBuilder
    func tabBarSurface() -> some View {
        Group {
            if #available(iOS 26.0, *) {
                self.glassEffect(.regular.tint(.black.opacity(0.12)).interactive(), in: .capsule)
            } else {
                self
                    .background(HomeTheme.tabBarScrim, in: Capsule())
                    .background(.ultraThinMaterial, in: Capsule())
            }
        }
            // Soft sheen on the upper half, so the capsule reads as rounded rather than flat.
        .background(
            Capsule()
                .fill(
                    LinearGradient(
                        colors: [.white.opacity(0.14), .white.opacity(0.03), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .allowsHitTesting(false)
        )
        // Light catching the top edge, fading out toward the bottom.
        .overlay(
            Capsule()
                .strokeBorder(
                    LinearGradient(
                        colors: [.white.opacity(0.65), .white.opacity(0.15), .white.opacity(0.05)],
                        startPoint: .top,
                        endPoint: .bottom
                    ),
                    lineWidth: 1
                )
                .allowsHitTesting(false)
        )
        .shadow(color: .black.opacity(0.55), radius: 22, x: 0, y: 12)
        .shadow(color: .black.opacity(0.35), radius: 4, x: 0, y: 2)
    }

    @ViewBuilder
    func bounceOnSelect(_ selected: Bool) -> some View {
        if #available(iOS 17.0, *) {
            symbolEffect(.bounce.down, value: selected)
        } else {
            self
        }
    }

    @ViewBuilder
    func selectionHaptic<T: Equatable>(trigger: T) -> some View {
        if #available(iOS 17.0, *) {
            sensoryFeedback(.selection, trigger: trigger)
        } else {
            self
        }
    }
}
