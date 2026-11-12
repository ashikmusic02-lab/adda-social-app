package com.example.services

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "adda_user_prefs")

interface PreferencesService {
  val languageFlow: Flow<AppLanguage>
  val themeModeFlow: Flow<AppThemeMode>
  suspend fun setLanguage(language: AppLanguage)
  suspend fun setThemeMode(themeMode: AppThemeMode)
}

class DataStorePreferencesService(
  private val context: Context
) : PreferencesService {

  private val appContext = context.applicationContext

  override val languageFlow: Flow<AppLanguage> = appContext.dataStore.data.map { prefs ->
    AppLanguage.fromCode(prefs[KEY_LANGUAGE])
  }

  override val themeModeFlow: Flow<AppThemeMode> = appContext.dataStore.data.map { prefs ->
    AppThemeMode.fromValue(prefs[KEY_THEME_MODE])
  }

  override suspend fun setLanguage(language: AppLanguage) {
    appContext.dataStore.edit { prefs ->
      prefs[KEY_LANGUAGE] = language.code
    }
  }

  override suspend fun setThemeMode(themeMode: AppThemeMode) {
    appContext.dataStore.edit { prefs ->
      prefs[KEY_THEME_MODE] = themeMode.value
    }
  }

  private companion object {
    val KEY_LANGUAGE = stringPreferencesKey("app_language")
    val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
  }
}
