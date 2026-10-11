import SwiftUI
import ComposeApp

/// SwiftUI side of the shared LoginScreenController.
@MainActor
final class LoginModel: ObservableObject {
    let controller: LoginScreenController
    @Published private(set) var ui: LoginScreenUi?
    /// A server error or an expired session. The form shows it inline; otherwise it is a banner.
    @Published var notice: SettingsNotice?

    init(session: AppSession) {
        controller = session.loginController()
    }

    deinit {
        let controller = controller
        DispatchQueue.main.async {
            controller.close()
        }
    }

    func run() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { @MainActor in
                for await frame in self.controller.state {
                    self.ui = frame
                }
            }
            group.addTask { @MainActor in
                for await event in self.controller.events {
                    self.notice = SettingsNotice(title: "Weatherify", message: event.message, isError: !event.isWarning)
                }
            }
        }
    }
}

enum LoginMode: String, Identifiable {
    case signIn, register
    var id: String {
        rawValue
    }
}

/// Welcome screen in the style of big consumer apps: today's forest full-bleed, a small
/// wordmark, one calm headline, and two buttons. The form itself lives in a sheet.
///
/// [handingOff] is set right after a successful sign-in: the sheet steps away, the copy and
/// buttons fade, and the leaf with a short welcome rests on the forest until Home takes over.
struct LoginView: View {
    let handingOff: Bool

    @StateObject private var model: LoginModel
    @State private var mode: LoginMode?
    @State private var web: WebLink?
    @State private var appeared = false

    init(session: AppSession, handingOff: Bool = false) {
        self.handingOff = handingOff
        _model = StateObject(wrappedValue: LoginModel(session: session))
    }

    var body: some View {
        ZStack {
            ForestBackdrop(photo: model.ui?.photo, onShown: model.controller.photoShown)
            if let ui = model.ui {
                welcome(ui)
                    .opacity(handingOff ? 0 : 1)
                    .blur(radius: handingOff ? 6 : 0)
                    .allowsHitTesting(!handingOff)
                if handingOff {
                    WelcomeMoment(text: ui.strings.welcome)
                        .transition(.opacity.combined(with: .scale(scale: 0.92)))
                }
            }
            if mode == nil && !handingOff {
                VStack {
                    NoticeBanner(notice: $model.notice)
                    Spacer()
                }
            }
        }
        .environment(\.colorScheme, .dark)
        .animation(.easeInOut(duration: 0.3), value: handingOff)
        .successHaptic(trigger: handingOff)
        .onChange(of: handingOff) { handing in
            if handing {
                mode = nil
            }
        }
        .task {
            await model.run()
        }
        .sheet(item: $mode) { mode in
            if let ui = model.ui {
                LoginFormSheet(ui: ui, model: model, initialMode: mode)
            }
        }
        .sheet(item: $web) { link in
            SafariView(url: link.url).ignoresSafeArea()
        }
        .environment(\.openURL, OpenURLAction { url in
            web = WebLink(url: url)
            return .handled
        })
    }

    private func welcome(_ ui: LoginScreenUi) -> some View {
        let strings = ui.strings
        return VStack(alignment: .leading, spacing: 0) {
            // One row: the leaf wordmark at the start, the photo credit at the end. The credit's
            // first line sits on the wordmark's baseline; a second line hangs below it.
            HStack(alignment: .firstTextBaseline, spacing: 16) {
                HStack(spacing: 8) {
                    Image(systemName: "leaf.fill").font(.system(size: 15, weight: .semibold))
                    Text("Weatherify").font(.system(size: 17, weight: .semibold, design: .rounded))
                }
                .foregroundStyle(.white.opacity(0.92))
                .fixedSize()
                Spacer(minLength: 0)
                if let photo = ui.photo {
                    PhotoCreditText(photo: photo)
                        .transition(.opacity)
                }
            }
            .padding(.top, 8)

            Spacer()

            VStack(alignment: .leading, spacing: 12) {
                Text(strings.headline)
                    .font(.system(size: 38, weight: .regular, design: .serif))
                    .foregroundStyle(.white)
                    .fixedSize(horizontal: false, vertical: true)
                Text(strings.tagline)
                    .font(.system(size: 16))
                    .foregroundStyle(.white.opacity(0.78))
            }
            .padding(.bottom, 32)
            .opacity(appeared ? 1 : 0)
            .offset(y: appeared ? 0 : 16)

            VStack(spacing: 12) {
                Button {
                    mode = .signIn
                } label: {
                    Text(strings.signInMode)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Color(hex: 0x10241A))
                        .frame(maxWidth: .infinity, minHeight: 54)
                        .background(.white, in: Capsule())
                }
                .buttonStyle(SoftPress())
                Button {
                    mode = .register
                } label: {
                    Text(strings.registerMode)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity, minHeight: 54)
                        .softGlass(in: Capsule())
                }
                .buttonStyle(SoftPress())
            }
            .opacity(appeared ? 1 : 0)
            .offset(y: appeared ? 0 : 24)

            AgreementText(strings: strings)
                .padding(.top, 18)
                .frame(maxWidth: .infinity)

        }
        .padding(.horizontal, 24)
        .padding(.bottom, 12)
        .animation(.easeInOut(duration: 0.6), value: ui.photo?.imageUrl)
        .onAppear {
            withAnimation(.easeOut(duration: 0.9).delay(0.15)) {
                appeared = true
            }
        }
    }
}

// MARK: - Form sheet

private enum LoginField: Hashable {
    case email, password
}

/// The sign-in or sign-up form, in a sheet over the forest: title, two quiet fields, one
/// button, and a link to switch modes. AutoFill fills or suggests passwords.
private struct LoginFormSheet: View {
    let ui: LoginScreenUi
    @ObservedObject var model: LoginModel

    @State private var mode: LoginMode
    @State private var email = ""
    @State private var password = ""
    @State private var showPassword = false
    @State private var error: String?
    @State private var shakes = 0
    @FocusState private var focus: LoginField?

    init(ui: LoginScreenUi, model: LoginModel, initialMode: LoginMode) {
        self.ui = ui
        self.model = model
        _mode = State(initialValue: initialMode)
    }

    private var strings: LoginStrings {
        ui.strings
    }
    private var registering: Bool {
        mode == .register
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(registering ? strings.registerTitle : strings.signInTitle)
                        .font(.system(size: 30, weight: .regular, design: .serif))
                    Text(registering ? strings.registerSubtitle : strings.signInSubtitle)
                        .font(.system(size: 15))
                        .foregroundStyle(.white.opacity(0.65))
                }
                .foregroundStyle(.white)
                .id(mode)
                .transition(.opacity)

                VStack(spacing: 12) {
                    QuietField(symbol: "envelope") {
                        TextField("", text: $email, prompt: prompt(strings.email))
                            .textContentType(.username)
                            .keyboardType(.emailAddress)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .focused($focus, equals: .email)
                            .submitLabel(.next)
                            .onSubmit {
                                focus = .password
                            }
                    }
                    QuietField(symbol: "lock") {
                        Group {
                            if showPassword {
                                TextField("", text: $password, prompt: prompt(strings.password))
                            } else {
                                SecureField("", text: $password, prompt: prompt(strings.password))
                            }
                        }
                        .textContentType(registering ? .newPassword : .password)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($focus, equals: .password)
                        .submitLabel(.go)
                        .onSubmit(submit)
                        Button {
                            showPassword.toggle()
                        } label: {
                            Text(showPassword ? strings.hidePassword : strings.showPassword)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.7))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .modifier(Shake(travel: CGFloat(shakes)))

                if let error {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: 0xFFB4A9))
                        .transition(.opacity)
                }

                Button(action: submit) {
                    ZStack {
                        Text(registering ? strings.registerMode : strings.submit)
                            .font(.system(size: 17, weight: .semibold))
                            .opacity(ui.isLoading ? 0 : 1)
                        if ui.isLoading {
                            ProgressView().tint(Color(hex: 0x10241A))
                        }
                    }
                    .foregroundStyle(Color(hex: 0x10241A))
                    .frame(maxWidth: .infinity, minHeight: 54)
                    .background(.white, in: Capsule())
                }
                .buttonStyle(SoftPress())
                .disabled(ui.isLoading)

                Button {
                    withAnimation(.easeInOut(duration: 0.25)) {
                        mode = registering ? .signIn : .register
                        error = nil
                    }
                } label: {
                    Text(registering ? strings.toggleToLogin : strings.toggleToRegister)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(.white.opacity(0.8))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 24)
            .padding(.top, 32)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.interactively)
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .sheetBackground()
        .animation(.easeInOut(duration: 0.2), value: error)
        .onAppear {
            focus = .email
        }
        .onChange(of: model.notice) { notice in
            // A server answer arrives while the form is open: show it here, not behind the sheet.
            guard let notice else {
                return
            }
            error = notice.message
            model.notice = nil
            withAnimation(.linear(duration: 0.4)) {
                shakes += 1
            }
        }
    }

    private func prompt(_ text: String) -> Text {
        Text(text).foregroundColor(.white.opacity(0.45))
    }

    private func submit() {
        Task {
            if let message = try? await model.controller.validate(email: email, password: password) {
                error = message
                withAnimation(.linear(duration: 0.4)) {
                    shakes += 1
                }
                return
            }
            error = nil
            focus = nil
            model.controller.submit(email: email, password: password, register: registering)
        }
    }
}

/// Underline-free, low-contrast field: a faint fill and a glyph.
private struct QuietField<Content: View>: View {
    let symbol: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 15))
                .foregroundStyle(.white.opacity(0.5))
                .frame(width: 20)
            content()
                .foregroundStyle(.white)
                .tint(.white)
        }
        .padding(.horizontal, 18)
        .frame(height: 56)
        .background(.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

// MARK: - Backdrop

/// Today's forest, slowly drifting closer. Until it loads, or when offline, a deep green
/// gradient with a soft sage glow.
private struct ForestBackdrop: View {
    let photo: LoginPhotoUi?
    let onShown: () -> Void

    @State private var image: UIImage?
    @State private var drift = false

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                LinearGradient(
                    colors: [Color(hex: 0x2C4A3A), Color(hex: 0x173026), Color(hex: 0x0C1712)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                RadialGradient(
                    colors: [Color(hex: 0x9CC5A1).opacity(0.28), .clear],
                    center: .init(x: 0.75, y: 0.2),
                    startRadius: 0,
                    endRadius: proxy.size.width
                )
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(width: proxy.size.width, height: proxy.size.height)
                        .scaleEffect(drift ? 1.08 : 1.0)
                        .clipped()
                        .transition(.opacity)
                }
                // Soft at the top for the wordmark, deep at the bottom for the copy and buttons.
                LinearGradient(
                    stops: [
                        .init(color: .black.opacity(0.35), location: 0),
                        .init(color: .clear, location: 0.22),
                        .init(color: .clear, location: 0.42),
                        .init(color: .black.opacity(0.55), location: 0.68),
                        .init(color: .black.opacity(0.85), location: 1),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
        }
        .ignoresSafeArea()
        .animation(.easeInOut(duration: 1.2), value: image)
        .task(id: photo?.imageUrl) {
            guard let url = photo.flatMap({ URL(string: $0.imageUrl) }) else {
                return
            }
            guard let loaded = await RemoteImage.load(url, retries: 2) else {
                return
            }
            image = loaded
            onShown()
            withAnimation(.easeInOut(duration: 30).repeatForever(autoreverses: true)) {
                drift = true
            }
        }
    }
}

// MARK: - Small pieces

/// The leaf and a one-word welcome, resting on the forest for a moment after signing in.
private struct WelcomeMoment: View {
    let text: String
    @State private var bloom = false

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: "leaf.fill")
                .font(.system(size: 44, weight: .regular))
                .foregroundStyle(.white.opacity(0.95))
                .scaleEffect(bloom ? 1 : 0.8)
                .rotationEffect(.degrees(bloom ? 0 : -12))
            Text(text)
                .font(.system(size: 30, weight: .regular, design: .serif))
                .foregroundStyle(.white)
                .opacity(bloom ? 1 : 0)
                .offset(y: bloom ? 0 : 8)
        }
        .shadow(color: .black.opacity(0.35), radius: 16, y: 6)
        .onAppear {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.72).delay(0.05)) {
                bloom = true
            }
        }
        .accessibilityElement(children: .combine)
    }
}

private struct AgreementText: View {
    let strings: LoginStrings

    var body: some View {
        Text(attributed)
            .font(.system(size: 12))
            // Room between lines when the links wrap, so they don't feel cramped.
            .lineSpacing(6)
            .foregroundStyle(.white.opacity(0.6))
            .tint(.white.opacity(0.85))
            .multilineTextAlignment(.center)
    }

    private var attributed: AttributedString {
        var text = AttributedString(strings.agreement)
        for (label, link) in [(strings.termsLink, strings.termsUrl), (strings.privacyLink, strings.privacyUrl)] {
            if let range = text.range(of: label), let url = URL(string: link) {
                text[range].link = url
                text[range].underlineStyle = .single
            }
        }
        return text
    }
}

/// Unsplash's required credit, linking the photographer and Unsplash: small, faint and right-aligned
/// at the top end, at most two lines.
private struct PhotoCreditText: View {
    let photo: LoginPhotoUi

    var body: some View {
        Text(attributed)
            .font(.system(size: 11))
            .foregroundStyle(.white.opacity(0.42))
            .tint(.white.opacity(0.55))
            .multilineTextAlignment(.trailing)
            .lineLimit(2)
            .frame(maxWidth: 170, alignment: .trailing)
            // Lightens what is under it rather than sitting on top, so it reads as part of the photo.
            .blendMode(.plusLighter)
    }

    private var attributed: AttributedString {
        var text = AttributedString(photo.credit)
        if let range = text.range(of: "Unsplash"), let url = URL(string: photo.unsplashUrl) {
            text[range].link = url
        }
        if let url = URL(string: photo.profileUrl),
           let start = text.range(of: "by ")?.upperBound,
           let end = text.range(of: " on")?.lowerBound,
           start < end {
            text[start..<end].link = url
        }
        return text
    }
}

private struct Shake: GeometryEffect {
    var travel: CGFloat
    var animatableData: CGFloat {
        get {
            travel
        }
        set {
            travel = newValue
        }
    }

    func effectValue(size: CGSize) -> ProjectionTransform {
        ProjectionTransform(CGAffineTransform(translationX: 8 * sin(travel * .pi * 6), y: 0))
    }
}

private struct SoftPress: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .opacity(configuration.isPressed ? 0.9 : 1)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

private struct WebLink: Identifiable {
    let url: URL
    var id: String {
        url.absoluteString
    }
}

private extension View {
    /// Liquid Glass on iOS 26, frosted material before.
    @ViewBuilder
    func softGlass<S: Shape>(in shape: S) -> some View {
        if #available(iOS 26.0, *) {
            glassEffect(.regular.tint(.white.opacity(0.08)).interactive(), in: shape)
        } else {
            background(.ultraThinMaterial, in: shape)
                .overlay(shape.stroke(.white.opacity(0.25), lineWidth: 1))
        }
    }

    /// A soft success tap when signing in completes (iOS 17+).
    @ViewBuilder
    func successHaptic(trigger: Bool) -> some View {
        if #available(iOS 17.0, *) {
            sensoryFeedback(.success, trigger: trigger) { _, new in
                new
            }
        } else {
            self
        }
    }

    /// Dark frosted sheet tinted forest green, so the photo stays faintly visible behind the form.
    /// Sheets don't inherit the presenter's color scheme, so it is set here too.
    @ViewBuilder
    func sheetBackground() -> some View {
        if #available(iOS 16.4, *) {
            environment(\.colorScheme, .dark)
            .presentationBackground {
                Color(hex: 0x0E1E16).opacity(0.78).background(.ultraThinMaterial)
            }
            .presentationCornerRadius(32)
        } else {
            environment(\.colorScheme, .dark)
                .background(Color(hex: 0x10241A).ignoresSafeArea())
        }
    }
}
