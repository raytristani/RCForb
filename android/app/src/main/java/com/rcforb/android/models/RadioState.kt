package com.rcforb.android.models

import com.rcforb.android.protocol.CommandParser

class RadioState {
    var frequencyA: Int = 0
    var frequencyB: Int = 0
    var smeterA: Double = 0.0
    var smeterALabel: String = ""
    var smeterB: Double = 0.0
    var smeterBLabel: String = ""
    val buttons: MutableMap<String, Int> = mutableMapOf()
    val buttonOrder: MutableList<String> = mutableListOf()
    val dropdowns: MutableMap<String, String> = mutableMapOf()
    val dropdownLists: MutableMap<String, MutableList<String>> = mutableMapOf()
    val dropdownOrder: MutableList<String> = mutableListOf()
    val sliders: MutableMap<String, Double> = mutableMapOf()
    val sliderRanges: MutableMap<String, SliderRange> = mutableMapOf()
    val sliderOrder: MutableList<String> = mutableListOf()
    val meters: MutableMap<String, MeterData> = mutableMapOf()
    val meterOrder: MutableList<String> = mutableListOf()
    val messages: MutableMap<String, String> = mutableMapOf()
    val messageOrder: MutableList<String> = mutableListOf()
    val statuses: MutableMap<String, Boolean> = mutableMapOf()
    val statusOrder: MutableList<String> = mutableListOf()
    var txEnabled: Boolean = false
    var isStateReady: Boolean = false
    var radioName: String = ""
    var radioDriver: String = ""

    fun reset() {
        frequencyA = 0; frequencyB = 0
        smeterA = 0.0; smeterALabel = ""
        smeterB = 0.0; smeterBLabel = ""
        buttons.clear(); buttonOrder.clear()
        dropdowns.clear(); dropdownLists.clear(); dropdownOrder.clear()
        sliders.clear(); sliderRanges.clear(); sliderOrder.clear()
        meters.clear(); meterOrder.clear()
        messages.clear(); messageOrder.clear()
        statuses.clear(); statusOrder.clear()
        txEnabled = false; isStateReady = false
        radioName = ""; radioDriver = ""
    }

    fun processCommand(command: String): Boolean {
        if (!isStateReady && (command.startsWith("chat::") || command == "radio::state-posted")) {
            isStateReady = true
        }

        if (!command.startsWith("radio::")) return false
        val rest = command.removePrefix("radio::")

        when {
            rest.startsWith("radio::") -> radioName = rest.removePrefix("radio::")
            rest.startsWith("driver::") -> radioDriver = rest.removePrefix("driver::")
            rest.startsWith("frequency::") -> frequencyA = rest.removePrefix("frequency::").toIntOrNull() ?: 0
            rest.startsWith("frequencyb::") || rest.startsWith("frequencyB::") -> {
                val prefix = if (rest.startsWith("frequencyB::")) "frequencyB::" else "frequencyb::"
                frequencyB = rest.removePrefix(prefix).toIntOrNull() ?: 0
            }
            rest.startsWith("smeter::") -> {
                val parsed = parseSMeter(rest.removePrefix("smeter::"))
                smeterA = parsed.first
                smeterALabel = parsed.second
            }
            rest.startsWith("smeterb::") || rest.startsWith("smeterB::") -> {
                val prefix = if (rest.startsWith("smeterB::")) "smeterB::" else "smeterb::"
                val parsed = parseSMeter(rest.removePrefix(prefix))
                smeterB = parsed.first
                smeterBLabel = parsed.second
            }
            rest.startsWith("button::") -> {
                val data = rest.removePrefix("button::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data).toIntOrNull() ?: 0
                if (key !in buttons) buttonOrder.add(key)
                buttons[key] = value
            }
            rest.startsWith("buttons::") -> {
                rest.removePrefix("buttons::").split(",").forEach { name ->
                    val n = name.trim()
                    if (n.isNotEmpty() && n !in buttons) {
                        buttonOrder.add(n)
                        buttons[n] = 0
                    }
                }
            }
            rest.startsWith("dropdowns::") -> {
                rest.removePrefix("dropdowns::").split(",").forEach { name ->
                    val n = name.trim()
                    if (n.isNotEmpty() && n !in dropdowns) {
                        dropdownOrder.add(n)
                        dropdowns[n] = ""
                    }
                }
            }
            rest.startsWith("dropdown::") -> {
                val data = rest.removePrefix("dropdown::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data)
                if (key !in dropdowns) dropdownOrder.add(key)
                dropdowns[key] = value
            }
            rest.startsWith("list::") -> {
                val data = rest.removePrefix("list::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data)
                dropdownLists[key] = value.split(",").toMutableList()
            }
            rest.startsWith("sliders::") -> {
                rest.removePrefix("sliders::").split(",").forEach { name ->
                    val n = name.trim()
                    if (n.isNotEmpty() && n !in sliders) {
                        sliderOrder.add(n)
                        sliders[n] = 0.0
                    }
                }
            }
            rest.startsWith("slider::") -> {
                val data = rest.removePrefix("slider::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data).toDoubleOrNull() ?: 0.0
                if (key !in sliders) sliderOrder.add(key)
                sliders[key] = value
            }
            rest.startsWith("range::") -> {
                val data = rest.removePrefix("range::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data)
                val parts = value.split(",")
                sliderRanges[key] = SliderRange(
                    min = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0,
                    max = parts.getOrNull(1)?.toDoubleOrNull() ?: 100.0,
                    step = parts.getOrNull(2)?.toDoubleOrNull() ?: 1.0,
                    displayOffset = parts.getOrNull(3) ?: ""
                )
            }
            rest.startsWith("meters::") -> {
                rest.removePrefix("meters::").split(",").forEach { name ->
                    val n = name.trim()
                    if (n.isNotEmpty() && n !in meters) {
                        meterOrder.add(n)
                        meters[n] = MeterData(0.0, 100.0, "")
                    }
                }
            }
            rest.startsWith("meter::") -> {
                val data = rest.removePrefix("meter::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data)
                if (key !in meters) meterOrder.add(key)
                meters[key] = MeterData(value.toDoubleOrNull() ?: 0.0, 100.0, "")
            }
            rest.startsWith("messages::") -> {
                rest.removePrefix("messages::").split(",").forEach { name ->
                    val n = name.trim()
                    if (n.isNotEmpty() && n !in messages) {
                        messageOrder.add(n)
                        messages[n] = ""
                    }
                }
            }
            rest.startsWith("message::") -> {
                val data = rest.removePrefix("message::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data)
                if (key !in messages) messageOrder.add(key)
                messages[key] = value
            }
            rest.startsWith("statuses::") -> {
                rest.removePrefix("statuses::").split(",").forEach { name ->
                    val n = name.trim()
                    if (n.isNotEmpty() && n !in statuses) {
                        statusOrder.add(n)
                        statuses[n] = false
                    }
                }
            }
            rest.startsWith("status::") -> {
                val data = rest.removePrefix("status::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data)
                if (key !in statuses) statusOrder.add(key)
                statuses[key] = value == "1" || value.lowercase() == "true"
            }
            rest == "state-posted" -> isStateReady = true
            rest.startsWith("tx-enabled") -> txEnabled = true
            rest.startsWith("tx-disabled") -> txEnabled = false
        }
        return true
    }

    private fun parseSMeter(value: String): Pair<Double, String> {
        if (value.contains(",")) {
            val parts = value.split(",")
            val label = parts.getOrNull(0) ?: ""
            val raw = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val maxVal = parts.getOrNull(2)?.toDoubleOrNull() ?: 255.0
            val v = (raw / maxVal) * 19.0
            return v to label
        }
        val parts = value.split("::")
        val v = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
        val label = if (parts.size > 1) parts[1] else formatSMeter(v)
        return v to label
    }

    private fun formatSMeter(value: Double): String {
        if (value <= 0) return ""
        if (value <= 9) return "S${value.toInt()}"
        val over = ((value - 9) * 6).toInt()
        return "S9+$over"
    }

    fun toData(): RadioStateData = RadioStateData(
        frequencyA = frequencyA,
        frequencyB = frequencyB,
        buttons = buttons.toMap(),
        buttonOrder = buttonOrder.toList(),
        dropdowns = dropdowns.toMap(),
        dropdownLists = dropdownLists.mapValues { it.value.toList() },
        dropdownOrder = dropdownOrder.toList(),
        sliders = sliders.toMap(),
        sliderRanges = sliderRanges.toMap(),
        sliderOrder = sliderOrder.toList(),
        meters = meters.toMap(),
        meterOrder = meterOrder.toList(),
        messages = messages.toMap(),
        messageOrder = messageOrder.toList(),
        statuses = statuses.toMap(),
        statusOrder = statusOrder.toList(),
        smeterA = smeterA,
        smeterALabel = smeterALabel,
        smeterB = smeterB,
        smeterBLabel = smeterBLabel,
        txEnabled = txEnabled
    )
}
