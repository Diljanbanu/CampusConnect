package com.example.campusconnect

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.campusconnect.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply Dark Mode Preference before super.onCreate
        val sharedPref = getSharedPreferences("CampusConnectPrefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("darkModeEnabled", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        listenForCalls()

        // Setup Bottom Navigation
        binding.bottomNavigation.setupWithNavController(navController)

        // Show/Hide bottom nav based on screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment, 
                R.id.loginFragment, 
                R.id.chatFragment,
                R.id.publicProfileFragment,
                R.id.resumeBuilderFragment,
                R.id.forgotPasswordSelectionFragment,
                R.id.otpVerificationFragment,
                R.id.resetPasswordFragment,
                R.id.callFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }

        // Setup Navigation Drawer
        binding.navView.setupWithNavController(navController)
        
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(R.id.loginFragment)
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun listenForCalls() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference.child("Calls")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds in snapshot.children) {
                        val receiverId = ds.child("receiverId").value?.toString()
                        val status = ds.child("status").value?.toString()
                        if (receiverId == uid && status == "ringing") {
                            val callerId = ds.child("callerId").value?.toString()
                            val bundle = Bundle().apply {
                                putString("userId", callerId)
                                putString("callId", ds.key)
                                putBoolean("isIncoming", true)
                            }
                            navController.navigate(R.id.callFragment, bundle)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, binding.drawerLayout) || super.onSupportNavigateUp()
    }
}
