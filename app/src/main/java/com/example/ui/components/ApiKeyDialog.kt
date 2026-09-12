package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.ApiKeyValidationState

@Composable
fun ApiKeyDialog(
    isOpen: Boolean,
    currentKey: String,
    validationState: ApiKeyValidationState = ApiKeyValidationState.Idle,
    onDismiss: () -> Unit,
    onValidateAndSave: (String) -> Unit,
    onForceSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var keyInput by remember(currentKey) { mutableStateOf(currentKey) }
    var showPassword by remember { mutableStateOf(false) }
    val isValidating = validationState is ApiKeyValidationState.Validating

    AlertDialog(
        onDismissRequest = { if (!isValidating) onDismiss() },
        modifier = modifier.testTag("api_key_dialog"),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Connect YouTube",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add your own YouTube API key to unlock full-length streaming instead of short previews.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                    },
                    label = { Text("API key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    enabled = !isValidating,
                    isError = validationState is ApiKeyValidationState.Invalid,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.testTag("toggle_key_visibility")
                        ) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide key" else "Show key"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Validation status - the actual point of this dialog: never let a
                // typo'd or dead key sit there looking "saved" while streams quietly fail.
                when (validationState) {
                    is ApiKeyValidationState.Validating -> {
                        StatusRow(
                            icon = null,
                            text = "Checking key with YouTube...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    is ApiKeyValidationState.Valid -> {
                        StatusRow(
                            icon = Icons.Default.CheckCircle,
                            text = "Key verified and saved",
                            color = MaterialTheme.colorScheme.primary,
                            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    }
                    is ApiKeyValidationState.Invalid -> {
                        StatusRow(
                            icon = Icons.Default.Error,
                            text = validationState.message,
                            color = MaterialTheme.colorScheme.error,
                            background = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                        )
                    }
                    ApiKeyValidationState.Idle -> {
                        if (currentKey.isNotBlank()) {
                            StatusRow(
                                icon = Icons.Default.CheckCircle,
                                text = "A key is currently saved",
                                color = MaterialTheme.colorScheme.primary,
                                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (validationState is ApiKeyValidationState.Invalid) {
                    TextButton(
                        onClick = { onValidateAndSave(keyInput) },
                        modifier = Modifier.testTag("retry_validate_btn")
                    ) {
                        Text("Retry")
                    }
                }
                Button(
                    onClick = {
                        if (validationState is ApiKeyValidationState.Invalid) {
                            onForceSave(keyInput.trim())
                        } else {
                            onValidateAndSave(keyInput.trim())
                        }
                    },
                    enabled = !isValidating,
                    modifier = Modifier.testTag("save_api_key_btn")
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (validationState is ApiKeyValidationState.Invalid) "Save anyway" else "Verify & Save")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentKey.isNotBlank()) {
                    TextButton(
                        onClick = {
                            keyInput = ""
                            onValidateAndSave("")
                        },
                        enabled = !isValidating,
                        modifier = Modifier.testTag("clear_api_key_btn")
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isValidating,
                    modifier = Modifier.testTag("cancel_api_key_btn")
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    text: String,
    color: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = color)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = color
        )
    }
}
