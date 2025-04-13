package com.example.majorapplication.presentation

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.majorapplication.models.Route

data class NavBarItems(val route: Route,val icon:ImageVector,val label:String)


@Composable
fun BottomNavBar(navController: NavController){

    val bottomNavItems = listOf<NavBarItems>(
        NavBarItems(Route.Activity,Icons.Rounded.DateRange, label = "Activity"),
        NavBarItems(Route.Home, icon = Icons.Rounded.Place, label = "Home"),
        NavBarItems(Route.profileScreen,Icons.Rounded.AccountCircle, label = "Profile")


    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination

    NavigationBar{
        bottomNavItems.forEach { item->
            NavigationBarItem(onClick = {
                navController.navigate(item.route){
                    launchSingleTop=true
                    popUpTo(navController.graph.findStartDestination().id){
                        saveState=true
                    }
                    restoreState=true
                } },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                selected = currentRoute?.hierarchy?.any {
                    it.hasRoute(item.route::class)
                }==true
            )

        }
    }

}