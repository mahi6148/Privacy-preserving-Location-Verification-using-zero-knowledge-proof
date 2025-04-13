package com.example.majorapplication.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.majorapplication.MainActivity
import com.example.majorapplication.R
import com.example.majorapplication.grpcclient.ErrorType
import com.example.majorapplication.grpcclient.GrpcSession
import com.example.majorapplication.grpcclient.VerificationResult
import com.example.majorapplication.location.PermissionHelper
import com.example.majorapplication.location.fetchLocationAndCoordinates
import com.example.majorapplication.messages.ProofData
import com.example.majorapplication.messages.StreamSessionRequest
import com.example.majorapplication.zkp.BulletproofsBinding
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.protobuf.Timestamp
import com.google.protobuf.kotlin.toByteString
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime


class StreamingForegroundService : Service() {

    private lateinit var  streamingService:GrpcSession

    private val TAG = "StreamingForegroundService"

    // Binder given to clients
    private val binder = StreamingBinder()

    // Create a StateFlow for streaming status and verification results
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    private val _verificationResult = MutableStateFlow<VerificationResult>(
        VerificationResult.Error(ErrorType.START_STREAMING_DEFAULT)
    )
    val verificationResult = _verificationResult.asStateFlow()

    // Streaming parameters
    private var refLat: Double = Double.NEGATIVE_INFINITY
    private var refLon: Double = Double.NEGATIVE_INFINITY
    private var sessionId: Int? = null
    private var sessionName: String = ""

    // Coroutine scope for the service
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var streamJob: Job? = null
    private var requestObserver: StreamObserver<StreamSessionRequest>? = null

    // Notification ID
    private val NOTIFICATION_ID = 1

    inner class StreamingBinder : Binder() {
        fun getService(): StreamingForegroundService = this@StreamingForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        streamingService = GrpcSession(applicationContext)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Action.START.toString() -> {
                val sessionNameText = intent.getStringExtra("sessionName") ?: "Unnamed Session"
                val notification = foregroundNotificationBuilder("Initializing streaming...")
                startForeground(NOTIFICATION_ID, notification)

                // Start streaming in the service
                startStreamingProcess(sessionNameText)
            }
            Action.STOP.toString() -> {
                stopStreamingProcess()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Streaming Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun foregroundNotificationBuilder(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Attendance Status")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = foregroundNotificationBuilder(text)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startStreamingProcess(sessionNameText: String) {
        if (_isStreaming.value) return

        serviceScope.launch {
            try {

                if (!PermissionHelper.checkLocationServices(applicationContext)){
                    Toast.makeText(applicationContext,"Enable Location and Internet services",Toast.LENGTH_LONG).show()
                    return@launch
                }

                val returnedCoords = streamingService.sendSessionReq(Firebase.auth.currentUser?.uid!!, sessionNameText)
                Log.d(TAG, "coordinates returned $returnedCoords")

                if (returnedCoords==null){
                    Toast.makeText(applicationContext,"Enable Location and Internet services",Toast.LENGTH_LONG).show()
                    return@launch
                }

                refLat = returnedCoords.first.first
                refLon = returnedCoords.first.second
                sessionId = returnedCoords.second.first
                sessionName = returnedCoords.second.second

                if (refLat == Double.NEGATIVE_INFINITY || refLon == Double.NEGATIVE_INFINITY ) {
                    _verificationResult.value = VerificationResult.Error(ErrorType.JAVA_TO_RUST_CONVERSION_ERROR)
                    updateNotification("Error: Cannot fetch reference coordinates")
                    stopSelf()
                    return@launch
                }

                if ( sessionId==-1 ) {
                    _verificationResult.value = VerificationResult.Error(ErrorType.DUPLICATE_SESSION_NAME)
                    updateNotification("Duplicate SessionName")
                    stopSelf()
                    return@launch
                }


                _isStreaming.value = true
                updateNotification("Streaming active: $sessionName")

                // Setup bidirectional streaming
                requestObserver = streamingService.streamSession(
                    verificationResultFlow = _verificationResult,
                    isStreaming = _isStreaming,
                   updateNotification =  ::updateNotification,
                    sessionName = sessionNameText
                )

                // Start the continuous data sending process
                streamJob = serviceScope.launch {
                    while (isActive) {
                        val (lat1, lon1) = fetchLocationAndCoordinates(
                            context = applicationContext,
                            fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
                        )

                        if (lat1 != null && lon1 != null ) {
                            Log.d(TAG, "Coords: $lat1, $lon1, Ref: $refLat, $refLon")

                            val proofData = BulletproofsBinding.generateProofForCoordinates(
                                lat1, lon1, refLat, refLon
                            )

                            val request = StreamSessionRequest.newBuilder()
                                .setUserId(Firebase.auth.currentUser!!.uid)
                                .setSessionId(sessionId!!)
                                .setData(
                                    ProofData.newBuilder()
                                        .setProofBytes(proofData?.proofBytes?.toByteString())
                                        .setRistrettoBytes(proofData?.ristrettoBytes?.toByteString())
                                )
                                .setRecordedTimestamp(
                                    Timestamp.newBuilder().setSeconds(LocalDateTime.now().second.toLong())
                                )
                                .build()

                            try {
                                requestObserver?.onNext(request)

                                // Update notification based on verification result
//                                when (val result = _verificationResult.value) {
//                                    is VerificationResult.Present -> {
//                                        updateNotification("Session: $sessionName - Present ✓")
//                                    }
//                                    is VerificationResult.Error -> {
//                                        updateNotification("Session: $sessionName - Error: ${result.errorType}")
//                                    }
//                                    is VerificationResult.Absent ->{
//                                        updateNotification("Session: $sessionName - Absent ❌")
//                                    }
//                                    else -> {
//                                        updateNotification("Session: $sessionName - Processing...")
//                                    }
//                                }

                            } catch (e: Exception) {
                                Log.e(TAG, "Error sending request: ${e.message}")
                                updateNotification("Error in streaming. Stopping service.")
                                stopStreamingProcess()
                                break
                            }
                        } else {
                            updateNotification("Unable to get location. Retrying...")
                        }

                        // Add delay between requests
                        delay(10000) // 10 seconds delay
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in streaming process: ${e.message} \n ${e.printStackTrace()}")
                _verificationResult.value = VerificationResult.Error(ErrorType.STREAM_ERROR)
                updateNotification("Error: ${e.message}")
                stopStreamingProcess()
            }
        }
    }

    private fun stopStreamingProcess() {
        streamJob?.cancel()
        streamJob = null

        try {
            requestObserver?.onError(Throwable(message = "Stream Completed"))
            // requestObserver?.onCompleted()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping stream: ${e.message}")
        }

        requestObserver = null
        _isStreaming.value = false
        refLat = Double.NEGATIVE_INFINITY
        refLon = Double.NEGATIVE_INFINITY
        sessionName = ""
//        sessionId = "" TODO()

        _verificationResult.value = VerificationResult.Error(ErrorType.START_STREAMING_DEFAULT)
    }

    override fun onDestroy() {
        stopStreamingProcess()
        streamingService.terminateChannel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        enum class Action {
            START, STOP
        }

        const val CHANNEL_ID = "StreamingServiceChannel"

        fun startService(context: Context, sessionName: String) {
            val intent = Intent(context, StreamingForegroundService::class.java).apply {
                action = Action.START.toString()
                putExtra("sessionName", sessionName)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, StreamingForegroundService::class.java).apply {
                action = Action.STOP.toString()
            }
            context.startService(intent)
        }
    }
}







//
//import android.app.Notification
//import android.app.Service
//import android.content.Intent
//import android.os.IBinder
//import androidx.core.app.NotificationCompat
//import com.example.majorapplication.R
//import com.example.majorapplication.viewmodels.StreamingViewModel
//
//class StreamingForegroundService(viewModel:StreamingViewModel):Service() {
//
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        when(intent?.action){
//            Action.START.toString() -> {
//            val notification = foregroundNotificationBuilder()
//                startStreamService(notification)
//            }
//            Action.STOP.toString() ->{
//                stopSelf()
//            }
//        }
//        return super.onStartCommand(intent, flags, startId)
//    }
//
//    private fun startStreamService(notification: Notification) {
//
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        TODO("Not yet implemented")
//    }
//
//    private fun foregroundNotificationBuilder(): Notification {
//        return NotificationCompat.Builder(this, CHANNEL_NAME)
//            .setSmallIcon(R.drawable.ic_launcher_foreground )
//            .setContentTitle("Attendance Status")
//            .setContentText("Default")
//            .build()
//    }
//
//    companion object{
//        enum class Action{
//            START,STOP
//        }
//        private val CHANNEL_NAME = "MajorAppForegroundServiceNotification"
//    }
//
//
//}