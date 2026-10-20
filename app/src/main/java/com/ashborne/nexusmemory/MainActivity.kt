package com.ashborne.nexusmemory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashborne.nexusmemory.ui.CameraQrScanner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                var showScanner by remember { mutableStateOf(false) }
                var syncStatus by remember { mutableStateOf("Prêt") }

                if (showScanner) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraQrScanner(modifier = Modifier.fillMaxSize()) { scannedData ->
                            syncStatus = "QR Scanné : $scannedData"
                            showScanner = false
                        }
                        Button(
                            onClick = { showScanner = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Text("Fermer Caméra")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "NexusMemory v6.8-Alpha",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { syncStatus = "Exportation .nexus effectuée" },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Exporter (.nexus)")
                            }
                            Button(
                                onClick = { syncStatus = "Importation .nexus réussie" },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Importer (.nexus)")
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { syncStatus = "Synchronisation WebDAV en cours..." },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Cloud WebDAV")
                            }
                            Button(
                                onClick = { showScanner = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Scanner QR (P2P)")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Statut : $syncStatus", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
