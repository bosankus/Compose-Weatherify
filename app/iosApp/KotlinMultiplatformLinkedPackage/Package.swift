// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/razorpay/razorpay-pod.git",
      from: "1.5.4"
    ),
    .package(path: "subpackages/_feature_home"),
    .package(path: "subpackages/_analytics")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(
          name: "RazorpayCheckout",
          package: "razorpay-pod"
        ),
        .product(name: "_feature_home", package: "_feature_home"),
        .product(name: "_analytics", package: "_analytics")
      ]
    )
  ]
)
