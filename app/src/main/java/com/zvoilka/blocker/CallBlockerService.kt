package com.zvoilka.blocker

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class CallBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val prefs = BlockerPrefs(this)
        val incoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        if (!prefs.enabled || !incoming) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart?.trim().orEmpty()

        val inContacts = number.isNotEmpty() && isInContacts(number)

        if (inContacts) {
            respondToCall(callDetails, CallResponse.Builder().build())
        } else {
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
            respondToCall(callDetails, response)
            MainActivity.ensureNotificationChannel(this)
            notifyBlocked(number.ifEmpty { getString(R.string.notif_unknown) })
        }
    }

    private fun isInContacts(number: String): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        return try {
            contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { it.moveToFirst() } ?: false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun notifyBlocked(number: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val notif = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_power)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text, number))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(number.hashCode(), notif)
    }
}
