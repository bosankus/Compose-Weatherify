// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_analytics",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_analytics",
      type: .none,
      targets: ["_analytics"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      exact: "12.19.2"
    )
  ],
  targets: [
    .target(
      name: "_analytics",
      dependencies: [
        .product(
          name: "FirebaseAnalytics",
          package: "firebase-ios-sdk"
        )
      ]
    )
  ]
)
