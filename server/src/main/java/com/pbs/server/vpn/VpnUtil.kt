package com.pbs.server.vpn

import android.content.Context
import android.content.Intent
import android.util.Log
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.File

fun Context.prepareVpnConsentIntent(): Intent? = GoBackend.VpnService.prepare(this)


fun startVpn(context: Context, config: File) {
  val conf = config.bufferedReader().use { Config.parse(it) }
  Log.i(TAG, "Parsed wg config: $conf")

  val tunnel = object : Tunnel {
    override fun getName() = "WireGuardTunnel"

    override fun onStateChange(newState: Tunnel.State) {
      Log.d(TAG, "Tunnel state changed to $newState")
    }
  }
  GoBackend(context).setState(tunnel, Tunnel.State.UP, conf)
}


private const val TAG = "VpnUtil"