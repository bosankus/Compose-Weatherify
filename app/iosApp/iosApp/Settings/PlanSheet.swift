import SwiftUI
import ComposeApp

/// Premium plan picker as a native sheet: plan choice, price, features, duration, subscribe.
/// Subscribing hands off to Razorpay through the session.
struct PlanSheet: View {
    let plans: PlanSheetUi
    let strings: SettingsStrings
    let controller: SettingsScreenController

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(strings.selectPlan).font(.system(size: 18, weight: .semibold))
                Spacer()
                Button(action: controller.closePlans) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .bold))
                        .frame(width: 36, height: 36)
                        .background(HomeTheme.sheetFieldFill, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(strings.close)
            }
            .foregroundStyle(HomeTheme.inkOnDark)
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 12)
            content
        }
        .presentationDetents([.medium, .large])
        .homeSheetStyle()
        .animation(.easeInOut(duration: 0.25), value: plans.selected?.price)
    }

    @ViewBuilder
    private var content: some View {
        if plans.isLoading {
            Spacer()
            ProgressView().tint(HomeTheme.yellow)
            Spacer()
        } else if let error = plans.error {
            VStack(spacing: 10) {
                Text(strings.oops).font(.system(size: 18, weight: .semibold))
                Text(error).font(.system(size: 14)).foregroundStyle(HomeTheme.sheetMuted).multilineTextAlignment(.center)
                Button(strings.retry, action: controller.retryPlans)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(HomeTheme.onYellow)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(HomeTheme.yellow, in: Capsule())
                    .buttonStyle(.plain)
            }
            .foregroundStyle(HomeTheme.inkOnDark)
            .padding(24)
            .frame(maxHeight: .infinity)
        } else if let plan = plans.selected {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    if plans.services.count > 1 {
                        serviceChips
                    }
                    VStack(spacing: 4) {
                        Text(plan.serviceName).font(.system(size: 20, weight: .bold))
                        Text(plan.price).font(.system(size: 34, weight: .bold)).foregroundStyle(HomeTheme.yellow)
                        Text(plan.duration).font(.system(size: 14)).foregroundStyle(HomeTheme.sheetMuted)
                    }
                    .frame(maxWidth: .infinity)
                    if !plan.features.isEmpty {
                        features(plan)
                    }
                    if !plan.tiers.isEmpty {
                        tiers(plan)
                    }
                }
                .foregroundStyle(HomeTheme.inkOnDark)
                .padding(.horizontal, 20)
                .padding(.bottom, 12)
            }
            Button(action: controller.subscribe) {
                Text(strings.subscribe)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(HomeTheme.onYellow)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .background(HomeTheme.yellow, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 20)
            .padding(.bottom, 12)
        }
    }

    private var serviceChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(plans.services, id: \.id) { service in
                    Button {
                        controller.selectService(id: service.id)
                    } label: {
                        Text(service.name)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(service.isSelected ? HomeTheme.onYellow : HomeTheme.inkOnDark)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(service.isSelected ? HomeTheme.yellow : HomeTheme.sheetFieldFill, in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func features(_ plan: SelectedPlanUi) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.features).font(.system(size: 13, weight: .semibold))
            ForEach(plan.features, id: \.id) { feature in
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "checkmark.circle.fill").foregroundStyle(Color(hex: 0x4CAF50)).font(.system(size: 14))
                    Text(feature.text).font(.system(size: 14))
                }
            }
            if let more = plan.moreFeatures {
                Text(more).font(.system(size: 12)).foregroundStyle(HomeTheme.yellow)
            }
        }
    }

    private func tiers(_ plan: SelectedPlanUi) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.selectDuration).font(.system(size: 13, weight: .semibold))
            ForEach(plan.tiers, id: \.id) { tier in
                Button {
                    controller.selectTier(id: tier.id)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(tier.duration).font(.system(size: 15, weight: .medium))
                            if tier.isFeatured {
                                Text("★").font(.system(size: 11)).foregroundStyle(HomeTheme.yellow)
                            }
                        }
                        Spacer()
                        Text(tier.price).font(.system(size: 16, weight: .semibold))
                        Image(systemName: tier.isSelected ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle(tier.isSelected ? HomeTheme.yellow : HomeTheme.sheetMuted)
                    }
                    .foregroundStyle(HomeTheme.inkOnDark)
                    .padding(14)
                    .background(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(tier.isSelected ? HomeTheme.yellow.opacity(0.1) : HomeTheme.sheetFieldFill)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(HomeTheme.yellow.opacity(tier.isSelected ? 0.6 : 0), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(tier.isSelected ? .isSelected : [])
            }
        }
    }
}
