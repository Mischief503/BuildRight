package com.buildright.cost

data class CatalogPrice(
    val materialKey: String,
    val description: String,
    val retailerId: String,
    val storeId: String?,
    val sku: String?,
    val packageQuantity: Double,
    val packageUnit: String,
    val price: Double,
    val recordedAt: Long
)

object LocalPriceCatalog {
    fun latest(prices: List<CatalogPrice>, materialKey: String, retailerId: String? = null): CatalogPrice? =
        prices.asSequence()
            .filter { it.materialKey == materialKey }
            .filter { retailerId == null || it.retailerId == retailerId }
            .maxByOrNull { it.recordedAt }
}
