package com.pbs.tv.util


import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.net.URLDecoder
import java.net.URLEncoder

const val SERVER_URL = "http://192.168.1.2:3000"
// const val SERVER_URL = "http://127.0.0.1:3000"

val HTTP_CLIENT by lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
  HttpClient {
    engine { CIO }
  }
}

fun String.encodeUriComponent(): String = URLEncoder.encode(this, "utf-8")

fun String.decodeUriComponent(): String = URLDecoder.decode(this, "utf-8")