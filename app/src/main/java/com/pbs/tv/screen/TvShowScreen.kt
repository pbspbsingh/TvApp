package com.pbs.tv.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.pbs.tv.Route
import com.pbs.tv.model.TvShowViewModel
import com.pbs.tv.util.Http
import com.pbs.tv.util.encodeUriComponent
import kotlinx.coroutines.launch


@Composable
fun TvShowScreen(
  params: Map<String, String>,
  navController: NavHostController,
  model: TvShowViewModel = viewModel(),
) {
  val scope = rememberCoroutineScope()
  val episodes = model.episodes.collectAsState(context = scope.coroutineContext).value
  val loading = model.loading.collectAsState(context = scope.coroutineContext).value

  val channel = params["channel"]!!
  val title = params["tvshow"]!!
  val icon = params["icon"]!!

  LaunchedEffect(channel, title) {
    if (episodes.episodes.isEmpty()) {
      model.loadEpisodes(channel, title)
    }
  }

  PreviewPanel(
    title = "$title (${episodes.episodes.size}${if (episodes.hasMore) "+" else ""})",
    previewUrl = Http.serverUrl + icon,
    navController = navController
  ) { isWideScreen ->
    LazyColumn {
      for ((idx, episode) in episodes.episodes.withIndex()) {
        item(key = episode) {
          TextButton(onClick = {
            navController.navigate(
              Route.Episode.routeWithParam(
                channel,
                title,
                episode,
                icon.encodeUriComponent(),
              )
            )
          }) {
            Text(text = "(${idx + 1}) $episode")
          }
        }
      }
      item {
        val modifier: Modifier = if (isWideScreen) {
          Modifier.offset(x = 25.dp)
        } else {
          Modifier.fillMaxWidth()
        }
        Surface(modifier = modifier) {
          if (episodes.hasMore && !loading) {
            IconButton(
              onClick = {
                scope.launch { model.loadEpisodes(channel, title, true) }
              }
            ) {
              Icon(Icons.Filled.KeyboardArrowDown, "Load More")
            }
          }
          if (loading) {
            CircularProgressIndicator(modifier = Modifier.requiredSize(25.dp))
          }
        }
      }
    }
  }
}

