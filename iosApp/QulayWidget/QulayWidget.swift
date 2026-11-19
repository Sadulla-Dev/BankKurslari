import SwiftUI
import WidgetKit

// Home-screen widget for iOS: where selling a currency pays most and where buying it costs least, ranked like the
// Android widget and the Telegram post. Reads the same public API as the app (api.qulaykurs.uz, rebuilt hourly).

// MARK: - Configuration

enum WidgetCurrency: String {
    case usd = "USD", eur = "EUR", rub = "RUB"
    var name: String { switch self { case .usd: L("Dollar", "Доллар"); case .eur: L("Yevro", "Евро"); case .rub: L("Rubl", "Рубль") } }
    var symbol: String { switch self { case .usd: "$"; case .eur: "€"; case .rub: "₽" } }
}

// MARK: - Data

struct RankedBank: Identifiable {
    let id = UUID()
    let name: String
    let rate: Int
    let place: Int
    let gap: Int
    let logo: String?
}

struct RatesEntry: TimelineEntry {
    let date: Date
    let currency: WidgetCurrency
    let cbu: Double
    let cbuDiff: Double
    let updated: String
    let sell: [RankedBank]   // bank buys from you: highest first
    let buy: [RankedBank]    // bank sells to you: lowest first
    let bankCount: Int
    let ok: Bool
}

private struct ApiRates: Decodable {
    struct Cbu: Decodable { let rate: Double; let diff: Double? }
    struct Bank: Decodable { let bank: String; let buy: Double?; let sell: Double? }
    let updatedAt: String?
    let cbu: Cbu
    let banks: [Bank]
}

enum RatesLoader {
    static func load(_ cur: WidgetCurrency) async -> RatesEntry {
        let key = "rates_\(cur.rawValue)"
        let url = URL(string: "https://api.qulaykurs.uz/api/rates/\(cur.rawValue.lowercased())")!
        var data: Data? = nil
        if let (d, resp) = try? await URLSession.shared.data(from: url), (resp as? HTTPURLResponse)?.statusCode == 200 {
            data = d
            UserDefaults.standard.set(d, forKey: key) // offline fallback
        } else {
            data = UserDefaults.standard.data(forKey: key)
        }
        guard let data, let api = try? JSONDecoder().decode(ApiRates.self, from: data) else { return placeholder(cur, ok: false) }
        return entry(cur, api)
    }

    private static func entry(_ cur: WidgetCurrency, _ api: ApiRates) -> RatesEntry {
        func rank(_ pairs: [(String, Int)], higherIsBetter: Bool) -> [RankedBank] {
            let sorted = pairs.sorted { a, b in
                a.1 != b.1 ? (higherIsBetter ? a.1 > b.1 : a.1 < b.1) : shortName(a.0) < shortName(b.0)
            }
            guard let best = sorted.first?.1 else { return [] }
            var place = 0
            return sorted.enumerated().map { i, p in
                if i == 0 || p.1 != sorted[i - 1].1 { place = i + 1 }
                return RankedBank(name: shortName(p.0), rate: p.1, place: place, gap: abs(p.1 - best), logo: logoSlug(p.0))
            }
        }
        let sell = rank(api.banks.compactMap { b in b.buy.map { (b.bank, Int($0.rounded())) } }, higherIsBetter: true)
        let buy = rank(api.banks.compactMap { b in b.sell.map { (b.bank, Int($0.rounded())) } }, higherIsBetter: false)
        return RatesEntry(date: .now, currency: cur, cbu: api.cbu.rate, cbuDiff: api.cbu.diff ?? 0,
                          updated: tashkentTime(api.updatedAt), sell: sell, buy: buy, bankCount: api.banks.count, ok: !sell.isEmpty && !buy.isEmpty)
    }

    /// Gallery sample: a plausible ranking at the currency's own scale (USD ≈ 12 000, EUR ≈ 14 000, RUB ≈ 146 so'm).
    static func placeholder(_ cur: WidgetCurrency, ok: Bool = true) -> RatesEntry {
        let (cbu, sellRates, buyRates): (Double, [Int], [Int]) = switch cur {
        case .usd: (11980.36, [11960, 11960, 11950, 11950, 11940, 11940, 11940, 11935], [11985, 11985, 11990, 11995, 12000, 12010, 12020, 12020])
        case .eur: (13995.10, [13950, 13900, 13900, 13850, 13800, 13800, 13750, 13700], [14100, 14150, 14150, 14200, 14250, 14250, 14300, 14350])
        case .rub: (145.62, [120, 116, 115, 102, 100, 95, 90, 90], [142, 142, 146, 146, 146, 147, 148, 150])
        }
        let names = ["Asia Alliance", "Kapitalbank", "Milliy bank", "Hamkorbank", "Orient Finans", "Aloqabank", "Apex Bank", "Infinbank"]
        func rank(_ rates: [Int], higher: Bool) -> [RankedBank] {
            var place = 0
            return rates.enumerated().map { i, r in
                if i == 0 || r != rates[i - 1] { place = i + 1 }
                return RankedBank(name: names[i], rate: r, place: place, gap: abs(r - rates[0]), logo: logoSlug(names[i]))
            }
        }
        return RatesEntry(date: .now, currency: cur, cbu: cbu, cbuDiff: -0.02 * cbu / 100, updated: "09:00",
                          sell: rank(sellRates, higher: true), buy: rank(buyRates, higher: false), bankCount: 29, ok: ok)
    }

    private static func tashkentTime(_ iso: String?) -> String {
        guard let iso else { return "" }
        let f = ISO8601DateFormatter(); f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let g = ISO8601DateFormatter(); g.formatOptions = [.withInternetDateTime]
        guard let d = f.date(from: iso) ?? g.date(from: iso) else { return "" }
        let out = DateFormatter(); out.dateFormat = "HH:mm"; out.timeZone = TimeZone(identifier: "Asia/Tashkent")
        return out.string(from: d)
    }

    /// Same short names as the app's widget data (WidgetData.kt).
    static func shortName(_ name: String) -> String {
        let key = name.lowercased().filter { $0.isLetter || $0.isNumber }
        if key.contains("milliy") { return "Milliy bank" }
        if key.contains("sanoatqurilish") { return "SQB" }
        if key.contains("mikrokredit") || key.contains("mkbank") { return "MKBank" }
        if key.contains("asiaalliance") { return "Asia Alliance" }
        if key.contains("orientfinans") { return "Orient Finans" }
        if key.contains("ipakyuli") { return "Ipak Yo‘li" }
        if key.hasPrefix("kdb") { return "KDB Bank" }
        if key.hasPrefix("apex") { return "Apex Bank" }
        let trimmed = name.replacingOccurrences(of: #"\s+(Bank|bank|banki|Banki)$"#, with: "", options: .regularExpression)
        return trimmed.isEmpty ? name : trimmed
    }

    /// Asset names "logo-<slug>", same slugs as the website's bank-logos.
    static func logoSlug(_ name: String) -> String? {
        let k = name.lowercased().filter { $0.isLetter || $0.isNumber }
        let table: [(String, String)] = [
            ("anor", "anorbank"), ("hayot", "hayot-bank"), ("tenge", "tenge-bank"), ("infin", "infinbank"), ("hamkor", "hamkorbank"),
            ("agro", "agrobank"), ("trast", "trastbank"), ("trust", "trastbank"), ("universal", "universal-bank"), ("poytaxt", "poytaxt-bank"),
            ("ipoteka", "ipoteka-bank"), ("asiaalliance", "asia-alliance"), ("aloqa", "aloqabank"), ("garant", "garant-bank"),
            ("turon", "turonbank"), ("orientfinans", "orient-finans"), ("sanoatqurilish", "sqb"), ("mkbank", "mkbank"),
            ("mikrokredit", "mkbank"), ("xalq", "xalq-bank"), ("ipakyuli", "ipak-yuli"), ("asaka", "asakabank"), ("davr", "davrbank"),
            ("apex", "apexbank"), ("milliy", "nbu"), ("openbank", "openbank"), ("ziraat", "ziraat-bank"), ("octo", "octobank"), ("kapital", "kapitalbank")
        ]
        if k == "brb" { return "logo-brb" }
        if k.hasPrefix("kdb") { return "logo-kdb-bank" }
        return table.first { k.contains($0.0) }.map { "logo-\($0.1)" }
    }
}

struct Provider: TimelineProvider {
    let currency: WidgetCurrency
    func placeholder(in context: Context) -> RatesEntry { RatesLoader.placeholder(currency) }
    func getSnapshot(in context: Context, completion: @escaping (RatesEntry) -> Void) {
        if context.isPreview { completion(RatesLoader.placeholder(currency)); return }
        Task { completion(await RatesLoader.load(currency)) }
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<RatesEntry>) -> Void) {
        Task {
            let entry = await RatesLoader.load(currency)
            // The API is rebuilt hourly; ask again in 30 minutes (sooner after a failed fetch).
            let next = Calendar.current.date(byAdding: .minute, value: entry.ok ? 30 : 10, to: .now)!
            completion(Timeline(entries: [entry], policy: .after(next)))
        }
    }
}

// MARK: - Look (the app's palette, light and dark)

private extension Color {
    init(light: UInt32, dark: UInt32) {
        self = Color(UIColor { t in
            let v = t.userInterfaceStyle == .dark ? dark : light
            return UIColor(red: CGFloat((v >> 16) & 0xFF) / 255, green: CGFloat((v >> 8) & 0xFF) / 255, blue: CGFloat(v & 0xFF) / 255, alpha: 1)
        })
    }
    static let wInk = Color(light: 0x121A1F, dark: 0xF2F4F5)
    static let wSoft = Color(light: 0x5A6670, dark: 0xA3ACB2)
    static let wEmerald = Color(light: 0x0A7C72, dark: 0x3DD6A0)
    static let wOnEmerald = Color(light: 0xFFFFFF, dark: 0x0E1113)
    static let wTile = Color(light: 0xF1F3F5, dark: 0x22282C)
    static let wTeal = Color(light: 0xD3EEEA, dark: 0x12332E)
    static let wRose = Color(light: 0xB4433A, dark: 0xFF8A80)
    static let wGold = Color(red: 0xF2 / 255, green: 0xC1 / 255, blue: 0x4E / 255)
}

private let ru = (Locale.preferredLanguages.first ?? "uz").hasPrefix("ru")
private func L(_ uz: String, _ ruText: String) -> String { ru ? ruText : uz }

private func grouped(_ n: Int) -> String {
    let s = String(abs(n)); var out = ""
    for (i, c) in s.reversed().enumerated() { if i > 0 && i % 3 == 0 { out.append(" ") }; out.append(c) }
    return (n < 0 ? "−" : "") + String(out.reversed())
}
private func rate2(_ v: Double) -> String {
    let whole = Int(v); let frac = Int(((v - Double(whole)) * 100).rounded())
    return frac == 0 ? grouped(whole) : "\(grouped(whole)),\(String(format: "%02d", frac))"
}

// MARK: - Views

struct Header: View {
    let e: RatesEntry
    let showCbu: Bool
    var body: some View {
        HStack(spacing: 6) {
            Text(e.currency.symbol)
                .font(.system(size: 12, weight: .bold, design: .rounded)).foregroundStyle(Color.wOnEmerald)
                .frame(width: 20, height: 20).background(Color.wEmerald, in: RoundedRectangle(cornerRadius: 6))
            Text(e.currency.rawValue).font(.system(size: 14, weight: .bold)).foregroundStyle(Color.wInk)
            if showCbu {
                Text("\(L("MB", "ЦБ")) \(rate2(e.cbu))").font(.system(size: 12)).foregroundStyle(Color.wSoft).lineLimit(1)
                if abs(e.cbuDiff) >= 0.005 {
                    Text("\(e.cbuDiff > 0 ? "▲" : "▼")\(rate2(abs(e.cbuDiff)))")
                        .font(.system(size: 12)).foregroundStyle(e.cbuDiff > 0 ? Color.wEmerald : Color.wRose).lineLimit(1)
                }
            }
            Spacer(minLength: 4)
            Text(e.updated).font(.system(size: 11)).foregroundStyle(Color.wSoft)
        }
    }
}

/// Small: both rankings stacked, three places each.
struct SmallView: View {
    let e: RatesEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Header(e: e, showCbu: false)
            mini("↑ " + L("Sotsangiz", "Продаёте"), Array(e.sell.prefix(3)))
            mini("↓ " + L("Olsangiz", "Покупаете"), Array(e.buy.prefix(3)))
        }
    }
    func mini(_ title: String, _ list: [RankedBank]) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(title).font(.system(size: 10, weight: .medium)).foregroundStyle(Color.wSoft)
            ForEach(list) { b in
                let lead = b.place == 1
                HStack(spacing: 4) {
                    Text("\(b.place)").font(.system(size: 8, weight: .bold))
                        .foregroundStyle(lead ? Color(red: 0x5A / 255, green: 0x3F / 255, blue: 0) : Color.wSoft)
                        .frame(width: 13, height: 13).background(lead ? Color.wGold : Color.wTile, in: Circle())
                    Text(b.name).font(.system(size: 11, weight: lead ? .bold : .regular)).foregroundStyle(Color.wInk)
                        .lineLimit(1).minimumScaleFactor(0.75)
                    Spacer(minLength: 2)
                    Text(grouped(b.rate)).font(.system(size: 11, weight: .bold, design: .rounded))
                        .foregroundStyle(lead ? Color.wEmerald : Color.wInk).fixedSize()
                }
                .padding(.horizontal, 3).frame(height: 16)
                .background(lead ? Color.wTeal : .clear, in: RoundedRectangle(cornerRadius: 5))
            }
        }
    }
}

struct RankRow: View {
    let b: RankedBank
    let sign: String
    let roomy: Bool
    var body: some View {
        let lead = b.place == 1
        HStack(spacing: 4) {
            Text("\(b.place)").font(.system(size: roomy ? 10 : 9, weight: .bold))
                .foregroundStyle(lead ? Color(red: 0x5A / 255, green: 0x3F / 255, blue: 0) : Color.wSoft)
                .frame(width: roomy ? 17 : 15, height: roomy ? 17 : 15).background(lead ? Color.wGold : Color.wTile, in: Circle())
            if roomy, let logo = b.logo {
                Image(logo).resizable().scaledToFit().frame(width: 16, height: 16).clipShape(RoundedRectangle(cornerRadius: 4))
            }
            // The name gives way (shrinks, then truncates); the rate never does.
            Text(b.name).font(.system(size: roomy ? 12 : 11, weight: lead ? .bold : .regular)).foregroundStyle(Color.wInk)
                .lineLimit(1).minimumScaleFactor(0.75)
            Spacer(minLength: 2)
            VStack(alignment: .trailing, spacing: 0) {
                Text(grouped(b.rate)).font(.system(size: roomy ? 12.5 : 12, weight: .bold, design: .rounded))
                    .foregroundStyle(lead ? Color.wEmerald : Color.wInk).lineLimit(1).fixedSize()
                if roomy && b.gap > 0 {
                    Text("\(sign)\(b.gap)").font(.system(size: 9)).foregroundStyle(Color.wSoft).fixedSize()
                }
            }
            .layoutPriority(1)
        }
        .padding(.horizontal, 4)
        .frame(maxWidth: .infinity, minHeight: roomy ? 28 : 18)
        .background(lead ? Color.wTeal : .clear, in: RoundedRectangle(cornerRadius: 8))
    }
}

struct BoardView: View {
    let e: RatesEntry
    let rows: Int
    let roomy: Bool
    var body: some View {
        VStack(alignment: .leading, spacing: roomy ? 8 : 5) {
            Header(e: e, showCbu: true)
            HStack(alignment: .top, spacing: 8) {
                column("↑ " + L("Sotsangiz", "Продаёте"), Array(e.sell.prefix(rows)), sign: "−")
                column("↓ " + L("Olsangiz", "Покупаете"), Array(e.buy.prefix(rows)), sign: "+")
            }
            if roomy {
                Spacer(minLength: 0)
                Text(L("\(e.bankCount) ta bank · 1 \(e.currency.rawValue) uchun, soʻm", "\(e.bankCount) банков · за 1 \(e.currency.rawValue), сум"))
                    .font(.system(size: 11)).foregroundStyle(Color.wSoft)
            }
        }
    }
    func column(_ title: String, _ list: [RankedBank], sign: String) -> some View {
        VStack(alignment: .leading, spacing: roomy ? 3 : 1) {
            Text(title).font(.system(size: 11, weight: .medium)).foregroundStyle(Color.wSoft).padding(.bottom, roomy ? 3 : 1)
            ForEach(list) { RankRow(b: $0, sign: sign, roomy: roomy) }
        }
        .frame(maxWidth: .infinity)
    }
}

struct QulayWidgetView: View {
    @Environment(\.widgetFamily) var family
    let entry: RatesEntry
    var body: some View {
        Group {
            if !entry.ok {
                Text(L("Kurslar yuklanmoqda…", "Загрузка курсов…")).font(.system(size: 13)).foregroundStyle(Color.wSoft)
            } else {
                switch family {
                case .systemSmall: SmallView(e: entry)
                case .systemMedium: BoardView(e: entry, rows: 3, roomy: false)
                case .accessoryRectangular: LockView(e: entry)
                default: BoardView(e: entry, rows: 8, roomy: true)
                }
            }
        }
        .containerBackground(for: .widget) { Color("WidgetBackground") }
    }
}

/// Lock screen: the two best rates in one glance.
struct LockView: View {
    let e: RatesEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 1) {
            Text("\(e.currency.rawValue) · \(e.updated)").font(.system(size: 11, weight: .semibold))
            if let s = e.sell.first { Text("↑ \(grouped(s.rate)) \(s.name)").font(.system(size: 13, weight: .bold)).lineLimit(1) }
            if let b = e.buy.first { Text("↓ \(grouped(b.rate)) \(b.name)").font(.system(size: 13, weight: .bold)).lineLimit(1) }
        }
    }
}

/// One widget per currency, each its own entry in the widget gallery.
private func ratesWidget(_ currency: WidgetCurrency) -> some WidgetConfiguration {
        StaticConfiguration(kind: "QulayKurs\(currency.rawValue)", provider: Provider(currency: currency)) { entry in
            QulayWidgetView(entry: entry)
        }
        // A plain String: an interpolated literal becomes formatted text, which WidgetKit rejects (the extension crashes).
        .configurationDisplayName(currency.name + " — Qulay Kurs" as String)
        .description(L("Banklar reytingi: qayerda sotish va olish foydali", "Рейтинг банков: где выгодно продать и купить"))
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryRectangular])
}

struct UsdRatesWidget: Widget { var body: some WidgetConfiguration { ratesWidget(.usd) } }
struct EurRatesWidget: Widget { var body: some WidgetConfiguration { ratesWidget(.eur) } }
struct RubRatesWidget: Widget { var body: some WidgetConfiguration { ratesWidget(.rub) } }

@main
struct QulayWidgetBundle: WidgetBundle {
    var body: some Widget {
        UsdRatesWidget()
        EurRatesWidget()
        RubRatesWidget()
    }
}

#Preview(as: .systemSmall) { UsdRatesWidget() } timeline: { RatesLoader.placeholder(.usd) }
