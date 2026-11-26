import SwiftUI
import Charts

struct PairDetailView: View {
    @StateObject private var model: PairViewModel

    init(base: String, quote: String) {
        _model = StateObject(wrappedValue: PairViewModel(base: base, quote: quote))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Picker("Period", selection: $model.days) {
                    Text("7D").tag(7)
                    Text("30D").tag(30)
                    Text("90D").tag(90)
                    Text("1Y").tag(365)
                }
                .pickerStyle(.segmented)

                switch model.points {
                case .loading:
                    ProgressView().frame(maxWidth: .infinity, minHeight: 260)
                case .failed(let message):
                    Label(message, systemImage: "exclamationmark.triangle")
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity, minHeight: 260)
                case .loaded(let points):
                    if let first = points.first, let last = points.last {
                        summary(first: first, last: last, points: points)
                        chart(points)
                    } else {
                        Text("No data for this period.")
                            .frame(maxWidth: .infinity, minHeight: 260)
                    }
                }
            }
            .padding()
        }
        .navigationTitle("\(model.base) → \(model.quote)")
        .navigationBarTitleDisplayMode(.inline)
        .task(id: model.days) { await model.load() }
    }

    private func summary(first: RatePoint, last: RatePoint, points: [RatePoint]) -> some View {
        let change = (last.rate - first.rate) / first.rate * 100
        let rates = points.map(\.rate)
        return VStack(alignment: .leading, spacing: 8) {
            Text(last.rate, format: .number.precision(.fractionLength(4)))
                .font(.system(size: 40, weight: .bold, design: .rounded).monospacedDigit())
            Text("\(change >= 0 ? "▲" : "▼") \(abs(change), format: .number.precision(.fractionLength(2)))% in \(model.days) days")
                .font(.headline)
                .foregroundStyle(change >= 0 ? .green : .red)
            HStack {
                stat("Low", rates.min() ?? 0)
                Spacer()
                stat("High", rates.max() ?? 0)
                Spacer()
                stat("Points", Double(points.count), digits: 0)
            }
        }
        .accessibilityElement(children: .combine)
    }

    private func stat(_ title: String, _ value: Double, digits: Int = 4) -> some View {
        VStack(alignment: .leading) {
            Text(title).font(.caption).foregroundStyle(.secondary)
            Text(value, format: .number.precision(.fractionLength(digits)))
                .font(.subheadline.monospacedDigit())
        }
    }

    private func chart(_ points: [RatePoint]) -> some View {
        let rates = points.map(\.rate)
        let low = rates.min() ?? 0
        let high = rates.max() ?? 1
        let padding = max((high - low) * 0.1, 0.0001)

        return Chart(points) { point in
            AreaMark(
                x: .value("Date", point.date),
                yStart: .value("Floor", low - padding),
                yEnd: .value("Rate", point.rate)
            )
            .foregroundStyle(.linearGradient(
                colors: [.accentColor.opacity(0.35), .accentColor.opacity(0.02)],
                startPoint: .top, endPoint: .bottom
            ))
            LineMark(x: .value("Date", point.date), y: .value("Rate", point.rate))
                .interpolationMethod(.monotone)
                .lineStyle(StrokeStyle(lineWidth: 2))
        }
        .chartYScale(domain: (low - padding)...(high + padding))
        .frame(height: 260)
        .accessibilityLabel("Rate history chart for \(model.base) to \(model.quote)")
        .accessibilityValue("From \(low.formatted(.number.precision(.fractionLength(4)))) to \(high.formatted(.number.precision(.fractionLength(4))))")
    }
}

#Preview {
    NavigationStack { PairDetailView(base: "USD", quote: "BRL") }
}
