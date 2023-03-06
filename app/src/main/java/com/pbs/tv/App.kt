package com.pbs.tv

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pbs.tv.screen.Channel
import com.pbs.tv.screen.EpisodePlayer
import com.pbs.tv.screen.EpisodeScreen
import com.pbs.tv.screen.Home
import com.pbs.tv.screen.TvShowScreen
import com.pbs.tv.ui.theme.TvTheme

@Composable
fun App(navController: NavHostController = rememberNavController()) {
  TvTheme(dynamicColor = true) {
    Surface(modifier = Modifier.fillMaxSize()) {
      NavHost(navController = navController, startDestination = Route.Home.route()) {
        composable(Route.Home.route()) { Home(navController = navController) }
        composable(Route.Channel.route()) {
          Channel(Route.Channel.extractParams(it.arguments), navController)
        }
        composable(Route.TvShow.route()) {
          TvShowScreen(Route.TvShow.extractParams(it.arguments), navController)
        }
        composable(Route.Episode.route()) {
          EpisodeScreen(Route.Episode.extractParams(it.arguments), navController)
        }
        composable(Route.EpisodePlayer.route()) {
          EpisodePlayer(Route.EpisodePlayer.extractParams(it.arguments), navController)
        }
      }
    }
  }
}

enum class Route(private val route: String, private val args: List<String> = emptyList()) {
  Home("home"),
  Channel("channel", listOf("title")),
  TvShow("tvshow", listOf("channel", "tvshow", "icon")),
  Episode("episode", listOf("channel", "tvshow", "episode", "icon")),
  EpisodePlayer("episode_player", listOf("index", "parts"));

  fun route(): String = StringBuilder(route).apply {
    for (arg in args) {
      append('/').append('{').append(arg).append('}')
    }
  }.toString()

  fun routeWithParam(vararg params: String): String = StringBuilder(route).apply {
    check(params.size == args.size)
    for (param in params) {
      append('/').append(param)
    }
  }.toString()

  fun extractParams(bundle: Bundle?): Map<String, String> =
    args.mapNotNull { key -> bundle?.getString(key)?.let { value -> key to value } }.toMap()
}