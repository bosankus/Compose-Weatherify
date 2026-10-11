import SwiftUI
import ComposeApp

/// Alerts, air quality, and the hourly strip. Empty reports stay off the page.
struct ForecastDetailsBlock: View {
    let page: WeatherPageUi
    let strings: HomeStrings
    let ink: Color
    let onOpenAlert: (AlertUi) -> Void

    var body: some View {
        if !page.alerts.isEmpty || page.airQuality != nil || !page.hourly.isEmpty {
            VStack(spacing: 16) {
                if !page.alerts.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(page.alerts, id: \.key) { alert in
                            AlertRow(alert: alert, ink: ink) {
                                onOpenAlert(alert)
                            }
                        }
                    }
                    .homeCard()
                    .accessibilityLabel(strings.weatherAlerts)
                }
                if let air = page.airQuality {
                    AirQualityCard(air: air, strings: strings, ink: ink)
                }
                if !page.hourly.isEmpty {
                    HourlyCard(hours: page.hourly, title: strings.hourlyForecast, ink: ink)
                }
            }
            .padding(.top, 16)
        }
    }
}

private struct AlertRow: View {
    let alert: AlertUi
    let ink: Color
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(spacing: 10) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(HomeTheme.alertYellow)
                    .font(.system(size: 17))
                VStack(alignment: .leading, spacing: 2) {
                    Text(alert.title).font(.system(size: 16, weight: .medium)).lineLimit(1)
                    if let start = alert.startText, !start.isEmpty {
                        Text(start).font(.system(size: 13)).lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.right").font(.system(size: 14, weight: .semibold))
            }
            .foregroundStyle(ink)
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(alert.title). Open alert details")
    }
}

/// Collapsed: band dot, status, AQI and the dominant pollutant. Tap expands the pollutant tiles.
private struct AirQualityCard: View {
    let air: AirQualityUi
    let strings: HomeStrings
    let ink: Color
    @State private var expanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button {
                withAnimation(.easeInOut(duration: 0.22)) {
                    expanded.toggle()
                }
            } label: {
                HStack(spacing: 10) {
                    Circle().fill(air.band.color).frame(width: 10, height: 10)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(air.status).font(.system(size: 16, weight: .medium)).lineLimit(1)
                        Text(air.aqiLabel).font(.system(size: 13)).foregroundStyle(ink.opacity(0.7))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    if let dominant = air.dominant {
                        Text(dominant)
                            .font(.system(size: 12))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
                    }
                    Image(systemName: "chevron.down")
                        .font(.system(size: 14, weight: .semibold))
                        .rotationEffect(.degrees(expanded ? 180 : 0))
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("\(strings.airQuality), \(air.status), \(air.aqiLabel)")
            .accessibilityHint(expanded ? "Collapse" : "Expand")
            if expanded {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                    ForEach(air.readings, id: \.key) { reading in
                        PollutantTile(reading: reading, ink: ink)
                    }
                }
                .padding(.top, 12)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .foregroundStyle(ink)
        .homeCard()
        .clipped()
    }
}

private struct PollutantTile: View {
    let reading: PollutantUi
    let ink: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(reading.name).font(.system(size: 12, weight: .medium)).lineLimit(1)
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(reading.value).font(.system(size: 16, weight: .medium)).lineLimit(1)
                Text(reading.unit).font(.system(size: 10)).foregroundStyle(ink.opacity(0.7)).lineLimit(1)
            }
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(ink.opacity(0.15))
                    Capsule().fill(reading.band.color).frame(width: proxy.size.width * CGFloat(reading.fraction))
                }
            }
            .frame(height: 3)
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(reading.name) \(reading.value) \(reading.unit)")
    }
}

extension AirBand {
    var color: Color {
        switch self {
        case .good: Color(hex: 0x4CAF50)
        case .fair: Color(hex: 0x9CCC65)
        case .moderate: Color(hex: 0xFFB300)
        case .poor: Color(hex: 0xFB8C00)
        case .veryPoor: Color(hex: 0xE53935)
        }
    }
}

/// Next 24 hours; tapping an hour lifts it like the Android zoom.
private struct HourlyCard: View {
    let hours: [HourUi]
    let title: String
    let ink: Color
    @State private var selected: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.system(size: 16, weight: .medium))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(hours, id: \.key) { hour in
                        let isSelected = (selected ?? hours.first?.key) == hour.key
                        Button {
                            withAnimation(.easeInOut(duration: 0.22)) {
                                selected = hour.key
                            }
                        } label: {
                            VStack(spacing: 2) {
                                Text(hour.time).font(.system(size: 13))
                                if let icon = hour.iconUrl {
                                    WeatherIcon(url: icon).frame(width: 28, height: 28)
                                }
                                Text(hour.temperature).font(.system(size: 16, weight: .medium))
                                Text(hour.caption).font(.system(size: 11)).lineLimit(2).multilineTextAlignment(.center)
                            }
                            .frame(width: 64)
                            .padding(8)
                            .background(isSelected ? Color.white.opacity(0.08) : .clear, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .scaleEffect(isSelected ? 1.07 : 1)
                            .shadow(color: .black.opacity(isSelected ? 0.35 : 0), radius: 3, y: 1)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel([hour.time, hour.temperature, hour.caption].filter {
                            !$0.isEmpty
                        }
                                            .joined(separator: ", "))
                        .accessibilityAddTraits(isSelected ? .isSelected : [])
                    }
                }
                .padding(.horizontal, 4)
                .padding(.vertical, 6)
            }
        }
        .foregroundStyle(ink)
        .homeCard()
    }
}
