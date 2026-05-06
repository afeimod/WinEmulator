package com.termux.x11.controller.core

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager

/**
 * Utility class for application-level operations - simplified version for Linbox compatibility.
 */
abstract class AppUtils {
    companion object {
        /**
         * Keep screen on
         */
        fun keepScreenOn(activity: Activity) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        /**
         * Get application version code
         */
        fun getVersionCode(context: Context): Int {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                }
            } catch (e: Exception) {
                0
            }
        }

        /**
         * Get screen width in pixels
         */
        fun getScreenWidth(): Int {
            return android.view.DisplayMetrics().widthPixels
        }

        /**
         * Get screen height in pixels
         */
        fun getScreenHeight(): Int {
            return android.view.DisplayMetrics().heightPixels
        }
    }
}