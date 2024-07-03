package com.example.usbcameraapp

import android.Manifest
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var usbMonitor: USBMonitor
    private var uvcCamera: UVCCamera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Dexter.withContext(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    setupUsbMonitor()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    // Handle the permission denial
                }

                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    private fun setupUsbMonitor() {
        usbMonitor = USBMonitor(this, onDeviceConnectListener)
        usbMonitor.register()

        cameraView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                uvcCamera?.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_MJPEG)
                uvcCamera?.startPreview()
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }

    private val onDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            Log.d("MainActivity", "USB device attached")
            usbMonitor.requestPermission(device)
        }

        override fun onDettach(device: UsbDevice) {
            Log.d("MainActivity", "USB device detached")
        }

        override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            Log.d("MainActivity", "USB device connected")
            uvcCamera = UVCCamera()
            uvcCamera?.open(ctrlBlock)
            val surface = Surface(cameraView.holder.surface)
            uvcCamera?.setPreviewDisplay(surface)
            uvcCamera?.startPreview()
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
            Log.d("MainActivity", "USB device disconnected")
            uvcCamera?.close()
        }

        override fun onCancel(device: UsbDevice) {
            Log.d("MainActivity", "USB device permission cancelled")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uvcCamera?.destroy()
        usbMonitor.unregister()
    }
}