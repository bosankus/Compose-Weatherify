import SwiftUI
import UIKit
import ComposeApp

/// What the photo tells the rest of the screen: which ink to use and the corner tint.
struct BackdropSample: Equatable {
    var headerLuminance: Double
    /// The lower part of the photo with the bottom scrim already composited over it.
    var bodyLuminance: Double
    var averageColor: UIColor
    /// Screen-space luminance for per-pixel adaptive ink; nil until the canvas size is known.
    var map: LuminanceMap? = nil

    static let dark = BackdropSample(headerLuminance: 0, bodyLuminance: 0, averageColor: .black)

    /// Same 0.45 threshold as Android's `contentColorForLuminance`.
    var ink: Color {
        headerLuminance < 0.45 ? HomeTheme.inkOnDark : HomeTheme.inkOnLight
    }

    /// For everything under the header: it sits on the bottom scrim, not on the bright sky.
    var bodyInk: Color {
        bodyLuminance < 0.45 ? HomeTheme.inkOnDark : HomeTheme.inkOnLight
    }

    /// Android's `cornerGlowColor`: brighter tops get a slightly stronger soft dark.
    var cornerGlow: Color {
        let share = min(max((headerLuminance - 0.30) / (0.80 - 0.30), 0), 1)
        let alpha = 0.08 + (0.16 - 0.08) * share
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, opacity: CGFloat = 0
        averageColor.getRed(&red, green: &green, blue: &blue, alpha: &opacity)
        let keep = 1 - 0.42
        return Color(red: red * keep, green: green * keep, blue: blue * keep).opacity(alpha)
    }
}

/// Full-bleed background: the condition gradient first, then the Unsplash photo crossfading in,
/// with the same top and bottom scrims as Android so type stays readable.
struct BackdropView: View {
    let backdrop: BackdropUi?
    let onSample: (BackdropSample) -> Void
    let onPhotoShown: () -> Void

    @State private var image: UIImage?
    @State private var shownURL: String?

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                LinearGradient(
                    colors: SkyPalette.colors(for: backdrop?.condition ?? .clouds),
                    startPoint: .top,
                    endPoint: .bottom
                )
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(width: proxy.size.width, height: proxy.size.height)
                        .clipped()
                        .transition(.opacity)
                }
                LinearGradient(
                    stops: [
                        .init(color: BackdropScrims.topColor.opacity(BackdropScrims.topStartAlpha), location: 0),
                        .init(color: BackdropScrims.topColor.opacity(BackdropScrims.topMidAlpha), location: BackdropScrims.topMid),
                        .init(color: .clear, location: BackdropScrims.topEnd),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                LinearGradient(
                    stops: [
                        .init(color: .clear, location: BackdropScrims.bottomStart),
                        .init(color: BackdropScrims.bottomColor.opacity(BackdropScrims.bottomEndAlpha), location: 1),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .task(id: backdrop?.photo?.imageUrl) {
                await load(backdrop?.photo?.imageUrl, canvas: proxy.size)
            }
            .onChange(of: proxy.size) { canvas in
                // Rotation or a window resize moves where the bright parts land on screen.
                if let image {
                    onSample(image.backdropSample(canvas: canvas))
                } else {
                    onSample(SkyPalette.sample(for: backdrop?.condition ?? .clouds, canvas: canvas))
                }
            }
        }
        .ignoresSafeArea()
        .animation(.easeInOut(duration: 0.45), value: image)
        .animation(.easeInOut(duration: 0.45), value: backdrop?.condition)
    }

    private func load(_ url: String?, canvas: CGSize) async {
        guard let url, url != shownURL, let remote = URL(string: url) else {
            if url == nil {
                image = nil
                shownURL = nil
                onSample(SkyPalette.sample(for: backdrop?.condition ?? .clouds, canvas: canvas))
            }
            return
        }
        guard let loaded = await RemoteImage.load(remote, retries: 3) else {
            return
        }
        image = loaded
        shownURL = url
        onSample(loaded.backdropSample(canvas: canvas))
        onPhotoShown()
    }
}

/// Shared by the drawn scrims and the ink sampling so the two can't drift apart.
enum BackdropScrims {
    static let topColor = Color(hex: 0x101418)
    static let topStartAlpha = Double(0x55) / 255
    static let topMidAlpha = Double(0x22) / 255
    static let topMid = 0.22
    static let topEnd = 0.40
    static let bottomColor = Color(hex: 0x101014)
    static let bottomStart = 0.38
    static let bottomEndAlpha = Double(0xF0) / 255

    /// Encoded sRGB channels, for compositing the way SwiftUI blends.
    static let top = (Double(0x10) / 255, Double(0x14) / 255, Double(0x18) / 255)
    static let bottom = (Double(0x10) / 255, Double(0x10) / 255, Double(0x14) / 255)
    static let bottomLuminance = srgbLuminance(bottom.0, bottom.1, bottom.2)

    /// Top scrim opacity at a vertical position (0 top, 1 bottom).
    static func topAlpha(at y: Double) -> Double {
        if y <= topMid {
            return topStartAlpha + (topMidAlpha - topStartAlpha) * y / topMid
        }
        return y >= topEnd ? 0 : topMidAlpha * (topEnd - y) / (topEnd - topMid)
    }

    /// Bottom scrim opacity at a vertical position (0 top, 1 bottom).
    static func bottomAlpha(at y: Double) -> Double {
        y <= bottomStart ? 0 : bottomEndAlpha * (y - bottomStart) / (1 - bottomStart)
    }
}

/// The dark gradients from Android's SkyConditionBackground.
enum SkyPalette {
    static func colors(for condition: SkyCondition) -> [Color] {
        let ends = hexEnds(for: condition)
        return [Color(hex: ends.top), Color(hex: ends.bottom)]
    }

    /// Before a photo is sampled the ink follows the gradient's top.
    static func sample(for condition: SkyCondition, canvas: CGSize) -> BackdropSample {
        let ends = hexEnds(for: condition)
        let top = UIColor(Color(hex: ends.top))
        let bottom = UIColor(Color(hex: ends.bottom))
        return BackdropSample(
            headerLuminance: top.relativeLuminance,
            bodyLuminance: bottom.relativeLuminance,
            averageColor: bottom,
            map: LuminanceMap.make(top: top, bottom: bottom, canvas: canvas)
        )
    }

    private static func hexEnds(for condition: SkyCondition) -> (top: UInt32, bottom: UInt32) {
        switch condition {
        case .clear: (0x1E5A9A, 0x071018)
        case .clouds: (0x6A7682, 0x12161C)
        case .rain: (0x3C5870, 0x0B1218)
        case .fog: (0x8B949C, 0x1A1E24)
        case .storm: (0x3A3358, 0x0C0A14)
        case .snow: (0x7E92A6, 0x101820)
        case .night: (0x1A2440, 0x05060C)
        }
    }
}

/// Small in-memory image loader with retries; Unsplash hotlinks often miss on a cold start.
enum RemoteImage {
    private static let cache = NSCache<NSURL, UIImage>()

    /// For callers that skip the URL cache: also never write to it, so nothing lands on disk.
    /// The account photo relies on this; its signed URL changes on every fetch.
    private static let uncachedSession: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.urlCache = nil
        return URLSession(configuration: configuration)
    }()

    static func load(_ url: URL, retries: Int, cachePolicy: URLRequest.CachePolicy = .returnCacheDataElseLoad) async -> UIImage? {
        if let cached = cache.object(forKey: url as NSURL) {
            return cached
        }
        for attempt in 0...retries {
            if attempt > 0 {
                try? await Task.sleep(nanoseconds: 1_500_000_000)
            }
            if Task.isCancelled {
                return nil
            }
            let request = URLRequest(url: url, cachePolicy: cachePolicy)
            let session = cachePolicy == .reloadIgnoringLocalCacheData ? uncachedSession : URLSession.shared
            if let (data, _) = try? await session.data(for: request), let image = UIImage(data: data) {
                cache.setObject(image, forKey: url as NSURL)
                return image
            }
        }
        return nil
    }
}

private extension UIColor {
    var relativeLuminance: Double {
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        return srgbLuminance(Double(red), Double(green), Double(blue))
    }
}

/// WCAG relative luminance of encoded sRGB channels.
func srgbLuminance(_ red: Double, _ green: Double, _ blue: Double) -> Double {
    0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)
}

private func linear(_ channel: Double) -> Double {
    let value = Double(channel)
    return value <= 0.04045 ? value / 12.92 : pow((value + 0.055) / 1.055, 2.4)
}

private extension UIImage {
    /// Downsamples to a small grid: header luminance from the top 45%, body luminance from the
    /// band under it (where the details grid sits) after the bottom scrim, plus the average color.
    func backdropSample(canvas: CGSize) -> BackdropSample {
        let side = 24
        var pixels = [UInt8](repeating: 0, count: side * side * 4)
        guard let cgImage,
              let context = CGContext(
                  data: &pixels, width: side, height: side, bitsPerComponent: 8, bytesPerRow: side * 4,
                  space: CGColorSpaceCreateDeviceRGB(),
                  bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
              )
        else {
            return .dark
        }
        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: side, height: side))
        let headerRows = Int((Double(side) * 0.45).rounded(.up))
        let bodyRows = headerRows..<Int((Double(side) * 0.75).rounded(.up))
        var header = 0.0
        var body = 0.0
        var red = 0.0, green = 0.0, blue = 0.0
        for row in 0..<side {
            for column in 0..<side {
                let index = (row * side + column) * 4
                let r = Double(pixels[index]) / 255, g = Double(pixels[index + 1]) / 255, b = Double(pixels[index + 2]) / 255
                red += r; green += g; blue += b
                // CGContext rows start at the top of the image here (memory order).
                let luminance = 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
                if row < headerRows {
                    header += luminance
                } else if bodyRows.contains(row) {
                    let alpha = BackdropScrims.bottomAlpha(at: (Double(row) + 0.5) / Double(side))
                    body += luminance * (1 - alpha) + BackdropScrims.bottomLuminance * alpha
                }
            }
        }
        let count = Double(side * side)
        return BackdropSample(
            headerLuminance: header / Double(headerRows * side),
            bodyLuminance: body / Double(bodyRows.count * side),
            averageColor: UIColor(red: red / count, green: green / count, blue: blue / count, alpha: 1),
            map: LuminanceMap.make(photo: self, canvas: canvas)
        )
    }
}
