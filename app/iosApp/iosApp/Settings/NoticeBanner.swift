import SwiftUI

/// Short notice that slides in under the status bar and hides itself after a few seconds:
/// yellow for success, red for a failure. Tap to dismiss.
struct NoticeBanner: View {
    @Binding var notice: SettingsNotice?

    var body: some View {
        Group {
            if let notice {
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: notice.isError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                        .font(.system(size: 16, weight: .semibold))
                    VStack(alignment: .leading, spacing: 1) {
                        Text(notice.title).font(.system(size: 14, weight: .semibold))
                        Text(notice.message).font(.system(size: 13)).lineLimit(3)
                    }
                    Spacer(minLength: 0)
                }
                .foregroundStyle(notice.isError ? Color.white : HomeTheme.onYellow)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(
                    notice.isError ? Color(hex: 0xC62828) : HomeTheme.yellow,
                    in: RoundedRectangle(cornerRadius: 16, style: .continuous)
                )
                .shadow(color: .black.opacity(0.25), radius: 8, y: 4)
                .padding(.horizontal, HomeTheme.pageInset)
                .transition(.move(edge: .top).combined(with: .opacity))
                .onTapGesture {
                    self.notice = nil
                }
                .task(id: notice.id) {
                    try? await Task.sleep(nanoseconds: 4_000_000_000)
                    if self.notice?.id == notice.id {
                        self.notice = nil
                    }
                }
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: notice)
    }
}
