package com.pbs.tv.util

import java.net.URLDecoder
import java.net.URLEncoder

fun String.encodeUriComponent(): String = URLEncoder.encode(this, "utf-8")

fun String.decodeUriComponent(): String = URLDecoder.decode(this, "utf-8")