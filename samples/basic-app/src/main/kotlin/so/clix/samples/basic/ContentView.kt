package so.clix.samples.basic

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import so.clix.core.Clix

@Composable
fun ContentView() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val deviceId by AppState.deviceId.collectAsState()
    val fcmToken by AppState.fcmToken.collectAsState()

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            val newDeviceId = Clix.getDeviceId()
            val newFcmToken = Clix.Notification.getToken()
            AppState.updateDeviceId(newDeviceId)
            AppState.updateFCMToken(newFcmToken)
        }
    }

    var userIdInput by remember {
        val storedUserId = BasicApplication.sharedPreferences.getString("user_id", "") ?: ""
        mutableStateOf(storedUserId)
    }
    var userPropertyKeyInput by remember { mutableStateOf("") }
    var userPropertyValueInput by remember { mutableStateOf("") }
    var eventNameInput by remember { mutableStateOf("test") }
    var eventParamsInput by remember {
        mutableStateOf(
            """
            {
              "string": "string",
              "number": 1.5,
              "boolean": true,
              "object": { "key": "value" }
            }
            """.trimIndent(),
        )
    }

    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 32.dp),
            contentScale = ContentScale.Fit,
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            tonalElevation = 2.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                InfoTextRow(
                    label = stringResource(R.string.clix_project_id),
                    value = ClixConfiguration.config.projectId,
                )
                InfoTextRow(
                    label = stringResource(R.string.api_key),
                    value = ClixConfiguration.config.apiKey,
                )
                InfoTextRow(
                    label = stringResource(R.string.device_id),
                    value = deviceId,
                )
                InfoTextRow(
                    label = stringResource(R.string.fcm_token),
                    value = fcmToken,
                    lastItem = true,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // User ID Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.user_id),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    OutlinedTextField(
                        value = userIdInput,
                        onValueChange = { userIdInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f,
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f,
                            ),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (userIdInput.isNotBlank()) {
                                coroutineScope.launch {
                                    Clix.setUserId(userIdInput)
                                    BasicApplication.sharedPreferences.edit()
                                        .putString("user_id", userIdInput).apply()
                                    alertMessage = "User ID set!"
                                    showAlert = true
                                }
                            } else {
                                alertMessage = "Please enter a User ID"
                                showAlert = true
                            }
                        },
                        modifier = Modifier.heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.buttonBackground,
                            contentColor = AppTheme.buttonText,
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                        ),
                    ) {
                        Text(
                            stringResource(R.string.submit),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = AppTheme.buttonText,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // User Property Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.user_property_key),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    OutlinedTextField(
                        value = userPropertyKeyInput,
                        onValueChange = { userPropertyKeyInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f,
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f,
                            ),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.user_property_value),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    OutlinedTextField(
                        value = userPropertyValueInput,
                        onValueChange = { userPropertyValueInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f,
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f,
                            ),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (userPropertyKeyInput.isNotBlank() && userPropertyValueInput.isNotBlank()) {
                                coroutineScope.launch {
                                    Clix.setUserProperty(
                                        userPropertyKeyInput,
                                        userPropertyValueInput,
                                    )
                                    alertMessage =
                                        "User property '$userPropertyKeyInput: $userPropertyValueInput' set successfully"
                                    showAlert = true

                                    userPropertyKeyInput = ""
                                    userPropertyValueInput = ""
                                }
                            } else {
                                alertMessage = "Please enter both key and value for user property"
                                showAlert = true
                            }
                        },
                        modifier = Modifier.heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.buttonBackground,
                            contentColor = AppTheme.buttonText,
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                        ),
                    ) {
                        Text(
                            stringResource(R.string.set_user_property),
                            style = MaterialTheme.typography.labelLarge.copy(color = AppTheme.buttonText),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Track Event Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.event_name),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    OutlinedTextField(
                        value = eventNameInput,
                        onValueChange = { eventNameInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f,
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f,
                            ),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.event_params),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    OutlinedTextField(
                        value = eventParamsInput,
                        onValueChange = { eventParamsInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f,
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f,
                            ),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                        minLines = 6,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (eventNameInput.isNotBlank()) {
                                try {
                                    var properties: Map<String, Any?> = emptyMap()
                                    if (eventParamsInput.isNotBlank() && eventParamsInput != "{}") {
                                        val jsonObject = JSONObject(eventParamsInput)
                                        val map = mutableMapOf<String, Any?>()
                                        val keys = jsonObject.keys()
                                        while (keys.hasNext()) {
                                            val key = keys.next()
                                            map[key] = jsonObject.get(key)
                                        }
                                        properties = map
                                    }

                                    coroutineScope.launch {
                                        Clix.trackEvent(eventNameInput, properties)
                                        alertMessage = "Event tracked: $eventNameInput"
                                        showAlert = true
                                    }
                                } catch (e: Exception) {
                                    alertMessage = "Invalid JSON format"
                                    showAlert = true
                                }
                            } else {
                                alertMessage = "Please enter an event name"
                                showAlert = true
                            }
                        },
                        modifier = Modifier.heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.buttonBackground,
                            contentColor = AppTheme.buttonText,
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                        ),
                    ) {
                        Text(
                            stringResource(R.string.track_event),
                            style = MaterialTheme.typography.labelLarge.copy(color = AppTheme.buttonText),
                        )
                    }
                }
            }
        }
    }

    // Alert Dialog (mirrors iOS Alert)
    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = { Text(alertMessage) },
            confirmButton = {
                TextButton(onClick = { showAlert = false }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
fun InfoTextRow(label: String, value: String, lastItem: Boolean = false) {
    Text(
        text = "$label: $value",
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (lastItem) 0.dp else 12.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}
