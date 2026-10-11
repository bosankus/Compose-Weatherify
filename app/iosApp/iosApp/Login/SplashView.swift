import SwiftUI

/// The app's opening moment: the leaf wordmark resting on a misty forest. The photo ships with
/// the app (no network at launch), under a soft dark wash so the mark reads.
///
/// The launch screen (`UILaunchScreen` in project.yml) is just `SplashBackground`, the average
/// color of this washed photo. This view starts on that same color and fades the forest and the
/// lockup in over it, so launch flows into the splash with no black frame and no jump.
/// No status bar, to match the launch screen.
struct SplashView: View {
    @State private var shown = false

    var body: some View {
        ZStack {
            Color("SplashBackground")
            ZStack {
                GeometryReader { proxy in
                    Image("SplashForest")
                        .resizable()
                        .scaledToFill()
                        .frame(width: proxy.size.width, height: proxy.size.height)
                        .clipped()
                }
                Color.black.opacity(0.38)
                Image("LaunchWordmark")
                    .accessibilityLabel("Weatherify")
            }
            .opacity(shown ? 1 : 0)
        }
        .ignoresSafeArea()
        .statusBarHidden(true)
        .onAppear {
            withAnimation(.easeOut(duration: 0.35)) {
                shown = true
            }
        }
    }
}
