import SwiftUI

struct ContentView: View {
    @StateObject private var model = RatesViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Picker("Base currency", selection: Binding(
                        get: { model.base },
                        set: { newBase in Task { await model.select(base: newBase) } }
                    )) {
                        ForEach(Currency.bases, id: \.self) { code in
                            Text("\(Currency.flag(for: code)) \(code)").tag(code)
                        }
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                }

                content
            }
            .navigationTitle("FX Rates")
            .refreshable { await model.loadLatest() }
            .task { await model.loadLatest() }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch model.quotes {
        case .loading:
            HStack { Spacer(); ProgressView("Loading rates…"); Spacer() }
        case .failed(let message):
            Section {
                Label(message, systemImage: "wifi.exclamationmark")
                    .foregroundStyle(.red)
                Button("Try again") { Task { await model.loadLatest() } }
            }
        case .loaded(let quotes):
            Section {
                ForEach(quotes) { quote in
                    NavigationLink {
                        PairDetailView(base: model.base, quote: quote.code)
                    } label: {
                        QuoteRow(base: model.base, quote: quote)
                    }
                }
            } header: {
                Text("1 \(model.base) equals")
            } footer: {
                if let updatedAt = model.updatedAt {
                    Text("Updated \(updatedAt.formatted(date: .omitted, time: .shortened)) · Source: Frankfurter (central bank reference rates)")
                }
            }
        }
    }
}

private struct QuoteRow: View {
    let base: String
    let quote: Quote

    var body: some View {
        HStack(spacing: 12) {
            Text(Currency.flag(for: quote.code))
                .font(.largeTitle)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(quote.code).font(.headline)
                Text(Currency.name(for: quote.code))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text(quote.rate, format: .number.precision(.fractionLength(4)))
                .font(.body.monospacedDigit())
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("1 \(base) equals \(quote.rate.formatted(.number.precision(.fractionLength(4)))) \(Currency.name(for: quote.code))")
    }
}

#Preview {
    ContentView()
}
