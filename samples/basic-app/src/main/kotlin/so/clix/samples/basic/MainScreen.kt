package so.clix.samples.basic

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject
import so.clix.core.Clix

@Composable
fun MainScreen() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Initialize userIdInput with the stored user ID if available
    var userIdInput by remember {
        val storedUserId = BasicApplication.userPreferences.getUserId() ?: ""
        mutableStateOf(TextFieldValue(storedUserId))
    }
    var userPropertyKeyInput by remember { mutableStateOf(TextFieldValue("")) }
    var userPropertyValueInput by remember { mutableStateOf(TextFieldValue("")) }
    var eventNameInput by remember { mutableStateOf(TextFieldValue("")) }
    var eventParamsInput by remember { mutableStateOf(TextFieldValue("{}")) }

    val projectIdText = "N/A"
    val apiKeyText = "N/A"
    val deviceIdText = try { Clix.getDeviceId() } catch (e: Exception) { "N/A" }
    val fcmTokenText = try { Clix.getToken() ?: "N/A" } catch (e: Exception) { "N/A" }
    val currentUserIdText = BasicApplication.userPreferences.getUserId() ?: "N/A"

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier.size(120.dp).padding(bottom = 32.dp),
            contentScale = ContentScale.Fit,
        )

        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            tonalElevation = 2.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                InfoTextRow(label = stringResource(R.string.clix_project_id), value = projectIdText)
                InfoTextRow(label = stringResource(R.string.api_key), value = apiKeyText)
                InfoTextRow(label = stringResource(R.string.device_id), value = deviceIdText)
                InfoTextRow(label = stringResource(R.string.fcm_token), value = fcmTokenText)
                InfoTextRow(
                    label = "User ID",
                    value = currentUserIdText,
                    lastItem = true,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = userIdInput,
                        onValueChange = { userIdInput = it },
                        label = { Text(stringResource(R.string.user_id)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary,
                            ),
                        maxLines = 3,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                val userId = userIdInput.text
                                if (userId.isNotBlank()) {
                                    coroutineScope.launch {
                                        // Set the user ID in Clix SDK
                                        Clix.setUserId(userId)
                                        // Store the user ID in SharedPreferences
                                        BasicApplication.userPreferences.saveUserId(userId)
                                        Toast.makeText(context, "User ID set!", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                } else {
                                    Toast.makeText(
                                            context,
                                            "Please enter a User ID",
                                            Toast.LENGTH_SHORT,
                                        )
                                        .show()
                                }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEBEBF5).copy(alpha = 0.9f),
                                    contentColor = Color.Black,
                                ),
                            elevation =
                                ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp,
                                ),
                        ) {
                            Text(
                                stringResource(R.string.submit),
                                style = MaterialTheme.typography.labelLarge.copy(color = Color.Black),
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Add a small button to remove the user ID
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    // Remove the user ID from SharedPreferences
                                    BasicApplication.userPreferences.removeUserId()
                                    // Clear the input field
                                    userIdInput = TextFieldValue("")
                                    Toast.makeText(context, "User ID removed!", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6B6B).copy(alpha = 0.9f),
                                    contentColor = Color.White,
                                ),
                            elevation =
                                ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp,
                                ),
                        ) {
                            Text(
                                "Remove",
                                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "User Property",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = userPropertyKeyInput,
                        onValueChange = { userPropertyKeyInput = it },
                        label = { Text("Property Key") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                        maxLines = 1,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = userPropertyValueInput,
                        onValueChange = { userPropertyValueInput = it },
                        label = { Text("Property Value") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                        maxLines = 1,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val key = userPropertyKeyInput.text
                            val value = userPropertyValueInput.text

                            if (key.isNotBlank() && value.isNotBlank()) {
                                coroutineScope.launch {
                                    Clix.setUserProperty(key, value)
                                    Toast.makeText(context, "User property set: $key = $value", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please enter both key and value", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End).heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEBEBF5).copy(alpha = 0.9f),
                            contentColor = Color.Black,
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                        ),
                    ) {
                        Text(
                            "Submit",
                            style = MaterialTheme.typography.labelLarge.copy(color = Color.Black),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Track Event",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = eventNameInput,
                        onValueChange = { eventNameInput = it },
                        label = { Text("Event Name") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                        maxLines = 1,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = eventParamsInput,
                        onValueChange = { eventParamsInput = it },
                        label = { Text("Event Parameters (JSON)") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                        maxLines = 5,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val eventName = eventNameInput.text
                            val eventParams = eventParamsInput.text

                            if (eventName.isNotBlank()) {
                                try {
                                    val jsonParams = if (eventParams.isNotBlank() && eventParams != "{}") {
                                        val jsonObject = JSONObject(eventParams)
                                        val map = mutableMapOf<String, Any?>()
                                        val keys = jsonObject.keys()
                                        while (keys.hasNext()) {
                                            val key = keys.next()
                                            map[key] = jsonObject.get(key)
                                        }
                                        map
                                    } else {
                                        emptyMap<String, Any?>()
                                    }

                                    coroutineScope.launch {
                                        Clix.trackEvent(eventName, jsonParams)
                                        Toast.makeText(context, "Event tracked: $eventName", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid JSON format", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please enter an event name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End).heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEBEBF5).copy(alpha = 0.9f),
                            contentColor = Color.Black,
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                        ),
                    ) {
                        Text(
                            "Trigger",
                            style = MaterialTheme.typography.labelLarge.copy(color = Color.Black),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoTextRow(label: String, value: String, lastItem: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = if (lastItem) 0.dp else 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
