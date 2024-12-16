package xyz.regulad.partnerportal

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class UserPreferencesRepository(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "user_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )


    companion object {
        private const val SUPABASE_URL = "supabase_url"
        private const val SUPABASE_ANON_KEY = "supabase_anon_key"
        private const val ROOM_CODE = "supabase_room_code"
        private const val AUTO_RECONNECT = "auto_reconnect"

        private const val MUTE_DESIRED = "mute_desired"
        private const val DEAFEN_DESIRED = "deafen_desired"

        private const val CAMERA_DESIRED = "camera_desired"
        private const val DISPLAY_DESIRED = "display_desired"
    }

    var supabaseUrl: String
        get() = sharedPreferences.getString(SUPABASE_URL, SupabaseDefaults.SUPABASE_URL) ?: ""
        set(value) = sharedPreferences.edit().putString(SUPABASE_URL, value).apply()

    var supabaseAnonKey: String
        get() = sharedPreferences.getString(SUPABASE_ANON_KEY, SupabaseDefaults.SUPABASE_ANON_KEY) ?: ""
        set(value) = sharedPreferences.edit().putString(SUPABASE_ANON_KEY, value).apply()

    var roomCode: String
        get() = sharedPreferences.getString(ROOM_CODE, "") ?: ""
        set(value) = sharedPreferences.edit().putString(ROOM_CODE, value).apply()

    var autoReconnect: Boolean
        get() = sharedPreferences.getBoolean(AUTO_RECONNECT, false)
        set(value) = sharedPreferences.edit().putBoolean(AUTO_RECONNECT, value).apply()

    var muteDesired: Boolean
        get() = sharedPreferences.getBoolean(MUTE_DESIRED, false)
        set(value) = sharedPreferences.edit().putBoolean(MUTE_DESIRED, value).apply()

    var deafenDesired: Boolean
        get() = sharedPreferences.getBoolean(DEAFEN_DESIRED, false)
        set(value) = sharedPreferences.edit().putBoolean(DEAFEN_DESIRED, value).apply()

    var cameraDesired: Boolean
        get() = sharedPreferences.getBoolean(CAMERA_DESIRED, true)
        set(value) = sharedPreferences.edit().putBoolean(CAMERA_DESIRED, value).apply()

    var displayDesired: Boolean
        get() = sharedPreferences.getBoolean(DISPLAY_DESIRED, true)
        set(value) = sharedPreferences.edit().putBoolean(DISPLAY_DESIRED, value).apply()
}
