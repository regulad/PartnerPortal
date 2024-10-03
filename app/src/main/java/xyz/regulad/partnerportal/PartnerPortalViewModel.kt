package xyz.regulad.partnerportal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val startingConnectionValue = "Connecting to partner..."
const val finishedConnectionValue = "Connected to partner!"

class PartnerPortalViewModel(application: Application) : AndroidViewModel(application) {
    val preferences = UserPreferencesRepository(application)

    private val _connectingStatus = MutableStateFlow(startingConnectionValue)
    val connectingStatus: StateFlow<String> = _connectingStatus.asStateFlow()

    fun updateConnectingStatus(newStatus: String) {
        _connectingStatus.value = newStatus
    }

    fun getSupabase() {
        createSupabaseClient(
            supabaseUrl = preferences.supabaseUrl,
            supabaseKey = preferences.supabaseAnonKey
        ) {
            install(Postgrest)
            install(Realtime)
        }
    }
}
