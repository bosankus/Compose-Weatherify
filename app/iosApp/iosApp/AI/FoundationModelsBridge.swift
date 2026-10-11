import Foundation
import ComposeApp
#if canImport(FoundationModels)
import FoundationModels
#endif

/// Apple's on-device model for the weather summary, handed to the shared Kotlin code through
/// `NativeAiBridge`. Foundation Models is Swift-only and needs iOS 26 with Apple Intelligence on,
/// so every use is guarded; anywhere else this reports "unavailable" and the card stays hidden.
final class FoundationModelsBridge: NativeAiBridge {
    // Mirrors BridgedOnDeviceAiClient: 0 unavailable, 1 downloadable, 2 downloading, 3 available.
    private enum Status {
        static let unavailable: Int32 = 0
        static let downloading: Int32 = 2
        static let available: Int32 = 3
    }

    func availability() -> Int32 {
        let real = realAvailability()
        #if DEBUG
        // Debug builds on a device without Apple Intelligence (simulators, older phones) fall back
        // to labelled demo text, like the Android debug build, so the card can be seen and tested.
        return real == Status.unavailable ? Status.available : real
        #else
        return real
        #endif
    }

    func generate(
        systemInstruction: String,
        prompt: String,
        onSnapshot: @escaping (String) -> Void,
        onDone: @escaping (String?) -> Void
    ) -> NativeAiCall {
        let task = Task {
            do {
                #if DEBUG
                if realAvailability() == Status.unavailable {
                    try await streamDemo(systemInstruction: systemInstruction, onSnapshot: onSnapshot)
                    if !Task.isCancelled {
                        onDone(nil)
                    }
                    return
                }
                #endif
                try await streamModel(systemInstruction: systemInstruction, prompt: prompt, onSnapshot: onSnapshot)
                if !Task.isCancelled {
                    onDone(nil)
                }
            } catch is CancellationError {
                // The collector went away; Kotlin has already closed its side.
            } catch {
                // localizedDescription says only "error -1" for Foundation Models; the case name is the useful part.
                if !Task.isCancelled {
                    onDone(String(describing: error))
                }
            }
        }
        return TaskCall {
            task.cancel()
        }
    }

    private func realAvailability() -> Int32 {
        #if DEBUG && targetEnvironment(simulator)
        // Simulators depend on the host Mac's Apple Intelligence setup and often cannot generate
        // (ModelManagerError 1026), so debug builds there use the labelled demo text instead.
        return Status.unavailable
        #else
        return modelAvailability()
        #endif
    }

    private func modelAvailability() -> Int32 {
        #if canImport(FoundationModels)
        if #available(iOS 26.0, *) {
            switch SystemLanguageModel.default.availability {
            case .available:
                return Status.available
            case .unavailable(.modelNotReady):
                return Status.downloading
            case .unavailable:
                return Status.unavailable
            }
        }
        #endif
        return Status.unavailable
    }

    /// Foundation Models streams the whole text so far on each step, which is what Kotlin expects.
    private func streamModel(
        systemInstruction: String,
        prompt: String,
        onSnapshot: @escaping (String) -> Void
    ) async throws {
        #if canImport(FoundationModels)
        if #available(iOS 26.0, *) {
            let session = LanguageModelSession(instructions: systemInstruction)
            let options = GenerationOptions(temperature: 0.3, maximumResponseTokens: 120)
            for try await snapshot in session.streamResponse(to: prompt, options: options) {
                try Task.checkCancellation()
                onSnapshot(snapshot.content)
            }
            return
        }
        #endif
        throw FoundationModelsError.unavailable
    }

    #if DEBUG
    private func streamDemo(systemInstruction: String, onSnapshot: @escaping (String) -> Void) async throws {
        let advice = systemInstruction
        .split(separator: "\n")
        .filter {
            $0.hasPrefix("- ")
        }
        .map {
            String($0.dropFirst(2)).components(separatedBy: " (").first ?? ""
        }
        .map {
            $0 + "."
        }
        .joined(separator: " ")
        let text = "[Demo text, not from the model.] " + (advice.isEmpty ? "No special advice today." : advice)
        try await Task.sleep(nanoseconds: 1_500_000_000)
        var shown = ""
        for word in text.split(separator: " ") {
            try await Task.sleep(nanoseconds: 90_000_000)
            shown = shown.isEmpty ? String(word) : shown + " " + word
            onSnapshot(shown)
        }
    }
    #endif
}

private enum FoundationModelsError: LocalizedError {
    case unavailable
    var errorDescription: String? {
        "Apple's on-device model is not available."
    }
}

private final class TaskCall: NativeAiCall {
    private let onCancel: () -> Void

    init(_ onCancel: @escaping () -> Void) {
        self.onCancel = onCancel
    }

    func cancel() {
        onCancel()
    }
}
