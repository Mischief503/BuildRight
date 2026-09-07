package com.buildright.cost

data class MaterialRequirement(
    val key: String,
    val description: String,
    val quantityNeeded: Double,
    val unit: String,
    val preferredSku: String? = null
)

data class RetailerStore(
    val retailerId: String,
    val storeId: String,
    val name: String,
    val postalCode: String? = null
)

data class PriceQuote(
    val requirementKey: String,
    val retailerId: String,
    val storeId: String?,
    val sku: String?,
    val productName: String,
    val packageQuantity: Double,
    val packageUnit: String,
    val packagePrice: Double,
    val currency: String = "USD",
    val checkedAt: Long = System.currentTimeMillis(),
    val sourceReference: String? = null
) {
    val normalizedUnitPrice: Double
        get() = if (packageQuantity <= 0) Double.POSITIVE_INFINITY else packagePrice / packageQuantity
}

data class PurchaseLine(
    val requirement: MaterialRequirement,
    val quote: PriceQuote,
    val packagesToBuy: Int,
    val purchasedQuantity: Double,
    val extendedPrice: Double
)

data class PurchasePlan(
    val lines: List<PurchaseLine>,
    val strategy: PurchaseStrategy
) {
    val total: Double get() = lines.sumOf { it.extendedPrice }
    val retailerCount: Int get() = lines.map { it.quote.retailerId }.distinct().size
}

enum class PurchaseStrategy { CHEAPEST_TOTAL, FEWEST_STORES, PREFERRED_RETAILER, MANUAL }


data class ProductPageResult(
    val retailerId: String,
    val productName: String,
    val sku: String?,
    val price: Double,
    val currency: String,
    val packageQuantity: Double,
    val packageUnit: String,
    val sourceUrl: String,
    val checkedAt: Long = System.currentTimeMillis()
)
