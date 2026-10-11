import SwiftUI
import PhotosUI
import SafariServices
import ComposeApp

/// Third page of the home shell: profile, premium, preferences, about, account and logout, as dark cards
/// over the same photo backdrop as the weather and places pages.
struct SettingsPage: View {
    let ui: SettingsScreenUi
    let ink: Color
    let model: SettingsModel
    let permissions: HomePermissions
    let onOpenLanguage: () -> Void

    @State private var safariURL: SafariLink?

    private var strings: SettingsStrings {
        ui.strings
    }
    private var controller: SettingsScreenController {
        model.controller
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Spacer().frame(height: 12)
            ProfileHeaderView(profile: ui.profile, strings: strings, ink: ink, model: model)
            PremiumCardView(premium: ui.premium, upgrade: strings.upgrade, ink: ink, onUpgrade: controller.openPlans)
            SettingsSection(title: strings.preferences, ink: ink) {
                if !model.notificationsAllowed {
                    SettingsRowView(symbol: "bell", title: strings.notifications, ink: ink) {
                        Task {
                            await model.enableNotifications(permissions)
                        }
                    }
                    RowDivider(ink: ink)
                }
                SettingsRowView(symbol: "globe", title: strings.language, ink: ink, action: onOpenLanguage)
            }
            SettingsSection(title: strings.about, ink: ink) {
                SettingsRowView(symbol: "hand.raised", title: strings.privacyPolicy, ink: ink) {
                    safariURL = SafariLink(url: strings.privacyPolicyUrl)
                }
                RowDivider(ink: ink)
                SettingsRowView(symbol: "doc.text", title: strings.termsOfUse, ink: ink) {
                    safariURL = SafariLink(url: strings.termsOfUseUrl)
                }
                RowDivider(ink: ink)
                SettingsRowView(symbol: "info.circle", title: strings.appVersion, ink: ink, trailing: Self.versionName)
            }
            SettingsSection(title: strings.account, ink: ink) {
                SettingsRowView(
                    symbol: "trash",
                    title: strings.deleteAccount,
                    ink: Color(hex: 0xFF6B6B),
                    action: controller.openDeleteAccount
                )
            }
            Button(role: .destructive, action: controller.openLogoutConfirm) {
                HStack(spacing: 8) {
                    if ui.isLoggingOut {
                        ProgressView().tint(Color(hex: 0xFF6B6B))
                    }
                    Text(strings.logout).font(.system(size: 16, weight: .semibold))
                }
                .foregroundStyle(Color(hex: 0xFF6B6B))
                .frame(maxWidth: .infinity, minHeight: 48)
                .background(HomeTheme.cardFill, in: RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(ui.isLoggingOut)
        }
        .onAppear {
            model.onAppear(permissions: permissions)
        }
        .confirmationDialog(strings.logout, isPresented: logoutBinding, titleVisibility: .visible) {
            Button(strings.confirm, role: .destructive, action: controller.confirmLogout)
            Button(strings.cancel, role: .cancel, action: controller.closeLogoutConfirm)
        } message: {
            Text(strings.logoutConfirmation)
        }
        .confirmationDialog(strings.removePhotoTitle, isPresented: removeBinding, titleVisibility: .visible) {
            Button(strings.removePhoto, role: .destructive, action: controller.confirmRemovePhoto)
            Button(strings.cancel, role: .cancel, action: controller.dismissRemovePhoto)
        } message: {
            Text(strings.removePhotoBody)
        }
        .sheet(isPresented: plansBinding) {
            PlanSheet(plans: ui.plans, strings: strings, controller: controller)
        }
        .sheet(isPresented: deleteAccountBinding) {
            if let deleteAccount = ui.deleteAccount {
                DeleteAccountSheet(ui: deleteAccount, strings: strings, controller: controller)
            }
        }
        .sheet(item: $safariURL) { link in
            SafariView(url: link.url).ignoresSafeArea()
        }
    }

    private var logoutBinding: Binding<Bool> {
        Binding(get: { ui.isLogoutConfirmVisible }, set: {
            if !$0 {
                controller.closeLogoutConfirm()
            }
        })
    }

    private var removeBinding: Binding<Bool> {
        Binding(get: { ui.profile.isRemoveConfirmVisible }, set: {
            if !$0 {
                controller.dismissRemovePhoto()
            }
        })
    }

    private var plansBinding: Binding<Bool> {
        Binding(get: { ui.plans.isOpen }, set: {
            if !$0 {
                controller.closePlans()
            }
        })
    }

    private var deleteAccountBinding: Binding<Bool> {
        Binding(get: { ui.deleteAccount != nil }, set: {
            if !$0 {
                controller.closeDeleteAccount()
            }
        })
    }

    private static var versionName: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
    }
}

// MARK: - Profile

/// Large round photo with a camera badge, the email, and the native photo picker.
private struct ProfileHeaderView: View {
    let profile: ProfileUi
    let strings: SettingsStrings
    let ink: Color
    let model: SettingsModel

    @State private var pick: PhotosPickerItem?
    @State private var image: UIImage?

    var body: some View {
        VStack(spacing: 14) {
            PhotosPicker(selection: $pick, matching: .images, photoLibrary: .shared()) {
                ZStack(alignment: .bottomTrailing) {
                    avatar
                    .frame(width: 104, height: 104)
                    .clipShape(Circle())
                    .overlay(Circle().strokeBorder(ink.opacity(0.18), lineWidth: 1))
                    .overlay {
                        if profile.isUploading || profile.isRemoving {
                            Circle().fill(.black.opacity(0.45))
                            ProgressView().tint(.white)
                        }
                    }
                    .shadow(color: .black.opacity(image == nil ? 0 : 0.35), radius: 12, y: 6)
                    Image(systemName: "camera.fill")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(HomeTheme.onYellow)
                        .frame(width: 32, height: 32)
                        .background(HomeTheme.yellow, in: Circle())
                        .overlay(Circle().strokeBorder(.black.opacity(0.25), lineWidth: 1))
                }
            }
            .buttonStyle(.plain)
            .disabled(!profile.canChangePhoto)
            .accessibilityLabel(strings.editPhoto)

            if let email = profile.email {
                Text(email).font(.system(size: 17, weight: .medium)).foregroundStyle(ink).headerShadow()
            }
            HStack(spacing: 10) {
                PhotosPicker(selection: $pick, matching: .images, photoLibrary: .shared()) {
                    Text(profile.photoUrl == nil ? strings.addPhoto : strings.changePhoto)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(ink)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(HomeTheme.cardFill, in: Capsule())
                }
                .buttonStyle(.plain)
                .disabled(!profile.canChangePhoto)
                if profile.canRemovePhoto {
                    Button(strings.removePhoto, action: model.controller.requestRemovePhoto)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(Color(hex: 0xFF6B6B))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(HomeTheme.cardFill, in: Capsule())
                        .buttonStyle(.plain)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .animation(.easeInOut(duration: 0.2), value: profile.canRemovePhoto)
        .onChange(of: pick) { item in
            guard let item else {
                return
            }
            pick = nil
            Task {
                await model.upload(item)
            }
        }
        // Signed URLs are short-lived and per request, so never read them from disk.
        .task(id: profile.photoUrl) {
            guard let url = profile.photoUrl.flatMap(URL.init(string:)) else {
                image = nil
                return
            }
            image = await RemoteImage.load(url, retries: 1, cachePolicy: .reloadIgnoringLocalCacheData)
        }
    }

    /// The photo, or the app's line-art dog drawn straight on the backdrop in the page ink.
    @ViewBuilder
    private var avatar: some View {
        if let image {
            Image(uiImage: image).resizable().scaledToFill()
        } else {
            Image("DefaultAvatar")
                .resizable()
                .scaledToFit()
                .foregroundStyle(ink)
                .padding(8)
                .accessibilityHidden(true)
        }
    }
}

// MARK: - Premium

private struct PremiumCardView: View {
    let premium: PremiumUi
    let upgrade: String
    let ink: Color
    let onUpgrade: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                Image(systemName: premium.status == .active ? "checkmark.seal.fill" : "crown.fill")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(HomeTheme.yellow)
                    .frame(width: 44, height: 44)
                    .background(HomeTheme.yellow.opacity(0.18), in: Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(premium.title).font(.system(size: 17, weight: .semibold)).foregroundStyle(ink)
                    Text(premium.subtitle).font(.system(size: 13)).foregroundStyle(ink.opacity(HomeTheme.mutedAlpha))
                }
            }
            switch premium.status {
            case .processing:
                ProgressView().progressViewStyle(.linear).tint(HomeTheme.yellow)
            case .pitch:
                Button(action: onUpgrade) {
                    Text(upgrade)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(HomeTheme.onYellow)
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .background(HomeTheme.yellow, in: Capsule())
                }
                .buttonStyle(.plain)
            case .active:
                EmptyView()
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [HomeTheme.yellow.opacity(0.22), HomeTheme.cardFill],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            ),
            in: RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous)
        )
        .background(HomeTheme.cardFill, in: RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous)
                .strokeBorder(HomeTheme.yellow.opacity(0.25), lineWidth: 1)
        )
        .animation(.easeInOut(duration: 0.25), value: premium.status)
    }
}

// MARK: - Rows and sections

private struct SettingsSection<Content: View>: View {
    let title: String
    let ink: Color
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title.uppercased())
                .font(.system(size: 12, weight: .semibold))
                .tracking(1.2)
                .foregroundStyle(ink.opacity(0.7))
                .padding(.leading, 6)
                .headerShadow()
            VStack(spacing: 0, content: content)
                .background(HomeTheme.cardFill, in: RoundedRectangle(cornerRadius: HomeTheme.cardRadius, style: .continuous))
        }
    }
}

private struct SettingsRowView: View {
    let symbol: String
    let title: String
    let ink: Color
    var trailing: String?
    var action: (() -> Void)?

    var body: some View {
        if let action {
            Button(action: action) {
                row(chevron: true)
            }
            .buttonStyle(.plain)
        } else {
            row(chevron: false)
        }
    }

    private func row(chevron: Bool) -> some View {
        HStack(spacing: 14) {
            Image(systemName: symbol).font(.system(size: 17)).frame(width: 24)
            Text(title).font(.system(size: 16))
            Spacer()
            if let trailing {
                Text(trailing).font(.system(size: 15)).foregroundStyle(ink.opacity(HomeTheme.mutedAlpha))
            }
            if chevron {
                Image(systemName: "chevron.right").font(.system(size: 13, weight: .semibold)).foregroundStyle(ink.opacity(0.5))
            }
        }
        .foregroundStyle(ink)
        .padding(.horizontal, 16)
        .frame(minHeight: 54)
        .contentShape(Rectangle())
    }
}

private struct RowDivider: View {
    let ink: Color

    var body: some View {
        Rectangle().fill(ink.opacity(0.1)).frame(height: 1).padding(.leading, 54)
    }
}

// MARK: - Safari

private struct SafariLink: Identifiable {
    let url: URL
    var id: String {
        url.absoluteString
    }

    init?(url string: String) {
        guard let url = URL(string: string) else {
            return nil
        }
        self.url = url
    }
}

/// Privacy and terms open in Safari's in-app view, with its own reader, share and done buttons.
struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: url)
        controller.preferredControlTintColor = UIColor(HomeTheme.yellow)
        controller.preferredBarTintColor = UIColor(HomeTheme.sheetFill)
        return controller
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {
    }
}
