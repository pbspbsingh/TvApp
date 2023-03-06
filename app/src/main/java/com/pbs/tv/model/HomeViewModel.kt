package com.pbs.tv.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pbs.tv.util.HTTP_CLIENT
import com.pbs.tv.util.SERVER_URL
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

class HomeViewModel : ViewModel() {

  private val _state: MutableStateFlow<HomeState> = MutableStateFlow(HomeState.Loading)

  val state: StateFlow<HomeState> = _state.asStateFlow()

  init {
    viewModelScope.launch { loadHome() }
  }

  private suspend fun loadHome() {
    try {
      withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading Channels info...")
        val response = HTTP_CLIENT.get("$SERVER_URL/home")
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

  fun getTvShows(tvChannel: String): Flow<List<TvShow>?> =
    state.map { homeState ->
      (homeState as? HomeState.ChannelsInfo)?.let { chanInfo ->
        chanInfo.channels[tvChannel]
      }
    }

  private companion object {
    const val TAG = "HomeViewModel"
  }

  sealed interface HomeState {
    object Loading : HomeState
    data class LoadError(val message: String? = null) : HomeState
    data class ChannelsInfo(val channels: Map<String, List<TvShow>>) : HomeState
  }
}