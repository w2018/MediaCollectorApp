package com.mediacollector.app.ui.cast

import android.content.Context
import android.net.wifi.WifiManager
import com.google.android.gms.cast.framework.CastContext

/**
 * 投屏助手（简化版）
 *
 * 基于 Android MediaRouter + Cast SDK。
 * 实际集成需配置 Google Cast SDK 和注册设备。
 */
object CastHelper {

    /**
     * 初始化 Cast Context
     * 需在 Application.onCreate() 中调用
     */
    fun initCast(context: Context) {
        try {
            CastContext.getSharedInstance(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取当前 WiFi 名称（用于显示投屏设备信息）
     */
    fun getWifiName(context: Context): String {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo.ssid?.removeSurrounding("\"") ?: "未知网络"
        } catch (_: Exception) {
            "未知网络"
        }
    }

    /**
     * 检查是否支持投屏（有没有可用 Cast 设备）
     */
    fun isCastAvailable(context: Context): Boolean {
        return try {
            val castContext = CastContext.getSharedInstance(context)
            castContext.sessionManager?.currentCastSession != null
        } catch (_: Exception) {
            false
        }
    }
}
