import UserNotifications
import FirebaseMessaging

/// Downloads a push's picture and attaches it before iOS shows the notification. iOS only runs this
/// for pushes sent with mutable-content, which the backend sets when there is an image.
final class NotificationService: UNNotificationServiceExtension {
    private var contentHandler: ((UNNotificationContent) -> Void)?
    private var bestAttempt: UNMutableNotificationContent?

    override func didReceive(
        _ request: UNNotificationRequest,
        withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void
    ) {
        self.contentHandler = contentHandler
        guard let content = request.content.mutableCopy() as? UNMutableNotificationContent else {
            contentHandler(request.content)
            return
        }
        bestAttempt = content
        Messaging.serviceExtension().populateNotificationContent(content, withContentHandler: contentHandler)
    }

    /// Out of time: show the text without the picture rather than nothing.
    override func serviceExtensionTimeWillExpire() {
        if let contentHandler, let bestAttempt {
            contentHandler(bestAttempt)
        }
    }
}
