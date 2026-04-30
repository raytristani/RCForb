package com.rcforb.android.ui.radio

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.rcforb.android.models.RadioStateData
import com.rcforb.android.models.RemoteStation
import com.rcforb.android.models.ServerInfoData
import com.rcforb.android.protocol.CommandParser
import com.rcforb.android.services.ConnectionManagerViewModel
import com.rcforb.android.services.FavoriteStation
import com.rcforb.android.services.FavoritesStore
import com.rcforb.android.ui.components.*
import com.rcforb.android.ui.theme.AppColors
import com.rcforb.android.ui.theme.noRippleClickable

private val VFO_STEPS = listOf(
    ".01" to 10, ".10" to 100, "1.0" to 1000, "5.0" to 5000, "10" to 10000
)

private val btnDesc = mapOf(
    "TXd" to "Transmit", "Tune" to "Antenna Tune", "ATU" to "Auto Tuner",
    "NB" to "Noise Blanker", "NR" to "Noise Reduction", "ANF" to "Auto Notch",
    "MNF" to "Manual Notch", "PB Clr" to "Passband Clear", "Comp" to "Compression",
    "Tone" to "Sub Tone", "TSQL" to "Tone Squelch", "Test" to "Test Mode",
    "MOX" to "Manual TX", "AGC" to "Auto Gain", "VOX" to "Voice Operate",
    "BK" to "Break-in", "Lock" to "Dial Lock", "Split" to "Split Mode",
    "TX" to "Transmit", "RIT" to "RX Increment", "XIT" to "TX Increment",
)

@Composable
fun RadioScreen(vm: ConnectionManagerViewModel) {
    val rs by vm.radioStateData.collectAsState()
    val si by vm.serverInfoData.collectAsState()
    val connectedStation by vm.connectedStation.collectAsState()
    var vfoStep by remember { mutableIntStateOf(100) }
    var showChat by remember { mutableStateOf(false) }
    var isPTT by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.5f) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var isFavorite by remember(connectedStation) {
        mutableStateOf(connectedStation?.let { FavoritesStore.isFavorite(context, it.serverId) } ?: false)
    }
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.DarkPanel)) {
        // Top Bar
        TopBar(vm, si, showChat, volume, isFavorite,
            onToggleChat = { showChat = !showChat },
            onVolumeChange = { volume = it; vm.audioBridge.setVolume(it) },
            onToggleFavorite = {
                connectedStation?.let { station ->
                    if (isFavorite) {
                        FavoritesStore.removeFavorite(context, station.serverId)
                    } else {
                        FavoritesStore.addFavorite(context, FavoriteStation(
                            serverId = station.serverId,
                            serverName = station.serverName,
                            radioModel = station.radioModel,
                            description = station.description,
                            host = station.host,
                            port = station.port,
                            voipPort = station.voipPort,
                            isV7 = station.isV7
                        ))
                    }
                    isFavorite = !isFavorite
                }
            }
        )

        Row(modifier = Modifier.weight(1f)) {
            // Main content - single column that matches macOS layout
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rs?.let { radio ->
                    // LCD Hero - full width
                    LcdHero(radio, si, vm)

                    // Mode & Filters (left) + Controls with knobs (center+right)
                    Row(
                        modifier = Modifier.weight(0.75f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Left: Mode & Filters
                        Column(
                            modifier = Modifier
                                .width(150.dp)
                                .fillMaxHeight()
                        ) {
                            PanelView(title = "Mode & Filters") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    radio.dropdownOrder.filter { it.isNotEmpty() }.forEach { name ->
                                        val value = radio.dropdowns[name] ?: ""
                                        val opts = radio.dropdownLists[name] ?: emptyList()
                                        Column {
                                            Text(name, color = AppColors.LabelDim, fontSize = AppColors.sp9)
                                            MetalDropdown(value = value, options = opts) { selected ->
                                                vm.sendCommand(CommandParser.setDropdown(name, selected))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Center+Right: Controls panel
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            PanelView(title = "Controls") {
                                Column(modifier = Modifier.weight(1f)) {
                                    // Step selector - centered above knobs
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("STEP (kHz)", color = AppColors.LabelDim, fontSize = AppColors.sp10)
                                        Spacer(Modifier.width(4.dp))
                                        VFO_STEPS.forEach { (label, value) ->
                                            MetalButton(title = label, isOn = vfoStep == value,
                                                fontSize = AppColors.sp10, height = 20.dp) {
                                                vfoStep = value
                                            }
                                            Spacer(Modifier.width(2.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    // Buttons left + Knobs center + Buttons right
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left buttons column
                                        val half = (radio.buttonOrder.size + 1) / 2
                                        Column(
                                            modifier = Modifier.width(130.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            radio.buttonOrder.take(half).filter { it.isNotEmpty() }.forEach { name ->
                                                val on = (radio.buttons[name] ?: 0) != 0
                                                val desc = btnDesc[name] ?: ""
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    MetalButton(
                                                        title = name, isOn = on,
                                                        width = 48.dp, height = 20.dp,
                                                        fontSize = if (name.length > 5) AppColors.sp9 else AppColors.sp10
                                                    ) {
                                                        vm.sendCommand(CommandParser.setButton(name, if (on) "0" else "1"))
                                                    }
                                                    Text(desc, color = AppColors.LabelMuted, fontSize = AppColors.sp9,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }

                                        // Center: VFO Knobs SIDE BY SIDE
                                        Row(
                                            modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                VFOKnobView(
                                                    size = 140,
                                                    vfo = "A",
                                                    step = vfoStep,
                                                    frequency = radio.frequencyA
                                                ) { hz ->
                                                    vm.sendCommand(CommandParser.setFrequencyA(hz.toString()))
                                                }
                                                Text("VFO A", color = AppColors.MutedForeground,
                                                    fontSize = AppColors.sp11, fontWeight = FontWeight.Bold)
                                            }

                                            if (radio.frequencyB > 0) {
                                                Spacer(Modifier.width(16.dp))
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    VFOKnobView(
                                                        size = 110,
                                                        vfo = "B",
                                                        step = vfoStep,
                                                        frequency = radio.frequencyB
                                                    ) { hz ->
                                                        vm.sendCommand(CommandParser.setFrequencyB(hz.toString()))
                                                    }
                                                    Text("VFO B", color = AppColors.MutedForeground,
                                                        fontSize = AppColors.sp11)
                                                }
                                            }
                                        } // Row (knobs)

                                        // Right buttons column
                                        Column(
                                            modifier = Modifier.width(130.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            radio.buttonOrder.drop(half).filter { it.isNotEmpty() }.forEach { name ->
                                                val on = (radio.buttons[name] ?: 0) != 0
                                                val desc = btnDesc[name] ?: ""
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(desc, color = AppColors.LabelMuted, fontSize = AppColors.sp9,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    MetalButton(
                                                        title = name, isOn = on,
                                                        width = 48.dp, height = 20.dp,
                                                        fontSize = if (name.length > 5) AppColors.sp9 else AppColors.sp10
                                                    ) {
                                                        vm.sendCommand(CommandParser.setButton(name, if (on) "0" else "1"))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Adjustments - compact inline sliders across full width
                    if (radio.sliderOrder.isNotEmpty()) {
                        CompactSlidersPanel(radio, vm)
                    }

                    // Status/Messages
                    if (radio.messageOrder.isNotEmpty()) {
                        CompactMessagesPanel(radio)
                    }

                    // Request Tune + Mic Test + PTT
                    var micTestResult by remember { mutableStateOf<String?>(null) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.MetalDarkTop)
                                .border(1.dp, AppColors.MetalDarkBorder, RoundedCornerShape(10.dp))
                                .noRippleClickable {
                                    vm.sendCommand(CommandParser.chatMessage("May I tune the remote?"))
                                    showChat = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Request Tune", color = AppColors.Cream, fontSize = AppColors.sp11, lineHeight = AppColors.sp11)
                                if (si?.radioOpen == true) {
                                    Text("(Again)", color = AppColors.MutedForeground, fontSize = AppColors.sp9, lineHeight = AppColors.sp9)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (micTestResult == null) AppColors.MetalDarkTop else if (micTestResult == "OK") AppColors.LedGreen.copy(alpha = 0.3f) else AppColors.LedRed.copy(alpha = 0.3f))
                                .border(1.dp, AppColors.MetalDarkBorder, RoundedCornerShape(10.dp))
                                .noRippleClickable {
                                    if (!hasMicPermission) {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        return@noRippleClickable
                                    }
                                    micTestResult = "..."
                                    vm.audioBridge.micTest { success ->
                                        micTestResult = if (success) "OK" else "FAIL"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (micTestResult == "...") "Testing..." else if (micTestResult != null) "Mic: $micTestResult" else "Mic Test",
                                color = AppColors.Cream,
                                fontSize = AppColors.sp10,
                                lineHeight = AppColors.sp10
                            )
                        }
                        PTTButton(isPTT, modifier = Modifier.weight(1f)) { on ->
                            if (on && !hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@PTTButton
                            }
                            isPTT = on
                            vm.sendPTT(on)
                        }
                    }
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading...", color = AppColors.Cream)
                    }
                }
            }

            // Chat sidebar
            if (showChat) {
                ChatSidebar(vm)
            }
        }
    }
}

@Composable
private fun TopBar(
    vm: ConnectionManagerViewModel,
    si: ServerInfoData?,
    showChat: Boolean,
    volume: Float,
    isFavorite: Boolean,
    onToggleChat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(AppColors.PanelBgBottom)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MetalButton(title = "Disconnect", isOn = false, fontSize = AppColors.sp11, height = 20.dp) { vm.disconnect() }
        MetalButton(title = "Reset", isOn = false, fontSize = AppColors.sp11, height = 20.dp) { vm.clearSliderOverrides() }

        MetalButton(title = if (isFavorite) "Saved Station" else "Save Station", isOn = isFavorite, fontSize = AppColors.sp11, height = 20.dp) { onToggleFavorite() }

        Text("Vol", color = AppColors.MutedForeground, fontSize = AppColors.sp10)
        CompactSlider(
            value = volume,
            min = 0f,
            max = 1f,
            onValueChange = onVolumeChange,
            modifier = Modifier.width(80.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        val ledColor = when {
            si?.radioOpen == true -> AppColors.LedGreen
            si?.radioInUse == true -> AppColors.LedRed
            else -> AppColors.MutedForeground.copy(alpha = 0.6f)
        }
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ledColor))
        Text(
            text = when {
                si?.radioOpen == true -> "Open"
                si?.radioInUse == true -> "In Use"
                else -> "Closed"
            },
            color = AppColors.MutedForeground, fontSize = AppColors.sp10
        )
        Text(si?.serverTime ?: "", color = AppColors.MutedForeground.copy(alpha = 0.6f), fontSize = AppColors.sp10)
        MetalButton(title = "Chat", isOn = showChat, fontSize = AppColors.sp11, height = 20.dp) { onToggleChat() }
    }
}

@Composable
private fun LcdHero(rs: RadioStateData, si: ServerInfoData?, vm: ConnectionManagerViewModel) {
    val stationName by vm.connectedStationName.collectAsState()
    val lcdShape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(lcdShape)
            .background(Color(0xFFE8D888))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOT: ${si?.tot ?: 180}s", color = Color(0xFF887744) /* LCD label */, fontSize = AppColors.sp9)
            Text(rs.smeterALabel, color = Color(0xFF887744) /* LCD label */, fontSize = AppColors.sp9)
        }
        SMeterView(value = rs.smeterA, label = rs.smeterALabel)
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            FrequencyDisplay(
                frequency = rs.frequencyA, vfo = "A", large = true,
                onSet = { hz -> vm.sendCommand(CommandParser.setFrequencyA(hz.toString())) }
            )
            if (rs.frequencyB > 0) {
                Spacer(Modifier.width(12.dp))
                FrequencyDisplay(
                    frequency = rs.frequencyB, vfo = "B", large = false,
                    onSet = { hz -> vm.sendCommand(CommandParser.setFrequencyB(hz.toString())) },
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            // Station name marquee - after frequencies, reduced width
            if (stationName.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .padding(start = 24.dp, bottom = 4.dp)
                ) {
                    MarqueeText(
                        text = stationName,
                        color = Color(0xFF553300),
                        fontSize = AppColors.sp24
                    )
                }
            }
        }
        if (rs.statusOrder.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            StatusPillsView(statuses = rs.statuses, order = rs.statusOrder)
        }
    }
}

@Composable
private fun CompactSlidersPanel(rs: RadioStateData, vm: ConnectionManagerViewModel) {
    val sliderOverrides by vm.sliderOverrides.collectAsState()
    val sliders = rs.sliderOrder.filter { it.isNotEmpty() }
    val cols = 8 // fixed column count

    PanelView(title = "Adjustments") {
        val chunked = sliders.chunked(cols)
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until cols) {
                        val name = row.getOrNull(i)
                        if (name != null) {
                            val v = rs.sliders[name] ?: 0.0
                            val raw = rs.sliderRanges[name]
                            val rMin = (raw?.min ?: 0.0).toFloat()
                            val rMax = if ((raw?.max ?: 0.0) > (raw?.min ?: 0.0))
                                raw!!.max.toFloat() else rMin + 100f
                            val displayValue = sliderOverrides[name]?.toFloat() ?: v.toFloat()

                            SliderCell(
                                name = name,
                                value = displayValue,
                                min = rMin,
                                max = rMax,
                                modifier = Modifier.weight(1f)
                            ) { newVal ->
                                vm.setSliderOverride(name, newVal.toDouble())
                                vm.sendCommand(CommandParser.setSlider(name, newVal.toInt().toString()))
                            }
                        } else {
                            // Empty spacer to maintain grid alignment
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderCell(
    name: String,
    value: Float,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Value badge - fills full row height, text centered
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.MetalDarkTop),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${value.toInt()}",
                color = AppColors.Cream,
                fontSize = AppColors.sp9,
                lineHeight = AppColors.sp9,
                textAlign = TextAlign.Center
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = AppColors.MutedForeground, fontSize = AppColors.sp9,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                lineHeight = AppColors.sp10)
            CompactSlider(
                value = value.coerceIn(min, max),
                min = min,
                max = max,
                onValueChange = onValueChange
            )
        }
    }
}

@Composable
private fun CompactMessagesPanel(rs: RadioStateData) {
    PanelView(title = "Status") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rs.messageOrder.filter { it.isNotEmpty() }.forEach { name ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = rs.messages[name] ?: "",
                        color = AppColors.Cream,
                        fontSize = AppColors.sp10,
                        maxLines = 1,
                        modifier = Modifier
                            .widthIn(min = 40.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.MetalDarkTop)
                            .padding(horizontal = 4.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                    )
                    Text(name, color = AppColors.MutedForeground, fontSize = AppColors.sp9)
                }
            }
        }
    }
}

@Composable
private fun PTTButton(isPTT: Boolean, modifier: Modifier = Modifier, onPTT: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val bgColor = if (isPTT) Color(0xFFCC3322) else Color(0xFF7A2222)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(bgColor)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPTT(true)
                    waitForUpOrCancellation()
                    onPTT(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PUSH TO TALK",
            color = if (isPTT) Color.White else AppColors.Cream,
            fontSize = AppColors.sp18,
            fontWeight = FontWeight.Bold,
            letterSpacing = AppColors.sp18 * 0.1f
        )
    }
}

@Composable
private fun ChatSidebar(vm: ConnectionManagerViewModel) {
    val chatMessages by vm.chatMessages.collectAsState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(AppColors.ChatBg)
            .imePadding()
    ) {
        Text(
            text = "Chat",
            color = AppColors.Cream,
            fontSize = AppColors.sp12,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.ChassisGradientTo)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )

        val scrollState = rememberScrollState()
        LaunchedEffect(chatMessages.size) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    chatMessages.forEach { msg ->
                        if (msg.isSystem) {
                            Text(msg.text, color = AppColors.MutedForeground, fontSize = AppColors.sp11)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("${msg.user}:", color = AppColors.Cream, fontSize = AppColors.sp11, fontWeight = FontWeight.Bold)
                                Text(msg.text, color = AppColors.CreamDark, fontSize = AppColors.sp11)
                            }
                        }
                    }
                    if (chatMessages.isEmpty()) {
                        Text("No messages yet", color = AppColors.MutedForeground.copy(alpha = 0.6f), fontSize = AppColors.sp11)
                    }
                }
            }
        }

        var input by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.InputBg)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (input.isEmpty()) {
                    Text("Message...", color = AppColors.LabelDim, fontSize = AppColors.sp12)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = AppColors.Cream,
                        fontSize = AppColors.sp12
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Cream),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            MetalButton(title = "Send", isOn = false, height = 36.dp, fontSize = AppColors.sp11) {
                val text = input.trim()
                if (text.isNotEmpty()) {
                    vm.sendCommand(CommandParser.chatMessage(text))
                    input = ""
                }
                focusManager.clearFocus()
            }
        }
    }
}

@Composable
private fun MarqueeText(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Always animate like a stock ticker
    LaunchedEffect(text) {
        delay(1500)
        while (true) {
            scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = tween(
                    durationMillis = (scrollState.maxValue * 20).coerceAtLeast(2000),
                    easing = LinearEasing
                )
            )
            delay(1500)
            scrollState.animateScrollTo(
                0,
                animationSpec = tween(
                    durationMillis = (scrollState.maxValue * 20).coerceAtLeast(2000),
                    easing = LinearEasing
                )
            )
            delay(1500)
        }
    }

    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontFamily = Digital7MonoFamily,
            lineHeight = fontSize,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(scrollState)
        )
    }
}
