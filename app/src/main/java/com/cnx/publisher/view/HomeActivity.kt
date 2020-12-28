package com.cnx.publisher.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.cnx.publisher.R
import com.cnx.publisher.databinding.ActivityHomeBinding
import com.cnx.publisher.utils.PermissionUtils
import com.github.faucamp.simplertmp.RtmpHandler
import net.ossrs.yasea.SrsCameraView
import net.ossrs.yasea.SrsEncodeHandler
import net.ossrs.yasea.SrsPublisher
import net.ossrs.yasea.SrsRecordHandler
import java.io.IOException
import java.net.SocketException

class HomeActivity : AppCompatActivity(), RtmpHandler.RtmpListener, SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {

    private lateinit var mPublisher: SrsPublisher
    private lateinit var mCameraView: SrsCameraView
    private lateinit var binding: ActivityHomeBinding
    private lateinit var permissionAssistant: PermissionUtils
    private var counter: CountDownTimer? = null

    private val PERMISSION_REQUEST_CODE: Int = 100

    //var recordingPath: String? = null

    companion object {
        const val rtmpUrl = "rtmp://192.168.0.102:1935/live/stream"
        const val mWidth: Int = 640
        const val mHeight: Int = 480
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        permissionAssistant = PermissionUtils(this)
    }

    override fun onStart() {
        super.onStart()
        disableButtonPanel()
        if (permissionAssistant.checkPermission(Manifest.permission.CAMERA) && permissionAssistant.checkPermission(Manifest.permission.RECORD_AUDIO) && permissionAssistant.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            initCameraView()
        } else {
            permissionAssistant.askPermission(setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        }
    }

    private fun disableButtonPanel() {
        binding.goLive.isEnabled = false
    }

    private fun enableButtonPanel() {
        binding.goLive.isEnabled = true
    }

    private fun initCameraView() {
        mCameraView = binding.surfaceCameraView
        mPublisher = SrsPublisher(mCameraView)
        mPublisher.setEncodeHandler(SrsEncodeHandler(this))
        mPublisher.setRtmpHandler(RtmpHandler(this))
        mPublisher.setRecordHandler(SrsRecordHandler(this))
        mPublisher.setPreviewResolution(mWidth, mHeight)
        mPublisher.setOutputResolution(mHeight, mWidth)
        mPublisher.setVideoHDMode()
        mPublisher.startCamera()

        enableButtonPanel()

        binding.goLive.setOnClickListener {

            if (binding.goLive.text.toString() == "GO LIVE") {
                Toast.makeText(this, "Streaming to {$rtmpUrl}", Toast.LENGTH_SHORT).show()
                mPublisher.switchToSoftEncoder()//can be switched between hardware and software
                mPublisher.startPublish(rtmpUrl)
                mPublisher.startCamera()
                startCounter()
                binding.goLive.text = "STOP"

            } else if (binding.goLive.text.toString() == "STOP") {
                mPublisher.stopPublish()
                stopCounter()
                binding.goLive.text = "GO LIVE"
                mPublisher.startCamera()
            }
        }

    }

    private fun stopCounter() {
        counter?.let { it.cancel() }
        binding.liveStateView.text = "NOT LIVE"
    }

    private fun startCounter() {
        counter = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val diff: Long = Long.MAX_VALUE - millisUntilFinished
                val seconds = diff / 1000 % 60
                val minutes = diff / 1000 / 60
                binding.liveStateView.text = "LIVE $minutes:$seconds"
            }

            override fun onFinish() {
                mPublisher.stopPublish()
                stopCounter()
            }
        }

        counter?.let { it.start() }
    }

    override fun onStop() {
        super.onStop()
        mPublisher.stopPublish()
        binding.goLive.text = "GO LIVE"
        counter?.let { stopCounter() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {

                var shouldWeInitCameraView = true

                for (result in grantResults) {
                    if (result == PackageManager.PERMISSION_DENIED) {
                        shouldWeInitCameraView = false
                    }
                }

                if (shouldWeInitCameraView) {
                    initCameraView()
                } else {
                    Toast.makeText(this, "Please allow all permissions to start streaming", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Please allow all permissions to start streaming", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mPublisher.stopPublish()
    }

    private fun handleException(e: Exception) {
        try {
            mPublisher.stopPublish()
            binding.goLive.text = "GO LIVE"
        } catch (e1: Exception) {
            //
        }
    }

    override fun onRtmpConnecting(msg: String?) {
    }

    override fun onRtmpConnected(msg: String?) {
    }

    override fun onRtmpVideoStreaming() {
    }

    override fun onRtmpAudioStreaming() {
    }

    override fun onRtmpStopped() {
    }

    override fun onRtmpDisconnected() {
    }

    override fun onRtmpVideoFpsChanged(fps: Double) {
    }

    override fun onRtmpVideoBitrateChanged(bitrate: Double) {
    }

    override fun onRtmpAudioBitrateChanged(bitrate: Double) {
    }

    override fun onRtmpSocketException(e: SocketException?) {
        e?.let { handleException(it) }
    }

    override fun onRtmpIOException(e: IOException?) {
        e?.let { handleException(it) }
    }

    override fun onRtmpIllegalArgumentException(e: IllegalArgumentException?) {
        e?.let { handleException(it) }
    }

    override fun onRtmpIllegalStateException(e: IllegalStateException?) {
        e?.let { handleException(it) }
    }

    override fun onRecordPause() {
    }

    override fun onRecordResume() {
    }

    override fun onRecordStarted(msg: String?) {
        Toast.makeText(applicationContext, "Recording file: $msg", Toast.LENGTH_SHORT).show()
    }

    override fun onRecordFinished(msg: String?) {
        Toast.makeText(applicationContext, "MP4 file saved: $msg", Toast.LENGTH_SHORT).show()
    }

    override fun onRecordIllegalArgumentException(e: IllegalArgumentException?) {
        e?.let { handleException(it) }
    }

    override fun onRecordIOException(e: IOException?) {
        e?.let { handleException(it) }
    }

    override fun onNetworkWeak() {
        Toast.makeText(applicationContext, "Network weak", Toast.LENGTH_SHORT).show()
    }

    override fun onNetworkResume() {
        Toast.makeText(applicationContext, "Network resume", Toast.LENGTH_SHORT).show()
    }

    override fun onEncodeIllegalArgumentException(e: IllegalArgumentException?) {
        e?.let { handleException(it) }
    }
}