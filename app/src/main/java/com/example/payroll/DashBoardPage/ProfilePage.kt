import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.payroll.data.ViewModel

class ProfilePage(private val navController: NavController, private val viewModel: ViewModel) {
    @SuppressLint("NotConstructor")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ProfileScreen() {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Profile") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black
                    )
                )
            }
        ) { paddingValues ->
            val user by viewModel.userData.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Profile Card
                Card(
                    modifier = Modifier

                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEF5350)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Image Placeholder
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Username
                        when (user) {
                            null -> {
                                viewModel.fetchUserData()
                                Text(
                                    "Loading...",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            else -> {
                                // Get both department and name from the username string
                                val parts =
                                    user?.username?.split("-")?.map { it.trim() } ?: listOf("", "")
                                val department = parts.firstOrNull() ?: ""
                                val displayName = parts.lastOrNull() ?: ""

                                Text(
                                    text = displayName,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "$department - ${user!!.empId}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp
                                )
                                Column(
                                    modifier = Modifier.padding(top = 8.dp).background(color = Color(
                                        0xFFE97370
                                    ), shape = RoundedCornerShape(15.dp))
                                ) {
                                    Text(
                                        text = "Employee ID Card",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column {  // Menu Items
                    MenuButton(
                        icon = Icons.Default.Person,
                        text = "Personal Information",
                        onClick = { /* Handle click */ }
                    )

                    MenuButton(
                        icon = Icons.Default.Settings,
                        text = "Settings",
                        onClick = { /* Handle click */ }
                    )

                    MenuButton(
                        icon = Icons.Default.Notifications,
                        text = "Notifications",
                        onClick = { /* Handle click */ }
                    )

                    MenuButton(
                        icon = Icons.Default.Info,
                        text = "Help & Support",
                        onClick = { navController.navigate("Help") }
                    )
                }
            }
        }
    }

    @Composable
    private fun MenuButton(
        icon: ImageVector,
        text: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFEF5350)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = text)
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
    }
}