package com.nirzor.voicebubble.updater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

class UpdateInstallReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_INSTALL_STATUS = "com.nirzor.voicebubble.updater.INSTALL_STATUS"
        private const val TAG = "UpdateInstallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""

        Log.d(TAG, "PackageInstaller status received: $status, message: $message")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "App update installed successfully!")
                UpdateManager.getInstance(context).onInstallSuccess()
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                UpdateManager.getInstance(context).onInstallFailure("ইনস্টলেশন বাতিল করা হয়েছে")
            }

            PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                UpdateManager.getInstance(context).onInstallFailure("ডিভাইস নীতিমালার কারণে ইনস্টলেশন ব্লক করা হয়েছে")
            }

            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                UpdateManager.getInstance(context).onInstallFailure("বিদ্যমান প্যাকেজ বা স্বাক্ষরের সাথে দ্বন্দ্ব রয়েছে")
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                UpdateManager.getInstance(context).onInstallFailure("এই সংস্করণটি আপনার ডিভাইসের জন্য উপযুক্ত নয়")
            }

            PackageInstaller.STATUS_FAILURE_INVALID -> {
                UpdateManager.getInstance(context).onInstallFailure("ইনস্টলার প্যাকেজটি অবৈধ বা ত্রুটিপূর্ণ")
            }

            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                UpdateManager.getInstance(context).onInstallFailure("ডিভাইসে পর্যাপ্ত মেমোরি খালি নেই")
            }

            else -> {
                UpdateManager.getInstance(context).onInstallFailure("ইনস্টলেশন ব্যর্থ হয়েছে ($status: $message)")
            }
        }
    }
}
