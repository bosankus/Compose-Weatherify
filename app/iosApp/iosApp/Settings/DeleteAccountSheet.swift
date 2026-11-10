import SwiftUI
import ComposeApp

/// Confirms account deletion with the current password. The password stays in this view, so it is
/// gone when the sheet closes. While the request runs the sheet can't be dismissed; on success the
/// session ends and the app returns to sign-in on its own.
struct DeleteAccountSheet: View {
    let ui: DeleteAccountUi
    let strings: SettingsStrings
    let controller: SettingsScreenController

    @State private var password = ""
    @FocusState private var isPasswordFocused: Bool

    private static let danger = Color(hex: 0xFF6B6B)

    private var canConfirm: Bool {
        !password.isEmpty && !ui.isDeleting
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(strings.deleteAccountTitle).font(.system(size: 18, weight: .semibold))
                Spacer()
                Button(action: controller.closeDeleteAccount) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .bold))
                        .frame(width: 36, height: 36)
                        .background(HomeTheme.sheetFieldFill, in: Circle())
                }
                .buttonStyle(.plain)
                .disabled(ui.isDeleting)
                .accessibilityLabel(strings.close)
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 12)

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text(strings.deleteAccountWarning).font(.system(size: 15))
                    if ui.showsPremiumWarning {
                        Text(strings.deleteAccountPremiumWarning)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(Self.danger)
                    }
                    Text(strings.deleteAccountRecords)
                        .font(.system(size: 13))
                        .foregroundStyle(HomeTheme.sheetMuted)

                    passwordField.padding(.top, 6)

                    if let error = ui.error {
                        Text(error)
                            .font(.system(size: 13))
                            .foregroundStyle(Self.danger)
                    }

                    Button(role: .destructive, action: confirm) {
                        HStack(spacing: 8) {
                            if ui.isDeleting {
                                ProgressView().tint(.white)
                            }
                            Text(strings.deleteAccount).font(.system(size: 16, weight: .semibold))
                        }
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .background(Self.danger.opacity(canConfirm || ui.isDeleting ? 1 : 0.4), in: Capsule())
                    }
                    .buttonStyle(.plain)
                    .disabled(!canConfirm)
                    .padding(.top, 6)

                    Button(strings.cancel, action: controller.closeDeleteAccount)
                        .font(.system(size: 15, weight: .medium))
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .buttonStyle(.plain)
                        .disabled(ui.isDeleting)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .foregroundStyle(HomeTheme.inkOnDark)
        .presentationDetents([.large])
        .interactiveDismissDisabled(ui.isDeleting)
        .homeSheetStyle()
        .onAppear {
            isPasswordFocused = true
        }
    }

    private var passwordField: some View {
        SecureField(strings.currentPassword, text: $password)
            .textContentType(.password)
            .submitLabel(.done)
            .focused($isPasswordFocused)
            .onSubmit(confirm)
            .disabled(ui.isDeleting)
            .font(.system(size: 16))
            .padding(.horizontal, 16)
            .frame(minHeight: 50)
            .background(HomeTheme.sheetFieldFill, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(ui.isPasswordError ? Self.danger : .clear, lineWidth: 1)
            )
            .onChange(of: password) { _ in
                // A stale "incorrect password" shouldn't sit under what the user is retyping.
                if ui.error != nil {
                    controller.deleteAccountPasswordEdited()
                }
            }
    }

    private func confirm() {
        guard canConfirm else {
            return
        }
        controller.confirmDeleteAccount(password: password)
    }
}
