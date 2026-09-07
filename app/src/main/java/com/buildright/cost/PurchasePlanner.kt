package com.buildright.cost

import kotlin.math.ceil

object PurchasePlanner {
    fun line(requirement: MaterialRequirement, quote: PriceQuote): PurchaseLine {
        require(quote.packageQuantity > 0)
        val packages = ceil(requirement.quantityNeeded / quote.packageQuantity).toInt()
        return PurchaseLine(
            requirement = requirement,
            quote = quote,
            packagesToBuy = packages,
            purchasedQuantity = packages * quote.packageQuantity,
            extendedPrice = packages * quote.packagePrice
        )
    }

    fun cheapestPlan(
        requirements: List<MaterialRequirement>,
        quotes: List<PriceQuote>
    ): PurchasePlan {
        val lines = requirements.mapNotNull { req ->
            quotes.filter { it.requirementKey == req.key }
                .map { line(req, it) }
                .minByOrNull { it.extendedPrice }
        }
        return PurchasePlan(lines, PurchaseStrategy.CHEAPEST_TOTAL)
    }

    fun preferredRetailerPlan(
        requirements: List<MaterialRequirement>,
        quotes: List<PriceQuote>,
        retailerId: String
    ): PurchasePlan {
        val lines = requirements.mapNotNull { req ->
            quotes.filter { it.requirementKey == req.key && it.retailerId == retailerId }
                .map { line(req, it) }
                .minByOrNull { it.extendedPrice }
        }
        return PurchasePlan(lines, PurchaseStrategy.PREFERRED_RETAILER)
    }

    fun fewestStoresPlan(
        requirements: List<MaterialRequirement>,
        quotes: List<PriceQuote>
    ): PurchasePlan {
        val retailers = quotes.map { it.retailerId }.distinct()
        val bestSingle = retailers.map { r -> preferredRetailerPlan(requirements, quotes, r) }
            .filter { it.lines.size == requirements.size }
            .minByOrNull { it.total }
        return bestSingle ?: cheapestPlan(requirements, quotes).copy(strategy = PurchaseStrategy.FEWEST_STORES)
    }
}
