import Foundation

enum LoadState<Value> {
    case loading
    case loaded(Value)
    case failed(String)
}

@MainActor
final class RatesViewModel: ObservableObject {
    @Published private(set) var base = "USD"
    @Published private(set) var quotes: LoadState<[Quote]> = .loading
    @Published private(set) var updatedAt: Date?

    let service: RatesService

    init(service: RatesService = FrankfurterClient()) {
        self.service = service
    }

    func select(base newBase: String) async {
        guard newBase != base else { return }
        base = newBase
        await loadLatest()
    }

    func loadLatest() async {
        if case .loaded = quotes {} else { quotes = .loading }
        do {
            let symbols = Currency.tracked.filter { $0 != base }
            quotes = .loaded(try await service.latest(base: base, quotes: symbols))
            updatedAt = Date()
        } catch is CancellationError {
            // View went away; keep the previous state.
        } catch {
            quotes = .failed(error.localizedDescription)
        }
    }
}

@MainActor
final class PairViewModel: ObservableObject {
    @Published private(set) var points: LoadState<[RatePoint]> = .loading
    @Published var days = 30

    let base: String
    let quote: String
    private let service: RatesService

    init(base: String, quote: String, service: RatesService = FrankfurterClient()) {
        self.base = base
        self.quote = quote
        self.service = service
    }

    func load() async {
        points = .loading
        do {
            points = .loaded(try await service.history(base: base, quote: quote, days: days))
        } catch is CancellationError {
        } catch {
            points = .failed(error.localizedDescription)
        }
    }
}
