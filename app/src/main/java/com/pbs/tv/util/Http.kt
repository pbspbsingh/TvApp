package com.pbs.tv.util


import android.content.Context
import android.util.Log
import com.pbs.server.http.ServerUtil
import com.pbs.server.vpn.startVpn
import com.pbs.tv.model.HomeViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.decodeBase64String
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
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
        if (mode == HomeViewModel.BackendMode.LocalWithVpn) {
          try {
            initVpn(context)
          } catch (e: Exception) {
            Log.w(TAG, "VPN init failed ", e)
          }
        }
        if (mode != HomeViewModel.BackendMode.Remote) {
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

  private suspend fun initVpn(context: Context) {
    val configFile = File(context.filesDir, "wg.txt")
    if (!configFile.exists() || System.currentTimeMillis() - configFile.lastModified() > DAY) {
      val configUrl = CONFIG_URL.decodeBase64String()
      Log.i(TAG, "Using config url: $configUrl")
      val encrypted = httpClient.get(configUrl).bodyAsText()
      val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
        val key = SecretKeySpec(KEY.decodeBase64Bytes(), "AES")
        val iv = IvParameterSpec(IV.decodeBase64Bytes())
        init(Cipher.DECRYPT_MODE, key, iv)
      }
      val config = String(cipher.doFinal(encrypted.decodeBase64Bytes()))
      Log.i(TAG, "Config:\n$config\n\n")
      configFile.writeText(config)
    } else {
      Log.d(TAG, "Config file already up to date, nothing to do")
    }

    startVpn(context, configFile)
    delay(500)
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

  // WG config
  private const val CONFIG_URL =
    "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3Bic3Bic2luZ2gvQ29uZmlnL21haW4vd2dfY29uZmlnLnR4dA=="
  private const val KEY = "FyW3YnUWkQlWTvZL69Yfv2EqV6fbcvNVNUaoCLjwMss="
  private const val IV = "zMyFnCo1UplSdtUGzseLlQ=="
  private const val DAY = 24 * 60 * 60 * 1000L
}
