package com.prnmjpngst.pemanggilan

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "pemanggilan_config")

data class AppConfig(
    val baseUrl: String = "https://ann.prnmjpngst.pw",
    val apiKey: String = "panggilan-dev-key-2026-change-me",
    val pollIntervalSec: Int = 30,
    val repeatCount: Int = 2,
    val repeatDelaySec: Int = 5,
    val autoPlayGagal: Boolean = true,
    val autoPlayLulus: Boolean = false,
    val plateColumn: String = "NOMOR KENDARAAN",
    val idColumn: String = "NOMOR UJI",
    val resultColumn: String = "HASIL UJI",
    val ownerColumn: String = "NOMOR WA",
    val gagalValue: String = "PANGGIL",
    val lulusValue: String = "LULUS",
)

object ConfigKeys {
    val BASE_URL = stringPreferencesKey("base_url")
    val API_KEY = stringPreferencesKey("api_key")
    val POLL_INTERVAL = intPreferencesKey("poll_interval")
    val REPEAT_COUNT = intPreferencesKey("repeat_count")
    val REPEAT_DELAY = intPreferencesKey("repeat_delay")
    val AUTO_GAGAL = booleanPreferencesKey("auto_gagal")
    val AUTO_LULUS = booleanPreferencesKey("auto_lulus")
    val PLATE_COL = stringPreferencesKey("plate_col")
    val ID_COL = stringPreferencesKey("id_col")
    val RESULT_COL = stringPreferencesKey("result_col")
    val OWNER_COL = stringPreferencesKey("owner_col")
    val GAGAL_VAL = stringPreferencesKey("gagal_val")
    val LULUS_VAL = stringPreferencesKey("lulus_val")
}

fun Context.configFlow(): Flow<AppConfig> = dataStore.data.map { p ->
    AppConfig(
        baseUrl = p[ConfigKeys.BASE_URL] ?: "https://ann.prnmjpngst.pw",
        apiKey = p[ConfigKeys.API_KEY] ?: "panggilan-dev-key-2026-change-me",
        pollIntervalSec = p[ConfigKeys.POLL_INTERVAL] ?: 30,
        repeatCount = p[ConfigKeys.REPEAT_COUNT] ?: 2,
        repeatDelaySec = p[ConfigKeys.REPEAT_DELAY] ?: 5,
        autoPlayGagal = p[ConfigKeys.AUTO_GAGAL] ?: true,
        autoPlayLulus = p[ConfigKeys.AUTO_LULUS] ?: false,
        plateColumn = p[ConfigKeys.PLATE_COL] ?: "NOMOR KENDARAAN",
        idColumn = p[ConfigKeys.ID_COL] ?: "NOMOR UJI",
        resultColumn = p[ConfigKeys.RESULT_COL] ?: "HASIL UJI",
        ownerColumn = p[ConfigKeys.OWNER_COL] ?: "NOMOR WA",
        gagalValue = p[ConfigKeys.GAGAL_VAL] ?: "PANGGIL",
        lulusValue = p[ConfigKeys.LULUS_VAL] ?: "LULUS",
    )
}

suspend fun Context.saveConfig(c: AppConfig) {
    dataStore.edit { p ->
        p[ConfigKeys.BASE_URL] = c.baseUrl
        p[ConfigKeys.API_KEY] = c.apiKey
        p[ConfigKeys.POLL_INTERVAL] = c.pollIntervalSec
        p[ConfigKeys.REPEAT_COUNT] = c.repeatCount
        p[ConfigKeys.REPEAT_DELAY] = c.repeatDelaySec
        p[ConfigKeys.AUTO_GAGAL] = c.autoPlayGagal
        p[ConfigKeys.AUTO_LULUS] = c.autoPlayLulus
        p[ConfigKeys.PLATE_COL] = c.plateColumn
        p[ConfigKeys.ID_COL] = c.idColumn
        p[ConfigKeys.RESULT_COL] = c.resultColumn
        p[ConfigKeys.OWNER_COL] = c.ownerColumn
        p[ConfigKeys.GAGAL_VAL] = c.gagalValue
        p[ConfigKeys.LULUS_VAL] = c.lulusValue
    }
}