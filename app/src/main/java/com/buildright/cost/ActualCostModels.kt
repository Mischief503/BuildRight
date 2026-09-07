package com.buildright.cost

data class Receipt(
    val id: String,
    val retailerName: String,
    val purchasedAt: Long,
    val total: Double,
    val localImageUri: String? = null,
    val note: String = ""
)

data class ActualPurchase(
    val id: String,
    val requirementKey: String?,
    val description: String,
    val quantity: Double,
    val unit: String,
    val amountPaid: Double,
    val receiptId: String? = null
)

data class CostSummary(
    val estimatedMaterials: Double,
    val actualMaterials: Double,
    val toolsAndRentals: Double,
    val otherCosts: Double
) {
    val estimatedTotal: Double get() = estimatedMaterials + toolsAndRentals + otherCosts
    val actualTotal: Double get() = actualMaterials + toolsAndRentals + otherCosts
    val variance: Double get() = actualTotal - estimatedTotal
}
