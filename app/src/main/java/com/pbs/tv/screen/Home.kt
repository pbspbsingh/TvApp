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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.pbs.server.vpn.VpnUtil
import com.pbs.tv.Route
import com.pbs.tv.model.HomeViewModel
import com.pbs.tv.model.TvShow
import kotlinx.coroutines.launch

private const val TAG = "HomeScreen"

@Composable
fun Home(
  navController: NavHostController,
  model: HomeViewModel = viewModel(),
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val activityLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
      scope.launch {
        if (it.resultCode == Activity.RESULT_OK) {
          Log.i(TAG, "Let's connect the VPN")
          VpnUtil.connect()
        } else {
          Log.w(TAG, "User denied VPN consent, will try next time")
          VpnUtil.denyConsent()
        }
        model.loadHome()
      }
    }

  LaunchedEffect(Unit) {
    if (HomeViewModel.BACKEND_MODE == HomeViewModel.BackendMode.LocalWithVpn) {
      Log.d(TAG, "Preparing VPN consent screen")
      when (val vpnState = VpnUtil.prepare(context)) {
        VpnUtil.VpnState.AlreadyConsented -> {
          Log.i(TAG, "VPN is consented, let's connect to it")
          VpnUtil.connect()
          model.loadHome()
        }

        is VpnUtil.VpnState.ConsentNeeded -> {
          Log.i(TAG, "Consent is required for VPN")
          activityLauncher.launch(vpnState.intent)
        }

        else -> {
          Log.i(TAG, "VPN State: $vpnState")
          model.loadHome()
        }
      }
    } else {
      model.loadHome()
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