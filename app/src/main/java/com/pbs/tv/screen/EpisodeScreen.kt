package com.pbs.tv.screen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.pbs.tv.Route
import com.pbs.tv.model.EpisodePart
import com.pbs.tv.model.EpisodeViewModel
import com.pbs.tv.util.SERVER_URL
import com.pbs.tv.util.encodeUriComponent
import org.json.JSONArray

@Composable
fun EpisodeScreen(
  params: Map<String, String>,
  navController: NavHostController,
  model: EpisodeViewModel = viewModel(),
) {
  val channel = params["channel"]!!
  val title = params["tvshow"]!!
  val episode = params["episode"]!!
  val icon = params["icon"]!!

  val state = model.state.collectAsState().value

  LaunchedEffect(channel, title, episode) {
    if (state is EpisodeViewModel.EpisodeState.Loading) {
      model.loadEpisode(channel, title, episode)?.let {
        Log.d("EpisodeScreen", "Number of parts found: ${it.parts.size}")
        if (it.parts.size == 1) {
          navController.popBackStack()
          val urls = it.parts.map(EpisodePart::url).stringify()
          navController.navigate(
            Route.EpisodePlayer.routeWithParam("0", urls)
          )
        }
      }
    }
  }

  PreviewPanel(
    title = episode, previewUrl = SERVER_URL + icon, navController = navController
  ) {
    when (state) {
      EpisodeViewModel.EpisodeState.Loading -> Loader(message = title)
      is EpisodeViewModel.EpisodeState.EpisodeError -> ErrorView(message = state.msg)
      is EpisodeViewModel.EpisodeState.EpisodeLoaded -> {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          for ((idx, part) in state.episode.parts.withIndex()) {
            TextButton(onClick = {
              val urls = state.episode.parts.map { it.url }.stringify()
              navController.navigate(Route.EpisodePlayer.routeWithParam(idx.toString(), urls))
            }) {
              Text(text = part.title)
            }
          }
        }
      }
    }
  }
}

@Composable
fun EpisodePlayer(params: Map<String, String>, navController: NavHostController) {
  val idx = remember { params["index"]!!.toInt() }
  val videoUrl = remember { params["parts"]!!.decodeUrls() }

  VideoPlayer(videoUrl = SERVER_URL + videoUrl[idx]) {
    navController.popBackStack()
    if (idx + 1 < videoUrl.size) {
      navController.navigate(
        Route.EpisodePlayer.routeWithParam((idx + 1).toString(), videoUrl.stringify())
      )
    }
  }
}

fun List<String>.stringify(): String {
  val array = JSONArray().apply {
    this@stringify.forEach(this::put)
  }
  return array.toString().encodeUriComponent()
}

fun String.decodeUrls(): List<String> {
  val list = mutableListOf<String>()
  val array = JSONArray(this)
  for (i in 0 until array.length()) {
    list += array.getString(i)
  }
  return list
}