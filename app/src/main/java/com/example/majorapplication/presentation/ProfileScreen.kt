package com.example.majorapplication.presentation



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.majorapplication.auth.models.UserData
import com.example.majorapplication.grpcclient.GrpcSession
import com.example.majorapplication.zkp.BulletproofsBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userData: UserData?,
    onSignOut: () -> Unit,
    onNavigatetoLocationScreen: () -> Unit,
    sendreqfun:()->Unit,
    navController:NavController
) {

    val coroutine = CoroutineScope(Dispatchers.IO)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column (modifier = Modifier.weight(weight = 1f),verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally){
            if(userData?.profilePictureUrl != null) {
                AsyncImage(
                    model = userData.profilePictureUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if(userData?.username != null) {
                Text(
                    text = userData.username,
                    textAlign = TextAlign.Center,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(
                        lineHeight = TextUnit(1f, TextUnitType.Em)
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
//            Button(onClick = onNavigatetoLocationScreen ) { Text(text = "Go to Location Screen") }
//            Button(onClick = sendreqfun ){ Text("Send Session GRPC request") }
//            Button(onClick = {
//                coroutine.launch {
//                    GrpcSession.sendAttendanceRequest(BulletproofsBinding.generateProofForCoordinates(12.823087, 80.044927,12.824595, 80.045152)!!,Firebase.auth.currentUser?.uid!!)
////                GrpcRequest().sendAttendanceRequest(GrpcClient().channel,BulletproofsBinding.generateProofForCoordinates(0.0,0.0,0.0,0.0)!!,Firebase.auth.currentUser?.uid!!)
//                }
//            }) { Text("Sent Attendance Request") }
            Button(onClick = onSignOut) {
                Text(text = "Sign out")
            }
        }
        BottomNavBar(navController = navController)
    }
}