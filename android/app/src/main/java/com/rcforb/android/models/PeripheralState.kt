package com.rcforb.android.models

import com.rcforb.android.protocol.CommandParser

class RotatorStateModel {
    var enabled = false
    var name = "Rotator"
    var bearing = 0
    var elevation = 0
    var moving = false
    val buttons: MutableMap<String, Int> = mutableMapOf()
    val buttonOrder: MutableList<String> = mutableListOf()

    fun reset() {
        enabled = false; name = "Rotator"
        bearing = 0; elevation = 0; moving = false
        buttons.clear(); buttonOrder.clear()
    }

    fun processCommand(command: String): Boolean {
        if (!command.startsWith("rotator::")) return false
        val rest = command.removePrefix("rotator::")

        when {
            rest == "enabled" || rest.startsWith("enabled") -> enabled = true
            rest.startsWith("name::") -> name = rest.removePrefix("name::")
            rest.startsWith("bearing::") -> bearing = rest.removePrefix("bearing::").toIntOrNull() ?: 0
            rest.startsWith("elevation::") -> elevation = rest.removePrefix("elevation::").toIntOrNull() ?: 0
            rest == "started" -> moving = true
            rest == "stopped" -> moving = false
            rest.startsWith("button::") -> {
                val data = rest.removePrefix("button::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data).toIntOrNull() ?: 0
                if (key !in buttons) buttonOrder.add(key)
                buttons[key] = value
            }
        }
        return true
    }

    fun toData(): RotatorStateData = RotatorStateData(
        bearing = bearing, elevation = elevation, moving = moving,
        buttons = buttons.toMap(), buttonOrder = buttonOrder.toList()
    )
}

class AmpStateModel {
    var enabled = false
    var name = "Amp"
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

    fun reset() {
        enabled = false; name = "Amp"
        buttons.clear(); buttonOrder.clear()
        dropdowns.clear(); dropdownLists.clear(); dropdownOrder.clear()
        sliders.clear(); sliderRanges.clear(); sliderOrder.clear()
        meters.clear(); meterOrder.clear()
    }

    fun processCommand(command: String): Boolean {
        if (!command.startsWith("amp::")) return false
        val rest = command.removePrefix("amp::")

        when {
            rest.startsWith("enabled") -> enabled = true
            rest.startsWith("name::") -> name = rest.removePrefix("name::")
            rest.startsWith("buttons::") -> {
                rest.removePrefix("buttons::").split(",").forEach { n ->
                    val name = n.trim()
                    if (name.isNotEmpty() && name !in buttons) {
                        buttonOrder.add(name)
                        buttons[name] = 0
                    }
                }
            }
            rest.startsWith("button::") -> {
                val data = rest.removePrefix("button::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data).toIntOrNull() ?: 0
                if (key !in buttons) buttonOrder.add(key)
                buttons[key] = value
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
            rest.startsWith("meter::") -> {
                val data = rest.removePrefix("meter::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data)
                if (key !in meters) meterOrder.add(key)
                meters[key] = MeterData(value.toDoubleOrNull() ?: 0.0, 100.0, "")
            }
        }
        return true
    }

    fun toData(): AmpStateData = AmpStateData(
        buttons = buttons.toMap(), buttonOrder = buttonOrder.toList(),
        dropdowns = dropdowns.toMap(), dropdownLists = dropdownLists.mapValues { it.value.toList() },
        dropdownOrder = dropdownOrder.toList(),
        sliders = sliders.toMap(), sliderRanges = sliderRanges.toMap(), sliderOrder = sliderOrder.toList(),
        meters = meters.toMap(), meterOrder = meterOrder.toList()
    )
}

class SwitchStateModel {
    var enabled = false
    var name = "Switch"
    val buttons: MutableMap<String, Int> = mutableMapOf()
    val buttonOrder: MutableList<String> = mutableListOf()

    fun reset() {
        enabled = false; name = "Switch"
        buttons.clear(); buttonOrder.clear()
    }

    fun processCommand(command: String): Boolean {
        if (!command.startsWith("switch::")) return false
        val rest = command.removePrefix("switch::")

        when {
            rest.startsWith("enabled") -> enabled = true
            rest.startsWith("name::") -> name = rest.removePrefix("name::")
            rest.startsWith("buttons::") -> {
                rest.removePrefix("buttons::").split(",").forEach { n ->
                    val name = n.trim()
                    if (name.isNotEmpty() && name !in buttons) {
                        buttonOrder.add(name)
                        buttons[name] = 0
                    }
                }
            }
            rest.startsWith("button::") -> {
                val data = rest.removePrefix("button::")
                val key = CommandParser.getKey(data)
                val value = CommandParser.getValue(data).toIntOrNull() ?: 0
                if (key !in buttons) buttonOrder.add(key)
                buttons[key] = value
            }
        }
        return true
    }

    fun toData(): SwitchStateData = SwitchStateData(
        buttons = buttons.toMap(), buttonOrder = buttonOrder.toList()
    )
}
