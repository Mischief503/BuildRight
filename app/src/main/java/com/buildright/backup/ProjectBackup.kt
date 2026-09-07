package com.buildright.backup

const val BUILD_RIGHT_SCHEMA_VERSION = 4

data class ProjectBackupEnvelope<T>(
    val app: String = "BuildRight",
    val schemaVersion: Int = BUILD_RIGHT_SCHEMA_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val project: T
)

sealed class ImportValidation {
    data object Valid : ImportValidation()
    data class UnsupportedVersion(val found: Int, val supported: Int) : ImportValidation()
    data class Invalid(val reason: String) : ImportValidation()
}

object BackupValidator {
    fun validate(app: String?, schemaVersion: Int?): ImportValidation {
        if (app != "BuildRight") return ImportValidation.Invalid("Not a BuildRight backup.")
        val version = schemaVersion ?: return ImportValidation.Invalid("Backup has no schema version.")
        if (version > BUILD_RIGHT_SCHEMA_VERSION) {
            return ImportValidation.UnsupportedVersion(version, BUILD_RIGHT_SCHEMA_VERSION)
        }
        return ImportValidation.Valid
    }
}
