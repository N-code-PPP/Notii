package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore by preferencesDataStore(name = "noted_user_preferences")

data class UserPreferences(
    val isDarkMode: Boolean = true,
    val isGridView: Boolean = true,
    val simulateOfflineMode: Boolean = false,
    val masterVaultPasswordHash: String = "",
    val folderOrderIds: List<String> = emptyList(),
    val categoryOrderNames: List<String> = emptyList(),
    val hiddenDefaultCategories: List<String> = emptyList()
)

class UserPreferencesRepository(private val context: Context) {
    private val appContext = context.applicationContext

    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val SIMULATE_OFFLINE_MODE = booleanPreferencesKey("simulate_offline_mode")
        val MASTER_VAULT_HASH = stringPreferencesKey("master_vault_hash")
        val FOLDER_ORDER_IDS = stringPreferencesKey("folder_order_ids")
        val CATEGORY_ORDER_NAMES = stringPreferencesKey("category_order_names")
        val HIDDEN_DEFAULT_CATEGORIES = stringPreferencesKey("hidden_default_categories")
    }

    val preferencesFlow: Flow<UserPreferences> = appContext.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
            isGridView = prefs[Keys.IS_GRID_VIEW] ?: true,
            simulateOfflineMode = prefs[Keys.SIMULATE_OFFLINE_MODE] ?: false,
            masterVaultPasswordHash = prefs[Keys.MASTER_VAULT_HASH] ?: "",
            folderOrderIds = parseJsonStringList(prefs[Keys.FOLDER_ORDER_IDS]),
            categoryOrderNames = parseJsonStringList(prefs[Keys.CATEGORY_ORDER_NAMES]),
            hiddenDefaultCategories = parseJsonStringList(prefs[Keys.HIDDEN_DEFAULT_CATEGORIES])
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        appContext.dataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    suspend fun setGridView(enabled: Boolean) {
        appContext.dataStore.edit { it[Keys.IS_GRID_VIEW] = enabled }
    }

    suspend fun setSimulateOfflineMode(enabled: Boolean) {
        appContext.dataStore.edit { it[Keys.SIMULATE_OFFLINE_MODE] = enabled }
    }

    suspend fun setMasterVaultPasswordHash(hash: String) {
        appContext.dataStore.edit { it[Keys.MASTER_VAULT_HASH] = hash }
    }

    suspend fun setFolderOrderIds(ids: List<String>) {
        appContext.dataStore.edit {
            it[Keys.FOLDER_ORDER_IDS] = JSONArray(ids.distinct()).toString()
        }
    }

    suspend fun setCategoryOrderNames(names: List<String>) {
        appContext.dataStore.edit {
            it[Keys.CATEGORY_ORDER_NAMES] = JSONArray(names.distinct()).toString()
        }
    }

    suspend fun setHiddenDefaultCategories(names: List<String>) {
        appContext.dataStore.edit {
            it[Keys.HIDDEN_DEFAULT_CATEGORIES] = JSONArray(names.distinct()).toString()
        }
    }

    private fun parseJsonStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i, "").trim()
                    if (item.isNotEmpty()) add(item)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

