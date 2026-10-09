// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_home",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_home",
      type: .none,
      targets: ["_feature_home"]
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
      name: "_feature_home",
      dependencies: [
        .product(
          name: "FirebaseCore",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseRemoteConfig",
          package: "firebase-ios-sdk"
        )
      ]
    )
  ]
)
