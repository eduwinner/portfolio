import Foundation

protocol RatesService {
    func latest(base: String, quotes: [String]) async throws -> [Quote]
    func history(base: String, quote: String, days: Int) async throws -> [RatePoint]
}

enum RatesError: LocalizedError {
    case badStatus(Int)

    var errorDescription: String? {
        switch self {
        case .badStatus(let code): return "The rates server answered with HTTP \(code)."
        }
    }
}

/// Client for the free, key-less Frankfurter API (https://frankfurter.dev).
struct FrankfurterClient: RatesService {
    var session: URLSession = .shared
    var endpoint = URL(string: "https://api.frankfurter.dev/v2/rates")!

    func latest(base: String, quotes: [String]) async throws -> [Quote] {
        let rows = try await fetch([
            URLQueryItem(name: "base", value: base),
            URLQueryItem(name: "quotes", value: quotes.joined(separator: ","))
        ])
        return rows
            .filter { $0.quote != base }
            .map { Quote(code: $0.quote, rate: $0.rate) }
            .sorted { $0.code < $1.code }
    }

    func history(base: String, quote: String, days: Int) async throws -> [RatePoint] {
        let start = Calendar.current.date(byAdding: .day, value: -days, to: Date()) ?? Date()
        let rows = try await fetch([
            URLQueryItem(name: "base", value: base),
            URLQueryItem(name: "quotes", value: quote),
            URLQueryItem(name: "from", value: APIDate.formatter.string(from: start))
        ])
        return rows
            .filter { $0.quote == quote }
            .compactMap { row in
                APIDate.formatter.date(from: row.date).map { RatePoint(date: $0, rate: row.rate) }
            }
            .sorted { $0.date < $1.date }
    }

    static func decodeRows(_ data: Data) throws -> [RateRow] {
        try JSONDecoder().decode([RateRow].self, from: data)
    }

    private func fetch(_ items: [URLQueryItem]) async throws -> [RateRow] {
        var components = URLComponents(url: endpoint, resolvingAgainstBaseURL: false)!
        components.queryItems = items
        var request = URLRequest(url: components.url!)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 15

        let (data, response) = try await session.data(for: request)
        let status = (response as? HTTPURLResponse)?.statusCode ?? -1
        guard (200..<300).contains(status) else { throw RatesError.badStatus(status) }
        return try Self.decodeRows(data)
    }
}
