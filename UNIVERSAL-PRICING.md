# BuildRight Universal Pricing Engine — v1.0.4

## What is implemented

BuildRight now has a provider-neutral pricing pipeline:

Material requirement -> quote -> package normalization -> purchase-line rounding -> retailer plans -> cheapest split plan.

### Universal product-page import

The Cost Intelligence screen can:
- select an existing project material;
- accept an http/https retailer product page URL;
- check robots.txt before automated access;
- read Product JSON-LD / schema.org structured price data when present;
- fall back to common OpenGraph / itemprop price metadata;
- infer package quantity from common pack and square-foot descriptions;
- normalize package price to a per-unit price;
- save the source URL and timestamp;
- persist the full quote for later comparison.

If robots.txt blocks the requested path, BuildRight does not bypass it. The user can still use manual price entry or an authorized API/feed.

### Persisted quote fields

Each imported quote can retain:
- material id
- retailer id and name
- store id and name
- SKU
- product name
- package quantity
- package unit
- package price
- currency
- source URL
- checked timestamp

### Purchase comparison

PriceComparisonEngine calculates:
- cheapest split order;
- fewest-store plan;
- per-retailer complete/incomplete plans;
- cheapest complete single-store/source plan;
- split-order savings versus the best complete single source.

PurchasePlanner rounds up complete packages before calculating extended price.

### Pluggable search providers

`WebSourceConfig` + `ConfigurableHtmlSearchProvider` provide a generic adapter for suppliers that permit automated search. A source profile controls search URL, selectors, package defaults, robots handling, and whether automation is authorized.

Home Depot remains registered as automation-disabled until an authorized feed/API/integration is supplied. The architecture does not contain bypass logic.

## Libraries

- jsoup 1.23.2 for HTML/structured-data parsing and web requests.
- kotlinx-coroutines-android 1.11.0 for non-blocking price imports.

## Tests

Added tests for:
- complete-package rounding;
- split-cart vs single-store comparisons;
- package quantity parsing.

The core price comparison code was also compiled and executed independently in the build workspace.
