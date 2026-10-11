import SwiftUI
import ComposeApp

/// SwiftUI side of the shared LanguageScreenController.
@MainActor
final class LanguageModel: ObservableObject {
    let controller = LanguageScreenController()
    @Published private(set) var ui: LanguageScreenUi?

    deinit {
        let controller = controller
        DispatchQueue.main.async {
            controller.close()
        }
    }

    func run() async {
        for await frame in controller.state {
            ui = frame
        }
    }
}

/// Language picker in the app's night look: round back button, title and subtitle, and one card
/// of languages with flags. Picking one applies it at once; [onChanged] lets the other pages
/// reload their copy. Swipe from the edge to go back, as on any pushed iOS screen.
struct LanguagePage: View {
    let onChanged: () -> Void

    @StateObject private var model = LanguageModel()
    @Environment(\.dismiss) private var dismiss
    @State private var revealed = false

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: 0x1C2430), Color(hex: 0x12161C), Color(hex: 0x0B0E12)],
                startPoint: .top,
                endPoint: .bottom
            )
                .ignoresSafeArea()
            if let ui = model.ui {
                content(ui)
            }
        }
        .environment(\.colorScheme, .dark)
        .toolbar(.hidden, for: .navigationBar)
        .task {
            await model.run()
        }
        .pickHaptic(trigger: model.ui?.rows.first(where: \.isSelected)?.tag)
    }

    private func content(_ ui: LanguageScreenUi) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "chevron.backward")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(HomeTheme.inkOnDark)
                        .frame(width: 40, height: 40)
                        .background(HomeTheme.inkOnDark.opacity(0.1), in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(ui.backLabel)
                .padding(.top, 8)

                VStack(alignment: .leading, spacing: 6) {
                    Text(ui.title).font(.system(size: 28, weight: .bold))
                    Text(ui.subtitle).font(.system(size: 15)).foregroundStyle(HomeTheme.inkOnDark.opacity(0.65))
                }
                .foregroundStyle(HomeTheme.inkOnDark)
                .padding(.top, 20)
                .padding(.bottom, 24)

                VStack(spacing: 0) {
                    ForEach(Array(ui.rows.enumerated()), id: \.element.tag) { index, row in
                        if index > 0 {
                            Rectangle().fill(HomeTheme.inkOnDark.opacity(0.08)).frame(height: 1).padding(.leading, 70)
                        }
                        LanguageRowView(row: row) {
                            model.controller.select(tag: row.tag)
                            onChanged()
                        }
                        .opacity(revealed ? 1 : 0)
                        .offset(y: revealed ? 0 : 14)
                        .animation(.spring(response: 0.45, dampingFraction: 0.85).delay(Double(index) * 0.035), value: revealed)
                    }
                }
                .background(HomeTheme.cardFill, in: RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous)
                        .strokeBorder(HomeTheme.inkOnDark.opacity(0.08), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous))
            }
            .padding(.horizontal, HomeTheme.pageInset)
            .padding(.bottom, 32)
        }
        .scrollIndicators(.hidden)
        .onAppear {
            revealed = true
        }
    }
}

private struct LanguageRowView: View {
    let row: LanguageRowUi
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 14) {
                FlagView(region: row.region)
                Text(row.name)
                    .font(.system(size: 16, weight: row.isSelected ? .semibold : .regular))
                    .foregroundStyle(HomeTheme.inkOnDark)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if row.isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(HomeTheme.onYellow)
                        .frame(width: 26, height: 26)
                        .background(HomeTheme.yellow, in: Circle())
                        .transition(.scale(scale: 0.5).combined(with: .opacity))
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(HomeTheme.yellow.opacity(row.isSelected ? 0.12 : 0))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.3, dampingFraction: 0.75), value: row.isSelected)
        .accessibilityAddTraits(row.isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

private extension View {
    /// A light tick when the picked language changes (iOS 17+).
    @ViewBuilder
    func pickHaptic(trigger: String?) -> some View {
        if #available(iOS 17.0, *) {
            sensoryFeedback(.selection, trigger: trigger)
        } else {
            self
        }
    }
}

/// The region's flag from the asset catalog (generated from the shared vector drawables), or its
/// two-letter code when there is no drawing.
private struct FlagView: View {
    let region: String

    var body: some View {
        Group {
            if UIImage(named: "Flag-\(region)") != nil {
                Image("Flag-\(region)").resizable().scaledToFill()
            } else {
                Text(region)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(HomeTheme.inkOnDark.opacity(0.7))
            }
        }
        .frame(width: 40, height: 40)
        .background(HomeTheme.inkOnDark.opacity(0.1))
        .clipShape(Circle())
        .accessibilityHidden(true)
    }
}
