package com.buildright.model

import java.util.UUID

enum class WallSide { FRONT, REAR, LEFT, RIGHT }
enum class OpeningType { DOOR, WINDOW }
enum class AttachmentType { PHOTO, RECEIPT, DOCUMENT }

data class WallOpening(
    val id: String = UUID.randomUUID().toString(),
    val type: OpeningType,
    val wall: WallSide,
    val offsetFt: Double,
    val widthFt: Double,
    val heightFt: Double,
    val sillHeightFt: Double = 0.0,
    val label: String = ""
)

data class FieldMeasurement(
    val id: String = UUID.randomUUID().toString(),
    val stageId: String,
    val label: String,
    val value: Double,
    val unit: String,
    val recordedAt: Long = System.currentTimeMillis()
)

data class ProjectAttachment(
    val id: String = UUID.randomUUID().toString(),
    val stageId: String?,
    val type: AttachmentType,
    val localUri: String,
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ProjectHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)
