package com.example.majorapplication.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.majorapplication.grpcclient.ErrorType
import com.example.majorapplication.grpcclient.VerificationResult
import com.example.majorapplication.services.StreamingForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class StreamingViewModel : ViewModel() {

    private val TAG = "StreamingViewModel"

    // Service connection state
    private var bound = false

    // Collection jobs - these need to be canceled when unbinding
    private var streamingCollectionJob: Job? = null
    private var resultCollectionJob: Job? = null

    // StateFlows to expose service state to the UI
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _responseFlow = MutableStateFlow<VerificationResult>(
        VerificationResult.Error(ErrorType.START_STREAMING_DEFAULT)
    )
    val responseFlow: StateFlow<VerificationResult> = _responseFlow.asStateFlow()

    val sessionId = mutableStateOf("")

    // Define the service connection that doesn't hold a direct reference to the service
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as StreamingForegroundService.StreamingBinder
            bound = true

            // Start collecting flows from the service but don't store a reference to it
            streamingCollectionJob = viewModelScope.launch {
                binder.getService().isStreaming.collect { streaming ->
                    _isStreaming.value = streaming
                }
            }

            resultCollectionJob = viewModelScope.launch {
                binder.getService().verificationResult.collect { result ->
                    _responseFlow.value = result
                }
            }

            Log.d(TAG, "Service connected")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            cancelCollectionJobs()
            Log.d(TAG, "Service disconnected")
        }
    }

    private fun cancelCollectionJobs() {
        streamingCollectionJob?.cancel()
        streamingCollectionJob = null
        resultCollectionJob?.cancel()
        resultCollectionJob = null
    }

    // Bind to the service
    fun bindService(context: Context) {
        val intent = Intent(context, StreamingForegroundService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    // Unbind from the service
    fun unbindService(context: Context) {
        if (bound) {
            cancelCollectionJobs()
            context.unbindService(connection)
            bound = false
        }
    }

    // Start the streaming process
    fun startStreaming(context: Context, sessionNameText: String) {
        if (_isStreaming.value) return

        // Start the foreground service
        StreamingForegroundService.startService(context, sessionNameText)

        // Make sure we're bound to the service to receive updates
        if (!bound) {
            bindService(context)
        }
    }

    // Stop the streaming process
    fun stopStreaming(context: Context) {
        if (!_isStreaming.value) return

        // Stop the foreground service
        StreamingForegroundService.stopService(context)
    }

    // Clean up when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing collection jobs
        cancelCollectionJobs()
        // The service will handle its own cleanup in onDestroy
    }
}





//
//
//import android.content.ContentValues.TAG
//import android.content.Context
//import android.util.Log
//import android.widget.Toast
//import androidx.compose.runtime.MutableState
//import androidx.compose.runtime.mutableDoubleStateOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.majorapplication.location.fetchLocationAndCoordinates
//import com.example.majorapplication.grpcclient.ErrorType
//import com.example.majorapplication.grpcclient.GrpcSession
//import com.example.majorapplication.grpcclient.VerificationResult
//import com.example.majorapplication.messages.ProofData
//import com.example.majorapplication.messages.StreamSessionRequest
//import com.example.majorapplication.zkp.BulletproofsBinding
//import com.google.android.gms.location.LocationServices
//import com.google.firebase.Firebase
//import com.google.firebase.auth.auth
//import com.google.protobuf.Timestamp
//import com.google.protobuf.kotlin.toByteString
//import io.grpc.stub.StreamObserver
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import java.time.LocalDateTime
//
//class StreamingViewModel : ViewModel() {
//
//    private var requestObserver: StreamObserver<StreamSessionRequest>? = null
//
//    private val refLat: MutableState<Double> = mutableDoubleStateOf(Double.NEGATIVE_INFINITY)
//    private val refLon: MutableState<Double> = mutableDoubleStateOf(Double.NEGATIVE_INFINITY)
//
//    private val _responseFlow = MutableStateFlow<VerificationResult>(VerificationResult.Error(ErrorType.START_STREAMING_DEFAULT))
//    val responseFlow = _responseFlow.asStateFlow()
//
//    private var streamJob: Job? = null
//    private val _isStreaming = MutableStateFlow<Boolean>(false)
//    val isStreaming = _isStreaming.asStateFlow()
//
//    val sessionId = mutableStateOf<String>("")
//    private val sessionName = mutableStateOf<String>("Start session")
//
//    // Start the streaming process
//    fun startStreaming(context: Context,sessionNameText:String) {
//
//        if (_isStreaming.value) return
//
//        val returnedCoords = GrpcSession.sendSessionReq(Firebase.auth.currentUser?.uid!!,sessionNameText)
//        Log.d(TAG,"coordinates returned $returnedCoords")
//
//        refLat.value = returnedCoords.first.first
//        refLon.value = returnedCoords.first.second
//        sessionId.value = returnedCoords.second.first
//        sessionName.value = returnedCoords.second.second
//
//        if (refLat.value==Double.NEGATIVE_INFINITY || refLat.value == Double.NEGATIVE_INFINITY){
//            Toast.makeText(context,"Cannot Fetch reference coordinates",Toast.LENGTH_LONG).show()
//            return
//        }
//
//        _isStreaming.value = true
//
//        // Setup bidirectional streaming
//
//        requestObserver = GrpcSession.streamSession(verificationResultFlow = _responseFlow, isStreaming = _isStreaming)
//
//        // Start the continuous data sending process
//        streamJob = viewModelScope.launch {
//            while (isActive) {
//                val (lat1, lon1) = fetchLocationAndCoordinates(context = context, fusedLocationClient = LocationServices.getFusedLocationProviderClient(context))
//                Log.d("viewmodel long lat error", "coord values are $lat1 $lon1 ${refLat.value} ${refLon.value}")
//                val proofData = BulletproofsBinding.generateProofForCoordinates(lat1!!,lon1!!,refLat.value,refLon.value)
//                val request = StreamSessionRequest.newBuilder()
//                    .setUserId(Firebase.auth.currentUser!!.uid)
//                    .setSessionId(sessionId.value)
//                    .setData(ProofData.newBuilder().setProofBytes(proofData?.proofBytes?.toByteString()).setRistrettoBytes(proofData?.ristrettoBytes?.toByteString()))
//                    .setRecordedTimestamp(Timestamp.newBuilder().setSeconds(LocalDateTime.now().second.toLong()))
//                    .build()
//
//                try {
//                    requestObserver?.onNext(request)
//                } catch (e: Exception) {
//                    stopStreaming()
//                    break
//                }
//
//                // Add delay between requests
//                delay(10000) // Adjust as needed
//            }
//        }
//    }
//
//    // Stop the streaming process
//    fun stopStreaming() {
//        streamJob?.cancel()
//        streamJob = null
//
//        try {
//            requestObserver?.onError(Throwable(message = "Stream Completed"))
////            requestObserver?.onCompleted()
//        } catch (e: Exception) {
//            // Handle any errors
//            Log.d("Stream Error",e.message.toString())
//        }
//
//        requestObserver = null
//        _isStreaming.value = false
//        refLat.value = Double.NEGATIVE_INFINITY
//        refLon.value = Double.NEGATIVE_INFINITY
//        sessionName.value = "Start session"
//        sessionId.value = ""
//    }
//
//    // Clean up when ViewModel is cleared
//    override fun onCleared() {
//        super.onCleared()
//        stopStreaming()
//        GrpcSession.terminateChannel()
//        refLat.value = Double.NEGATIVE_INFINITY
//        refLon.value = Double.NEGATIVE_INFINITY
//        sessionName.value = "Start session"
//        sessionId.value = ""
//    }
//}
