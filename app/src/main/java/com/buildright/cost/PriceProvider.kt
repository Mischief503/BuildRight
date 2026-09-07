package com.buildright.cost

interface PriceProvider {
    val retailerId: String
    suspend fun search(requirement: MaterialRequirement, store: RetailerStore?): List<PriceQuote>
}

/** In-memory/manual quote provider used for entered, imported, or supplier-provided quotes. */
class ManualPriceProvider(
    private val quotes: List<PriceQuote>
) : PriceProvider {
    override val retailerId: String = "manual"
    override suspend fun search(requirement: MaterialRequirement, store: RetailerStore?): List<PriceQuote> =
        quotes.filter { q -> q.requirementKey == requirement.key && (store == null || q.storeId == null || q.storeId == store.storeId) }
}

/**
 * Deliberately disabled adapter. Home Depot data must enter through a permitted
 * feed/API or the universal product-page importer when that URL allows it.
 */
class HomeDepotPriceProvider : PriceProvider {
    override val retailerId: String = "home_depot"
    override suspend fun search(requirement: MaterialRequirement, store: RetailerStore?): List<PriceQuote> =
        throw UnsupportedOperationException("No authorized Home Depot search feed is configured")
}
