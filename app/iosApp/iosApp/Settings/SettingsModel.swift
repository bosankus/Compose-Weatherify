import SwiftUI
import PhotosUI
import ComposeApp

/// SwiftUI side of the shared SettingsScreenController: publishes each SettingsScreenUi frame and
/// turns the controller's notices into a banner.
@MainActor
final class SettingsModel: ObservableObject {
    let controller: SettingsScreenController

    @Published private(set) var ui: SettingsScreenUi?
    @Published var notice: SettingsNotice?
    @Published private(set) var notificationsAllowed = true
    @Published private(set) var notificationsDenied = false

    init(session: AppSession) {
        controller = session.settingsController()
    }

    deinit {
        let controller = controller
        DispatchQueue.main.async {
            controller.close()
        }
    }

    /// Runs for as long as the home screen is on screen.
    func run() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { @MainActor in
                for await frame in self.controller.state {
                    self.ui = frame
                }
            }
            group.addTask { @MainActor in
                for await event in self.controller.events {
                    switch onEnum(of: event) {
                    case .notice(let notice):
                        self.notice = SettingsNotice(title: notice.title, message: notice.message, isError: notice.isError)
                    }
                }
            }
        }
    }

    func refresh() async {
        try? await controller.refresh()
    }

    /// The Settings page came on screen: fresh photo URL and notification status.
    func onAppear(permissions: HomePermissions) {
        controller.onAppear()
        Task {
            await syncNotifications(permissions)
        }
    }

    func syncNotifications(_ permissions: HomePermissions) async {
        notificationsAllowed = await permissions.notificationsGranted()
        notificationsDenied = await permissions.notificationsDenied()
    }

    /// Asks once; after a denial iOS never asks again, so the row goes straight to the switch.
    func enableNotifications(_ permissions: HomePermissions) async {
        if notificationsDenied {
            permissions.openNotificationSettings()
        } else {
            _ = await permissions.requestNotifications()
        }
        await syncNotifications(permissions)
    }

    /// Loads the pick, scales it to at most 1080 px and re-encodes it as JPEG (the server takes
    /// JPEG up to 5 MB; HEIC from the camera is refused).
    func upload(_ item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: data),
              let jpeg = image.scaledToFit(maxSide: 1080).jpegData(compressionQuality: 0.85)
        else {
            controller.photoUnreadable()
            return
        }
        controller.uploadPhoto(jpeg: jpeg)
    }
}

struct SettingsNotice: Equatable, Identifiable {
    let id = UUID()
    let title: String
    let message: String
    let isError: Bool
}

private extension UIImage {
    func scaledToFit(maxSide: CGFloat) -> UIImage {
        let longest = max(size.width, size.height)
        guard longest > maxSide else {
            return self
        }
        let scale = maxSide / longest
        let target = CGSize(width: (size.width * scale).rounded(), height: (size.height * scale).rounded())
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: target))
        }
    }
}
