@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)

package com.pbs.tv.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.pbs.tv.Route
import com.pbs.tv.model.TvShow
import com.pbs.tv.util.Http
import com.pbs.tv.util.encodeUriComponent

@Composable
fun Loader(
  modifier: Modifier = Modifier,
  message: String = "Loading...",
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      CircularProgressIndicator(modifier = Modifier.size(50.dp))
      Text(
        text = message,
        modifier = Modifier.padding(top = 10.dp),
        style = MaterialTheme.typography.bodyLarge,
      )
    }
  }
}

@Preview
@Composable
fun ErrorView(
  modifier: Modifier = Modifier,
  message: String? = null,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "Something went wrong!",
        style = MaterialTheme.typography.headlineMedium,
      )
      if (message != null) {
        Text(
          text = message,
          modifier = Modifier.padding(top = 10.dp),
          style = MaterialTheme.typography.bodyLarge,
        )
      }
    }
  }
}

@Composable
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
          channel, tvShow.title, tvShow.icon.encodeUriComponent()
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
        model = Http.serverUrl + tvShow.icon,
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

@Composable
fun AppBar(navController: NavHostController, title: String) {
  TopAppBar(
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    navigationIcon = {
      IconButton(onClick = { navController.popBackStack() }) {
        Icon(Icons.Filled.ArrowBack, "Go Back")
      }
    },
  )
}

@Composable
fun PreviewPanel(
  title: String,
  previewUrl: String,
  navController: NavHostController,
  content: @Composable (Boolean) -> Unit,
) {
  Scaffold(topBar = { AppBar(navController = navController, title = title) }) {
    BoxWithConstraints(
      modifier = Modifier
        .padding(it)
        .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
      val isWideScreen = maxHeight < maxWidth
      Row(modifier = Modifier.fillMaxSize()) {
        if (isWideScreen) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(.4f),
            contentAlignment = Alignment.Center,
          ) {
            GlideImage(
              model = previewUrl,
              contentDescription = title,
              contentScale = ContentScale.FillWidth,
              modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp),
            )
          }
        }
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = if (isWideScreen) Alignment.Center else Alignment.CenterStart,
        ) {
          content(isWideScreen)
        }
      }
    }
  }
}