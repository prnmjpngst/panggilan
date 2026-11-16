package com.prnmjpngst.pemanggilan

data class Vehicle(
    val id: String,
    val raw: Map<String, Any?>,
    val status: String,
    val calledCount: Int,
    val lastCalledAt: String?,
    val createdAt: String? = null,
) {
    fun field(cfg: AppConfig, col: String): String {
        val key = when (col) {
            "plate" -> cfg.plateColumn
            "id" -> cfg.idColumn
            "result" -> cfg.resultColumn
            "owner" -> cfg.ownerColumn
            else -> col
        }
        return raw[key]?.toString()?.trim() ?: ""
    }
}

fun VehicleDto.toVehicle() = Vehicle(
    id = id,
    raw = raw ?: emptyMap(),
    status = status,
    calledCount = calledCount,
    lastCalledAt = lastCalledAt,
    createdAt = createdAt,
)

fun isGagal(v: Vehicle, cfg: AppConfig): Boolean =
    v.field(cfg, "result").equals(cfg.gagalValue, ignoreCase = true)

fun isLulus(v: Vehicle, cfg: AppConfig): Boolean =
    v.field(cfg, "result").equals(cfg.lulusValue, ignoreCase = true)