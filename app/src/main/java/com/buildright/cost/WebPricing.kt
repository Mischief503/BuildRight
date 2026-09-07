package com.buildright.cost

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Universal web pricing primitives.
 *
 * BuildRight only runs automated fetching for sources marked automationAllowed=true.
 * robots.txt is checked by default before a page/search fetch. A blocked source is not bypassed.
 */
data class WebSourceConfig(
    val retailerId: String,
    val displayName: String,
    val baseUrl: String,
    val automationAllowed: Boolean,
    val enabled: Boolean = true,
    val respectRobots: Boolean = true,
    val searchUrlTemplate: String? = null,
    val resultSelector: String? = null,
    val nameSelector: String? = null,
    val priceSelector: String? = null,
    val linkSelector: String? = null,
    val skuSelector: String? = null,
    val defaultPackageQuantity: Double = 1.0,
    val defaultPackageUnit: String = "pcs"
)

data class WebFetchDecision(
    val allowed: Boolean,
    val reason: String
)

sealed class ProductPageImportResult {
    data class Success(val product: ProductPageResult) : ProductPageImportResult()
    data class Blocked(val reason: String) : ProductPageImportResult()
    data class Failure(val reason: String) : ProductPageImportResult()
}

object PriceTextParser {
    private val number = Regex("[-+]?[0-9][0-9,]*(?:\\.[0-9]{1,4})?")

    fun parse(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        return number.find(text)?.value?.replace(",", "")?.toDoubleOrNull()
    }
}

object PackageQuantityParser {
    private val packRegex = Regex("(?i)(?:pack|pk|case|box|bundle)\\s*(?:of\\s*)?(\\d+(?:\\.\\d+)?)|(?:\\b(\\d+(?:\\.\\d+)?)\\s*[- ]?(?:pack|pk)\\b)")
    private val areaRegex = Regex("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:sq\\.?\\s*ft|square\\s*feet|ft²)")

    fun fromText(text: String, fallbackQuantity: Double = 1.0, fallbackUnit: String = "pcs"): Pair<Double, String> {
        areaRegex.find(text)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let { return it to "sq ft" }
        val m = packRegex.find(text)
        if (m != null) {
            val q = listOf(m.groupValues.getOrNull(1), m.groupValues.getOrNull(2)).firstNotNullOfOrNull { it?.toDoubleOrNull() }
            if (q != null && q > 0) return q to "pcs"
        }
        return fallbackQuantity to fallbackUnit
    }
}

object RetailerIdentity {
    fun fromUrl(url: String): Pair<String, String> {
        val host = runCatching { URI(url).host.orEmpty().lowercase(Locale.US) }.getOrDefault("")
            .removePrefix("www.")
        val id = host.substringBefore('.').ifBlank { "web" }.replace(Regex("[^a-z0-9_]+"), "_")
        val display = host.ifBlank { "Web source" }
        return id to display
    }
}

object RobotsPolicy {
    private const val USER_AGENT = "BuildRight"

    fun check(url: String, timeoutMs: Int = 8000): WebFetchDecision {
        val uri = runCatching { URI(url) }.getOrElse { return WebFetchDecision(false, "Invalid URL") }
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            return WebFetchDecision(false, "Only http/https product pages are supported")
        }
        val robotsUrl = "${uri.scheme}://${uri.host}${if (uri.port > 0) ":${uri.port}" else ""}/robots.txt"
        val body = runCatching {
            Jsoup.connect(robotsUrl)
                .userAgent("BuildRight/1.0")
                .timeout(timeoutMs)
                .ignoreContentType(true)
                .execute()
                .body()
        }.getOrElse {
            // If robots.txt is unavailable, do not treat the source as explicitly blocked.
            return WebFetchDecision(true, "robots.txt unavailable; source policy still applies")
        }

        val path = (uri.rawPath ?: "/") + (uri.rawQuery?.let { "?$it" } ?: "")
        var applies = false
        val rules = mutableListOf<Pair<Boolean, String>>() // allow?, path
        body.lineSequence().forEach { raw ->
            val line = raw.substringBefore('#').trim()
            if (line.isBlank() || !line.contains(':')) return@forEach
            val key = line.substringBefore(':').trim().lowercase(Locale.US)
            val value = line.substringAfter(':').trim()
            when (key) {
                "user-agent" -> {
                    val ua = value.lowercase(Locale.US)
                    applies = ua == "*" || ua.contains(USER_AGENT.lowercase(Locale.US))
                }
                "disallow" -> if (applies && value.isNotBlank()) rules += false to value
                "allow" -> if (applies && value.isNotBlank()) rules += true to value
            }
        }
        val matching = rules.filter { (_, rule) ->
            if (rule == "/") true else path.startsWith(rule.substringBefore('*'))
        }.maxByOrNull { it.second.length }
        return if (matching != null && !matching.first) WebFetchDecision(false, "robots.txt disallows automated access to this path")
        else WebFetchDecision(true, "robots.txt allows this path")
    }
}

object ProductPageImporter {
    fun fetch(url: String, respectRobots: Boolean = true, timeoutMs: Int = 12000): ProductPageImportResult {
        if (respectRobots) {
            val decision = RobotsPolicy.check(url, timeoutMs.coerceAtMost(8000))
            if (!decision.allowed) return ProductPageImportResult.Blocked(decision.reason)
        }
        return runCatching {
            val doc = Jsoup.connect(url)
                .userAgent("BuildRight/1.0")
                .timeout(timeoutMs)
                .followRedirects(true)
                .get()
            extract(doc, doc.location().ifBlank { url })
                ?: return ProductPageImportResult.Failure("No structured product price was found on this page")
        }.getOrElse { ProductPageImportResult.Failure(it.message ?: "Unable to read product page") }
    }

    internal fun extract(doc: Document, sourceUrl: String): ProductPageImportResult.Success? {
        extractJsonLd(doc, sourceUrl)?.let { return ProductPageImportResult.Success(it) }

        val name = doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: doc.selectFirst("meta[name=twitter:title]")?.attr("content")
            ?: doc.title().takeIf { it.isNotBlank() }
            ?: return null
        val rawPrice = doc.selectFirst("meta[property=product:price:amount]")?.attr("content")
            ?: doc.selectFirst("meta[itemprop=price]")?.attr("content")
            ?: doc.selectFirst("[itemprop=price]")?.attr("content")
            ?: doc.selectFirst("[itemprop=price]")?.text()
        val price = PriceTextParser.parse(rawPrice) ?: return null
        val currency = doc.selectFirst("meta[property=product:price:currency]")?.attr("content")
            ?: doc.selectFirst("meta[itemprop=priceCurrency]")?.attr("content")
            ?: "USD"
        val sku = doc.selectFirst("meta[itemprop=sku]")?.attr("content")
            ?: doc.selectFirst("[itemprop=sku]")?.text()?.takeIf { it.isNotBlank() }
        val (qty, unit) = PackageQuantityParser.fromText(name)
        val retailer = RetailerIdentity.fromUrl(sourceUrl).first
        return ProductPageImportResult.Success(ProductPageResult(retailer, name, sku, price, currency, qty, unit, sourceUrl))
    }

    private fun extractJsonLd(doc: Document, sourceUrl: String): ProductPageResult? {
        val candidates = mutableListOf<JSONObject>()
        doc.select("script[type=application/ld+json]").forEach { script ->
            val raw = script.data().ifBlank { script.html() }.trim()
            if (raw.isBlank()) return@forEach
            runCatching {
                when {
                    raw.startsWith("[") -> collectProducts(JSONArray(raw), candidates)
                    raw.startsWith("{") -> collectProducts(JSONObject(raw), candidates)
                }
            }
        }
        for (p in candidates) {
            val name = p.optString("name").takeIf { it.isNotBlank() } ?: continue
            val sku = p.optString("sku").takeIf { it.isNotBlank() }
            val offer = when (val o = p.opt("offers")) {
                is JSONObject -> o
                is JSONArray -> (0 until o.length()).mapNotNull { o.optJSONObject(it) }.firstOrNull()
                else -> null
            } ?: continue
            val price = when (val raw = offer.opt("price")) {
                is Number -> raw.toDouble()
                is String -> PriceTextParser.parse(raw)
                else -> PriceTextParser.parse(offer.optString("lowPrice"))
            } ?: continue
            val currency = offer.optString("priceCurrency", "USD")
            val (qty, unit) = PackageQuantityParser.fromText(name)
            val retailer = RetailerIdentity.fromUrl(sourceUrl).first
            return ProductPageResult(retailer, name, sku, price, currency, qty, unit, sourceUrl)
        }
        return null
    }

    private fun collectProducts(value: Any, out: MutableList<JSONObject>) {
        when (value) {
            is JSONArray -> (0 until value.length()).forEach { value.opt(it)?.let { collectProducts(it, out) } }
            is JSONObject -> {
                val type = value.opt("@type")
                val isProduct = when (type) {
                    is String -> type.equals("Product", true)
                    is JSONArray -> (0 until type.length()).any { type.optString(it).equals("Product", true) }
                    else -> false
                }
                if (isProduct) out += value
                value.opt("@graph")?.let { collectProducts(it, out) }
            }
        }
    }
}

class ConfigurableHtmlSearchProvider(private val config: WebSourceConfig) : PriceProvider {
    override val retailerId: String = config.retailerId

    override suspend fun search(requirement: MaterialRequirement, store: RetailerStore?): List<PriceQuote> {
        if (!config.enabled || !config.automationAllowed) return emptyList()
        val template = config.searchUrlTemplate ?: return emptyList()
        val q = URLEncoder.encode(requirement.description, StandardCharsets.UTF_8.toString())
        val postal = URLEncoder.encode(store?.postalCode.orEmpty(), StandardCharsets.UTF_8.toString())
        val url = template.replace("{query}", q).replace("{postalCode}", postal)
        if (config.respectRobots && !RobotsPolicy.check(url).allowed) return emptyList()

        val doc = runCatching {
            Jsoup.connect(url).userAgent("BuildRight/1.0").timeout(12000).followRedirects(true).get()
        }.getOrElse { return emptyList() }
        val resultSelector = config.resultSelector ?: return emptyList()
        return doc.select(resultSelector).mapNotNull { row ->
            val productName = config.nameSelector?.let { row.selectFirst(it)?.text() }?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val priceText = config.priceSelector?.let { sel ->
                row.selectFirst(sel)?.let { el -> el.attr("content").ifBlank { el.text() } }
            }
            val price = PriceTextParser.parse(priceText) ?: return@mapNotNull null
            val href = config.linkSelector?.let { row.selectFirst(it)?.absUrl("href") }?.takeIf { it.isNotBlank() }
            val sku = config.skuSelector?.let { row.selectFirst(it)?.text() }?.takeIf { it.isNotBlank() }
            val (packageQty, packageUnit) = PackageQuantityParser.fromText(productName, config.defaultPackageQuantity, config.defaultPackageUnit)
            PriceQuote(
                requirementKey = requirement.key,
                retailerId = config.retailerId,
                storeId = store?.storeId,
                sku = sku,
                productName = productName,
                packageQuantity = packageQty,
                packageUnit = packageUnit,
                packagePrice = price,
                checkedAt = System.currentTimeMillis(),
                sourceReference = href ?: url
            )
        }
    }
}

object RetailerRegistry {
    /**
     * Home Depot is deliberately registered as disabled for automated search until an authorized
     * endpoint/feed is supplied. BuildRight will not bypass a site's automation restrictions.
     */
    val homeDepot = WebSourceConfig(
        retailerId = "home_depot",
        displayName = "Home Depot",
        baseUrl = "https://www.homedepot.com",
        automationAllowed = false,
        enabled = false
    )

    fun provider(config: WebSourceConfig): PriceProvider = ConfigurableHtmlSearchProvider(config)
}
