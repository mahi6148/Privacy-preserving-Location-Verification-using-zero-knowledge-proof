package com.example.majorapplication.presentation


import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.majorapplication.grpcclient.GRPCFetchSession
import com.example.majorapplication.grpcclient.Session
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ActivityScreen(navController: NavController){
    var searchValue by remember{ mutableStateOf("") }
    var sessionsList by remember { mutableStateOf<List<Session>>(emptyList()) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {

        sessionsList = refreshSessionsList(context = context)

    }
    Column(modifier =Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Fetch Sessions and Attendance", style = TextStyle(fontSize = 24.sp))
            Box(modifier = Modifier.height(25.dp))
            Row (horizontalArrangement = Arrangement.SpaceEvenly){
                TextField(value = searchValue, onValueChange = { searchValue = it }, singleLine = true, placeholder = { Text("search for sessions") })
                Spacer(Modifier)
                IconButton(onClick = {sessionsList = refreshSessionsList(context = context) }) {
                    Icon(Icons.Rounded.Refresh,"Refresh")
                }
            }

            Box(modifier = Modifier.height(25.dp))
            Box(
                modifier = Modifier
                    .weight(0.8f)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Your list items go here
                    val filteredSessions = sessionsList.filter {
                        it.sessionName.contains(searchValue, ignoreCase = true) ||
                                it.sessionId.toString().contains(searchValue)
                    }

                    items(filteredSessions, key = {it.sessionId}) { session ->
                        SessionDropdownCard(session = session)
                    }

                }

            }
            BottomNavBar(navController = navController)
        }
    }
}

fun refreshSessionsList(context: Context): List<Session> {
   return GRPCFetchSession(context).fetchSessions(Firebase.auth.currentUser?.uid!!)
}

@Composable
fun SessionDropdownCard(session: Session) {
    var expanded by remember { mutableStateOf(false) }
    val attendancePercentage = session.attendancePercentage // This should be calculated or fetched from your data

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header - always visible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Session #${session.sessionId}",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = session.sessionName,
                        style = TextStyle(fontSize = 16.sp)
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Attendance:",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$attendancePercentage%",
                            fontWeight = FontWeight.Bold,
                            color = when {
                                attendancePercentage >= 90 -> Color.Green
                                attendancePercentage >= 75 -> Color.Blue
                                attendancePercentage >= 60 -> Color(0xFFFFA500) // Orange
                                else -> Color.Red
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
//                        Column {
//                            Text("Started: ${formatDateTime(session.startTime)}")
//                            session.endTime.let {
//                                Text("Ended: ${formatDateTime(it)}")
//                            }
//                        }

//                        Button(
//                            onClick = { /* View details action */ },
//                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
//                        ) {
//                            Text("View Details")
//                        }
                    }
                }
            }
        }
    }
}

// Helper function to format timestamp
fun formatDateTime(timestamp: Timestamp): String {
    val instant = Instant.ofEpochSecond(timestamp.seconds.toLong(), timestamp.nanos.toLong())
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    return dateTime.format(formatter)
}