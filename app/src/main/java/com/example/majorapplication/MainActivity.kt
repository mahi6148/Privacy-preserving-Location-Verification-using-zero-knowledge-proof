package com.example.majorapplication



import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.majorapplication.auth.GoogleAuthUiClient
import com.example.majorapplication.auth.ui.SignInScreen
import com.example.majorapplication.auth.viewmodels.SignInViewModel
import com.example.majorapplication.presentation.ProfileScreen
import com.example.majorapplication.grpcclient.GrpcSession
import com.example.majorapplication.grpcclient.GrpcUser
import com.example.majorapplication.location.PermissionHelper
import com.example.majorapplication.models.Route
import com.example.majorapplication.presentation.ActivityScreen
import com.example.majorapplication.presentation.HomeScreen
import com.example.majorapplication.ui.theme.MajorApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var grpcUser:GrpcUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         grpcUser = GrpcUser(applicationContext)

        PermissionHelper.checkAndRequestPermissions(this,{},{
            this.finishAffinity()
        },
            {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            this.startActivity(intent)
        })
        val activity = this
        setContent {
            MajorApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val navController = rememberNavController()
                    val viewModel = viewModel<SignInViewModel>()
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    val googleAuthUiClient by lazy {
                        GoogleAuthUiClient(
                            context = applicationContext,
                            activity = activity,
                            onSignInResult = viewModel::onSignInResult
                        )
                    }

                    NavHost(navController = navController, startDestination = Route.Auth){
                        navigation<Route.Auth>(startDestination = Route.loginScreen){
                            composable<Route.loginScreen> {
                                LaunchedEffect(key1 = Unit) {
                                    if(googleAuthUiClient.getSignedInUser() != null) {
                                        val user = Firebase.auth.currentUser!!
                                        navController.navigate(Route.profileScreen)
                                        try {
                                            withContext(Dispatchers.IO){
                                                grpcUser.sendUserRequest(user.uid,user.displayName!!,user.email!!)
                                            }
                                        }catch (e:Exception){
                                            Log.d("GRPC user Request exception", e.message.toString())
                                        }
                                    }
                                }

                                LaunchedEffect(key1 = state.isSignInSuccessful) {
                                    if(state.isSignInSuccessful) {
                                        Toast.makeText(
                                            applicationContext,
                                            "Sign in successful",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        try {
                                            val user = Firebase.auth.currentUser!!
                                            withContext(Dispatchers.IO){
                                                grpcUser.sendUserRequest(user.uid,user.displayName!!,user.email!!)
                                            }
                                        }catch (e:Exception){
                                            Log.d("GRPC user Request exception", e.message.toString())
                                        }



                                        navController.navigate(Route.Main){
                                            popUpTo(Route.Auth) {
                                                inclusive=true
                                            }
                                        }
                                        viewModel.resetState()
                                    }
                                }

                                SignInScreen(
                                    state = state,
                                    onSignInClick = {
                                        lifecycleScope.launch {
                                            googleAuthUiClient.signIn()
                                        }
                                    }
                                )
                            }
                        }
                        navigation<Route.Main>(Route.profileScreen){
                            composable<Route.profileScreen> {
                                ProfileScreen(
                                    userData = googleAuthUiClient.getSignedInUser(),
                                    onSignOut = {
                                        lifecycleScope.launch {
                                            googleAuthUiClient.signOut()
                                            Toast.makeText(
                                                applicationContext,
                                                "Signed out",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            navController.navigate(Route.Auth){
                                                popUpTo(Route.Main){
                                                    inclusive=true
                                                }
                                            }
                                        }
                                    },
                                    onNavigatetoLocationScreen = {

                                    },
                                    sendreqfun = {
//
                                    },
                                    navController = navController
                                )
                            }
                            composable<Route.Home> { HomeScreen(navController = navController) }
                            composable<Route.Activity> { ActivityScreen(navController = navController) }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        grpcUser.terminateChannel()
        super.onDestroy()
    }

}


