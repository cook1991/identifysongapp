package net.spritzlive.identifysong.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.spritzlive.identifysong.widget.IdentifySongWidget

private val Context.historyDataStore by preferencesDataStore(name = "history")
private val HISTORY_KEY = stringPreferencesKey("entries_json")
private const val MAX_ENTRIES = 100

/** Persists a capped, most-recent-first list of past song identifications on-device. */
class HistoryRepository(private val context: Context) {

    val history: Flow<List<HistoryEntry>> = context.historyDataStore.data.map { prefs ->
        prefs[HISTORY_KEY]?.let { json ->
            runCatching { Json.decodeFromString<List<HistoryEntry>>(json) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun add(entry: HistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val current = prefs[HISTORY_KEY]?.let { json ->
                runCatching { Json.decodeFromString<List<HistoryEntry>>(json) }.getOrDefault(emptyList())
            } ?: emptyList()
            val updated = (listOf(entry) + current).take(MAX_ENTRIES)
            prefs[HISTORY_KEY] = Json.encodeToString(updated)
        }
        IdentifySongWidget().updateAll(context)
    }

    suspend fun clear() {
        context.historyDataStore.edit { prefs -> prefs.remove(HISTORY_KEY) }
        IdentifySongWidget().updateAll(context)
    }
}
