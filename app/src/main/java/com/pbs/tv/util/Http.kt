package com.pbs.tv.util


import android.content.Context
import android.util.Log
import com.pbs.server.http.ServerUtil
import com.pbs.tv.model.HomeViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


object Http {

  private val mutex = Mutex()

  private val isAlreadyInit = AtomicBoolean(false)

  private lateinit var httpClient: HttpClient

  lateinit var serverUrl: String
    private set

  suspend fun init(context: Context, mode: HomeViewModel.BackendMode) {
    if (isAlreadyInit.get()) {
      Log.d(TAG, "Http is already initialized, NOP")
      return
    }

    mutex.withLock {
      if (isAlreadyInit.get()) {
        return
      }

      httpClient = HttpClient { engine { CIO } }
      serverUrl = if (mode == HomeViewModel.BackendMode.Remote) {
        REMOTE_SERVER_URL
      } else {
        LOCAL_SERVER_URL
      }

      withContext(Dispatchers.IO) {
        if (mode.isLocal()) {
          initLocalServer(context)
        }
      }

      isAlreadyInit.set(true)
    }
  }

  suspend operator fun <T> invoke(callback: suspend HttpClient.(String) -> T): T {
    if (!isAlreadyInit.get()) {
      Log.e(TAG, "Http is not initialized, failing")
      throw IllegalStateException("Http is not initialized")
    }
    return callback(httpClient, serverUrl)
  }

  private suspend fun initLocalServer(context: Context) {
    val cacheDir = File(context.filesDir, "server").also {
      Files.createDirectories(it.toPath())
    }
    Log.i(TAG, "Starting server with cache dir: $cacheDir")
    ServerUtil.startServer(cacheDir)
    delay(500)
  }

  private const val TAG = "HttpUtil"
  private const val REMOTE_SERVER_URL = "http://192.168.1.2:3000"
  private const val LOCAL_SERVER_URL = "http://127.0.0.1:3000"
}
