package com.example.parkspace.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.parkspace.R
import com.example.parkspace.databinding.ActivityMainBinding
import com.example.parkspace.models.data.RefreshTokenModel
import com.example.parkspace.utils.Resource
import com.example.parkspace.utils.UserPreverence
import com.example.parkspace.viewmodels.AuthViewModel
import com.example.parkspace.viewmodels.UserViewModel
import com.example.parkspace.view.activity.OnBoardingActivity
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    //get name
    private val userViewModel: UserViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var userPreverence: UserPreverence? = null

    //gps
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        userPreverence = UserPreverence(this)
        userPreverence?.getToken()?.let { setUpName(it) }

        setUpRefreshToken(userPreverence?.getRefreshToken()!!)

        //gps
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()

        //bar
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        val view = getWindow().decorView

        when (resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                window.statusBarColor = Color.parseColor("#003346")
                view.systemUiVisibility = view.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                window.statusBarColor = Color.parseColor("#AAE5FF")
                view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {}
        }

//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding.profiles.setOnClickListener { startActivity(Intent(this, ProfilActivity::class.java)) }
        binding.button.setOnClickListener { startActivity(Intent(this, BookingActivity::class.java)) }
    }

    private fun getFirstName(name: String?): String? = name?.substring(0,name.lastIndexOf(' '))

    private fun setUpName(token: String){
        userViewModel.getName(token).observe(this){
            if (it != null){
                when(it){
                    is Resource.Loading -> {  }
                    is Resource.Success -> {
                        binding.name.text = getFirstName(it.data.name)
                    }
                    is Resource.Error -> {
                        Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setUpRefreshToken(refreshToken: String){
        lifecycleScope.launch {
            delay(59000)
            authViewModel.refreshToken(RefreshTokenModel(refreshToken)).observe(this@MainActivity){
                if (it != null){
                    when(it){
                        is Resource.Loading -> { }
                        is Resource.Success -> {
                            it.data.accessToken?.let { token -> userPreverence?.setToken(token) }
                            it.data.refreshToken?.let { refreshToken-> userPreverence?.setRefreshToken(refreshToken) }
                        }
                        is Resource.Error -> {
                            Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
                            userPreverence?.setLogin(false)
                            userPreverence?.setToken("")
                            startActivity(Intent(this@MainActivity, SignActivity::class.java))
                            finish()
                        }
                    }
                }
            }
        }
    }

    //location service
    private fun isLocationEnable(): Boolean{
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    //last location
    @SuppressLint("SetTextI18n")
    private fun getLastLocation(){
        //check permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (isLocationEnable()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener{ task ->
                    var location: Location? = task.result
                    if (location == null){
                        getNewLocation()
                    }else{
                        binding.locatext.text = getCityName(location.latitude,location.longitude)
                    }
                }
            } else{
                Toast.makeText(this,"Please enable your location service", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getNewLocation(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationRequest = LocationRequest()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 0
            locationRequest.fastestInterval = 0
            locationRequest.numUpdates = 2
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,locationCallback,Looper.myLooper()
            )
        }
    }

    private fun getCityName(lat: Double, long: Double):String{
        var CityName = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var Adress = geoCoder.getFromLocation(lat,long,1)

        CityName = Adress?.get(0)!!.locality
        return CityName
    }

    private val locationCallback = object : LocationCallback(){
        @SuppressLint("SetTextI18n")
        override fun onLocationResult(p0: LocationResult) {
            var lastLocation = p0.lastLocation
            if (lastLocation != null) {
                binding.locatext.text = getCityName(lastLocation.latitude,lastLocation.longitude)
                println(getCityName(lastLocation.latitude,lastLocation.longitude))
            }
        }
    }

    override fun isActivityTransitionRunning(): Boolean {
        return super.isActivityTransitionRunning()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}