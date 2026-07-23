package com.mediacollector.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore 扩展，用于认证 Token 持久化 */
private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

@Singleton
class AuthStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_USER_ID = intPreferencesKey("user_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    /** Token 流 */
    val token: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[KEY_TOKEN]
    }

    /** 用户 ID 流 */
    val userId: Flow<Int> = context.authDataStore.data.map { prefs ->
        prefs[KEY_USER_ID] ?: 0
    }

    /** 用户名流 */
    val username: Flow<String> = context.authDataStore.data.map { prefs ->
        prefs[KEY_USERNAME] ?: ""
    }

    /** 显示名流 */
    val displayName: Flow<String> = context.authDataStore.data.map { prefs ->
        prefs[KEY_DISPLAY_NAME] ?: ""
    }

    /** 是否已登录 */
    suspend fun isLoggedIn(): Boolean {
        return token.first() != null
    }

    /** 保存登录信息 */
    suspend fun saveAuth(
        tokenStr: String,
        userId: Int,
        username: String,
        displayName: String
    ) {
        context.authDataStore.edit { prefs ->
            prefs[KEY_TOKEN] = tokenStr
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USERNAME] = username
            prefs[KEY_DISPLAY_NAME] = displayName
        }
    }

    /** 清除登录信息（退出登录） */
    suspend fun clear() {
        context.authDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
