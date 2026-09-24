package com.vt.smart.switchapp.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.vt.smart.switchapp.ads.ManageAds
import com.vt.smart.switchapp.connectivity.MyClient
import com.vt.smart.switchapp.connectivity.MyServer
import com.vt.smart.switchapp.connectivity.MySocketHandler
import com.vt.smart.switchapp.databinding.ActivityChooseConnectionBinding
import com.vt.smart.switchapp.ui.interfaces.ClickInterface
import com.vt.smart.switchapp.ui.interfaces.ConnectionInterface
import com.vt.smart.switchapp.ui.models.HotSpot
import com.vt.smart.switchapp.receiver.WifiReceiver
import com.vt.smart.switchapp.ui.adapters.WifiAdapter
import com.vt.smart.switchapp.utils.utilities.Qrgenerator
import com.vt.smart.switchapp.utils.utilities.MyConstant
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.AppUtils.isGPSEnabled
import com.vt.smart.switchapp.AppUtils.isNetworkEnabled
import com.vt.smart.switchapp.AppUtils.loadingBar
import com.vt.smart.switchapp.AppUtils.progressDG
import com.google.gson.Gson
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Method
import java.net.InetAddress
import androidx.core.view.isVisible

class ChooseConnection : AppCompatActivity(), ConnectionInterface {
    private val PERMISSION_REQUEST_NEARBY_DEVICES = 100
    private val PERMISSION_REQUEST_LOCATION_SCAN = 101
    private lateinit var binding: ActivityChooseConnectionBinding
    private var user: String = "sender"
    private lateinit var locationManager: LocationManager
    private lateinit var wifiManager: WifiManager
    private val TAG = "SCAN"
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var wifiP2PChannel: WifiP2pManager.Channel
    private lateinit var WifiReceiver: WifiReceiver
    private lateinit var intentFilter: IntentFilter
    private val peersList: ArrayList<WifiP2pDevice> = ArrayList()
    private var devicesArrayList: ArrayList<String> = ArrayList()
    private lateinit var adapter: WifiAdapter
    private var ipAddress = ""
    private var p2pReceiverRegistered = false
    private var p2pLinkStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ManageAds.showAdMobBanner(this, this, binding.bannerLayout)
        initViews()
        initListeners()
    }

    private fun initViews() {
        try {
            user = intent.getStringExtra("user") ?: "sender"
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        } catch (e: Exception) {
           Log.e("checkCrash","initi views : $e")
        }
    }

    private fun initListeners() {
        try {
            ManageAds.loadAdMobInterstitialAds(this)
            binding.available.setOnClickListener {
                scanDevices()
            }

            binding.wifi.setOnClickListener {

                ManageAds.showInterstitialAd(this){
                    binding.title.text="Wifi Connection"
                    binding.nwifiLayout.visibility=View.VISIBLE
                    binding.hotspotLayout.visibility=View.GONE
                    binding.wifiLayout.visibility=View.GONE
                    handleWifiClick()
                }
            }

            binding.hotspot.setOnClickListener {
                ManageAds.showInterstitialAd(this) {

                    handleHotspotClick()
                }
            }

            binding.ivBack.setOnClickListener {
                ManageAds.showInterstitialAd(this) {
                    if(binding.wifiLayout.isVisible){
                        super.onBackPressed()
                    }
                    else{
                        binding.title.text="Choose Connection"
                        binding.hotspotLayout.visibility=View.GONE
                        binding.nwifiLayout.visibility=View.GONE
                        binding.wifiLayout.visibility=View.VISIBLE
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("checkCrash","initi listener : $e")
        }
    }

    private fun handleWifiClick() {
        try {
            binding.hotspotLayout.visibility = View.GONE
            binding.nwifiLayout.visibility = View.VISIBLE

            // 1. RecyclerView initialize karein
            initRV()

            // 2. Wi-Fi Direct setup aur scanning start karein
            initAndStartScanning()

            if (!wifiManager.isWifiEnabled) {
                AppUtils.presentToast(this, "Please turn on wifi")
            }
        } catch (e: Exception) {
            Log.e("checkCrash","handle wifi clicks : $e")
        }
    }
    private fun handleHotspotClick() {
        try {



            if (user == "sender") {
                binding.title.text="HotSpot"
                binding.nwifiLayout.visibility=View.GONE
                binding.hotspotLayout.visibility=View.VISIBLE
                binding.wifiLayout.visibility=View.GONE
                handleSenderHotspot()
            } else {
                binding.title.text="HotSpot"
                binding.nwifiLayout.visibility=View.GONE
                binding.hotspotLayout.visibility=View.GONE
                binding.wifiLayout.visibility=View.VISIBLE
                handleReceiverHotspot()
            }
        } catch (e: Exception) {
            Log.e("checkCrash","handleHotspotClick : $e")
        }
    }

    private fun handleSenderHotspot() {
        try {
            if (!isNetworkEnabled(locationManager) && !isGPSEnabled(locationManager)) {
                AppUtils.presentToast(this, "Please turn on location")
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                if (true && !Settings.System.canWrite(this)) {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        MyConstant.reservationOfHotspot?.close()
                        turnOnHotspotForO()
                    } else {
                        turnOnHotspot()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("checkCrash","handleSenderHotspot : $e")
        }
    }

    private fun handleReceiverHotspot() {
        try {
            if (!wifiManager.isWifiEnabled) {
                AppUtils.presentToast(this, "Please turn on wifi")
            } else {
                startActivity(Intent(this, JoinHotSpot::class.java))
                finish()
            }
        } catch (e: Exception) {
            Log.e("checkCrash","handleReceiverHotspot : $e")
        }
    }

    private fun initRV() {
        adapter = WifiAdapter(this, devicesArrayList, object : ClickInterface {
            override fun onItemClick(position: Int) {
                requestPairing(position)
            }
        })
        binding.wifiRecyclerlist.layoutManager = LinearLayoutManager(this)
        binding.wifiRecyclerlist.adapter = adapter
    }

    private fun initAndStartScanning() {
        try {
            wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
            wifiP2PChannel = wifiP2pManager.initialize(this, Looper.getMainLooper(), null)
            disconnectWifiDirect()

            WifiReceiver = WifiReceiver(this, wifiP2pManager, wifiP2PChannel, this)
            intentFilter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            if (!p2pReceiverRegistered) {
                registerP2pReceiver()
            }

            scanDevices()
        } catch (e: Exception) {
            Log.e("checkCrash","initAndStartScanning : $e")

        }
    }

    private fun scanDevices() {
        try {
            progressDG(this)
            loadingBar?.show()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_LOCATION_SCAN
                )
                return
            }

            wifiP2pManager.discoverPeers(wifiP2PChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Discover peers success")
                    loadingBar?.dismiss()
                }

                    override fun onFailure(reason: Int) {
                    loadingBar?.dismiss()
                    Log.e(TAG, "Failed: $reason")
                }

            })
        } catch (e: Exception) {
            Log.e("checkCrash","scanDevices : $e")
        }
    }

    private fun requestPairing(pos: Int) {
        val device = peersList[pos]
        val wifiConfig = WifiP2pConfig()
        wifiConfig.deviceAddress = device.deviceAddress
        wifiConfig.wps.setup = WpsInfo.PBC

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        wifiP2pManager.connect(wifiP2PChannel, wifiConfig, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                AppUtils.presentToast(this@ChooseConnection, "Connecting...")
            }

            override fun onFailure(reason: Int) {
                AppUtils.presentToast(
                    this@ChooseConnection,
                    "Connection failed\nPlease retry."
                )
                return
            }
        })
    }

    val connectionListener = WifiP2pManager.ConnectionInfoListener()
    {
        val wifiP2pInfo = it ?: return@ConnectionInfoListener
        val groupOwnerAddress = wifiP2pInfo.groupOwnerAddress ?: return@ConnectionInfoListener
        ipAddress = groupOwnerAddress.hostAddress ?: ""
        if (wifiP2pInfo.groupFormed) {
            MyConstant.savedIpAddress = ipAddress
            MyConstant.isWifiP2pConnected = true
            if (p2pLinkStarted) return@ConnectionInfoListener
            p2pLinkStarted = true
            if (wifiP2pInfo.isGroupOwner) {
                MyServer.startListening(this)
            } else {
                MyClient(groupOwnerAddress, this).start()
            }
        }
    }
    val peerList = WifiP2pManager.PeerListListener {

        if (it.deviceList != peersList) {
            peersList.clear()
            peersList.addAll(it.deviceList)
            devicesArrayList.clear()

            for (device in it.deviceList.withIndex()) {
                devicesArrayList.add(device.value.deviceName)
            }
            if (::adapter.isInitialized) {
                adapter.notifyDataSetChanged()
            }
            try {
                loadingBar?.dismiss()
            } catch (e: Exception) {
               //
            }
        }
    }

    private fun registerP2pReceiver() {
        if (p2pReceiverRegistered) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(WifiReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(WifiReceiver, intentFilter)
        }
        p2pReceiverRegistered = true
    }

    private fun disconnectWifiDirect() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        wifiP2pManager.requestGroupInfo(wifiP2PChannel) { group ->
            if (group != null) {
                wifiP2pManager.removeGroup(
                    wifiP2PChannel,
                    object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d(TAG, "removeGroup onSuccess -")
                        }

                        override fun onFailure(reason: Int) {
                            Log.d(TAG, "removeGroup onFailure -$reason")
                        }
                    })
            }
        }
    }
    override fun onConnectionSuccessful() {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            Log.d("FLOW_CHECK", "onConnectionSuccessful called! Moving to next screen.")
            if (user == "sender") {
                val intent = Intent(this, ActivitySender::class.java)
                intent.putExtra("ip_address", ipAddress)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, ActivityReceiver::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            p2pLinkStarted = false
            if (isFinishing || isDestroyed) return@runOnUiThread
            AppUtils.presentToast(this, "Connection failed: $reason")
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        try {
            if (p2pReceiverRegistered) {
                unregisterReceiver(WifiReceiver)
                p2pReceiverRegistered = false
            }
        } catch (e: Exception) {
            Log.e("checkCrash", "onDestroy : $e")
        }
        if (MySocketHandler.getSocket() == null) {
            MyServer.stopListening()
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        try {
            initViews()

            // 1. IntentFilter ko yahan initialize karna zaroori hai taake receiver ko pata ho kya listen karna hai
            intentFilter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }

            // 2. Location Check
            if (!isNetworkEnabled(locationManager) && !isGPSEnabled(locationManager)) {
                AppUtils.presentToast(this, "Please turn on location")
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {

                // 3. Agar WifiReceiver initialize ho chuka hai (yani user ne Wifi button click kiya tha)
                // to hi register karein, warna null pointer exception aa sakta hai.
                if (::WifiReceiver.isInitialized && !p2pReceiverRegistered) {
                    registerP2pReceiver()
                    Log.d(TAG, "Receiver Registered")
                }
            }
        } catch (e: Exception) {
            Log.e("checkCrash", "onResume Error: ${e.message}")
        }
    }    private fun turnOnHotspot() {
        var method: Method = wifiManager.javaClass.getMethod("getWifiApConfiguration")
        val netConfig = method.invoke(wifiManager) as WifiConfiguration
        method = wifiManager.javaClass.getMethod(
            "setWifiApEnabled",
            WifiConfiguration::class.java,
            Boolean::class.javaPrimitiveType
        )
        val result = method.invoke(wifiManager, netConfig, true) as Boolean
        generateQRCode(netConfig.SSID, netConfig.preSharedKey)
    }
    private fun turnOnHotspotForO() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES),
                    PERMISSION_REQUEST_NEARBY_DEVICES
                )
            } else {
                // Permission is already granted, perform your actions here
                startLocalOnlyHotspotAsync()
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            startLocalOnlyHotspotAsync()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startLocalOnlyHotspotAsync() {
        GlobalScope.launch(Dispatchers.IO) {
            if (MyConstant.reservationOfHotspot != null) {
                MyConstant.reservationOfHotspot?.close()
                MyConstant.reservationOfHotspot = null
            }

            try {
                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                        super.onStarted(reservation)
                        MyConstant.reservationOfHotspot = reservation
                        val config = reservation.wifiConfiguration
                        val ssid = config?.SSID
                        val password = config?.preSharedKey
                        if (ssid.isNullOrBlank() || password.isNullOrBlank()) {
                            runOnUiThread {
                                if (!isFinishing && !isDestroyed) {
                                    AppUtils.presentToast(
                                        this@ChooseConnection,
                                        "Hotspot started but QR details unavailable"
                                    )
                                }
                            }
                            MyServer.startListening(this@ChooseConnection)
                            return
                        }
                        generateQRCode(ssid, password)
                    }

                    override fun onStopped() {
                        super.onStopped()
                        Log.d(TAG, "Local Hotspot Stopped")
                    }

                    override fun onFailed(reason: Int) {
                        super.onFailed(reason)
                        Log.d(TAG, "Local Hotspot failed to start: $reason")
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                AppUtils.presentToast(
                                    this@ChooseConnection,
                                    "Hotspot failed to start"
                                )
                            }
                        }
                    }
                }, Handler(Looper.getMainLooper()))
            }  catch (e: Exception) {
                // Handle other exceptions
                Log.e(TAG, "Local Hotspot request failed: ${e.message}")
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_NEARBY_DEVICES) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startLocalOnlyHotspotAsync()
            } else {
                AppUtils.presentToast(this, "Nearby devices permission required for hotspot")
            }
        } else if (requestCode == PERMISSION_REQUEST_LOCATION_SCAN) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                scanDevices()
            }
        }
    }
    private fun generateQRCode(ssid: String, password: String) {
      //  Toast.makeText(this, "ssid: $ssid password: $password", Toast.LENGTH_SHORT).show()

        Log.d("ssid",ssid)
        Log.d("password",password)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val serializeString = Gson().toJson(HotSpot(ssid, password))
                val bitmap: Bitmap = Qrgenerator
                    .newInstance(this@ChooseConnection)
                    ?.setContent(serializeString)
                    ?.setErrorCorrectionLevel(ErrorCorrectionLevel.Q)
                    ?.setMargin(2)
                    ?.qRCOde
                    ?: return@launch

                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        binding.ivCode.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "QR generation failed", e)
            }
        }
        MyServer.startListening(this)
    }

    override fun onBackPressed() {
        if(binding.wifiLayout.visibility==View.VISIBLE){
            super.onBackPressed()
        }
        else{
            binding.title.text="Choose Connection"
            binding.hotspotLayout.visibility=View.GONE
            binding.nwifiLayout.visibility=View.GONE
            binding.wifiLayout.visibility=View.VISIBLE
        }
    }

}