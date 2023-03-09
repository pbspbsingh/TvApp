package com.pbs.tv.model

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pbs.tv.util.Http
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray

class EpisodeViewModel : ViewModel() {

  private val _state: MutableStateFlow<EpisodeState> = MutableStateFlow(EpisodeState.Loading)

  val state: StateFlow<EpisodeState> = _state.asStateFlow()

  suspend fun loadEpisode(
    channel: String,
    title: String,
    episode: String,
  ): Episode? {
    try {
      return withContext(Dispatchers.IO) {
        val url =
          "episode/${channel.encodeURLPathPart()}/${title.encodeURLPathPart()}/${episode.encodeURLPathPart()}"
        Log.i(TAG, "Loading Episodes from $url")
        val response = Http { get("$it/$url") }
        Log.i(TAG, "Got http response: ${response.status}")
        if (!response.status.isSuccess()) {
          throw IllegalStateException("Status: ${response.status}, Error: ${response.bodyAsText()}")
        }
        val episodeParts = response.bodyAsText().parseEpisode()
        _state.value = EpisodeState.EpisodeLoaded(episodeParts)
        episodeParts
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error while loading episodes", e)
      _state.value = EpisodeState.EpisodeError(e.stackTraceToString())
      return null
    }
  }

  private fun String.parseEpisode(): Episode {
    val list = mutableListOf<EpisodePart>()
    val arr = JSONArray(this)
    for (i in 0 until arr.length()) {
      val partInfo = arr.getJSONArray(i)
      list += EpisodePart(partInfo.getString(0), partInfo.getString(1))
    }
    return Episode(list)
  }

  private companion object {
    private const val TAG = "EpisodeViewModel"
  }

  sealed interface EpisodeState {
    object Loading : EpisodeState
    data class EpisodeError(val msg: String) : EpisodeState
    data class EpisodeLoaded(val episode: Episode) : EpisodeState
  }
}