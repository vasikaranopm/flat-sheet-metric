package com.example.ui.screens

import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleLoginScreen(
    userEmail: String,
    webClientId: String,
    onLogin: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val secretClientId = try {
        val key = com.example.BuildConfig.GCP_WEB_CLIENT_ID
        if (key == "GCP_WEB_CLIENT_ID_PLACEHOLDER") "" else key
    } catch (e: Exception) { "" }
    val defaultWebClientId = "644847385425-kh2mmndms4djsl9nhei06elib3vb0052.apps.googleusercontent.com"
    val activeClientId = webClientId.ifEmpty { secretClientId.ifEmpty { defaultWebClientId } }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isCancelledByPlayServices by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var manualEmailInput by remember { mutableStateOf(userEmail) }
    var showManualEmailInput by remember { mutableStateOf(false) }

    // Legacy Google Sign-In Launcher (play-services-auth)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val email = account?.email
            if (!email.isNullOrEmpty()) {
                onLogin(email, activeClientId)
            } else {
                statusMessage = "Google Sign-In prompt closed. Enter your email below to continue."
                isCancelledByPlayServices = true
                showManualEmailInput = true
            }
        } catch (e: ApiException) {
            Log.e("GoogleLoginScreen", "GoogleSignIn failed with code ${e.statusCode}", e)
            // If code 12500, 10, or 8 (OAuth ID token or SHA1 mismatch), retry with basic email sign-in
            if (e.statusCode == 12500 || e.statusCode == 10 || e.statusCode == 8) {
                try {
                    val basicGso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(context, basicGso)
                    // If lastSignedInAccount exists, use it
                    val existingAccount = GoogleSignIn.getLastSignedInAccount(context)
                    if (existingAccount?.email != null) {
                        onLogin(existingAccount.email!!, activeClientId)
                        return@rememberLauncherForActivityResult
                    }
                } catch (ex: Exception) {
                    Log.e("GoogleLoginScreen", "Error checking last signed in account", ex)
                }
            }
            isCancelledByPlayServices = true
            showManualEmailInput = true
            statusMessage = "Google Sign-In prompt closed by Play Services. You can enter your email address directly below to proceed."
        }
    }

    fun performGoogleSignIn() {
        isLoading = true
        statusMessage = null
        isCancelledByPlayServices = false

        // Check if user is already signed in via GoogleSignIn
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount?.email != null) {
            isLoading = false
            onLogin(lastAccount.email!!, activeClientId)
            return
        }

        try {
            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
            if (activeClientId.isNotEmpty()) {
                gsoBuilder.requestIdToken(activeClientId)
            }
            val googleSignInClient = GoogleSignIn.getClient(context, gsoBuilder.build())
            // Sign out first to force account selection dialog
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        } catch (e: Exception) {
            Log.e("GoogleLoginScreen", "Failed to launch Google Sign In", e)
            showManualEmailInput = true
            isCancelledByPlayServices = true
            statusMessage = "Enter your Google email address below to log in."
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Amber500
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = "Apartment Icon",
                            tint = Slate900,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Gomathi Ilam Thendral",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Flat Owners Portal",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Sign in with your Google account to access real-time maintenance collections, expense records, and owner directory.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Google Sign-In Button
                Button(
                    onClick = { performGoogleSignIn() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_login_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign in with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = statusMessage ?: "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                if (showManualEmailInput || isCancelledByPlayServices) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = manualEmailInput,
                        onValueChange = { manualEmailInput = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("your.email@gmail.com") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_email_input_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (manualEmailInput.isNotBlank()) {
                                onLogin(manualEmailInput.trim(), activeClientId)
                            }
                        },
                        enabled = manualEmailInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("one_tap_login_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (manualEmailInput.isBlank()) "Enter Email to Continue" else "Continue as ${manualEmailInput.trim()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showManualEmailInput = true }
                    ) {
                        Text(
                            text = "Or enter Google email manually",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Security info banner
                Surface(
                    color = Emerald100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Secured",
                            tint = Emerald600,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Authenticated with Read-Only Google Sheet integration",
                            fontSize = 11.sp,
                            color = Emerald600,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}




