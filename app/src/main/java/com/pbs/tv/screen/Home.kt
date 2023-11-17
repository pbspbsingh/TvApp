package com.pbs.tv.screen


import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.pbs.tv.Route
import com.pbs.tv.model.HomeViewModel
import com.pbs.tv.model.TvShow

private const val TAG = "HomeScreen"

@Composable
fun Home(
    navController: NavHostController,
    model: HomeViewModel = viewModel(),
) {
    LaunchedEffect(Unit) {
        Log.i(TAG, "Loading home screen...")
        model.loadHome()
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