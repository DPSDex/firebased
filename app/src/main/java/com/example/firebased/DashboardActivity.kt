package com.example.firebased

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.firebased.databinding.ActivityDashboardBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "DashboardActivity"
class DashboardActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var storedVerificationId : String
    private lateinit var resendToken : PhoneAuthProvider.ForceResendingToken
    val db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    var callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:$credential")
            signInWithPhoneAuthCredential(credential)
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

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId
            resendToken = token

        }
    }



    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!isLocationPermissionGrantedandRequest()){

        } else {
            setup()
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.SEND_SMS
            ),
            2
        )


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }


    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
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



    private fun isLocationPermissionGrantedandRequest(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.SEND_SMS
                ),
                1
            )
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 || requestCode == 2) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setup()
            } else {
                finish()
            }
        }

    }


    private fun setup() {
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)



        binding.send.setOnClickListener {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val currentDateandTime: String = sdf.format(Date())
            var loc = getLocation(this)
            val timesheet = timesheet(currentDateandTime, loc)

            Log.d(TAG, "location" + loc)
            db.collection("users").document(prefs.mobileNumPref).update("timesheet", FieldValue.arrayUnion(timesheet))

            //sms code
            val sms: SmsManager = SmsManager.getDefault()
            sms.sendTextMessage("+919971409151", null, "hello javatpoint", null, null)
        }


        val docRef = db.collection("admin").document(prefs.mobileNumPref)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    binding.admin.visibility = View.VISIBLE
                    if(document.data == null) {
                        binding.admin.visibility = View.GONE
                    }
                } else {
                    Log.d(TAG, "No such document")
                    binding.admin.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
                binding.admin.visibility = View.GONE
            }


        binding.admin.setOnClickListener {
            startActivity(Intent(this,AdminActivity::class.java))
            finish()
        }



        binding.get.setOnClickListener {
            val ctquery = db.collection("users").count()
            var ct=0;
            ctquery.get(AggregateSource.SERVER).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val snapshot = task.result
                    ct = snapshot.count.toInt()
                    Log.d(TAG, "Count: ${snapshot.count}")
                    val allData = Array<String>(ct){"empty"}
                    var itr=0;
                    db.collection("users")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                Log.d(TAG, "${document.id} => ${document.data}")
                                var cancatdata = "Phone: " + document.data.get("mob") +" Name: "  + document.data.get("name")

                                val timeSheetData = document["timesheet"] as List<Map<String,String>>?
                                Log.d(TAG, "timeSheetData " + timeSheetData)
                                if (timeSheetData != null) {
                                    for(timedata in timeSheetData) {
                                        for((k,v) in timedata) {
                                            cancatdata += ("  " + k + "  " + v)
                                        }
                                    }
                                }
                                allData[itr] = (cancatdata)
                                itr++
                            }
                            val arr: ArrayAdapter<String>
                            arr = ArrayAdapter<String>(
                                this,
                                R.layout.activity_listview,
                                allData
                            )
                            binding.list.setAdapter(arr)
                        }
                        .addOnFailureListener { exception ->
                            Log.d(TAG, "Error getting documents: ", exception)
                        }
                } else {
                    Log.d(TAG, "Count failed: ", task.getException())
                }
            }

        }
    }

}