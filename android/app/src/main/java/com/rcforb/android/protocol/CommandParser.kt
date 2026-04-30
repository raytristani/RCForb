package com.rcforb.android.protocol

object CommandParser {
    fun splitCommand(command: String): List<String> = command.split("::")

    fun getKey(data: String): String {
        val idx = data.indexOf("::")
        return if (idx >= 0) data.substring(0, idx) else data
    }

    fun getValue(data: String): String {
        val parts = splitCommand(data)
        return if (parts.size > 1) parts[1] else ""
    }

    fun getValueAt(index: Int, data: String): String {
        val parts = splitCommand(data)
        return if (index < parts.size) parts[index] else ""
    }

    // Command builders
    fun loginCmd(user: String, passwordMD5: String): String = "login $user $passwordMD5"
    fun setProtocolRCS(): String = "set protocol rcs"
    fun requestRadioState(): String = "radio::request-state"

    fun setFrequencyA(hz: String): String = "radio::frequency::$hz"
    fun setFrequencyB(hz: String): String = "radio::frequencyb::$hz"
    fun setButton(name: String, value: String): String = "radio::button::$name::$value"
    fun setDropdown(name: String, value: String): String = "radio::dropdown::$name::$value"
    fun setSlider(name: String, value: String): String = "radio::slider::$name::$value"
    fun setMessage(name: String, value: String): String = "radio::message::$name::$value"
    fun chatMessage(text: String): String = "post::chat::$text"
    fun heartbeatCmd(): String = "post::heartbeat::${System.currentTimeMillis()}"
    fun checkCanTune(): String = "post::check::cantune"

    fun rotatorBearing(value: String): String = "rotator::bearing::$value"
    fun rotatorElevation(value: String): String = "rotator::elevation::$value"
    fun rotatorStart(): String = "rotator::start"
    fun rotatorStop(): String = "rotator::stop"
    fun ampButton(name: String, value: String): String = "amp::button::$name::$value"
    fun switchButton(name: String, value: String): String = "switch::button::$name::$value"
}
