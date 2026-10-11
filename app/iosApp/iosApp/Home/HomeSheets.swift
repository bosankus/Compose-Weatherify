import SwiftUI
import ComposeApp

// MARK: - Shared sheet chrome

/// Title row with a round close button over a hairline, as on the Android task sheets.
private struct SheetTitleBar: View {
    let title: String
    let closeLabel: String
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Text(title).font(.system(size: 18, weight: .semibold)).foregroundStyle(HomeTheme.inkOnDark)
                Spacer()
                Button(action: onClose) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(HomeTheme.inkOnDark)
                        .frame(width: 36, height: 36)
                        .background(HomeTheme.sheetFieldFill, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(closeLabel)
            }
            Rectangle().fill(HomeTheme.inkOnDark.opacity(0.08)).frame(height: 1)
        }
        .padding(.top, 20)
    }
}

/// Full-width primary action. Muted until enabled; busy adds a spinner before the label.
private struct SheetPrimaryButton: View {
    let title: String
    let enabled: Bool
    let busy: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if busy {
                    ProgressView().tint(HomeTheme.sheetMuted)
                }
                Text(title).font(.system(size: 16, weight: .semibold))
            }
            .foregroundStyle(enabled ? HomeTheme.onYellow : HomeTheme.sheetMuted)
            .frame(maxWidth: .infinity, minHeight: 52)
            .background(enabled ? HomeTheme.yellow : HomeTheme.sheetFieldFill, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .animation(.easeInOut(duration: 0.2), value: enabled)
    }
}

/// Pill text field with a leading glyph and an optional clear button.
private struct SheetField: View {
    let placeholder: String
    let symbol: String
    @Binding var text: String
    var clearLabel: String?
    var focus: FocusState<Bool>.Binding

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: symbol).foregroundStyle(focus.wrappedValue ? HomeTheme.inkOnDark : HomeTheme.sheetMuted)
            TextField("", text: $text, prompt: Text(placeholder).foregroundColor(HomeTheme.sheetMuted))
                .foregroundStyle(HomeTheme.inkOnDark)
                .tint(HomeTheme.yellow)
                .focused(focus)
            if let clearLabel, !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill").foregroundStyle(HomeTheme.sheetMuted)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(clearLabel)
                .transition(.scale.combined(with: .opacity))
            }
        }
        .padding(.horizontal, 14)
        .frame(height: 52)
        .background(HomeTheme.sheetFieldFill, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .animation(.easeInOut(duration: 0.15), value: text.isEmpty)
    }
}

extension View {
    /// Dark sheet surface and rounded top, where the OS supports styling the sheet itself.
    @ViewBuilder
    func homeSheetStyle() -> some View {
        if #available(iOS 16.4, *) {
            self
                .presentationBackground(HomeTheme.sheetFill)
                .presentationCornerRadius(24)
                .presentationDragIndicator(.visible)
                .environment(\.colorScheme, .dark)
        } else {
            self
                .background(HomeTheme.sheetFill.ignoresSafeArea())
                .presentationDragIndicator(.visible)
                .environment(\.colorScheme, .dark)
        }
    }
}

// MARK: - Add a place

/// Starts compact (subtitle, search, button). When results arrive the sheet grows to full
/// height and the list slides in; clearing the query folds it back.
struct PlaceSearchSheet: View {
    let search: PlaceSearchUi
    let strings: PlacesStrings
    let controller: HomeScreenController

    @State private var query = ""
    @State private var detent: PresentationDetent = .height(PlaceSearchSheet.compactHeight)
    @FocusState private var focused: Bool

    static let compactHeight: CGFloat = 330

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SheetTitleBar(title: strings.add, closeLabel: strings.searchClose, onClose: controller.placeActions.closeSearch)
            Text(strings.addSubtitle).font(.system(size: 15)).foregroundStyle(HomeTheme.sheetMuted)
            SheetField(placeholder: strings.searchHint, symbol: "magnifyingglass", text: $query, clearLabel: strings.searchClear, focus: $focused)
                .submitLabel(.search)
                .textInputAutocapitalization(.words)
                .autocorrectionDisabled()
                .onSubmit {
                    focused = false
                }
            ProgressView()
                .progressViewStyle(.linear)
                .tint(HomeTheme.yellow)
                .opacity(search.isSearching || search.isSaving ? 1 : 0)
                .frame(height: 2)
            if let status = search.status {
                Text(status).font(.system(size: 13)).foregroundStyle(HomeTheme.sheetMuted).transition(.opacity)
            }
            if !search.results.isEmpty {
                ScrollView {
                    LazyVStack(spacing: 6) {
                        ForEach(search.results, id: \.key) { suggestion in
                            SuggestionRow(suggestion: suggestion, savedBadge: strings.savedBadge, enabled: !search.isSaving) {
                                focused = false
                                controller.placeActions.pick(key: suggestion.key)
                            }
                        }
                    }
                }
                .scrollDismissesKeyboard(.interactively)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            } else {
                Spacer(minLength: 0)
            }
            SheetPrimaryButton(title: search.actionLabel, enabled: search.actionEnabled, busy: search.isSaving) {
                focused = false
                controller.placeActions.savePicked()
            }
            .padding(.bottom, 8)
        }
        .padding(.horizontal, 20)
        .animation(.easeInOut(duration: 0.28), value: search.results.map(\.key))
        .animation(.easeInOut(duration: 0.2), value: search.status)
        .presentationDetents([.height(Self.compactHeight), .large], selection: $detent)
        .homeSheetStyle()
        .onAppear {
            query = search.query
            focused = true
        }
        .onChange(of: query) {
            controller.placeActions.queryChanged(query: $0)
        }
        .onChange(of: search.results.isEmpty) { empty in
            withAnimation(.spring(response: 0.4, dampingFraction: 0.86)) {
                detent = empty ? .height(Self.compactHeight) : .large
            }
        }
    }
}

private struct SuggestionRow: View {
    let suggestion: SuggestionUi
    let savedBadge: String
    let enabled: Bool
    let onPick: () -> Void

    var body: some View {
        Button(action: onPick) {
            HStack(spacing: 12) {
                Image(systemName: "mappin.circle.fill")
                    .font(.system(size: 20))
                    .foregroundStyle(suggestion.isSelected ? HomeTheme.yellow : HomeTheme.sheetMuted)
                VStack(alignment: .leading, spacing: 2) {
                    Text(suggestion.name).font(.system(size: 16)).foregroundStyle(HomeTheme.inkOnDark).lineLimit(1)
                    if !suggestion.detail.isEmpty {
                        Text(suggestion.detail).font(.system(size: 13)).foregroundStyle(HomeTheme.sheetMuted).lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if suggestion.isSaved {
                    Text(savedBadge)
                        .font(.system(size: 12))
                        .foregroundStyle(HomeTheme.inkOnDark)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(HomeTheme.sheetFieldFill, in: Capsule())
                }
                if suggestion.isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(HomeTheme.onYellow)
                        .frame(width: 24, height: 24)
                        .background(HomeTheme.yellow, in: Circle())
                        .transition(.scale(scale: 0.6).combined(with: .opacity))
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(HomeTheme.yellow.opacity(suggestion.isSelected ? 0.10 : 0))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(HomeTheme.yellow.opacity(suggestion.isSelected ? 0.6 : 0), lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .animation(.easeInOut(duration: 0.2), value: suggestion.isSelected)
        .accessibilityAddTraits(suggestion.isSelected ? .isSelected : [])
    }
}

// MARK: - New event

/// Title, then native date and time pickers. The draft travels to the shared ViewModel as
/// `YYYY-MM-DD` and `HH:mm`, the formats its validation expects.
struct EventSheet: View {
    let composer: EventComposerUi
    let strings: EventSheetStrings
    let controller: HomeScreenController

    @State private var title = ""
    @State private var startsAt = EventSheet.nextHour()
    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SheetTitleBar(title: strings.title, closeLabel: strings.close, onClose: controller.nearbyActions.dismissComposer)
            Text(composer.subtitle).font(.system(size: 15)).foregroundStyle(HomeTheme.sheetMuted)
            SheetField(placeholder: strings.titleHint, symbol: "square.and.pencil", text: $title, focus: $focused)
                .textInputAutocapitalization(.sentences)
                .submitLabel(.done)
                .onSubmit {
                    focused = false
                }
            HStack(spacing: 10) {
                pickerField(symbol: "calendar", label: strings.date, components: .date)
                pickerField(symbol: "clock", label: strings.time, components: .hourAndMinute)
            }
            if let error = composer.error {
                Text(error).font(.system(size: 13)).foregroundStyle(HomeTheme.sheetMuted).transition(.opacity)
            }
            Spacer(minLength: 0)
            SheetPrimaryButton(title: composer.actionLabel, enabled: composer.canSubmit, busy: composer.isSubmitting) {
                focused = false
                controller.nearbyActions.submit()
            }
            .padding(.bottom, 8)
        }
        .padding(.horizontal, 20)
        .animation(.easeInOut(duration: 0.2), value: composer.error)
        .presentationDetents([.height(360)])
        .homeSheetStyle()
        .onAppear(perform: restoreDraft)
        .onChange(of: title) {
            controller.nearbyActions.titleChanged(value: $0)
        }
        .onChange(of: startsAt) {
            sendDate($0)
        }
    }

    private func pickerField(symbol: String, label: String, components: DatePickerComponents) -> some View {
        HStack(spacing: 8) {
            Image(systemName: symbol).foregroundStyle(HomeTheme.sheetMuted)
            DatePicker(label, selection: $startsAt, in: Date()..., displayedComponents: components)
                .labelsHidden()
                .tint(HomeTheme.yellow)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .frame(maxWidth: .infinity, minHeight: 52)
        .background(HomeTheme.sheetFieldFill, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    /// The draft survives closing and reopening; a fresh one starts at the next whole hour.
    private func restoreDraft() {
        title = composer.title
        if let saved = Self.parse(date: composer.dateText, time: composer.timeText) {
            startsAt = saved
        } else {
            sendDate(startsAt)
        }
        if title.isEmpty {
            focused = true
        }
    }

    private func sendDate(_ date: Date) {
        controller.nearbyActions.dateChanged(value: Self.dayFormat.string(from: date))
        controller.nearbyActions.timeChanged(value: Self.timeFormat.string(from: date))
    }

    private static func nextHour() -> Date {
        let calendar = Calendar.current
        let hour = calendar.dateInterval(of: .hour, for: Date())?.start ?? Date()
        return calendar.date(byAdding: .hour, value: 1, to: hour) ?? Date()
    }

    private static func parse(date: String, time: String) -> Date? {
        guard !date.isEmpty, !time.isEmpty else {
            return nil
        }
        return combinedFormat.date(from: "\(date) \(time)")
    }

    private static let dayFormat = fixedFormatter("yyyy-MM-dd")
    private static let timeFormat = fixedFormatter("HH:mm")
    private static let combinedFormat = fixedFormatter("yyyy-MM-dd HH:mm")

    private static func fixedFormatter(_ pattern: String) -> DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = pattern
        return formatter
    }
}

// MARK: - Alert details

extension AlertUi: Identifiable {
    public var id: String {
        key
    }
}

/// Alert details: opens at half height and drags up to full, like the Android peek.
struct AlertSheet: View {
    let alert: AlertUi
    let strings: HomeStrings
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 10) {
                    Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(HomeTheme.alertYellow).font(.system(size: 20))
                    Text(alert.title).font(.system(size: 20, weight: .medium)).frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .bold))
                            .frame(width: 36, height: 36)
                            .background(HomeTheme.sheetFieldFill, in: Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Close alert details")
                }
                .padding(.top, 20)
                if let summary = alert.summary, !summary.isEmpty {
                    Text(summary).font(.system(size: 14)).lineLimit(1)
                }
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 8) {
                        chips
                    }
                    VStack(alignment: .leading, spacing: 8) {
                        chips
                    }
                }
                ForEach(alert.sections, id: \.key) { section in
                    AlertSectionView(section: section, strings: strings)
                }
                if let fallback = alert.fallback {
                    Text(fallback).font(.system(size: 14))
                }
            }
            .foregroundStyle(HomeTheme.inkOnDark)
            .padding(.horizontal, 20)
            .padding(.bottom, 20)
        }
        .presentationDetents([.medium, .large])
        .homeSheetStyle()
    }

    @ViewBuilder
    private var chips: some View {
        AlertChip(text: alert.duration)
        AlertChip(text: alert.source)
    }
}

private struct AlertChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 12))
            .lineLimit(1)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(HomeTheme.inkOnDark.opacity(0.12), in: Capsule())
    }
}

private struct AlertSectionView: View {
    let section: AlertSectionUi
    let strings: HomeStrings
    @State private var showAll = false

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let heading = section.heading {
                Text(heading.uppercased())
                    .font(.system(size: 11, weight: .medium))
                    .tracking(0.8)
                    .foregroundStyle(HomeTheme.sheetMuted)
            }
            if let items = section.items {
                ForEach(showAll ? items : Array(items.prefix(4)), id: \.self) { area in
                    HStack(spacing: 8) {
                        Circle().fill(HomeTheme.sheetMuted).frame(width: 4, height: 4)
                        Text(area).font(.system(size: 14))
                    }
                }
                if items.count > 4 {
                    Button(showAll ? strings.showLess : "\(strings.showAll) (\(items.count))") {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            showAll.toggle()
                        }
                    }
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(HomeTheme.inkOnDark)
                    .padding(.vertical, 4)
                }
            } else {
                Text(section.body).font(.system(size: 14))
            }
        }
    }
}
