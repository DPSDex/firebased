package com.example.firebased

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "SendLocationWorker"
class SendLocationWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val loc = inputData.getString("CUR_LOC")
        val cur_usr = inputData.getString("CUR_USER")
//
//        return try {
//            if (resourceUri.isNullOrEmpty()) {
//                Timber.e("Invalid input uri")
//                throw IllegalArgumentException("Invalid input uri")
//            }
//
//            val outputData = blurAndWriteImageToFile(resourceUri)
//            Result.success(outputData)
//        } catch (throwable: Throwable) {
//            Timber.e(throwable, "Error applying blur")
//            Result.failure()
//        }

        if(cur_usr != null) {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val currentDateandTime: String = sdf.format(Date())
            //var loc = getLocation(ctx)
            val timesheet = timesheet(currentDateandTime, loc)
            val db = Firebase.firestore
            Log.d(TAG, "location" + loc)
            db.collection("users").document(prefs.mobileNumPref)
                .update("timesheet", FieldValue.arrayUnion(timesheet))
            return Result.success()
        }
        return Result.failure()
    }

}