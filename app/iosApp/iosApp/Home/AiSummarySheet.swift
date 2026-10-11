import SwiftUI
import ComposeApp

/// Weather summary sheet. Draws whatever `AiSummaryUiState` says and nothing else: placeholder
/// lines, the text as it arrives, tip chips, or a retry. Same chrome as the other home sheets.
struct AiSummarySheet: View {
    let state: AiSummaryUiState
    let strings: AiSummaryStrings
    let onRetry: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                titleBar
                if let place = state.place, !place.isEmpty {
                    Text(strings.forPlace.replacingOccurrences(of: "{place}", with: place))
                        .font(.system(size: 15))
                        .foregroundStyle(HomeTheme.sheetMuted)
                }
                content
                if state.phase == .ready && !state.tips.isEmpty {
                    TipsBlock(tips: state.tips, heading: strings.tipsHeading)
                }
                Text(strings.privacy)
                    .font(.system(size: 13))
                    .foregroundStyle(HomeTheme.sheetMuted)
                    .padding(.top, 4)
            }
            .foregroundStyle(HomeTheme.inkOnDark)
            .padding(.horizontal, 20)
            .padding(.bottom, 20)
        }
        .presentationDetents([.medium, .large])
        .homeSheetStyle()
    }

    private var titleBar: some View {
        VStack(spacing: 12) {
            HStack {
                Text(strings.title).font(.system(size: 18, weight: .semibold))
                Spacer()
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .bold))
                        .frame(width: 36, height: 36)
                        .background(HomeTheme.sheetFieldFill, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(strings.close)
            }
            Rectangle().fill(HomeTheme.inkOnDark.opacity(0.08)).frame(height: 1)
        }
        .padding(.top, 20)
    }

    @ViewBuilder
    private var content: some View {
        switch state.phase {
        case .loading:
            LoadingLines(caption: strings.loading)
        case .ready:
            VStack(alignment: .leading, spacing: 10) {
                Text(state.text)
                    .font(.system(size: 17))
                    .lineSpacing(4)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if state.isStreaming {
                    HStack(spacing: 8) {
                        LeafSpinner(size: 22)
                        Text(strings.writing).font(.system(size: 13)).foregroundStyle(HomeTheme.sheetMuted)
                    }
                }
            }
        case .failed:
            VStack(alignment: .leading, spacing: 14) {
                Text(strings.failed).font(.system(size: 16))
                Button(action: onRetry) {
                    Text(strings.retry)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(HomeTheme.onYellow)
                        .frame(maxWidth: .infinity, minHeight: 52)
                        .background(HomeTheme.yellow, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// The leaf thinking, then placeholder lines with a light sweeping across while nothing has arrived.
private struct LoadingLines: View {
    let caption: String
    @State private var sweep = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                LeafSpinner(size: 48)
                Text(caption).font(.system(size: 14)).foregroundStyle(HomeTheme.sheetMuted)
            }
            ForEach([1.0, 0.92, 0.6], id: \.self) { width in
                GeometryReader { proxy in
                    let bar = proxy.size.width * width
                    RoundedRectangle(cornerRadius: 7, style: .continuous)
                        .fill(HomeTheme.sheetFieldFill)
                        .overlay(shimmer(width: bar))
                        .clipShape(RoundedRectangle(cornerRadius: 7, style: .continuous))
                        .frame(width: bar)
                }
                .frame(height: 14)
            }
        }
        .onAppear {
            sweep = true
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(caption)
    }

    private func shimmer(width: CGFloat) -> some View {
        LinearGradient(
            colors: [.clear, HomeTheme.yellow.opacity(0.28), .clear],
            startPoint: .leading,
            endPoint: .trailing
        )
            .frame(width: width * 0.45)
            .offset(x: reduceMotion ? 0 : (sweep ? width : -width * 0.45))
            .frame(maxWidth: .infinity, alignment: .leading)
            .animation(reduceMotion ? nil : .linear(duration: 1.4).repeatForever(autoreverses: false), value: sweep)
            .opacity(reduceMotion ? 0 : 1)
    }
}

/// The app's leaf being made: a multicolor comet circles it while the leaf draws its outline,
/// then its vein, blooms into colour, and fades to start again. Shown while the summary is written.
struct LeafSpinner: View {
    let size: CGFloat
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private static let spinSeconds = 2.2
    private static let cycleSeconds = 3.2
    /// Comet colours from a transparent tail to a bright head; the stops end at the arc's end (300/360).
    private static let ringStops: [Gradient.Stop] = [
        .init(color: Color(hex: 0x4285F4, opacity: 0), location: 0),
        .init(color: Color(hex: 0x4285F4), location: 0.15),
        .init(color: Color(hex: 0x9B72CB), location: 0.4),
        .init(color: Color(hex: 0xD96570), location: 0.6),
        .init(color: HomeTheme.yellow, location: 0.76),
        .init(color: Color(hex: 0x34A853), location: 0.83),
        .init(color: Color(hex: 0x34A853, opacity: 0), location: 1),
    ]

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            // Reduce Motion: hold a finished leaf, no orbit.
            let spin = reduceMotion ? 0 : (t / Self.spinSeconds).truncatingRemainder(dividingBy: 1)
            let cycle = reduceMotion ? 0.7 : (t / Self.cycleSeconds).truncatingRemainder(dividingBy: 1)
            ZStack {
                ring(spin: spin)
                leaf(cycle: cycle)
            }
            .frame(width: size, height: size)
        }
        .accessibilityHidden(true)
    }

    private func ring(spin: Double) -> some View {
        let stroke = size * 0.07
        let glow = stroke * 3
        let gradient = AngularGradient(gradient: Gradient(stops: Self.ringStops), center: .center)
        return ZStack {
            Circle().trim(from: 0, to: 300.0 / 360)
                .stroke(gradient, style: StrokeStyle(lineWidth: glow, lineCap: .round))
                .opacity(0.22)
            Circle().trim(from: 0, to: 300.0 / 360)
                .stroke(gradient, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
        }
        .padding(glow / 2)
        .rotationEffect(.degrees(spin * 360))
    }

    private func leaf(cycle: Double) -> some View {
        let outline = ease(phase(cycle, 0, 0.4))
        let vein = ease(phase(cycle, 0.22, 0.5))
        let bloom = ease(phase(cycle, 0.4, 0.58))
        let fade = 1 - phase(cycle, 0.88, 1)
        let sway = sin(cycle * 2 * 2 * .pi) * 6 * bloom
        let line = StrokeStyle(lineWidth: size * 0.05, lineCap: .round, lineJoin: .round)
        return ZStack {
            LeafBody().trim(from: 0, to: outline).stroke(HomeTheme.yellow, style: line)
            LeafBody().fill(HomeTheme.yellow).opacity(bloom)
            LeafVein().trim(from: 0, to: vein).stroke(HomeTheme.yellow, style: line)
            LeafVein().trim(from: 0, to: vein).stroke(HomeTheme.onYellow, style: line).opacity(bloom)
        }
        .frame(width: size * 0.56, height: size * 0.56)
        .scaleEffect(0.88 + 0.12 * bloom)
        .rotationEffect(.degrees(sway))
        .opacity(fade)
    }

    /// 0 before `from`, 1 after `to`, linear between.
    private func phase(_ t: Double, _ from: Double, _ to: Double) -> Double {
        min(max((t - from) / (to - from), 0), 1)
    }

    /// Smoothstep, close to the ease-in-out the Android leaf uses.
    private func ease(_ t: Double) -> Double {
        t * t * (3 - 2 * t)
    }
}

/// Leaf outline: base at the lower left, tip at the upper right. Same curves as the Android leaf.
private struct LeafBody: Shape {
    func path(in rect: CGRect) -> Path {
        let s = min(rect.width, rect.height)

        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
            CGPoint(x: rect.minX + x * s, y: rect.minY + y * s)
        }

        var path = Path()
        path.move(to: p(0.22, 0.78))
        path.addCurve(to: p(0.82, 0.18), control1: p(0.12, 0.38), control2: p(0.45, 0.15))
        path.addCurve(to: p(0.22, 0.78), control1: p(0.85, 0.55), control2: p(0.58, 0.88))
        path.closeSubpath()
        return path
    }
}

/// Stem and midrib, from outside the base up toward the tip.
private struct LeafVein: Shape {
    func path(in rect: CGRect) -> Path {
        let s = min(rect.width, rect.height)
        var path = Path()
        path.move(to: CGPoint(x: rect.minX + 0.1 * s, y: rect.minY + 0.9 * s))
        path.addLine(to: CGPoint(x: rect.minX + 0.62 * s, y: rect.minY + 0.38 * s))
        return path
    }
}

private struct TipsBlock: View {
    let tips: [AiSummaryTip]
    let heading: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(heading).font(.system(size: 16, weight: .medium))
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 8) {
                    chips
                }
                VStack(alignment: .leading, spacing: 8) {
                    chips
                }
            }
        }
    }

    @ViewBuilder
    private var chips: some View {
        ForEach(Array(tips.enumerated()), id: \.offset) { _, tip in
            HStack(spacing: 8) {
                Image(systemName: tip.kind.symbol).font(.system(size: 14)).foregroundStyle(HomeTheme.yellow)
                Text(tip.label).font(.system(size: 14)).lineLimit(1)
            }
            .padding(.leading, 10)
            .padding(.trailing, 14)
            .padding(.vertical, 8)
            .background(HomeTheme.sheetFieldFill, in: Capsule())
        }
    }
}

extension AiSummaryTipKind {
    var symbol: String {
        switch self {
        case .umbrella: "umbrella"
        case .sunscreen: "sun.max"
        case .wind: "wind"
        case .jacket: "tshirt"
        }
    }
}
