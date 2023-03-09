package com.pbs.tv.screen


import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.pbs.server.vpn.prepareVpnConsentIntent
import com.pbs.tv.Route
import com.pbs.tv.model.HomeViewModel
import com.pbs.tv.model.HomeViewModel.BackendMode
import com.pbs.tv.model.TvShow

private const val TAG = "HomeScreen"

private val BACKEND_MODE = BackendMode.LocalWithVpn // Use Remote for development

@Composable
fun Home(
  navController: NavHostController,
  model: HomeViewModel = viewModel(),
) {
  val context = LocalContext.current
  val activityLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
      val mode =
        if (it.resultCode == Activity.RESULT_OK) BackendMode.LocalWithVpn else BackendMode.Local
      Log.i(TAG, "Got result back from activity: $it, so using mode: $mode")
      model.init(mode)
    }

  LaunchedEffect(Unit) {
    if (BACKEND_MODE == BackendMode.LocalWithVpn) {
      Log.i(TAG, "Preparing VPN consent screen")
      val intent = context.applicationContext.prepareVpnConsentIntent()
      if (intent != null) {
        Log.i(TAG, "Launching VPN consent screen")
        activityLauncher.launch(intent)
      } else {
        Log.i(TAG, "Already consented, using mode ${BackendMode.LocalWithVpn}")
        model.init(BackendMode.LocalWithVpn)
      }
    } else {
      Log.i(TAG, "Looks like it's DEV mode, using remote mode")
      model.init(BackendMode.Remote)
    }
  }

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