import Foundation

/// One row returned by the Frankfurter v2 API (`GET /v2/rates`).
struct RateRow: Decodable, Hashable {
    let date: String
    let base: String
    let quote: String
    let rate: Double
}

/// Latest rate of one currency against the selected base.
struct Quote: Identifiable, Hashable {
    let code: String
    let rate: Double
    var id: String { code }
}

/// One point of a historical series, used by the chart.
struct RatePoint: Identifiable, Hashable {
    let date: Date
    let rate: Double
    var id: Date { date }
}

enum Currency {
    /// Currencies shown in the list (the selected base is filtered out).
    static let tracked = ["USD", "BRL", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "MXN"]
    /// Currencies the user can pick as base.
    static let bases = ["USD", "EUR", "BRL"]

    static func name(for code: String) -> String {
        Locale.current.localizedString(forCurrencyCode: code) ?? code
    }

    /// Builds a flag emoji from the first two letters of the ISO code (EUR -> EU flag).
    static func flag(for code: String) -> String {
        var scalars = String.UnicodeScalarView()
        for letter in code.prefix(2).uppercased().unicodeScalars {
            if let indicator = UnicodeScalar(127_397 + letter.value) {
                scalars.append(indicator)
            }
        }
        return String(scalars)
    }
}

enum APIDate {
    static let formatter: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "UTC")
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()
}
