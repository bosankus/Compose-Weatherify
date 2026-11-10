import SwiftUI
import MapKit
@preconcurrency import ComposeApp

/// Dark street map behind the places page, centred on the place Home is showing. Same map as the
/// Android `PlacesMapBackdrop`: Esri's World Dark Gray Canvas tiles (drawn by MapKit in place of
/// Apple's own map), a pin per other saved place, and a pulsing pin on the shown place.
///
/// Loads gracefully: a flat map-coloured fill shows at once, then the map fades in and settles
/// from a slight zoom once most tiles have arrived (or after a short wait, so a slow network
/// still gets a map). Moving to another place crossfades to the new map. It only draws, so swipes and
/// taps go to the page above it.
struct PlacesMapBackdrop: View {
    let map: PlacesMapUi
    /// Runs the shown place's pulse; off while the map is hidden so it asks for no frames.
    let pulsing: Bool

    var body: some View {
        ZStack {
            Color(uiColor: PlacesMapStyle.baseFill)
            PlacesMapView(map: map, pulsing: pulsing)
                .id(centerKey)
                .transition(.opacity)
            LinearGradient(
                stops: [
                    .init(color: .black.opacity(0.25), location: 0),
                    .init(color: .clear, location: 0.45),
                    .init(color: .black.opacity(0.55), location: 1),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        }
        .animation(.easeInOut(duration: 0.6), value: centerKey)
        .ignoresSafeArea()
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    /// A new place means a new map, so the old one can crossfade out.
    private var centerKey: String {
        "\(map.centerLat),\(map.centerLon)"
    }
}

private enum PlacesMapStyle {
    /// Dark Gray Canvas's own land colour, so the gaps before tiles arrive don't flash.
    static let baseFill = UIColor(red: 0x24 / 255, green: 0x24 / 255, blue: 0x28 / 255, alpha: 1)
    static let yellow = UIColor(red: 0xF8 / 255, green: 0xBF / 255, blue: 0x1D / 255, alpha: 1)
    static let startScale: CGFloat = 1.08
    static let settleSeconds = 1.4
    static let revealTimeout = 1.5
    /// Reveal once this share of tiles is in; the rest appear as they arrive.
    static let revealTileShare = 0.8
    /// Esri's Dark Gray Canvas has no tiles past this level.
    static let maxTileZoom = 16
}

private struct PlacesMapView: UIViewRepresentable {
    let map: PlacesMapUi
    let pulsing: Bool

    func makeUIView(context: Context) -> PlacesMapContainer {
        PlacesMapContainer()
    }

    func updateUIView(_ view: PlacesMapContainer, context: Context) {
        view.update(map: map, pulsing: pulsing)
    }
}

/// Holds the MKMapView so the reveal can scale it about the shown place, not the middle.
private final class PlacesMapContainer: UIView, MKMapViewDelegate {
    private let mapView = MKMapView()
    /// Hides the map until it is ready. The map itself stays opaque: MapKit doesn't draw a view
    /// at alpha 0, and tiles that land then are never drawn later.
    private let veil = UIView()
    private var placeCenter = CLLocationCoordinate2D()
    private var zoom = 11
    private var focusY: CGFloat = 0.3
    private var tileTemplate: String?
    private var pinKeys: [String] = []
    private var pulsing = false
    private var revealed = false
    private var loadedTiles = 0
    /// What the map was last positioned for. Re-setting the visible rect restarts MapKit's tile
    /// drawing, and SwiftUI lays this view out far more often than any of these change.
    private var positionedFor: (size: CGSize, center: CLLocationCoordinate2D, zoom: Int, focusY: CGFloat)?
    private let shownPin = PlacePinAnnotation(key: "", isShown: true)

    init() {
        super.init(frame: .zero)
        backgroundColor = PlacesMapStyle.baseFill
        clipsToBounds = true
        isUserInteractionEnabled = false

        mapView.delegate = self
        mapView.isUserInteractionEnabled = false
        mapView.showsCompass = false
        mapView.showsScale = false
        mapView.showsBuildings = false
        mapView.pointOfInterestFilter = .excludingAll
        mapView.overrideUserInterfaceStyle = .dark
        mapView.backgroundColor = PlacesMapStyle.baseFill
        if !UIAccessibility.isReduceMotionEnabled {
            mapView.transform = CGAffineTransform(scaleX: PlacesMapStyle.startScale, y: PlacesMapStyle.startScale)
        }
        addSubview(mapView)
        mapView.addAnnotation(shownPin)
        veil.backgroundColor = PlacesMapStyle.baseFill
        veil.frame = bounds
        veil.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        addSubview(veil)

        DispatchQueue.main.asyncAfter(deadline: .now() + PlacesMapStyle.revealTimeout) { [weak self] in
            self?.reveal()
        }
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    func update(map: PlacesMapUi, pulsing: Bool) {
        if tileTemplate != map.tileUrlTemplate {
            mapView.removeOverlays(mapView.overlays)
            let overlay = CountingTileOverlay(urlTemplate: map.tileUrlTemplate)
            overlay.onTileLoaded = { [weak self] in
                self?.tileLoaded()
            }
            overlay.canReplaceMapContent = true
            // Left at the default 256: MapKit already fetches one zoom level deeper on Retina
            // screens and draws each 256px tile at 128pt, the same tiles the Android map asks for.
            overlay.maximumZ = PlacesMapStyle.maxTileZoom
            mapView.addOverlay(overlay, level: .aboveLabels)
            tileTemplate = map.tileUrlTemplate
        }

        let newCenter = CLLocationCoordinate2D(latitude: map.centerLat, longitude: map.centerLon)
        let newZoom = Int(map.zoom)
        let newFocus = CGFloat(map.focusY)
        if newCenter.latitude != placeCenter.latitude || newCenter.longitude != placeCenter.longitude
               || newZoom != zoom || newFocus != focusY {
            placeCenter = newCenter
            zoom = newZoom
            focusY = newFocus
            shownPin.coordinate = newCenter
            setNeedsLayout()
        }

        let keys = map.pins.map(\.key)
        if keys != pinKeys {
            mapView.removeAnnotations(mapView.annotations.filter {
                $0 !== shownPin
            })
            mapView.addAnnotations(map.pins.map { pin in
                let annotation = PlacePinAnnotation(key: pin.key, isShown: false)
                annotation.coordinate = CLLocationCoordinate2D(latitude: pin.lat, longitude: pin.lon)
                return annotation
            })
            pinKeys = keys
        }

        self.pulsing = pulsing
        (mapView.view(for: shownPin) as? ShownPinView)?.setPulsing(pulsing)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        guard bounds.width > 0, bounds.height > 0 else {
            return
        }
        if let last = positionedFor, last.size == bounds.size, last.zoom == zoom, last.focusY == focusY,
           last.center.latitude == placeCenter.latitude, last.center.longitude == placeCenter.longitude {
            return
        }
        positionedFor = (bounds.size, placeCenter, zoom, focusY)
        // Frame is undefined under the reveal's transform, so size by bounds and position. The
        // anchor sits on the shown place so the settle zooms towards it.
        mapView.layer.anchorPoint = CGPoint(x: 0.5, y: focusY)
        mapView.bounds = CGRect(origin: .zero, size: bounds.size)
        mapView.center = CGPoint(x: bounds.midX, y: bounds.height * focusY)
        mapView.setVisibleMapRect(visibleRect(for: bounds.size), animated: false)
    }

    /// The map rect that puts [placeCenter] at (width / 2, height * focusY) at [zoom], where one
    /// screen point is one unit of a 256pt slippy-map tile.
    private func visibleRect(for size: CGSize) -> MKMapRect {
        let mapPointsPerPoint = MKMapSize.world.width / (256 * pow(2, Double(zoom)))
        let width = Double(size.width) * mapPointsPerPoint
        let height = Double(size.height) * mapPointsPerPoint
        let point = MKMapPoint(placeCenter)
        return MKMapRect(x: point.x - width / 2, y: point.y - height * Double(focusY), width: width, height: height)
    }

    /// MapKit calls the map rendered before overlay tiles arrive, so tiles are counted instead.
    private func tileLoaded() {
        loadedTiles += 1
        // Tiles are 128pt on screen; count the grid that covers the view, a partial tile each side.
        let columns = (bounds.width / 128).rounded(.up) + 1
        let rows = (bounds.height / 128).rounded(.up) + 1
        if Double(loadedTiles) >= Double(columns * rows) * PlacesMapStyle.revealTileShare {
            reveal()
        }
    }

    private func reveal() {
        guard !revealed else {
            return
        }
        revealed = true
        // FastOutSlowIn, like the Android settle.
        let animator = UIViewPropertyAnimator(
            duration: PlacesMapStyle.settleSeconds,
            controlPoint1: CGPoint(x: 0.4, y: 0),
            controlPoint2: CGPoint(x: 0.2, y: 1)
        ) { [mapView, veil] in
            veil.alpha = 0
            mapView.transform = .identity
        }
        animator.startAnimation()
    }

    // MARK: MKMapViewDelegate

    func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
        if let tiles = overlay as? MKTileOverlay {
            return MKTileOverlayRenderer(tileOverlay: tiles)
        }
        return MKOverlayRenderer(overlay: overlay)
    }

    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        guard let pin = annotation as? PlacePinAnnotation else {
            return nil
        }
        if pin.isShown {
            let view = ShownPinView(annotation: pin, reuseIdentifier: nil)
            view.setPulsing(pulsing)
            return view
        }
        return SavedPinView(annotation: pin, reuseIdentifier: nil)
    }
}

/// Reports each tile that loads, on the main thread.
private final class CountingTileOverlay: MKTileOverlay {
    var onTileLoaded: (() -> Void)?

    override func loadTile(at path: MKTileOverlayPath, result: @escaping (Data?, Error?) -> Void) {
        super.loadTile(at: path) { [weak self] data, error in
            result(data, error)
            guard data != nil else {
                return
            }
            DispatchQueue.main.async {
                self?.onTileLoaded?()
            }
        }
    }
}

private final class PlacePinAnnotation: NSObject, MKAnnotation {
    let key: String
    let isShown: Bool
    @objc dynamic var coordinate = CLLocationCoordinate2D()

    init(key: String, isShown: Bool) {
        self.key = key
        self.isShown = isShown
    }
}

/// A saved place within view: small yellow dot with a white ring.
private final class SavedPinView: MKAnnotationView {
    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        let size: CGFloat = 14
        frame = CGRect(x: 0, y: 0, width: size, height: size)
        layer.cornerRadius = size / 2
        layer.borderWidth = 2
        layer.borderColor = UIColor.white.withAlphaComponent(0.85).cgColor
        backgroundColor = PlacesMapStyle.yellow.withAlphaComponent(0.85)
        canShowCallout = false
        collisionMode = .circle
        displayPriority = .required
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }
}

/// The place Home is showing: a larger dot with a ring that pulses outwards while visible.
private final class ShownPinView: MKAnnotationView {
    private let dot = CALayer()
    private let pulse = CALayer()
    private static let pulseKey = "pulse"

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        let pulseSize: CGFloat = 56
        let dotSize: CGFloat = 18
        frame = CGRect(x: 0, y: 0, width: pulseSize, height: pulseSize)
        backgroundColor = .clear
        canShowCallout = false
        displayPriority = .required
        zPriority = .max

        pulse.frame = bounds
        pulse.cornerRadius = pulseSize / 2
        pulse.backgroundColor = PlacesMapStyle.yellow.withAlphaComponent(0.45).cgColor
        pulse.opacity = 0
        layer.addSublayer(pulse)

        dot.frame = CGRect(x: (pulseSize - dotSize) / 2, y: (pulseSize - dotSize) / 2, width: dotSize, height: dotSize)
        dot.cornerRadius = dotSize / 2
        dot.borderWidth = 3
        dot.borderColor = UIColor.white.cgColor
        dot.backgroundColor = PlacesMapStyle.yellow.cgColor
        layer.addSublayer(dot)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    func setPulsing(_ on: Bool) {
        let running = pulse.animation(forKey: Self.pulseKey) != nil
        let wanted = on && !UIAccessibility.isReduceMotionEnabled
        guard running != wanted else {
            return
        }
        guard wanted else {
            pulse.removeAnimation(forKey: Self.pulseKey)
            return
        }
        let scale = CABasicAnimation(keyPath: "transform.scale")
        scale.fromValue = 0.3
        scale.toValue = 1
        let fade = CABasicAnimation(keyPath: "opacity")
        fade.fromValue = 1
        fade.toValue = 0
        let group = CAAnimationGroup()
        group.animations = [scale, fade]
        group.duration = 2
        group.timingFunction = CAMediaTimingFunction(name: .linear)
        group.repeatCount = .infinity
        group.isRemovedOnCompletion = false
        pulse.add(group, forKey: Self.pulseKey)
    }
}
