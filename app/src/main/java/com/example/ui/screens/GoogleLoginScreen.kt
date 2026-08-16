package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoogleSheetConfig
import com.example.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleLoginScreen(
    config: GoogleSheetConfig,
    isLoading: Boolean = false,
    onLoginSuccess: (email: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val googleSignInOptions = remember(config.webClientId) {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        if (config.webClientId.isNotBlank()) {
            builder.requestIdToken(config.webClientId)
        }
        builder.build()
    }

    val googleSignInClient = remember(googleSignInOptions) {
        GoogleSignIn.getClient(context, googleSignInOptions)
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email
                if (!email.isNullOrBlank()) {
                    onLoginSuccess(email)
                } else {
                    authError = "Unable to retrieve Google email. Please try again."
                }
            } catch (e: ApiException) {
                authError = "Google Sign-In failed (${e.statusCode}): ${e.localizedMessage ?: "Please try again."}"
            } catch (e: Exception) {
                authError = "Google Sign-In failed: ${e.localizedMessage ?: "Please try again."}"
            }
        }
    }

    val handleSignIn = {
        isSigningIn = true
        authError = null
        try {
            signInLauncher.launch(googleSignInClient.signInIntent)
        } catch (e: Exception) {
            isSigningIn = false
            authError = "Unable to launch Google Sign-In: ${e.localizedMessage}"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Slate900,
                        Slate800,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // App Icon / Logo Shield
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Amber500)
                    .shadow(8.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Apartment,
                    contentDescription = "Apartment Icon",
                    tint = Slate900,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = config.spreadsheetTitle.ifBlank { "Apartment Maintenance Ledger" },
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Secure Google Authentication Required",
                fontSize = 13.sp,
                color = Amber100,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Box Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Protected",
                            tint = Amber600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Resident & Admin Portal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sign in with your Google account to view live maintenance ledger, payment collections, and yearly accounts.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Google Sign In Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .border(1.dp, Slate200, RoundedCornerShape(26.dp))
                            .clickable(enabled = !isSigningIn && !isLoading) {
                                handleSignIn()
                            }
                            .testTag("google_sign_in_button"),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSigningIn || isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Slate900,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Signing in with Google...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Amber100,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "G",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = Color(0xFF4285F4)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Sign in with Google",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                }
                            }
                        }
                    }

                    if (!authError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = Amber50,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Amber600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = authError ?: "",
                                    fontSize = 12.sp,
                                    color = Slate800,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security note footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    Icons.Default.GppGood,
                    contentDescription = "Verified",
                    tint = Emerald500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Google Sign-In protected ledger • Restricted access",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

