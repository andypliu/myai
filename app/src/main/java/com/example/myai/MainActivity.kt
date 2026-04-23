package com.example.myai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.ui.graphics.Color
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

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.primary,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.primary,
            navigationBarContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> ChatScreen(
                    profileViewModel = profileViewModel,
                    viewModel = chatViewModel
                )
                AppDestinations.FAVORITES -> Greeting(
                    name = "Favorites",
                    modifier = Modifier.padding(innerPadding)
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