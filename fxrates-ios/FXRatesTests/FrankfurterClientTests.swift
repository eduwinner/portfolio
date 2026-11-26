import XCTest
@testable import FXRates

final class FrankfurterClientTests: XCTestCase {
    func testDecodesV2RateRows() throws {
        let json = """
        [
          {"date":"2026-11-25","base":"USD","quote":"USD","rate":1},
          {"date":"2026-11-25","base":"USD","quote":"BRL","rate":5.4321},
          {"date":"2026-11-25","base":"USD","quote":"EUR","rate":0.9123}
        ]
        """.data(using: .utf8)!

        let rows = try FrankfurterClient.decodeRows(json)

        XCTAssertEqual(rows.count, 3)
        XCTAssertEqual(rows[1].quote, "BRL")
        XCTAssertEqual(rows[1].rate, 5.4321, accuracy: 0.00001)
    }

    func testFlagForCurrencyCode() {
        XCTAssertEqual(Currency.flag(for: "BRL"), "🇧🇷")
        XCTAssertEqual(Currency.flag(for: "EUR"), "🇪🇺")
    }

    func testAPIDateRoundTrip() {
        let date = APIDate.formatter.date(from: "2026-01-31")
        XCTAssertNotNil(date)
        XCTAssertEqual(APIDate.formatter.string(from: date!), "2026-01-31")
    }
}
