package com.vt.smart.switchapp.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.ads.ManageAds
import com.vt.smart.switchapp.connectivity.MyClient
import com.vt.smart.switchapp.connectivity.MyServer
import com.vt.smart.switchapp.databinding.ActivityExploreDevicesBinding
import com.vt.smart.switchapp.ui.interfaces.ClickInterface
import com.vt.smart.switchapp.ui.interfaces.ConnectionInterface
import com.vt.smart.switchapp.receiver.WifiReceiver
import com.vt.smart.switchapp.ui.adapters.WifiAdapter
import com.vt.smart.switchapp.AppUtils
import dmax.dialog.SpotsDialog
import java.net.InetAddress

class ExploreDevicesNearby : AppCompatActivity(), ConnectionInterface {
    private val TAG = javaClass.simpleName
    private lateinit var binding: ActivityExploreDevicesBinding
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var wifiP2PChannel: WifiP2pManager.Channel
    private lateinit var WifiReceiver: WifiReceiver
    private lateinit var intentFilter: IntentFilter
    private val peersList: ArrayList<WifiP2pDevice> = ArrayList()
    private var devicesArrayList: ArrayList<String> = ArrayList()
    private lateinit var adapter: WifiAdapter
    private lateinit var spotsDialog: SpotsDialog
    private lateinit var user: String
    var ipAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExploreDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ManageAds.showAdMobBanner(this,this,binding.bannerLayout)
        user = intent.getStringExtra("user").toString()
        initRV()
        initAndStartScanning()

        binding.btnRetry.setOnClickListener {
            scanDevices()
        }
        binding.customToolbar.title.text="Connect Wifi"
        binding.customToolbar.backarrow.setOnClickListener {
            onBackPressed()
        }
    }

    private fun initRV() {
        adapter = WifiAdapter(this, devicesArrayList, object : ClickInterface {
            override fun onItemClick(position: Int) {
                requestPairing(position)
            }
        })
        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.adapter = adapter
    }

    private fun initAndStartScanning() {
        try {
            wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
            wifiP2PChannel = wifiP2pManager.initialize(this, Looper.getMainLooper(), null)
            disconnectWifiDirect()
//            receiver = receiver(
//                this,
//                wifiP2pManager,
//                wifiP2PChannel,
//                this
//            )
            intentFilter = IntentFilter()
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

            scanDevices()
        } catch (e: Exception) {
        }
    }

    private fun scanDevices() {

        spotsDialog = SpotsDialog.Builder().setContext(this)
            .setContext(this)
            .setMessage("Scanning")
            .setTheme(R.style.MyDialog)
            .build() as SpotsDialog
        spotsDialog.show()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        wifiP2pManager.discoverPeers(wifiP2PChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.e(TAG, "Success")

            }

            override fun onFailure(reason: Int) {

                Log.e(TAG, "Failed: $reason")
            }

        })
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

            }

            override fun onFailure(reason: Int) {
                AppUtils.presentToast(
                    this@ExploreDevicesNearby,
                    "Connection failed\nPlease retry."
                )
                return
            }
        })
    }

    val connectionListener = WifiP2pManager.ConnectionInfoListener()
    {
        val wifiP2pInfo = it
        ipAddress = wifiP2pInfo?.groupOwnerAddress?.hostAddress.toString()
        if (wifiP2pInfo.groupFormed) {
            val groupOwnerAddress: InetAddress = wifiP2pInfo.groupOwnerAddress

            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                MyServer.startListening(this)
            } else if (wifiP2pInfo.groupFormed) {
                val clientClass = MyClient(groupOwnerAddress, this)
                clientClass.start()
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
            spotsDialog.dismiss()
            adapter.notifyDataSetChanged()
        }
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

    override fun onResume() {
        super.onResume()
        registerReceiver(WifiReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(WifiReceiver)
    }

    override fun onConnectionSuccessful() {
        runOnUiThread { openTransferScreen() }
    }

    private fun openTransferScreen() {
        if (isFinishing || isDestroyed) return
        if (user == "sender") {
            Intent(
                this,
                ActivitySender::class.java
            ).apply {
                putExtra("ip_address",ipAddress)
                startActivity(
                    this
                )
            }

            finish()
        } else {
            ManageAds.showInterstitialAd( this) {
                startActivity(Intent(this, ActivityReceiver::class.java))
                finish()
            }

        }
    }

    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            Toast.makeText(this, "Connection failed: $reason", Toast.LENGTH_SHORT).show()
        }
    }
}