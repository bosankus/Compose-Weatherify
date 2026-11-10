import SwiftUI
import ComposeApp

/// Second page: GPS plus every saved place, as dark cards over the photo.
struct PlacesPage: View {
    let places: PlacesPageUi
    let strings: PlacesStrings
    let retry: String
    let ink: Color
    let controller: HomeScreenController
    let onUpgrade: () -> Void

    /// Removing a place hits the account, so it asks first (the native destructive pattern).
    @State private var pendingDelete: PlaceUi?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Each child resolves sentinel ink on its own layer.
            Group {
                Text(strings.title)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(ink)
                    .headerShadow()
                if let notice = places.notice {
                    Text(notice).font(.system(size: 13)).foregroundStyle(ink.opacity(0.9))
                }
                PlaceCard(
                    title: strings.current,
                    detail: strings.currentDetail,
                    symbol: "location.fill",
                    isActive: places.currentIsActive,
                    isSwitching: places.currentIsSwitching,
                    deleteLabel: nil,
                    ink: ink,
                    onSelect: controller.placeActions.useCurrentLocation,
                    onDelete: {}
                )
                content
                if let map = places.map {
                    Text(map.attribution)
                        .font(.system(size: 11))
                        .foregroundStyle(ink.opacity(HomeTheme.mutedAlpha))
                }
                // Keeps the last card clear of the add button.
                Spacer().frame(height: 72)
            }
            .adaptiveInk()
        }
        .animation(.easeInOut(duration: 0.2), value: places.places.map(\.key))
        .confirmationDialog(
            pendingDelete?.deleteLabel ?? "",
            isPresented: Binding(get: { pendingDelete != nil }, set: {
                if !$0 {
                    pendingDelete = nil
                }
            }),
            titleVisibility: .visible,
            presenting: pendingDelete
        ) { place in
            Button(place.deleteLabel ?? place.title, role: .destructive) {
                controller.placeActions.delete(key: place.key)
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if !places.isPremium {
            PremiumCard(strings: strings, ink: ink, onUpgrade: onUpgrade)
        } else if places.places.isEmpty && places.loadFailed {
            MessageCard(text: strings.loadError, ink: ink) {
                PillButton(title: retry, ink: ink, action: controller.placeActions.load)
            }
        } else if places.places.isEmpty && places.isLoading {
            HStack(spacing: 8) {
                ProgressView().tint(ink).controlSize(.small)
                Text(strings.loading).font(.system(size: 13)).foregroundStyle(ink.opacity(HomeTheme.mutedAlpha))
            }
        } else if places.places.isEmpty {
            MessageCard(text: strings.empty, ink: ink) {
                EmptyView()
            }
        } else {
            ForEach(places.places, id: \.key) { place in
                PlaceCard(
                    title: place.title,
                    detail: place.detail,
                    symbol: "mappin.and.ellipse",
                    isActive: place.isActive,
                    isSwitching: place.isSwitching,
                    deleteLabel: place.deleteLabel,
                    ink: ink,
                    onSelect: { controller.placeActions.select(key: place.key) },
                    onDelete: { pendingDelete = place }
                )
                    .transition(.opacity.combined(with: .move(edge: .trailing)))
            }
        }
    }
}

private struct PlaceCard: View {
    let title: String
    let detail: String
    let symbol: String
    let isActive: Bool
    let isSwitching: Bool
    let deleteLabel: String?
    let ink: Color
    let onSelect: () -> Void
    let onDelete: () -> Void

    private var phase: PlaceStatusMark.Phase {
        if isActive {
            return isSwitching ? .switching : .selected
        }
        return deleteLabel != nil ? .remove : .none
    }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 17))
                .frame(width: 36, height: 36)
                .background(Color.white.opacity(0.12), in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.system(size: 16, weight: .medium)).lineLimit(1)
                Text(detail).font(.system(size: 13)).foregroundStyle(ink.opacity(HomeTheme.mutedAlpha)).lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: onDelete) {
                PlaceStatusMark(phase: phase, ink: ink)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            // Only the X is a button; on the tick or spinner a tap falls through to the card.
            .allowsHitTesting(phase == .remove)
            .accessibilityLabel(deleteLabel ?? "")
            .accessibilityHidden(phase != .remove)
        }
        .foregroundStyle(ink)
        .padding(.leading, 14)
        .padding(.trailing, 6)
        .padding(.vertical, 12)
        .adaptiveInk(surface: HomeTheme.cardFillOpacity)
        .background(HomeTheme.cardFill, in: RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous)
                .strokeBorder(HomeTheme.yellow.opacity(isActive ? 0.8 : 0), lineWidth: 1)
        )
        .contentShape(RoundedRectangle(cornerRadius: HomeTheme.cardRadius))
        .onTapGesture(perform: onSelect)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(isActive ? [.isButton, .isSelected] : .isButton)
        .contextMenu {
            if let deleteLabel {
                Button(role: .destructive, action: onDelete) {
                    Label(deleteLabel, systemImage: "trash")
                }
            }
        }
    }
}

/// Right end of a place card, one mark that morphs between states (same as Android's
/// `PlaceStatusMark`): the X twists down into a small spinning ring while Home fetches the
/// picked place, then the ring fills into the yellow disc as the tick draws itself in.
private struct PlaceStatusMark: View {
    enum Phase {
        case remove, switching, selected, none
    }

    let phase: Phase
    let ink: Color

    var body: some View {
        ZStack {
            Circle()
                .fill(HomeTheme.yellow)
                .frame(width: 28, height: 28)
                .scaleEffect(phase == .selected ? 1 : 0.001)
                .animation(
                    phase == .selected ? .spring(response: 0.35, dampingFraction: 0.6) : .easeInOut(duration: Self.morph),
                    value: phase
                )
            CrossShape()
                .stroke(ink.opacity(HomeTheme.mutedAlpha), style: StrokeStyle(lineWidth: 2, lineCap: .round))
                .frame(width: 10, height: 10)
                .rotationEffect(.degrees(phase == .remove ? 0 : 90))
                .scaleEffect(phase == .remove ? 1 : 0.001)
            TimelineView(.animation(paused: phase != .switching)) { context in
                Circle()
                    .trim(from: 0, to: phase == .switching ? 0.75 : 0)
                    .stroke(HomeTheme.yellow, style: StrokeStyle(lineWidth: 2, lineCap: .round))
                    .frame(width: 16, height: 16)
                    .rotationEffect(.degrees(Self.spinAngle(at: context.date)))
            }
            TickShape()
                .trim(from: 0, to: phase == .selected ? 1 : 0)
                .stroke(HomeTheme.onYellow, style: StrokeStyle(lineWidth: 2.2, lineCap: .round, lineJoin: .round))
                .frame(width: 9.1, height: 6.6)
                .animation(
                    phase == .selected ? .easeOut(duration: 0.26).delay(0.09) : .easeIn(duration: Self.morph / 2),
                    value: phase
                )
        }
        .frame(width: 44, height: 44)
        .animation(.easeInOut(duration: Self.morph), value: phase)
    }

    private static let morph = 0.22
    private static let spinPeriod = 0.8

    private static func spinAngle(at date: Date) -> Double {
        date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: spinPeriod) / spinPeriod * 360 - 90
    }
}

private struct CrossShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.move(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        return path
    }
}

/// Short stroke first, so trimming draws the tick the way a hand would.
private struct TickShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.minY + rect.height * 0.55))
        path.addLine(to: CGPoint(x: rect.minX + rect.width * 0.34, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        return path
    }
}

private struct PremiumCard: View {
    let strings: PlacesStrings
    let ink: Color
    let onUpgrade: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.premiumTitle).font(.system(size: 16, weight: .medium))
            Text(strings.premiumBody).font(.system(size: 14)).foregroundStyle(ink.opacity(HomeTheme.mutedAlpha))
            Button(action: onUpgrade) {
                Text(strings.upgrade)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(HomeTheme.onYellow)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(HomeTheme.yellow, in: Capsule())
            }
            .buttonStyle(.plain)
            .padding(.top, 4)
        }
        .foregroundStyle(ink)
        .homeCard(padding: 16)
    }
}

private struct MessageCard<Action: View>: View {
    let text: String
    let ink: Color
    @ViewBuilder let action: () -> Action

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(text).font(.system(size: 14))
            action()
        }
        .foregroundStyle(ink)
        .homeCard(padding: 16)
    }
}
