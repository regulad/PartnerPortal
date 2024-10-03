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
}
