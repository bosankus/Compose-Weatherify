import SwiftUI
import UIKit

/// Per-pixel ink that follows the photo while the page scrolls (iOS 17+).
///
/// Pages draw their text in `AdaptiveInk.sentinel` (pure white). Sections wrapped in `adaptiveInk()`
/// run a color shader that swaps every sentinel pixel for light or dark ink, chosen from the
/// backdrop luminance right behind that pixel. The lookup uses the layer's live screen position,
/// so as text scrolls from the bright sky onto the dark sea its color wipes across, glyph by glyph.
enum AdaptiveInk {
    /// Never used as a visible color: the shader replaces it. Real whites in the UI stay just under it.
    static let sentinel = Color.white

    /// Backdrop luminance where light and dark ink read about equally well. Lower than the 0.45
    /// header threshold because per-pixel decisions no longer need to cover a whole block.
    static let crossover: Float = 0.32

    static var isSupported: Bool {
        if #available(iOS 17.0, *) {
            return true
        }
        return false
    }
}

/// The composited backdrop (photo, then both scrims) as a coarse luminance grid in screen space.
struct LuminanceMap: Equatable, Sendable {
    static let columns = 12
    static let rows = 28

    /// Row-major, top row first, linear relative luminance 0...1.
    var values: [Float]
    /// The backdrop size the grid was laid out for (the full window).
    var canvas: CGSize
}

private struct BackdropLuminanceKey: EnvironmentKey {
    static let defaultValue: LuminanceMap? = nil
}

extension EnvironmentValues {
    /// Set only while sentinel ink is in use; nil means pages already have a real ink color.
    var backdropLuminance: LuminanceMap? {
        get {
            self[BackdropLuminanceKey.self]
        }
        set {
            self[BackdropLuminanceKey.self] = newValue
        }
    }
}

private struct AdaptiveInkModifier: ViewModifier {
    /// Opacity of a black surface between the backdrop and the text, e.g. a card.
    let surface: Double
    @Environment(\.backdropLuminance) private var map

    func body(content: Content) -> some View {
        if #available(iOS 17.0, *), let map {
            content.visualEffect { content, proxy in
                content.colorEffect(
                    ShaderLibrary.adaptiveInk(
                        .float2(proxy.frame(in: .global).origin),
                        .float2(map.canvas),
                        .floatArray(map.values),
                        .float(Float(LuminanceMap.columns)),
                        .float(Float(LuminanceMap.rows)),
                        .color(HomeTheme.inkOnDark),
                        .color(HomeTheme.inkOnLight),
                        .float(Float(surface)),
                        .float(AdaptiveInk.crossover)
                    )
                )
            }
        } else {
            content
        }
    }
}

extension View {
    /// Resolves sentinel ink in this view against the backdrop behind it. Inner calls win, so a card
    /// can resolve its own text (with its fill as `surface`) before the section around it does.
    func adaptiveInk(surface: Double = 0) -> some View {
        modifier(AdaptiveInkModifier(surface: surface))
    }

    /// Keeps photos and icons out of the ink swap by nudging their pure whites just below the sentinel.
    func inkShield() -> some View {
        brightness(-0.02)
    }
}

extension LuminanceMap {
    /// Lays the photo out like `scaledToFill` in `canvas`, then composites the scrims on top.
    static func make(photo: UIImage, canvas: CGSize) -> LuminanceMap? {
        guard let cgImage = photo.cgImage, canvas.width > 0, canvas.height > 0 else {
            return nil
        }
        var pixels = [UInt8](repeating: 0, count: columns * rows * 4)
        guard let context = CGContext(
            data: &pixels, width: columns, height: rows, bitsPerComponent: 8, bytesPerRow: columns * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        )
        else {
            return nil
        }
        // The grid stretches the canvas into columns x rows, so fill in canvas units, then scale.
        let imageSize = CGSize(width: cgImage.width, height: cgImage.height)
        let fill = max(canvas.width / imageSize.width, canvas.height / imageSize.height)
        let drawn = CGSize(width: imageSize.width * fill, height: imageSize.height * fill)
        let sx = CGFloat(columns) / canvas.width, sy = CGFloat(rows) / canvas.height
        context.interpolationQuality = .medium
        context.draw(cgImage, in: CGRect(
            x: (canvas.width - drawn.width) / 2 * sx,
            y: (canvas.height - drawn.height) / 2 * sy,
            width: drawn.width * sx,
            height: drawn.height * sy
        ))
        return composite { row, column in
            // Memory order: row 0 is the top of the image.
            let index = (row * columns + column) * 4
            return (Double(pixels[index]) / 255, Double(pixels[index + 1]) / 255, Double(pixels[index + 2]) / 255)
        }
        .map {
            LuminanceMap(values: $0, canvas: canvas)
        }
    }

    /// Before a photo arrives: the condition gradient, top to bottom.
    static func make(top: UIColor, bottom: UIColor, canvas: CGSize) -> LuminanceMap? {
        guard canvas.width > 0, canvas.height > 0 else {
            return nil
        }
        var t = (r: CGFloat(0), g: CGFloat(0), b: CGFloat(0), a: CGFloat(0))
        var b = t
        top.getRed(&t.r, green: &t.g, blue: &t.b, alpha: &t.a)
        bottom.getRed(&b.r, green: &b.g, blue: &b.b, alpha: &b.a)
        return composite { row, _ in
            let y = (CGFloat(row) + 0.5) / CGFloat(rows)
            return (Double(t.r + (b.r - t.r) * y), Double(t.g + (b.g - t.g) * y), Double(t.b + (b.b - t.b) * y))
        }
        .map {
            LuminanceMap(values: $0, canvas: canvas)
        }
    }

    /// Blends both scrims over each cell (in encoded sRGB, like SwiftUI) and keeps the luminance.
    private static func composite(_ color: (Int, Int) -> (Double, Double, Double)) -> [Float]? {
        var values = [Float]()
        values.reserveCapacity(columns * rows)
        for row in 0..<rows {
            let y = (Double(row) + 0.5) / Double(rows)
            let layers = [(BackdropScrims.top, BackdropScrims.topAlpha(at: y)), (BackdropScrims.bottom, BackdropScrims.bottomAlpha(at: y))]
            for column in 0..<columns {
                var (r, g, b) = color(row, column)
                for (scrim, alpha) in layers where alpha > 0 {
                    r += (scrim.0 - r) * alpha
                    g += (scrim.1 - g) * alpha
                    b += (scrim.2 - b) * alpha
                }
                values.append(Float(srgbLuminance(r, g, b)))
            }
        }
        return values
    }
}
