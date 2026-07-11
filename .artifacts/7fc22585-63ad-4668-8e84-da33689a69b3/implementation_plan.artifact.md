# Implementation Plan - Custom CMP-Compatible Colors for NotificationToast

The user approved the custom color plan and requested that the colors be accessible across the app and CMP-compatible.

## Proposed Changes

### [common-ui]

#### [NEW] [Color.kt](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/common-ui/src/commonMain/kotlin/bose/ankush/commonui/theme/Color.kt)
- Create a new file in `commonMain` to store shared colors.
- Define:
    - `SuccessGreen = Color(0xFF22A543)`
    - `WarningYellow = Color(0xFFF8BF1D)`
    - `ErrorRed = Color(0xFFD32F2F)`

#### [MODIFY] [NotificationToast.kt](file:///Users/t0304iw/Desktop/androidplay/weatherify/Compose-Weatherify/common-ui/src/commonMain/kotlin/bose/ankush/commonui/components/NotificationToast.kt)
- Import the new colors from `bose.ankush.commonui.theme`.
- Update the `when(type)` block to return a 4-tuple: `(backgroundColor, icon, iconColor, contentColor)`.
- **Success**:
    - Background: `SuccessGreen`
    - Icon/Content Color: `Color.White`
- **Warning**:
    - Background: `WarningYellow`
    - Icon/Content Color: `Color(0xFF212121)` (Dark text for readability on yellow)
- **Error**:
    - Background: `ErrorRed`
    - Icon/Content Color: `Color.White`
- Update the UI to use `contentColor`.

## Verification Plan

### Manual Verification
- Render Compose Preview for `NotificationToast` with different types.
- Verify color contrast.

### Automated Tests
- Build the project to ensure no CMP compatibility issues.
