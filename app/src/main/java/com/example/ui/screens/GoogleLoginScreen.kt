package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import android.util.Log
import com.example.ui.theme.*
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
    val activeClientId = webClientId.ifEmpty { secretClientId }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun performGoogleSignIn() {
        coroutineScope.launch {
            isLoading = true
            statusMessage = null

            if (activeClientId.isEmpty()) {
                statusMessage = "Google OAuth Client ID is missing. Please ensure GCP_WEB_CLIENT_ID is configured."
                isLoading = false
                return@launch
            }

            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(activeClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdTokenCredential.id
                    onLogin(email, activeClientId)
                } else {
                    statusMessage = "Unable to retrieve Google credential token from device Play Services."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.e("GoogleLoginScreen", "GetCredentialCancellationException: prompt cancelled or dismissed", e)
                statusMessage = "Google Sign-In prompt was closed or cancelled by Play Services.\n" +
                    "Ensure your SHA-1 fingerprint and package name match your Android OAuth Client configuration."
            } catch (e: GetCredentialException) {
                Log.e("GoogleLoginScreen", "GetCredentialException occurred", e)
                val err = e.localizedMessage ?: e.message ?: ""
                statusMessage = if (err.contains("cancelled", ignoreCase = true) || err.contains("canceled", ignoreCase = true)) {
                    "Google Sign-In prompt was closed or cancelled by Play Services."
                } else if (err.contains("No credentials available", ignoreCase = true) || err.contains("16", ignoreCase = true)) {
                    "Android Play Services reports: [16] No credentials available on this device account."
                } else {
                    "Google Sign-In Error: $err"
                }
            } catch (e: Exception) {
                Log.e("GoogleLoginScreen", "General Exception during Google Sign-In", e)
                statusMessage = "Authentication Error: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
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
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(12.dp)
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



