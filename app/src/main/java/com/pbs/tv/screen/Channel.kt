package com.pbs.tv.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.pbs.tv.model.HomeViewModel


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Channel(
  params: Map<String, String>,
  navController: NavHostController,
  model: HomeViewModel = viewModel(),
) {
  val title = params["title"] ?: ""
  val tvShowsState = model.getTvShows(title).collectAsState(initial = null)
  val tvShows = tvShowsState.value

  Scaffold(topBar = {
    AppBar(
      navController = navController,
      title = "$title (${tvShows?.size ?: ""})"
    )
  }) {
    Box(modifier = Modifier.padding(it)) {
      if (tvShows != null) {
        LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Adaptive(300.dp)) {
          tvShows.withIndex().forEach { (idx, tvShow) ->
            item(key = tvShow.title) {
              TvShowView(
                channel = title,
                tvShow = tvShow,
                titlePrefix = "${idx + 1}. ",
                navController = navController,
              )
            }
          }
        }
      } else {
        Loader(message = "Please wait")
      }
    }
  }
}