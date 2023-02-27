package com.example.firebased

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast



class SmsListener : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "SMS Received!")

        val txt = getTextFromSms(intent?.extras)
        Log.d(TAG, "message=" + txt)
        Toast.makeText(context,txt,Toast.LENGTH_SHORT).show()

        //Getting intent and PendingIntent instance
        //Getting intent and PendingIntent instance
//        val intent = Intent(getCo, MainActivity::class.java)
//        val pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0)

//Get the SmsManager instance and call the sendTextMessage method to send message

//Get the SmsManager instance and call the sendTextMessage method to send message
//        val sms: SmsManager = SmsManager.getDefault()
//        sms.sendTextMessage("9810509597", null, "hello javatpoint", null, null)
    }

    private fun getTextFromSms(extras: Bundle?): String {
        val pdus = extras?.get("pdus") as Array<*>
        val format = extras.getString("format")
        var txt = ""
        for (pdu in pdus) {
            val smsmsg = getSmsMsg(pdu as ByteArray?, format)
            val submsg = smsmsg?.displayMessageBody
            submsg?.let { txt = "$txt$it" }
        }
        return txt
    }

    private fun getSmsMsg(pdu: ByteArray?, format: String?): SmsMessage? {
        return when {
            SDK_INT >= Build.VERSION_CODES.M -> SmsMessage.createFromPdu(pdu, format)
            else -> SmsMessage.createFromPdu(pdu)
        }
    }

    companion object {
        private val TAG = SmsListener::class.java.simpleName
    }
}