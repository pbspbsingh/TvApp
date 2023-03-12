package com.pbs.server.vpn

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import de.blinkt.openvpn.api.IOpenVPNAPIService
import de.blinkt.openvpn.api.IOpenVPNStatusCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout


object VpnUtil {
  private val mutex = Mutex()

  private var vpnState: VpnState = VpnState.NotInstalled

  private lateinit var vpnService: IOpenVPNAPIService

  private val statusFlow = MutableSharedFlow<String>(replay = 1)

  private val callback = object : IOpenVPNStatusCallback.Stub() {
    override fun newStatus(uuid: String?, state: String?, message: String?, level: String?) {
      // Log.v(TAG, "VPN Status changed: $uuid, $state, $message, $level")
      if (!state.isNullOrBlank()) {
        statusFlow.tryEmit(state)
      }
    }
  }

  suspend fun prepare(context: Context): VpnState = mutex.withLock {
    if (vpnState is VpnState.Connected || vpnState is VpnState.ConsentDenied) {
      Log.d(TAG, "Vpn already connected or connection denied, nothing to do")
      return vpnState
    }

    if (!isOpenVpnInstalled(context)) {
      vpnState = VpnState.NotInstalled
      Log.w(TAG, "OpenVPN is not installed, launching marketplace to install it")
      val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$OPEN_VPN_PACKAGE")).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
      return VpnState.NotInstalled
    } else {
      Log.d(TAG, "OpenVPN is already installed")
    }

    withContext(Dispatchers.IO) {
      try {
        if (!::vpnService.isInitialized) {
          Log.i(TAG, "Trying to bind with OpenVPN service")
          vpnService = withTimeout(5000L) { bindService(context) }
        }
        val intent = vpnService.prepare(context.packageName)
        vpnState = if (intent != null) {
          VpnState.ConsentNeeded(intent)
        } else {
          VpnState.AlreadyConsented
        }
        Log.i(TAG, "Prepared vpn service: $vpnState")
      } catch (e: Exception) {
        Log.w(TAG, "Error connecting to VPN service", e)
        vpnState = VpnState.ServiceError(e)
      }
    }
    return vpnState
  }

  suspend fun denyConsent() = mutex.withLock {
    vpnState = VpnState.ConsentDenied
  }

  suspend fun connect() = mutex.withLock {
    if (vpnState is VpnState.Connected) {
      Log.w(TAG, "VPN is already connected, why do you need to connect again?")
      return
    }

    withContext(Dispatchers.IO) {
      try {
        vpnService.unregisterStatusCallback(callback)
        vpnService.registerStatusCallback(callback)
        vpnService.startVPN(VPN_CONFIG)
        withTimeout(30_000) {
          launch {
            statusFlow.asSharedFlow().collectIndexed { i, nextStatus ->
              Log.i(TAG, "Received new VPN status: $i $nextStatus")
              if (i > 0 && nextStatus == "CONNECTED") {
                vpnState = VpnState.Connected
                delay(1000L)
                this@launch.cancel()
              }
            }
          }.join()
        }
      } catch (e: RemoteException) {
        Log.e(TAG, "Error while connecting to VPN", e)
        vpnState = VpnState.ServiceError(e)
      }
    }
  }

  private suspend fun bindService(context: Context): IOpenVPNAPIService =
    suspendCancellableCoroutine {
      val vpnService = Intent(IOpenVPNAPIService::class.java.name).apply {
        setPackage(OPEN_VPN_PACKAGE)
      }
      context.bindService(vpnService, object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
          Log.i(TAG, "Successfully bound with VPN Service: $name")
          val actualService = IOpenVPNAPIService.Stub.asInterface(service)
          it.resumeWith(Result.success(actualService))
        }

        override fun onServiceDisconnected(name: ComponentName?) {
          Log.w(TAG, "Unbound with VPN service: $name")
        }
      }, Context.BIND_AUTO_CREATE)
    }

  private fun isOpenVpnInstalled(context: Context): Boolean {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(OPEN_VPN_PACKAGE) ?: return false
    val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return list.isNotEmpty()
  }

  sealed interface VpnState {
    object NotInstalled : VpnState
    class ConsentNeeded(val intent: Intent) : VpnState
    object AlreadyConsented : VpnState
    object ConsentDenied : VpnState
    object Connected : VpnState
    class ServiceError(val err: Exception) : VpnState
  }

  private const val TAG = "VpnUtil"
  private const val OPEN_VPN_PACKAGE = "de.blinkt.openvpn"
  private const val VPN_CONFIG = """
# ==============================================================================
# Copyright (c) 2016-2020 Proton Technologies AG (Switzerland)
# Email: contact@protonvpn.com
#
# The MIT License (MIT)
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR # OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
# IN THE SOFTWARE.
# ==============================================================================

# If you are a paying user you can also enable the ProtonVPN ad blocker (NetShield) or Moderate NAT:
# Use: "MQ6w4RagsccG0ux1+f1" as username to enable anti-malware filtering
# Use: "MQ6w4RagsccG0ux1+f2" as username to additionally enable ad-blocking filtering
# Use: "MQ6w4RagsccG0ux1+nr" as username to enable Moderate NAT
# Note that you can combine the "+nr" suffix with other suffixes.

<auth-user-pass>
username
password
</auth-user-pass>

<ca>
</ca>

<tls-auth>
</tls-auth>
"""
}