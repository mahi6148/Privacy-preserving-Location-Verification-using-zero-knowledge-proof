package com.example.majorapplication.presentation


import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.majorapplication.grpcclient.VerificationResult.Companion.toTextString
import com.example.majorapplication.viewmodels.StreamingViewModel


@Composable
fun HomeScreen(navController: NavController){
    val localContext = LocalContext.current
    val viewModel = viewModel<StreamingViewModel>()
    val isStreamingCompose = viewModel.isStreaming.collectAsState()
    val verificationResult = viewModel.responseFlow.collectAsState()
    val startButtonContainerColor = if (isStreamingCompose.value) {
        Color.Gray // Or a color indicating streaming is active
    } else {
        Color.Green // Or a default color
    }
    val stopButtonContainerColor = if (isStreamingCompose.value) {
        Color.Red // Or a color indicating streaming is active
    } else {
        Color.Gray // Or a default color
    }
    val isDialogOpen = remember {
        mutableStateOf<Boolean>(false)
    }
   Column {
       if (isDialogOpen.value){
           SessionNameDialog(isDialogOpen,viewModel,localContext)
       }
       Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
           Text(verificationResult.value.toTextString())
           Button(onClick = {
               if(!isStreamingCompose.value){
                  isDialogOpen.value=true
               }

           }, colors = ButtonDefaults.buttonColors(containerColor =startButtonContainerColor)) { Icon(Icons.Rounded.PlayArrow,"Start") }
           Button(onClick = {
               if (isStreamingCompose.value){
                   viewModel.stopStreaming(context =localContext )
               }
           },colors = ButtonDefaults.buttonColors(containerColor =stopButtonContainerColor)) { Icon(Icons.Rounded.Clear,"Cancel") }
       }
       BottomNavBar(navController=navController)

   }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionNameDialog(isDialogOpen: MutableState<Boolean>, viewModel: StreamingViewModel,ctx:Context){
    var text by remember { mutableStateOf("") }
    val charLimit = 10
    BasicAlertDialog(
        modifier = Modifier.fillMaxSize(),
        onDismissRequest = {
            isDialogOpen.value = false
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    if (it.length <= charLimit) {
                        text = it
                    }
                },
                label = { Text("Enter Session Name") },
                placeholder = { Text("Write something...") },
                modifier = Modifier
                    .fillMaxWidth(),
                maxLines = 1, // Allows for multiple lines
                textStyle = TextStyle(color = Color.Black),
                colors = TextFieldDefaults.colors(),
                singleLine = false
            )

            Row(modifier = Modifier.fillMaxWidth()){
                Button(
                    onClick = {
                        if (text.isNotBlank()){
                            viewModel.startStreaming(context = ctx,text)
                            isDialogOpen.value=false
                        }else{
                            Toast.makeText(ctx,"Enter at least 1 character or at most 10 characters",Toast.LENGTH_SHORT).show()
                        }
                    }) {
                    Text("Start")
                }
                Button(onClick = {isDialogOpen.value=false}) {
                    Text("Cancel")
                }
            }
        }

    }
}

