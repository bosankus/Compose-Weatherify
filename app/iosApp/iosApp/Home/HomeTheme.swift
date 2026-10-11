import SwiftUI

/// Colors and metrics shared by the home screen, matching the Android home (HomeScreen.kt and friends).
enum HomeTheme {
    /// Off-white ink on dark photos.
    static let inkOnDark = Color(hex: 0xE7E4DC)
    /// Dark ink when the top of the photo is bright.
    static let inkOnLight = Color(hex: 0x1C2430)
    /// Every dark card over the photo.
    static let cardFill = Color.black.opacity(0.38)
    static let cardRadius: CGFloat = 20
    static let yellow = Color(hex: 0xF8BF1D)
    static let onYellow = Color(hex: 0x212121)
    static let alertYellow = Color(hex: 0xF5C400)
    static let sheetFill = Color(hex: 0x14171C)
    static let sheetMuted = inkOnDark.opacity(0.6)
    static let sheetFieldFill = inkOnDark.opacity(0.08)
    static let mutedAlpha = 0.72
    static let tabBarScrim = Color(hex: 0x101418).opacity(0.4)
    static let tabSelectedIcon = Color(hex: 0x161616)
    static let pageInset: CGFloat = 20
    static let tabBarHeight: CGFloat = 40
}

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: opacity
        )
    }
}

/// Soft shadow under header text so it reads on any photo.
struct HeaderShadow: ViewModifier {
    func body(content: Content) -> some View {
        content.shadow(color: .black.opacity(0.45), radius: 4, x: 0, y: 1)
    }
}

/// The dark rounded card every home section sits in.
struct HomeCard: ViewModifier {
    var padding: CGFloat = 14

    func body(content: Content) -> some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(HomeTheme.cardFill, in: RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous))
    }
}

extension View {
    func headerShadow() -> some View {
        modifier(HeaderShadow())
    }

    func homeCard(padding: CGFloat = 14) -> some View {
        modifier(HomeCard(padding: padding))
    }

    /// Numbers roll like the Android count-up where the OS supports it.
    @ViewBuilder
    func numericTransition() -> some View {
        if #available(iOS 17.0, *) {
            contentTransition(.numericText())
        } else {
            self
        }
    }
}
