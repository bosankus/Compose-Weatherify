import SwiftUI
import ComposeApp

/// First page: every home section under the header, in the Android order.
struct WeatherPage: View {
    let ui: HomeScreenUi
    let ink: Color
    let model: HomeModel
    /// Passed in, not read from the model, so the card appears the moment availability is known.
    let showAiSummary: Bool
    let onOpenPlaces: () -> Void

    private var page: WeatherPageUi {
        ui.weather
    }
    private var strings: HomeStrings {
        ui.strings
    }
    private var controller: HomeScreenController {
        model.controller
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            DetailsGrid(cells: page.details, ink: ink)
            if let report = page.report {
                CurrentReportCard(report: report, strings: strings, ink: ink).padding(.top, 16)
            }
            if let trend = page.trend {
                TrendCard(trend: trend, ink: ink).padding(.top, 12)
            }
            if let week = page.week {
                WeekStrip(week: week, strings: strings, ink: ink, onOpen: controller.nearbyActions.openComposer, onRetry: controller.retryForecast)
                    .padding(.top, 20)
            }
            if let message = ui.chrome.statusMessage {
                Text(message).font(.system(size: 13)).foregroundStyle(ink.opacity(0.84)).padding(.top, 8)
            }
            if ui.chrome.gpsDisabled || ui.chrome.locationPermissionDenied || ui.chrome.forecastFailed {
                WaitingActions(chrome: ui.chrome, strings: strings, ink: ink, model: model).padding(.top, 8)
            }
            ForecastDetailsBlock(page: page, strings: strings, ink: ink, onOpenAlert: { model.openAlert = $0 })
            if showAiSummary {
                AiSummaryEntry(
                    strings: strings.aiSummary,
                    ink: ink,
                    action: {
                        model.showAiSummary = true
                        controller.openAiSummary()
                    }
                )
                .padding(.top, 16)
            }
            if page.showPromoCards {
                PromoCards(ink: ink).padding(.top, 16)
            }
            if let nearby = page.nearby {
                NearbyBlocks(nearby: nearby, strings: strings, ink: ink, onRetry: controller.nearbyActions.retryEvents, onOpenSaved: onOpenPlaces)
                    .padding(.top, 16)
            }
            if let photo = ui.backdrop.photo {
                PhotoCredit(photo: photo, ink: ink).padding(.top, 8)
            }
        }
        .padding(.bottom, 12)
    }
}

// MARK: - Details grid

/// Feel, wind, UV, humidity, pressure and clouds as a 3-column grid straight on the photo,
/// with hairline dividers. Rain cells add a padded row only when they have values.
private struct DetailsGrid: View {
    let cells: [DetailCellUi]
    let ink: Color

    var body: some View {
        let rows = stride(from: 0, to: cells.count, by: 3).map {
            Array(cells[$0..<min($0 + 3, cells.count)])
        }
        VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                if index > 0 {
                    Rectangle().fill(ink.opacity(0.15)).frame(height: 1)
                }
                HStack(spacing: 0) {
                    ForEach(0..<3, id: \.self) { column in
                        if column > 0 {
                            Rectangle().fill(column < row.count ? ink.opacity(0.15) : .clear).frame(width: 1)
                        }
                        if column < row.count {
                            DetailCell(cell: row[column], ink: ink)
                        } else {
                            Color.clear.frame(maxWidth: .infinity)
                        }
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

private struct DetailCell: View {
    let cell: DetailCellUi
    let ink: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Image(systemName: cell.kind.symbol).font(.system(size: 15)).padding(.bottom, 2)
            Text(cell.value)
                .font(.system(size: 16, weight: .medium))
                .monospacedDigit()
                .numericTransition()
                .animation(.easeInOut(duration: 0.5), value: cell.value)
            if let secondary = cell.secondary {
                Text(secondary).font(.system(size: 11)).foregroundStyle(ink.opacity(HomeTheme.mutedAlpha))
            }
            Text(cell.label).font(.system(size: 12)).foregroundStyle(ink.opacity(HomeTheme.mutedAlpha)).lineLimit(1)
        }
        .foregroundStyle(ink)
        .shadow(color: .black.opacity(0.4), radius: 3, y: 1)
        .padding(.horizontal, 10)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel([cell.label, cell.value, cell.secondary].compactMap {
            $0
        }
                            .joined(separator: ", "))
    }
}

extension DetailKind {
    var symbol: String {
        switch self {
        case .feel: "thermometer.medium"
        case .wind: "wind"
        case .uv: "sun.max"
        case .humidity: "drop"
        case .pressure: "gauge.with.dots.needle.33percent"
        case .clouds: "cloud"
        case .rainToday: "umbrella"
        case .nextRain: "clock"
        }
    }
}

// MARK: - Current report and daylight arc

private struct CurrentReportCard: View {
    let report: CurrentReportUi
    let strings: HomeStrings
    let ink: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: report.glyph.symbol).font(.system(size: 24))
                Text(report.condition).font(.system(size: 16, weight: .medium))
            }
            HStack(alignment: .center, spacing: 6) {
                SunLabel(time: report.sunrise, caption: strings.sunrise, ink: ink)
                DaylightArc(progress: CGFloat(report.sunProgress)).frame(height: 78)
                SunLabel(time: report.sunset, caption: strings.sunset, ink: ink)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(strings.sunrise) \(report.sunrise), \(strings.sunset) \(report.sunset)")
        }
        .foregroundStyle(ink)
        .homeCard()
    }
}

private struct SunLabel: View {
    let time: String
    let caption: String
    let ink: Color

    var body: some View {
        VStack(alignment: .leading) {
            Text(time).font(.system(size: 16, weight: .medium))
            Text(caption).font(.system(size: 12)).foregroundStyle(ink.opacity(0.62))
        }
    }
}

/// Dashed half circle over a hatched bowl, with a glowing sun at today's progress.
private struct DaylightArc: View {
    let progress: CGFloat

    var body: some View {
        Canvas { context, size in
            let glow: CGFloat = 12
            let bottom = size.height - glow
            let radius = max(1, min(size.width / 2 - glow, bottom - glow))
            let center = CGPoint(x: size.width / 2, y: bottom)
            var bowl = Path()
            bowl.addArc(center: center, radius: radius, startAngle: .degrees(180), endAngle: .degrees(360), clockwise: false)
            bowl.closeSubpath()
            context.drawLayer { layer in
                layer.clip(to: bowl)
                var x = -size.height
                while x < size.width + size.height {
                    var line = Path()
                    line.move(to: CGPoint(x: x, y: 0))
                    line.addLine(to: CGPoint(x: x + size.height, y: size.height))
                    layer.stroke(line, with: .color(.black.opacity(0.42)), lineWidth: 1.1)
                    x += 5
                }
            }
            var arc = Path()
            arc.addArc(center: center, radius: radius, startAngle: .degrees(180), endAngle: .degrees(360), clockwise: false)
            context.stroke(
                arc,
                with: .color(Color(hex: 0xD7D4CC)),
                style: StrokeStyle(lineWidth: 1.4, lineCap: .round, dash: [1.6, 3.4])
            )
            let theta = Double.pi * (1 - Double(progress))
            let sun = CGPoint(x: center.x + radius * cos(theta), y: center.y - radius * sin(theta))
            let sunColor = Color(hex: 0xFFD24A)
            context.fill(
                Path(ellipseIn: CGRect(x: sun.x - glow, y: sun.y - glow, width: glow * 2, height: glow * 2)),
                with: .radialGradient(Gradient(colors: [sunColor.opacity(0.55), .clear]), center: sun, startRadius: 0, endRadius: glow)
            )
            context.fill(Path(ellipseIn: CGRect(x: sun.x - 5, y: sun.y - 5, width: 10, height: 10)), with: .color(sunColor))
        }
        .padding(.horizontal, 6)
    }
}

extension SkyGlyph {
    var symbol: String {
        switch self {
        case .clearDay: "sun.max"
        case .clearNight: "moon.stars"
        case .clouds: "cloud"
        case .rain: "cloud.rain"
        case .snow: "snowflake"
        case .thunder: "cloud.bolt"
        case .mist: "cloud.fog"
        }
    }
}

// MARK: - Trend, week, waiting actions

private struct TrendCard: View {
    let trend: TrendUi
    let ink: Color

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: symbol).font(.system(size: 17, weight: .semibold))
            Text(trend.line).font(.system(size: 15, weight: .medium)).lineLimit(2)
        }
        .foregroundStyle(ink)
        .homeCard()
        .accessibilityElement(children: .combine)
    }

    private var symbol: String {
        switch trend.direction {
        case .cooling: "arrow.down.right"
        case .warming: "arrow.up.right"
        case .steady: "arrow.right"
        }
    }
}

private struct WeekStrip: View {
    let week: WeekUi
    let strings: HomeStrings
    let ink: Color
    let onOpen: () -> Void
    let onRetry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Text(strings.dailyForecast).font(.system(size: 18, weight: .medium)).frame(maxWidth: .infinity, alignment: .leading)
                PillButton(title: strings.eventCalendar, ink: ink, action: onOpen)
                    .accessibilityLabel(strings.addEvent)
                if week.showRetry {
                    PillButton(title: strings.retry, ink: ink, action: onRetry)
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(alignment: .top, spacing: 8) {
                    ForEach(week.days, id: \.key) { day in
                        Button(action: onOpen) {
                            DayCell(day: day, ink: ink)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel([strings.addEvent, "\(day.label) \(day.date)", day.range, day.caption].filter {
                            !$0.isEmpty
                        }
                                            .joined(separator: ", "))
                    }
                }
            }
        }
        .foregroundStyle(ink)
    }
}

private struct DayCell: View {
    let day: DayUi
    let ink: Color

    var body: some View {
        VStack(spacing: 4) {
            Text(day.label).font(.system(size: 13))
            Text(day.date)
                .font(.system(size: 16, weight: day.isToday ? .semibold : .regular))
                .foregroundStyle(day.isToday ? HomeTheme.inkOnLight : ink)
                .frame(width: 32, height: 32)
                .background(day.isToday ? Color.white : .clear, in: Circle())
                .padding(.top, 4)
            if day.hasEvent {
                Circle().fill(HomeTheme.alertYellow).frame(width: 5, height: 5)
            }
            if let icon = day.iconUrl {
                WeatherIcon(url: icon).frame(width: 28, height: 28)
            }
            if !day.range.isEmpty {
                Text(day.range).font(.system(size: 13)).multilineTextAlignment(.center).lineLimit(2)
            }
            if !day.caption.isEmpty {
                Text(day.caption).font(.system(size: 11)).multilineTextAlignment(.center).lineLimit(2)
            }
        }
        .foregroundStyle(ink)
        .frame(width: 84)
    }
}

/// OpenWeather condition icon.
struct WeatherIcon: View {
    let url: String

    var body: some View {
        AsyncImage(url: URL(string: url)) { image in
            image.resizable().scaledToFit()
        } placeholder: {
            Image(systemName: "sun.max").resizable().scaledToFit().opacity(0.4).padding(4)
        }
    }
}

private struct WaitingActions: View {
    let chrome: ChromeUi
    let strings: HomeStrings
    let ink: Color
    let model: HomeModel

    var body: some View {
        HStack(spacing: 8) {
            if chrome.gpsDisabled {
                PillButton(title: strings.enableGps, ink: ink, action: model.permissions.openAppSettings)
            }
            if chrome.locationPermissionDenied {
                PillButton(title: strings.requestLocation, ink: ink, action: model.controller.requestLocationPermission)
            }
            if chrome.forecastFailed {
                PillButton(title: strings.retry, ink: ink, action: model.controller.retryForecast)
            }
        }
    }
}

/// Small rounded action on a dark fill, like Android's ActionLabel.
struct PillButton: View {
    let title: String
    let ink: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(ink)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(HomeTheme.cardFill, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Promo, nearby, credit

/// Entry row for the weather summary. Opens the summary sheet.
private struct AiSummaryEntry: View {
    let strings: AiSummaryStrings
    let ink: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: "sparkles").font(.system(size: 18)).foregroundStyle(HomeTheme.yellow)
                VStack(alignment: .leading, spacing: 0) {
                    Text(strings.title).font(.system(size: 16, weight: .medium))
                    Text(strings.body).font(.system(size: 14))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "arrow.right")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(HomeTheme.onYellow)
                    .frame(width: 28, height: 28)
                    .background(HomeTheme.yellow, in: Circle())
            }
            .foregroundStyle(ink)
            .homeCard()
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(strings.title). \(strings.body)")
        .accessibilityHint(strings.action)
        .accessibilityAddTraits(.isButton)
    }
}

private struct PromoCards: View {
    let ink: Color

    var body: some View {
        HStack(spacing: 12) {
            ForEach(["16 Top beautiful trails nearby", "5 Personalized activity recommendations"], id: \.self) { title in
                VStack(alignment: .leading, spacing: 14) {
                    Text(title).font(.system(size: 16, weight: .medium)).frame(maxWidth: .infinity, alignment: .leading)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(HomeTheme.onYellow)
                        .frame(width: 28, height: 28)
                        .background(HomeTheme.yellow, in: Circle())
                }
                .foregroundStyle(ink)
                .homeCard()
            }
        }
    }
}

private struct NearbyBlocks: View {
    let nearby: NearbyUi
    let strings: HomeStrings
    let ink: Color
    let onRetry: () -> Void
    let onOpenSaved: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            if nearby.showEvents {
                VStack(alignment: .leading, spacing: 8) {
                    Text(strings.nearbyEvents).font(.system(size: 16, weight: .medium))
                    if nearby.eventsFailed {
                        ShimmerBar(widthFraction: 0.46)
                        ShimmerBar(widthFraction: 0.32)
                        PillButton(title: strings.retry, ink: ink, action: onRetry)
                    } else {
                        ForEach(nearby.events, id: \.key) { event in
                            Text(event.title.isEmpty ? event.whenLabel : event.title).font(.system(size: 16, weight: .medium))
                            if !event.title.isEmpty {
                                Text(event.whenLabel).font(.system(size: 13))
                            }
                        }
                    }
                }
                .foregroundStyle(ink)
                .homeCard()
            }
            if let place = nearby.savedPlace {
                Button(action: onOpenSaved) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(strings.savedPlace).font(.system(size: 13))
                        if !place.name.isEmpty {
                            Text(place.name).font(.system(size: 16, weight: .medium))
                        }
                        if !place.subtitle.isEmpty {
                            Text(place.subtitle).font(.system(size: 14))
                        }
                    }
                    .foregroundStyle(ink)
                    .homeCard()
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// Loading bar with a travelling highlight, like Android's shimmer brush.
struct ShimmerBar: View {
    let widthFraction: CGFloat
    @State private var travel: CGFloat = -1

    var body: some View {
        GeometryReader { proxy in
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.white.opacity(0.10))
                .overlay(
                    LinearGradient(colors: [.clear, .white.opacity(0.24), .clear], startPoint: .leading, endPoint: .trailing)
                        .frame(width: proxy.size.width * 0.6)
                        .offset(x: travel * proxy.size.width)
                )
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .frame(width: proxy.size.width * widthFraction)
        }
        .frame(height: 14)
        .onAppear {
            withAnimation(.linear(duration: 1.1).repeatForever(autoreverses: false)) {
                travel = 1.2
            }
        }
    }
}

private struct PhotoCredit: View {
    let photo: BackdropPhotoUi
    let ink: Color

    var body: some View {
        Text(credit)
            .font(.system(size: 13))
            .foregroundStyle(ink)
            .tint(ink)
    }

    private var credit: AttributedString {
        var text = AttributedString("Photo by ")
        var name = AttributedString(photo.photographer)
        name.link = URL(string: photo.profileUrl)
        name.underlineStyle = .single
        var site = AttributedString("Unsplash")
        site.link = URL(string: photo.unsplashUrl)
        site.underlineStyle = .single
        text.append(name)
        text.append(AttributedString(" on "))
        text.append(site)
        return text
    }
}
