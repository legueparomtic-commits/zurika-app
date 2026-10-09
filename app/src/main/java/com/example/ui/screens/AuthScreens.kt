package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import com.example.data.ServiceCategory
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    nameVal: String,
    phoneVal: String,
    onContinue: (String, String) -> Unit
) {
    var fullName by remember { mutableStateOf(nameVal) }
    var phoneNumber by remember { mutableStateOf(if (phoneVal.isBlank()) "" else phoneVal.replace("+228 ", "")) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("welcome_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon
            Surface(
                shape = CircleShape,
                color = ZurikaPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Car",
                        tint = ZurikaAmber,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Bienvenue sur ZURIKA",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Créez votre compte pour accéder à tous les services de géolocalisation et dépannage.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Nom complet input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Nom complet",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = { Text("Entrez votre nom", color = Color.Gray) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Person",
                            tint = ZurikaPrimary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("full_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = ZurikaPrimary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Numéro de téléphone input (+228 Togo default)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Numéro de téléphone",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Country Code box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
                        ) {
                            Text(text = "🇹🇬 +228", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        }
                    }

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { if (it.length <= 10) phoneNumber = it },
                        placeholder = { Text("90 XX XX XX", color = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone",
                                tint = ZurikaPrimary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_number_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = ZurikaPrimary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = errorMessage!!, color = ZurikaStatusClosed, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CONTINUER button
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        errorMessage = "Veuillez entrer votre nom complet."
                    } else if (phoneNumber.isBlank() || phoneNumber.length < 8) {
                        errorMessage = "Veuillez entrer un numéro de téléphone valide."
                    } else {
                        errorMessage = null
                        onContinue(fullName, phoneNumber)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("continue_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZurikaPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "CONTINUER",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next")
            }
        }
    }
}

@Composable
fun OTPScreen(
    phoneNumber: String = "+228 90 12 34 56",
    timerSeconds: Int = 30,
    generatedCode: String = "",
    errorMsg: String? = null,
    isAlreadyRegistered: Boolean = false,
    onVerifyCode: (String) -> Unit = {},
    onResendCode: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var codeState by remember { mutableStateOf("") }
    var localTimer by remember(timerSeconds) { mutableIntStateOf(timerSeconds) }

    LaunchedEffect(localTimer) {
        if (localTimer > 0) {
            delay(1000)
            localTimer -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("otp_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Étape 2 / 2", color = Color.Gray, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = CircleShape,
                color = ZurikaAmber.copy(alpha = 0.15f),
                modifier = Modifier.size(70.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Sms, contentDescription = "SMS", tint = ZurikaAmber, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Vérifiez votre numéro",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Un code de vérification à 6 chiffres a été envoyé par SMS au :",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Text(
                text = phoneNumber,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ZurikaPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (isAlreadyRegistered) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = ZurikaAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "ℹ️ Ce numéro est déjà associé à un compte. Saisissez le code OTP pour vous connecter.",
                        color = ZurikaAmber,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simulated SMS Code notification bar for quick auto-fill in app demo
            if (generatedCode.isNotEmpty()) {
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            codeState = generatedCode
                            onVerifyCode(generatedCode)
                        }
                        .border(1.dp, ZurikaPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Message, contentDescription = "Code", tint = ZurikaAmber)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Code ZURIKA reçu :", fontSize = 11.sp, color = Color.Gray)
                            Text(text = generatedCode, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
                        }
                        Button(
                            onClick = {
                                codeState = generatedCode
                                onVerifyCode(generatedCode)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZurikaPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(text = "Remplir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 6 Horizontally Aligned Numeric Boxes with Auto Focus
            OtpSixDigitInput(
                code = codeState,
                onCodeChanged = { codeState = it },
                onComplete = { fullCode ->
                    onVerifyCode(fullCode)
                }
            )

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = errorMsg, color = ZurikaStatusClosed, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Resend code timer button with 30-second countdown
            if (localTimer > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Timer",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Renvoyer le code dans ${localTimer}s",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                TextButton(
                    onClick = {
                        onResendCode()
                        localTimer = 30
                    },
                    modifier = Modifier.testTag("resend_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Resend",
                        tint = ZurikaPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Renvoyer le code",
                        color = ZurikaPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onVerifyCode(codeState) },
                enabled = codeState.length == 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("validate_otp_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZurikaPrimary)
            ) {
                Text(text = "VALIDER LE CODE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    timerSeconds: Int,
    generatedCode: String,
    errorMsg: String?,
    isAlreadyRegistered: Boolean,
    onVerifyCode: (String) -> Unit,
    onResendCode: () -> Unit,
    onBack: () -> Unit
) {
    OTPScreen(
        phoneNumber = phoneNumber,
        timerSeconds = timerSeconds,
        generatedCode = generatedCode,
        errorMsg = errorMsg,
        isAlreadyRegistered = isAlreadyRegistered,
        onVerifyCode = onVerifyCode,
        onResendCode = onResendCode,
        onBack = onBack
    )
}

@Composable
fun OtpSixDigitInput(
    code: String,
    onCodeChanged: (String) -> Unit,
    onComplete: (String) -> Unit
) {
    val otpLength = 6
    val focusRequesters = remember { List(otpLength) { FocusRequester() } }

    LaunchedEffect(Unit) {
        delay(300)
        try {
            focusRequesters[0].requestFocus()
        } catch (e: Exception) {
            // Ignore focus request error if composed asynchronously
        }
    }

    val otpDigits = remember(code) {
        val list = MutableList(otpLength) { "" }
        for (i in 0 until minOf(code.length, otpLength)) {
            list[i] = code[i].toString()
        }
        list
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until otpLength) {
            val digit = otpDigits.getOrElse(index) { "" }

            OutlinedTextField(
                value = digit,
                onValueChange = { newValue ->
                    // Handle pasting multiple digits
                    if (newValue.length > 1) {
                        val digitsOnly = newValue.filter { it.isDigit() }
                        if (digitsOnly.length >= otpLength) {
                            val fullCode = digitsOnly.take(otpLength)
                            onCodeChanged(fullCode)
                            onComplete(fullCode)
                        } else {
                            val nextCode = (code + digitsOnly).take(otpLength)
                            onCodeChanged(nextCode)
                        }
                        return@OutlinedTextField
                    }

                    // Handle single character digit input
                    val newDigit = newValue.filter { it.isDigit() }.takeLast(1)
                    val updatedList = otpDigits.toMutableList()
                    updatedList[index] = newDigit
                    val newFullCode = updatedList.joinToString("")
                    onCodeChanged(newFullCode)

                    if (newDigit.isNotEmpty()) {
                        // Move focus to next field if available
                        if (index < otpLength - 1) {
                            focusRequesters[index + 1].requestFocus()
                        } else {
                            // Reached the 6th field
                            if (newFullCode.length == otpLength) {
                                onComplete(newFullCode)
                            }
                        }
                    } else {
                        // Digit deleted
                        if (index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = ZurikaPrimary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .width(48.dp)
                    .height(58.dp)
                    .focusRequester(focusRequesters[index])
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                            if (otpDigits[index].isEmpty() && index > 0) {
                                focusRequesters[index - 1].requestFocus()
                                val updatedList = otpDigits.toMutableList()
                                updatedList[index - 1] = ""
                                onCodeChanged(updatedList.joinToString(""))
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    .testTag("otp_digit_$index")
            )
        }
    }
}

@Composable
fun InitialCategoryScreen(
    userName: String,
    onCategorySelected: (ServiceCategory) -> Unit
) {
    val firstName = userName.split(" ").firstOrNull() ?: userName

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("initial_category_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "✓ Numéro vérifié", color = ZurikaStatusOpen, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Bienvenue, $firstName 👋",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ZURIKA est prêt à vous aider. Que recherchez-vous ?",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Category Selection Cards
            CategoryTile(
                title = "Station-service",
                subtitle = "Essence, Gazole, Recharges",
                icon = Icons.Default.LocalGasStation,
                color = ZurikaStatusOpen,
                onClick = { onCategorySelected(ServiceCategory.STATION) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            CategoryTile(
                title = "Mécanicien",
                subtitle = "Réparation moteur, Entretien, Vidange",
                icon = Icons.Default.Build,
                color = ZurikaPrimary,
                onClick = { onCategorySelected(ServiceCategory.MECHANIC) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            CategoryTile(
                title = "Vulcanisateur",
                subtitle = "Crevaison, Pression, Pneus neufs",
                icon = Icons.Default.TwoWheeler,
                color = ZurikaAmber,
                onClick = { onCategorySelected(ServiceCategory.VULCANIZER) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            CategoryTile(
                title = "Service Dépannage",
                subtitle = "Remorquage urgent 24/7, Boost batterie",
                icon = Icons.Default.Warning,
                color = ZurikaStatusClosed,
                onClick = { onCategorySelected(ServiceCategory.TOWING) }
            )
        }
    }
}

@Composable
private fun CategoryTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionScreen(
    onGrantPermission: (allowed: Boolean, isFine: Boolean) -> Unit
) {
    val context = LocalContext.current
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val fineLocationGranted = permissionState.permissions.find {
        it.permission == Manifest.permission.ACCESS_FINE_LOCATION
    }?.status?.isGranted == true

    val coarseLocationGranted = permissionState.permissions.find {
        it.permission == Manifest.permission.ACCESS_COARSE_LOCATION
    }?.status?.isGranted == true

    val anyLocationGranted = fineLocationGranted || coarseLocationGranted
    val shouldShowRationale = permissionState.shouldShowRationale

    // Check if permission was requested and denied without rationale (permanently denied)
    var hasRequestedPermission by remember { mutableStateOf(false) }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Auto-advance if permission is granted
    LaunchedEffect(fineLocationGranted, coarseLocationGranted) {
        if (anyLocationGranted) {
            kotlinx.coroutines.delay(600)
            onGrantPermission(true, fineLocationGranted)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF0284C7).copy(alpha = 0.3f),
                        Color(0xFF0F172A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("location_permission_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Radar GPS Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                // Pulsing outer aura
                Box(
                    modifier = Modifier
                        .size(100.dp * pulseScale)
                        .clip(CircleShape)
                        .background(ZurikaAmber.copy(alpha = pulseAlpha))
                )

                Surface(
                    shape = CircleShape,
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(2.dp, ZurikaAmber),
                    modifier = Modifier.size(80.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (fineLocationGranted) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                            contentDescription = "GPS Icon",
                            tint = if (fineLocationGranted) ZurikaStatusOpen else ZurikaAmber,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Title
            Text(
                text = if (fineLocationGranted) "Position Précise Activée !" else "Activer la Localisation Fine",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "ZURIKA utilise la localisation fine (GPS haute précision) pour calculer vos distances et dépanner votre véhicule à Lomé et dans tout le Togo.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Service Breakdown Mini Cards
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LocationBenefitItem(
                        icon = Icons.Default.LocalGasStation,
                        iconColor = ZurikaPrimary,
                        title = "Stations & Carburant",
                        description = "Trouvez les stations ouvertes avec Super/Gazole à moins de 2 km."
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    LocationBenefitItem(
                        icon = Icons.Default.Build,
                        iconColor = ZurikaAmber,
                        title = "Mécaniciens & Garages",
                        description = "Guidage GPS direct vers le réparateur le plus proche."
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    LocationBenefitItem(
                        icon = Icons.Default.Warning,
                        iconColor = ZurikaStatusClosed,
                        title = "Dépannage & SOS Remorquage",
                        description = "Envoi instantané de vos coordonnées GPS exactes au dépanneur."
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Banner / Rationale feedback
            if (anyLocationGranted) {
                Surface(
                    color = ZurikaStatusOpen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaStatusOpen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ZurikaStatusOpen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (fineLocationGranted) "✓ Localisation Haute Précision (Fine Location) activée" else "✓ Localisation Approximative activée",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZurikaStatusOpen
                        )
                    }
                }
            } else if (shouldShowRationale) {
                Surface(
                    color = ZurikaAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ZurikaAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sans position précise, l'itinéraire vers les stations et dépanneurs peut être imprécis.",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            } else if (hasRequestedPermission && !anyLocationGranted) {
                // Permanently denied state -> Provide settings link
                Surface(
                    color = ZurikaStatusClosed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaStatusClosed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Permission de localisation refusée.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Vous pouvez l'activer manuellement dans les paramètres de l'application.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = ZurikaAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ouvrir les Paramètres", color = ZurikaAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action Button (Accompanist permission launcher)
            Button(
                onClick = {
                    hasRequestedPermission = true
                    if (anyLocationGranted) {
                        onGrantPermission(true, fineLocationGranted)
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("grant_location_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZurikaAmber,
                    contentColor = DarkBackground
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (anyLocationGranted) Icons.Default.Check else Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = DarkBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (anyLocationGranted) "CONTINUER VERS LA CARTE" else "AUTORISER LA POSITION PRÉCISE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary "Plus tard" button
            TextButton(
                onClick = { onGrantPermission(false, false) },
                modifier = Modifier.testTag("later_location_button")
            ) {
                Text(
                    text = "Continuer sans localisation (Lomé par défaut)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun LocationBenefitItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.2f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.LightGray,
                lineHeight = 16.sp
            )
        }
    }
}
