package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InterventionEntity
import com.example.data.ProblemType
import com.example.data.RequestStatus
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun InterventionDispatchDialog(
    targetServiceName: String?,
    userLat: Double,
    userLng: Double,
    activeIntervention: InterventionEntity?,
    onSubmitRequest: (ProblemType, String) -> Unit,
    onPostInterventionReview: (interventionId: String, serviceId: String, serviceName: String, rating: Float, comment: String, tags: List<String>, onDone: () -> Unit) -> Unit = { _, _, _, _, _, _, onDone -> onDone() },
    onCancelActive: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(ProblemType.PANNE_SECHE) }
    var descText by remember { mutableStateOf("") }
    var showRatingScreen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = DarkBackground,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag("intervention_dispatch_dialog")
    ) {
        AnimatedContent(
            targetState = when {
                showRatingScreen -> "RATING"
                activeIntervention != null -> "TRACKING"
                else -> "FORM"
            },
            label = "intervention_dialog_content"
        ) { targetState ->
            when (targetState) {
                "RATING" -> {
                    val intervention = activeIntervention
                    val effectiveServiceId = intervention?.serviceId ?: "station_1"
                    val effectiveServiceName = intervention?.serviceName ?: targetServiceName ?: "Garage Partenaire ZURIKA"

                    PostInterventionRatingScreen(
                        serviceId = effectiveServiceId,
                        serviceName = effectiveServiceName,
                        intervention = intervention,
                        onSubmitReview = { rating, comment, tags ->
                            if (intervention != null) {
                                onPostInterventionReview(
                                    intervention.id,
                                    effectiveServiceId,
                                    effectiveServiceName,
                                    rating,
                                    comment,
                                    tags
                                ) {
                                    showRatingScreen = false
                                    onDismiss()
                                }
                            } else {
                                showRatingScreen = false
                                onDismiss()
                            }
                        },
                        onSkip = {
                            showRatingScreen = false
                            onDismiss()
                        }
                    )
                }

                "TRACKING" -> {
                    val currentIntervention = activeIntervention ?: return@AnimatedContent
                    ActiveInterventionTracker(
                        intervention = currentIntervention,
                        onCompleteAndRate = {
                            showRatingScreen = true
                        },
                        onClose = onDismiss,
                        onCancel = onCancelActive
                    )
                }

                else -> {
                    // Form Mode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Demande de Dépannage Urgent 🚨",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (targetServiceName != null) {
                                    Text(
                                        text = "À destination de : $targetServiceName",
                                        fontSize = 13.sp,
                                        color = ZurikaAmber
                                    )
                                }
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(text = "Nature du problème :", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

                        Spacer(modifier = Modifier.height(10.dp))

                        val problemList = listOf(
                            ProblemType.PANNE_SECHE to ("Panne sèche ⛽"),
                            ProblemType.CREVAISON to ("Crevaison 🛞"),
                            ProblemType.BATTERIE to ("Batterie 🔋"),
                            ProblemType.MOTEUR to ("Panne Moteur ⚙️"),
                            ProblemType.REMORQUAGE to ("Remorquage 🚚"),
                            ProblemType.AUTRE to ("Autre ❓")
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            items(problemList) { (type, label) ->
                                val isSel = type == selectedType
                                Surface(
                                    onClick = { selectedType = type },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) ZurikaAmber else DarkSurface,
                                    border = BorderStroke(1.dp, if (isSel) ZurikaAmber else Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) DarkBackground else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // GPS Location Confirmation Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.GpsFixed, contentDescription = "GPS", tint = ZurikaStatusOpen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Position GPS transmise :", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = "Lomé Togo (Lat: %.4f, Lng: %.4f)".format(userLat, userLng),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Description ou marque du véhicule :", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = descText,
                            onValueChange = { descText = it },
                            placeholder = { Text("Ex: Toyota Corolla blanche, pneu avant gauche à plat près du carrefour.", color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("intervention_desc_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onSubmitRequest(selectedType, descText) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_intervention_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ZurikaStatusClosed, contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "ENVOYER L'ALERTE SOS", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveInterventionTracker(
    intervention: InterventionEntity,
    onCompleteAndRate: () -> Unit,
    onClose: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = ZurikaStatusOpen.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Checked", tint = ZurikaStatusOpen, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Intervention en Cours 🚨",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Professionnel assigné : ${intervention.serviceName ?: "Dépanneur Réseau ZURIKA"}",
            fontSize = 13.sp,
            color = ZurikaAmber,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Progress Steps Tracker
        TrackerStepRow(title = "Alerte reçue par la centrale ZURIKA", isDone = true)
        TrackerStepRow(title = "Dépanneur local affecté", isDone = true)
        TrackerStepRow(title = "En route vers votre position GPS", isDone = true)
        TrackerStepRow(title = "Intervention sur place & Résolution", isDone = true)

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Action: Finish & Rate
        Button(
            onClick = onCompleteAndRate,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("finish_and_rate_intervention_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ZurikaAmber, contentColor = DarkBackground)
        ) {
            Icon(imageVector = Icons.Default.Star, contentDescription = "Rate", tint = DarkBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Intervention terminée • Noter (1-5 ⭐)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) {
                Text(text = "Suivre sur la carte", color = Color.Gray, fontSize = 13.sp)
            }

            TextButton(onClick = onCancel) {
                Text(text = "Annuler l'alerte", color = ZurikaStatusClosed, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun PostInterventionRatingScreen(
    serviceId: String,
    serviceName: String,
    intervention: InterventionEntity?,
    onSubmitReview: (rating: Float, comment: String, tags: List<String>) -> Unit,
    onSkip: () -> Unit
) {
    var rating by remember { mutableStateOf(5.0f) }
    var comment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    var isSubmitted by remember { mutableStateOf(false) }

    if (isSubmitted) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = ZurikaStatusOpen.copy(alpha = 0.2f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = ZurikaStatusOpen,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Merci pour votre évaluation ! 🎉",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Votre note ($rating ⭐) et votre avis ont été enregistrés avec succès dans le sous-document 'reviews' de $serviceName sur Firestore.",
                fontSize = 13.sp,
                color = Color.LightGray,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("close_rating_confirmation_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZurikaPrimary)
            ) {
                Text(text = "Retour à la carte", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Évaluez l'Intervention ⭐",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Professionnel : $serviceName",
                        fontSize = 13.sp,
                        color = ZurikaAmber,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onSkip) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1-5 Star Interactive Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Attribuez une note globale (1 à 5 étoiles) :",
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                InteractiveStarRatingSelector(
                    rating = rating,
                    onRatingChange = { newRating -> rating = newRating },
                    starSize = 40.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                RatingAppraisalBadge(rating = rating)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Feedback Tags
            QuickFeedbackTagsSelector(
                selectedTags = selectedTags.toSet(),
                onTagToggle = { tag ->
                    if (selectedTags.contains(tag)) {
                        selectedTags.remove(tag)
                    } else {
                        selectedTags.add(tag)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Comment input
            Text(
                text = "Votre commentaire / avis détaillé :",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                placeholder = {
                    Text(
                        "Ex: Dépannage rapide en moins de 15 minutes, garagiste très courtois et efficace.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .testTag("post_intervention_comment_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Firestore Subdocument Badge
            FirestoreSubDocumentIndicator(serviceId = serviceId)

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Button(
                onClick = {
                    isSubmitted = true
                    onSubmitReview(rating, comment, selectedTags.toList())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_post_intervention_review_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZurikaPrimary, contentColor = Color.White)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "PUBLIER MON ÉVALUATION", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Passer cette étape", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TrackerStepRow(title: String, isDone: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = "Step",
            tint = if (isDone) ZurikaStatusOpen else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            color = if (isDone) Color.White else Color.Gray,
            fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
