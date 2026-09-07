package com.buildright.code

interface CodeProvider {
    val providerId: String
    suspend fun rulesFor(jurisdiction: Jurisdiction): List<CodeRule>
}

/** Use for manually curated or imported official rules. */
class LocalCodeProvider(private val rules: List<CodeRule>) : CodeProvider {
    override val providerId: String = "local"
    override suspend fun rulesFor(jurisdiction: Jurisdiction): List<CodeRule> = rules.filter { it.jurisdiction == jurisdiction }
}

/**
 * Integration boundary for an official source. A loader is mandatory so the
 * provider cannot silently pretend to be connected when it is not.
 */
class OfficialSourceCodeProvider(
    private val loader: suspend (Jurisdiction) -> List<CodeRule>
) : CodeProvider {
    override val providerId: String = "official_source"
    override suspend fun rulesFor(jurisdiction: Jurisdiction): List<CodeRule> = loader(jurisdiction)
}
