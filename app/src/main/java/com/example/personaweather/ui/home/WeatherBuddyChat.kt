package com.example.personaweather.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.personaweather.ui.localization.LocalAppStrings
import com.example.personaweather.ui.theme.PersonaWeatherPalette

private data class WeatherBuddyMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val isTyping: Boolean = false,
)

@Composable
fun WeatherBuddyFloatingEntry(state: HomeUiState) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    var savedX by rememberSaveable { mutableIntStateOf(-1) }
    var savedY by rememberSaveable { mutableIntStateOf(-1) }
    val strings = LocalAppStrings.current

    BoxWithConstraints(Modifier.fillMaxSize().navigationBarsPadding()) {
        val density = LocalDensity.current
        val entryWidthPx = with(density) { 176.dp.roundToPx() }
        val entryHeightPx = with(density) { 56.dp.roundToPx() }
        val maxX = (constraints.maxWidth - entryWidthPx).coerceAtLeast(0)
        val maxY = (constraints.maxHeight - entryHeightPx).coerceAtLeast(0)
        val x = if (savedX < 0) maxX else savedX.coerceIn(0, maxX)
        val y = if (savedY < 0) maxY else savedY.coerceIn(0, maxY)

        FloatingActionButton(
            onClick = { isOpen = true },
            modifier = Modifier
                .offset { IntOffset(x, y) }
                .width(176.dp)
                .height(56.dp)
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragEnd = {
                            val currentX = if (savedX < 0) maxX else savedX.coerceIn(0, maxX)
                            val currentY = if (savedY < 0) maxY else savedY.coerceIn(0, maxY)
                            savedX = if (currentX + entryWidthPx / 2 < maxX / 2) 0 else maxX
                            savedY = currentY
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        savedX = ((if (savedX < 0) maxX else savedX) + dragAmount.x.roundToInt()).coerceIn(0, maxX)
                        savedY = ((if (savedY < 0) maxY else savedY) + dragAmount.y.roundToInt()).coerceIn(0, maxY)
                    }
                }
                .semantics {
                    contentDescription = strings.text("weather_buddy_open", "Open WeatherBuddy chat")
                },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color(0xFFB9E8F5),
            contentColor = Color(0xFF09283A),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.WbSunny, contentDescription = null, modifier = Modifier.size(24.dp))
                Text(
                    strings.text("weather_buddy_entry", "Ask about weather"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isOpen) {
            WeatherBuddyChat(state = state, onDismiss = { isOpen = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherBuddyChat(state: HomeUiState, onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val messages = remember(strings.languageCode) {
        mutableStateListOf(
            WeatherBuddyMessage(
                id = 0L,
                text = strings.format(
                    "weather_buddy_intro",
                    "Hi! I'm your local forecast guide for %1\$s. What would you like to know?",
                    state.location.name,
                ),
                isUser = false,
            ),
        )
    }
    var draft by rememberSaveable(strings.languageCode) { mutableStateOf("") }
    var nextMessageId by remember(strings.languageCode) { mutableLongStateOf(1L) }
    var isReplying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun sendMessage(rawText: String) {
        val question = rawText.trim()
        if (question.isEmpty() || isReplying) return
        isReplying = true
        messages += WeatherBuddyMessage(nextMessageId++, question, true)
        messages += WeatherBuddyMessage(nextMessageId++, "", false, isTyping = true)
        val replyIndex = messages.lastIndex
        draft = ""
        keyboardController?.hide()
        scope.launch {
            delay(320)
            val reply = WeatherBuddyResponder.reply(question, state, strings)
            reply.chunked(2).forEach { chunk ->
                messages[replyIndex] = messages[replyIndex].copy(
                    text = messages[replyIndex].text + chunk,
                    isTyping = false,
                )
                delay(22)
            }
            if (reply.isEmpty()) messages[replyIndex] = messages[replyIndex].copy(isTyping = false)
            isReplying = false
        }
    }

    fun choosePrompt(prompt: String) {
        if (isReplying) return
        scope.launch {
            isReplying = true
            draft = ""
            prompt.forEachIndexed { index, _ ->
                draft = prompt.substring(0, index + 1)
                delay(18)
            }
            delay(180)
            isReplying = false
            sendMessage(prompt)
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = PersonaWeatherPalette.trip.copy(alpha = 0.18f),
                ) {
                    Icon(
                        Icons.Outlined.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = PersonaWeatherPalette.trip,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        strings.text("weather_buddy_title", "WeatherBuddy"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        strings.text("weather_buddy_subtitle", "Answers use the forecast available on this device."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).semantics {
                        contentDescription = strings.text("weather_buddy_close", "Close chat")
                    },
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(messages, key = { _, message -> message.id }) { _, message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(if (message.isUser) 0.84f else 0.9f)
                                .then(
                                    if (message.isUser) Modifier
                                    else Modifier.border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(20.dp),
                                    ),
                                ),
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomEnd = if (message.isUser) 6.dp else 20.dp,
                                bottomStart = if (message.isUser) 20.dp else 6.dp,
                            ),
                            color = if (message.isUser) Color(0xFFB9E8F5)
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 5 },
                            ) {
                                Text(
                                    text = if (message.isTyping && message.text.isEmpty()) "•••" else message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (message.isUser) Color(0xFF09283A) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .semantics {
                                            if (message.isTyping && message.text.isEmpty()) {
                                                contentDescription = strings.text("weather_buddy_typing", "Weather guide is typing")
                                            }
                                        },
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    strings.text("weather_buddy_suggestions", "TRY ASKING"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val prompts = listOf(
                        strings.text("weather_buddy_prompt_current", "What's the weather now?"),
                        strings.text("weather_buddy_prompt_rain", "Will it rain soon?"),
                        strings.text("weather_buddy_prompt_air", "How is air quality?"),
                        strings.text("weather_buddy_prompt_trip", "Can I go out tomorrow?"),
                        strings.text("weather_buddy_prompt_location", "Where can I change location?"),
                    )
                    prompts.forEach { prompt ->
                        FilterChip(
                            selected = false,
                            onClick = { choosePrompt(prompt) },
                            enabled = !isReplying,
                            modifier = Modifier.heightIn(min = 44.dp),
                            label = { Text(prompt) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    enabled = !isReplying,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    placeholder = {
                        Text(strings.text("weather_buddy_input", "Type a question"))
                    },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage(draft) }),
                )
                IconButton(
                    onClick = { sendMessage(draft) },
                    enabled = draft.isNotBlank() && !isReplying,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (draft.isNotBlank() && !isReplying) PersonaWeatherPalette.trip else Color.White.copy(alpha = 0.08f),
                            CircleShape,
                        )
                        .semantics {
                            contentDescription = strings.text("weather_buddy_send", "Send message")
                        },
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        tint = if (draft.isNotBlank() && !isReplying) Color(0xFF09283A) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
