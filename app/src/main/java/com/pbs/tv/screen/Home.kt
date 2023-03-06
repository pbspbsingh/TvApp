package com.pbs.tv.screen


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.pbs.tv.Route
import com.pbs.tv.model.HomeViewModel
import com.pbs.tv.model.TvShow
import com.pbs.tv.util.SERVER_URL
import com.pbs.tv.util.encodeUriComponent

@Composable
fun Home(
  navController: NavHostController,
  model: HomeViewModel = viewModel(),
) {
  val state = model.state.collectAsState()
  when (val value = state.value) {
    HomeViewModel.HomeState.Loading -> Loader()
    is HomeViewModel.HomeState.LoadError -> ErrorView(message = value.message)
    is HomeViewModel.HomeState.ChannelsInfo -> {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(vertical = 5.dp, horizontal = 10.dp)
      ) {
        value.channels.forEach { (title, tvShows) ->
          item(key = title) { ChannelRow(title, tvShows, navController) }
        }
      }
    }
  }
}

@Composable
private fun ChannelRow(title: String, tvShows: List<TvShow>, navController: NavHostController) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 15.dp)
  ) {
    TextButton(onClick = { navController.navigate(Route.Channel.routeWithParam(title)) }) {
      Text(text = "$title (${tvShows.size})", style = MaterialTheme.typography.headlineSmall)
    }
    LazyRow(modifier = Modifier.fillMaxWidth()) {
      tvShows.forEach {
        item(key = it.title) { TvShowView(title, it, navController) }
      }
    }
  }
}

@Composable
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
fun TvShowView(
  channel: String,
  tvShow: TvShow,
  navController: NavHostController,
  titlePrefix: String = "",
) {
  Surface(
    shape = RoundedCornerShape(10.0f),
    onClick = {
      navController.navigate(
        Route.TvShow.routeWithParam(
          channel,
          tvShow.title,
          tvShow.icon.encodeUriComponent()
        )
      )
    },
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .padding(vertical = 10.dp)
        .width(300.dp),
    ) {
      GlideImage(
        model = SERVER_URL + tvShow.icon,
        contentDescription = tvShow.title,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .size(width = 300.dp, height = 150.dp)
          .clip(RoundedCornerShape(10.0f)),
      )
      Text(
        text = titlePrefix + tvShow.title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 5.dp)
      )
    }
  }
}