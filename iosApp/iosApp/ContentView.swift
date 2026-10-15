import SwiftUI
import UIKit
import ComposeApp

/// Hosts the shared Compose UI (composeApp/src/iosMain/.../MainViewController.kt).
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Compose draws edge to edge and handles insets itself (WindowInsets).
        ComposeView().ignoresSafeArea()
    }
}
