package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun ProSpaceScreen(
    services: List<ServiceEntity>,
    interventions: List<InterventionEntity>,
    onUpdateStatus: (String, ServiceStatus) -> Unit,
    onBack: () -> Unit
) {
    // Pro users manage their listed services
    var selectedServiceId by remember(services) { mutableStateOf(services.firstOrNull()?.id ?: "") }
    val currentProService = services.find { it.id == selectedServiceId } ?: services.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("pro_space_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Espace Professionnel 🛠️", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Gérez votre visibilité et votre disponibilité en temps réel", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (currentProService != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(text = "Établissement sélectionné :", fontSize = 12.sp, color = Color.Gray)
                        Text(text = currentProService.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = currentProService.address, fontSize = 13.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Modifier votre statut actuel :", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusToggleChip(
                                label = "🟢 Ouvert",
                                isSelected = currentProService.status == ServiceStatus.OPEN,
                                onClick = { onUpdateStatus(currentProService.id, ServiceStatus.OPEN) },
                                activeColor = ZurikaStatusOpen
                            )
                            StatusToggleChip(
                                label = "🟠 Occupé",
                                isSelected = currentProService.status == ServiceStatus.BUSY,
                                onClick = { onUpdateStatus(currentProService.id, ServiceStatus.BUSY) },
                                activeColor = ZurikaStatusBusy
                            )
                            StatusToggleChip(
                                label = "🔴 Fermé",
                                isSelected = currentProService.status == ServiceStatus.CLOSED,
                                onClick = { onUpdateStatus(currentProService.id, ServiceStatus.CLOSED) },
                                activeColor = ZurikaStatusClosed
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = ZurikaPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mis à jour instantanément sur la carte via Firestore",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Demandes d'intervention reçues (${interventions.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Spacer(modifier = Modifier.height(12.dp))

                if (interventions.isEmpty()) {
                    Text(text = "Aucune alerte reçue actuellement.", color = Color.Gray, fontSize = 14.sp)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(interventions, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "🚨 ${item.problemType.name}", fontWeight = FontWeight.Bold, color = ZurikaAmber, fontSize = 15.sp)
                                        Text(text = item.status.name, fontSize = 12.sp, color = ZurikaPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Client: ${item.userName} (${item.userPhone})", color = Color.White, fontSize = 13.sp)
                                    Text(text = item.description, color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatusToggleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) activeColor else DarkSurfaceVariant,
            contentColor = if (isSelected) DarkBackground else Color.White
        )
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AdminDashboardScreen(
    services: List<ServiceEntity>,
    onAddNewService: (String, ServiceCategory, ServiceStatus, String, String, Double, Double, String) -> Unit,
    onDeleteService: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("admin_dashboard_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Tableau de Bord Admin 👑", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Gestion globale des points d'eau, stations & dépanneurs", fontSize = 12.sp, color = Color.Gray)
                }

                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = ZurikaPrimary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("add_service_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Service")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Metrics overview row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard("Total Services", "${services.size}", ZurikaPrimary, Modifier.weight(1f))
                MetricCard("Ouverts", "${services.count { it.status == ServiceStatus.OPEN }}", ZurikaStatusOpen, Modifier.weight(1f))
                MetricCard("Fermés", "${services.count { it.status == ServiceStatus.CLOSED }}", ZurikaStatusClosed, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "Tous les établissements référencés", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(services, key = { it.id }) { service ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = service.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Text(text = "${service.category.name} • ${service.address}", fontSize = 12.sp, color = Color.Gray)
                            }

                            IconButton(onClick = { onDeleteService(service.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ZurikaStatusClosed)
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddServiceDialog(
                onAdd = { name, cat, status, address, phone, lat, lng, tags ->
                    onAddNewService(name, cat, status, address, phone, lat, lng, tags)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 11.sp, color = Color.Gray)
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun AddServiceDialog(
    onAdd: (String, ServiceCategory, ServiceStatus, String, String, Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ServiceCategory.STATION) }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+228 ") }
    var latText by remember { mutableStateOf("6.1450") }
    var lngText by remember { mutableStateOf("1.2150") }
    var tags by remember { mutableStateOf("Service 24h, Vidange, Shop") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un nouvel établissement", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de l'établissement") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adresse à Lomé") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            name, category, ServiceStatus.OPEN, address, phone,
                            latText.toDoubleOrNull() ?: 6.1450,
                            lngText.toDoubleOrNull() ?: 1.2150,
                            tags
                        )
                    }
                }
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
