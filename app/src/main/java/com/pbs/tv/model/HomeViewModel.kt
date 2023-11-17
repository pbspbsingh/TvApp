package com.pbs.tv.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pbs.tv.util.Http
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class HomeViewModel(application: Application) : AndroidViewModel(application) {

  private val _state: MutableStateFlow<HomeState> = MutableStateFlow(HomeState.Loading)

  val state: StateFlow<HomeState> = _state.asStateFlow()

  init {
    viewModelScope.launch { init() }
  }

  private suspend fun init() {
    val context = getApplication<Application>().applicationContext
    Http.init(context, BACKEND_MODE)
  }

  suspend fun loadHome() {
    try {
      init()
      withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading Channels info...")
        val response = Http { get("$it/home") }
        Log.i(TAG, "Got http response: ${response.status}")
        if (!response.status.isSuccess()) {
          throw IllegalStateException("Status: ${response.status}, Error: ${response.bodyAsText()}")
        }
        _state.value = HomeState.ChannelsInfo(response.bodyAsText().parseChannelInfo())
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error while loading home screen", e)
      _state.value = HomeState.LoadError(e.stackTraceToString())
    }
  }

  private fun String.parseChannelInfo(): Map<String, List<TvShow>> {
    val map = LinkedHashMap<String, MutableList<TvShow>>()
    val obj = JSONObject(this)
    for (key in obj.keys()) {
      map[key] = mutableListOf()
      val array = obj.getJSONArray(key)
      for (i in 0 until array.length()) {
        map[key]!! += array.getJSONObject(i).let {
          TvShow(
            title = it.getString("title"),
            icon = it.getString("icon"),
          )
        }
      }
    }
    return map
  }

  fun getTvShows(tvChannel: String): Flow<List<TvShow>?> = state.map { homeState ->
    (homeState as? HomeState.ChannelsInfo)?.let { chanInfo ->
      chanInfo.channels[tvChannel]
    }
  }

  companion object {
    private const val TAG = "HomeViewModel"
    val BACKEND_MODE = BackendMode.Local // Use Remote for development
  }

  sealed interface HomeState {
    object Loading : HomeState
    data class LoadError(val message: String? = null) : HomeState
    data class ChannelsInfo(val channels: Map<String, List<TvShow>>) : HomeState
  }

  enum class BackendMode {
    Remote, Local;

    fun isLocal() = this == Local
  }
}