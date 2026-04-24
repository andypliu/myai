package com.example.myai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myai.presentation.chat.ChatScreen
import com.example.myai.presentation.chat.ChatViewModel
import com.example.myai.presentation.chat.ChatViewModelFactory
import com.example.myai.presentation.login.LoginScreen
import com.example.myai.presentation.login.LoginViewModel
import com.example.myai.presentation.login.LoginViewModelFactory
import com.example.myai.presentation.profile.ProfileScreen
import com.example.myai.presentation.profile.ProfileViewModel
import com.example.myai.presentation.profile.ProfileViewModelFactory
import com.example.myai.ui.theme.GreenDark
import com.example.myai.ui.theme.MyAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyAITheme {
                MyAIApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun MyAIApp() {
    val context = LocalContext.current
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(context)
    )
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(viewModel = loginViewModel)
    } else {
        MainAppContent(onLogout = loginViewModel::logout)
    }
}

@Composable
fun MainAppContent(onLogout: () -> Unit) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val context = LocalContext.current
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(context)
    )
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(context)
    )

    // Detect if keyboard is open to hide navigation bar
    val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isKeyboardOpen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    AppDestinations.entries.forEach {
                        NavigationBarItem(
                            selected = currentDestination == it,
                            onClick = { currentDestination = it },
                            icon = {
                                Icon(
                                    painterResource(it.icon),
                                    contentDescription = it.label
                                )
                            },
                            label = { Text(it.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = GreenDark,
                                unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (currentDestination) {
                AppDestinations.HOME -> ChatScreen(
                    profileViewModel = profileViewModel,
                    viewModel = chatViewModel
                )
                AppDestinations.FAVORITES -> Greeting(
                    name = "Favorites",
                    modifier = Modifier.fillMaxSize()
                )
                AppDestinations.PROFILE -> ProfileScreen(onLogout = onLogout)
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    FAVORITES("Favorites", R.drawable.ic_favorite),
    PROFILE("Profile", R.drawable.ic_account_box),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyAITheme {
        Greeting("Android")
    }
}
