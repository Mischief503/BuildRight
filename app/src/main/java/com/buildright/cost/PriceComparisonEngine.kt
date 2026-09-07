package com.buildright.cost

data class RetailerPlanSummary(
    val retailerId: String,
    val plan: PurchasePlan,
    val coverageCount: Int,
    val requirementCount: Int
) {
    val complete: Boolean get() = coverageCount == requirementCount
}

data class PriceComparisonResult(
    val cheapestSplit: PurchasePlan,
    val fewestStores: PurchasePlan,
    val byRetailer: List<RetailerPlanSummary>
) {
    val cheapestCompleteSingleStore: RetailerPlanSummary?
        get() = byRetailer.filter { it.complete }.minByOrNull { it.plan.total }

    val splitSavingsVsSingleStore: Double
        get() = (cheapestCompleteSingleStore?.plan?.total ?: cheapestSplit.total) - cheapestSplit.total
}

object PriceComparisonEngine {
    fun compare(requirements: List<MaterialRequirement>, quotes: List<PriceQuote>): PriceComparisonResult {
        val retailers = quotes.map { it.retailerId }.distinct()
        val byRetailer = retailers.map { retailer ->
            val p = PurchasePlanner.preferredRetailerPlan(requirements, quotes, retailer)
            RetailerPlanSummary(retailer, p, p.lines.map { it.requirement.key }.distinct().size, requirements.size)
        }.sortedWith(compareBy<RetailerPlanSummary> { !it.complete }.thenBy { it.plan.total })
        return PriceComparisonResult(
            cheapestSplit = PurchasePlanner.cheapestPlan(requirements, quotes),
            fewestStores = PurchasePlanner.fewestStoresPlan(requirements, quotes),
            byRetailer = byRetailer
        )
    }

    fun quoteFromProductPage(requirement: MaterialRequirement, product: ProductPageResult, storeId: String? = null): PriceQuote =
        PriceQuote(
            requirementKey = requirement.key,
            retailerId = product.retailerId,
            storeId = storeId,
            sku = product.sku,
            productName = product.productName,
            packageQuantity = product.packageQuantity,
            packageUnit = product.packageUnit,
            packagePrice = product.price,
            currency = product.currency,
            checkedAt = product.checkedAt,
            sourceReference = product.sourceUrl
        )
}
