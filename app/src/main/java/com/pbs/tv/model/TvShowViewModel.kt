package com.pbs.tv.model

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pbs.tv.util.HTTP_CLIENT
import com.pbs.tv.util.SERVER_URL
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class TvShowViewModel : ViewModel() {

  private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

  private val _episodes: MutableStateFlow<TvShowEpisodes> = MutableStateFlow(
    TvShowEpisodes(
      emptyList(), true
    )
  )

  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  val episodes: StateFlow<TvShowEpisodes> = _episodes.asStateFlow()

  suspend fun loadEpisodes(channel: String, tvshow: String, loadMore: Boolean = false) {
    if (!_episodes.value.hasMore) {
      Log.w(TAG, "There is no more Episodes, NOOP")
    }
    try {
      withContext(Dispatchers.IO) {
        _loading.value = true
        val url =
          "$SERVER_URL/episodes/${channel.encodeURLPathPart()}/${tvshow.encodeURLPathPart()}?load_more=$loadMore"
        Log.i(TAG, "Get $url")
        val response = HTTP_CLIENT.get(url)
        Log.i(TAG, "Got http response: ${response.status}")
        if (!response.status.isSuccess()) {
          throw IllegalStateException("Status: ${response.status}, Error: ${response.bodyAsText()}")
        }
        _episodes.value = response.bodyAsText().parseEpisodes()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Something went wrong while loading episodes", e)
    } finally {
      _loading.value = false
    }
  }

  private fun String.parseEpisodes(): TvShowEpisodes {
    val episodes = mutableListOf<String>()
    val obj = JSONObject(this)
    val episodesArr = obj.getJSONArray("episodes")
    for (i in 0 until episodesArr.length()) {
      episodes += episodesArr.getString(i)
    }
    return TvShowEpisodes(episodes, obj.getBoolean("has_more"))
  }

  private companion object {
    const val TAG = "TvShowViewModel"
  }
}