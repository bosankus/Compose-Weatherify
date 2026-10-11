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
                deleteLabel: nil,
                ink: ink,
                onSelect: controller.placeActions.useCurrentLocation,
                onDelete: {}
            )
            content
            // Keeps the last card clear of the add button.
            Spacer().frame(height: 72)
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
    let deleteLabel: String?
    let ink: Color
    let onSelect: () -> Void
    let onDelete: () -> Void

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
            trailing
                .frame(width: 44, height: 44)
                .animation(.easeInOut(duration: 0.2), value: isActive)
        }
        .foregroundStyle(ink)
        .padding(.leading, 14)
        .padding(.trailing, 6)
        .padding(.vertical, 12)
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

    @ViewBuilder
    private var trailing: some View {
        if isActive {
            Image(systemName: "checkmark")
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(HomeTheme.onYellow)
                .frame(width: 28, height: 28)
                .background(HomeTheme.yellow, in: Circle())
                .transition(.scale(scale: 0.6).combined(with: .opacity))
        } else if let deleteLabel {
            Button(action: onDelete) {
                Image(systemName: "xmark").font(.system(size: 14, weight: .semibold)).foregroundStyle(ink.opacity(HomeTheme.mutedAlpha))
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(deleteLabel)
            .transition(.scale(scale: 0.6).combined(with: .opacity))
        }
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
