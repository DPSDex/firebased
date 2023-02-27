package com.example.firebased


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebased.databinding.ActivityAdminBinding
import com.example.firebased.databinding.DropItemsBinding
import com.example.firebased.databinding.DropdownItemBinding
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private const val TAG = "AdminActivity"
class AdminActivity : AppCompatActivity() {
//    var users =
//        arrayOf("Suresh Dasari", "Trishika Dasari", "Rohini Alavala", "Praveen Kumar", "Madhav Sai")

    private lateinit var binding: ActivityAdminBinding
    private lateinit var binding1: DropItemsBinding
    private lateinit var users: ArrayList<String>
    private lateinit var usermap: HashMap<String,String>
    private lateinit var adapter: ArrayAdapter<String>
    //private lateinit var adapter2: ArrayAdapter<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        binding1 = DropItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getUsers()


        binding.autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            Toast.makeText(this,"$position selected",Toast.LENGTH_LONG).show()
            usermap[users[position]]?.let {
                val arr: ArrayAdapter<String>


                Log.d(TAG, "get user data called")
                getUserData(it)


            }
        }

    }


//    override fun onItemSelected(arg0: AdapterView<*>?, arg1: View?, position: Int, id: Long) {
//        Toast.makeText(applicationContext, "Selected User: " + users[position], Toast.LENGTH_SHORT)
//            .show()
//    }
//
//    override fun onNothingSelected(arg0: AdapterView<*>?) {
//        // TODO - Custom Code
//    }

    fun getUsers() {
        val db = Firebase.firestore
        val ctquery = db.collection("users").count()
        var ct=0;
        //var userlist = ArrayList<String>()
        users = ArrayList()
        usermap = HashMap<String,String>()
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
                            users.add(document.data.get("name").toString())
                            usermap[document.data.get("name").toString()] = document.data.get("mob").toString()
                            itr++
                        }
                        //users.reverse()
                        adapter = ArrayAdapter(this,R.layout.dropdown_item, users)

                        binding.autoCompleteTextView.setAdapter(adapter)

                    }
                    .addOnFailureListener { exception ->
                        Log.d(TAG, "Error getting documents: ", exception)
                    }
            } else {
                Log.d(TAG, "Count failed: ", task.getException())
            }
        }

        //adapter.notifyDataSetChanged()

    }

    fun getUserData(mob: String){
        var userData= ""
        val data = ArrayList<ItemsViewModel>()
        val db = Firebase.firestore
        val docRef = db.collection("users").document(mob)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")

                    var cancatdata = "Phone: " + document.data!!.get("mob") +" Name: "  + document.data!!.get("name")

                    val timeSheetData = document["timesheet"] as List<Map<String,String>>?
                    Log.d(TAG, "timeSheetData " + timeSheetData)
                    if (timeSheetData != null) {
                        for(timedata in timeSheetData) {
                            for((k,v) in timedata) {
                                cancatdata += ("  " + k + "  " + v)

                                data.add(ItemsViewModel(R.drawable.ic_launcher_background, ("  " + k + "  " + v)))
                            }
                        }
                    }
                    userData = cancatdata
                    val allData = Array<String>(1) {"empty"}
//                    allData[0] = userData
//                    Log.d(TAG, "alldata: ${allData[0]}")
//                    adapter2 = ArrayAdapter<String>(
//                        this,
//                        R.layout.activity_listview,
//                        allData
//
//                    )
//                    binding.list1.setAdapter(adapter2)
                    data.reverse()
                    val adapter = CustomAdapter(data)
                    binding.recyclerview.layoutManager = LinearLayoutManager(this)
                    binding.recyclerview.adapter = adapter

                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }



    }

}