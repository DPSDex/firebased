package com.example.firebased

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.firebased.databinding.ActivityMainBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mob: EditText
    private lateinit var name: EditText
    private lateinit var otp: EditText
    private lateinit var sendOtp: Button
    private lateinit var submitOtp: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mContext: Context
    private lateinit var storedVerificationId : String

    var callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:$credential")
            var code = credential.smsCode
            code?.let {
                verifyOTP(code)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w(TAG, "onVerificationFailed", e)

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
            }
            Toast.makeText(mContext,"Enter valid phone number and name",Toast.LENGTH_SHORT).show()
            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:$verificationId")
            super.onCodeSent(verificationId, token)
            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId

            Toast.makeText(mContext,"Code send",Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SmsListener()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mContext = this

        mAuth = Firebase.auth
        binding.send.setOnClickListener {
            if(TextUtils.isEmpty(binding.mob.text.toString()) || TextUtils.isEmpty(binding.name.text.toString())){
                Toast.makeText(this,"Enter valid phone number and name",Toast.LENGTH_SHORT).show()
            }else {
                sendVerificationCode(binding.mob.text.toString())
            }
        }

        binding.submitOTP.setOnClickListener {
            if(TextUtils.isEmpty(binding.otp.text.toString())){
                Toast.makeText(this,"Enter valid OTP",Toast.LENGTH_SHORT).show()
            }else {
                verifyOTP(binding.otp.text.toString())
            }
        }

    }

    private fun verifyOTP(otp: String) {
        var credential = PhoneAuthProvider.getCredential(storedVerificationId,otp)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        var firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    Toast.makeText(
                        this,
                        "signInWithCredential:success",
                        Toast.LENGTH_SHORT
                    ).show()
                    val user = task.result?.user
                    addUser()
                    startActivity(Intent(mContext,DashboardActivity::class.java))
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    Toast.makeText(
                        this,
                        "signInWithCredential:failed",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Update UI
                }
            }
    }

    private fun addUser() {
        val db = Firebase.firestore
        prefs.mobileNumPref = binding.mob.text.toString()
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val currentDateandTime: String = sdf.format(Date())
        val timesheet = timesheet(currentDateandTime, getLocation(mContext))
        val user = hashMapOf(
            "name" to binding.name.text.toString(),
            "mob" to binding.mob.text.toString(),
            "timesheet" to arrayListOf<timesheet>(timesheet)
        )

// Add a new document with a generated ID
        db.collection("users").document(binding.mob.text.toString())
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot added with ID")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }

        val data = workDataOf("CUR_LOC" to getLocation(this), "CUR_USER" to FirebaseAuth.getInstance().currentUser)
        val periodicWorkRequest = PeriodicWorkRequest
            .Builder(SendLocationWorker::class.java,120,TimeUnit.MINUTES)
            .setInputData(data)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(periodicWorkRequest)
    }

    private fun sendVerificationCode(phone: String) {
        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber("+91$phone")       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override fun onStart() {
        super.onStart()
        var currentUser = FirebaseAuth.getInstance().currentUser
        //Firebase.auth.signOut()
        currentUser?.let {
            startActivity(Intent(mContext,DashboardActivity::class.java))
            finish()
        }
    }

}