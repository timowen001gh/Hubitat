/**
 *  ZXT-120 IR Sender Unit from Remotec
 *  tested on V1.6H version of the device
 *
 *  Original Author: Ronald Gouldner (based on b.dahlem@gmail.com version)
 *  Original Date: 2015-01-20
 *  Original Code: https://github.com/gouldner/ST-Devices/src/ZXT-120
 *
 *  Modified for Hubitat by Tim Owen
 *  Porting Date: 2019-04-22
 *  Modified Code: https://github.com/timowen001gh/Hubitat/blob/master/zxt-120-hubitat.groovy
 *
 * Copyright (C) 2013 Ronald Gouldner
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions: The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */


//***** Metadata */
//
// A description of the ZXT-120 IR Extender for HVAC and its options and commands for the SmartThings hub.

// Preferences pane
//
// options that the user can directly edit in the SmartThings app

// 2019-04-22 v1.0 - Ported to Hubitat by Tim Owen, but in a very rough state. Learning mode works and on / off work but no other functions. Some functions did not work on SmarThings.


preferences {
    input description: "Press Configure after making any changes", displayDuringSetup: true,
            type: "paragraph", element: "paragraph"
    input("remoteCode", "number", title: "Remote Code", description: "The number of the remote to emulate")
    input("tempOffset", "enum", title: "Temp correction offset?", options: ["-5","-4","-3","-2","-1","0","1","2","3","4","5"])
    input("shortName", "string", title: "Short Name for Home Page Temp Icon", description: "Short Name:")
    input("onCommand", "enum", title: "Command to send when 'On' Button is Pressed?", options: ["on(resume)","cool","heat","dry"])
    // Added by Tim Owen
    input("learningPosition", "number", title: "Learning Position?", description: "IR Code ro Store")
}

metadata {
    definition (name: "ZXT-120 IR Sender Hubitat Port", namespace: "timowen001gh", author: "Tim Owen") {
        // Device capabilities of the ZXT-120
        capability "Actuator"
        capability "Temperature Measurement"
        capability "Thermostat"
        capability "Configuration"
        // Polling has never worked on Smart Things.
        capability "Polling"
        // Try Health Check to aquire temp from device
        capability "Health Check"
        capability "Sensor"
        capability "Battery"
        capability "Switch"

        // Commands that this device-type exposes for controlling the ZXT-120 directly
        command "switchModeOff"
        command "switchModeHeat"
        command "switchModeCool"
        command "switchModeDry"
        command "switchModeAuto"
        command "switchFanLow"
        command "switchFanMed"
        command "switchFanHigh"
        command "switchFanAuto"
        command "switchFanMode"
        command "switchFanOscillate"
        command "setRemoteCode"
        command "swingModeOn"
        command "swingModeOff"


        //commands for thermostat interface
        command "cool"
        command "heat"
        command "dry"
        command "off"
        command "setLearningPosition"
        command "issueLearningCommand"

        attribute "swingMode", "STRING"
        attribute "lastPoll", "STRING"
        attribute "currentConfigCode", "STRING"
        attribute "currentTempOffset", "STRING"
        attribute "temperatureName", "STRING"
        attribute "reportedCoolingSetpoint", "STRING"
        attribute "reportedHeatingSetpoint", "STRING"
        attribute "learningPosition", "NUMBER"
        attribute "learningPositionTemp", "STRING"

        // Z-Wave description of the ZXT-120 device
        fingerprint deviceId: "0x0806"
        fingerprint inClusters: "0x20,0x27,0x31,0x40,0x43,0x44,0x70,0x72,0x80,0x86"
    }

}

def installed() {
    log.debug "ZXT-120 installed()"
    configure()
}

//***** Enumerations */
// modes - Possible heating/cooling modes for the device
def modes() {
    ["off", "auto", "heat", "emergencyHeat", "cool", "dry", "autoChangeover"]
}
// setpointModeMap - Link the possible modes the device can be in to the possible temperature setpoints.
def getSetpointModeMap() { [
        "heat": "heatingSetpoint"
        ,"cool": "coolingSetpoint"
        ,"dry": "dryingSetpoint"
        //,"autoChangeover": "autoChangeoverSetpoint"
]}
// setpointMap - Link the setpoint descriptions with ZWave id numbers
def getSetpointMap() { [
        "heatingSetpoint": hubitat.zwave.commands.thermostatsetpointv1.ThermostatSetpointSet.SETPOINT_TYPE_HEATING_1,
        "coolingSetpoint": hubitat.zwave.commands.thermostatsetpointv1.ThermostatSetpointSet.SETPOINT_TYPE_COOLING_1,
        "dryingSetpoint": hubitat.zwave.commands.thermostatsetpointv1.ThermostatSetpointSet.SETPOINT_TYPE_DRY_AIR,
        //"reportedAutoChangeoverSetpoint": hubitat.zwave.commands.thermostatsetpointv1.ThermostatSetpointSet.SETPOINT_TYPE_AUTO_CHANGEOVER
]}
// setpointReportingMap - Link the setpointReportingMap tiles with ZWave id numbers
def getSetpointReportingMap() { [
        "reportedHeatingSetpoint": hubitat.zwave.commands.thermostatsetpointv1.ThermostatSetpointSet.SETPOINT_TYPE_HEATING_1,
        "reportedCoolingSetpoint": hubitat.zwave.commands.thermostatsetpointv1.ThermostatSetpointSet.SETPOINT_TYPE_COOLING_1,
]}
// modeMap - Link the heating/cooling modes with their ZWave id numbers
def getModeMap() { [
        "off": hubitat.zwave.commands.thermostatmodev1.ThermostatModeSet.MODE_OFF,
        "resume": hubitat.zwave.commands.thermostatmodev1.ThermostatModeSet.MODE_RESUME,
        "heat": hubitat.zwave.commands.thermostatmodev1.ThermostatModeSet.MODE_HEAT,
        "cool": hubitat.zwave.commands.thermostatmodev1.ThermostatModeSet.MODE_COOL,
        "auto": hubitat.zwave.commands.thermostatmodev1.ThermostatModeSet.MODE_AUTO,
        "emergencyHeat": hubitat.zwave.commands.thermostatmodev1.ThermostatModeSet.MODE_AUXILIARY_HEAT,
        "dry": hubitat.zwave.commands.thermostatmodev1.ThermostatModeSet.MODE_DRY_AIR,
        "autoChangeover": hubitat.zwave.commands.thermostatmodev1.ThermostatModeSet.MODE_AUTO_CHANGEOVER
]}
def fanModes() {
    ["fanAuto", "fanLow", "fanMedium", "fanHigh"]
}
// fanModeMap - Link the possible fan speeds with their ZWave id numbers
def getFanModeMap() { [
        "fanAuto": hubitat.zwave.commands.thermostatfanmodev2.ThermostatFanModeReport.FAN_MODE_AUTO_LOW,
        "fanLow": hubitat.zwave.commands.thermostatfanmodev2.ThermostatFanModeReport.FAN_MODE_LOW,
        "fanMedium": hubitat.zwave.commands.thermostatfanmodev2.ThermostatFanModeReport.FAN_MODE_MEDIUM,
        "fanHigh": hubitat.zwave.commands.thermostatfanmodev2.ThermostatFanModeReport.FAN_MODE_HIGH
]}
// Command parameters
def getCommandParameters() { [
        "remoteCode": 27,
        "tempOffsetParam": 37,
        "oscillateSetting": 33,
        "learningMode": 25
]}


//***** Commands */
// parse - Handle events coming from the user and the device
def parse(String description)
{
    // If the device sent an update, interpret it
    log.info "Parsing Description=$description"
    // 0X20=Basic - V1 supported
    // 0x27=Switch All - V1 supported
    // 0X31=Sensor Multilevel - V1 supported
    // 0X40=Thermostat Mode - V2 supported
    // -- 0x42=Thermostat Operating State (NOT SUPPORTED, was in original device handler)
    // 0x43=Thermostat Setpoint - V2 supported
    // 0x44=Thermostat Fan Mode - V2 supported
    // 0x70=Configuration - V1 supported
    // 0x72=Manufacturer Specific - V1 supported
    // 0x80=Battery - V1 supported
    // 0x86=Version - V1 supported
    def cmd = zwave.parse(description, [0X20:1, 0X27:1, 0x31:1, 0x40:2, 0x43:2, 0x44:2, 0x70:1, 0x72:1, 0x80:1, 0x86:1])
    def map = []
    def result = null
    if (cmd) {
        log.debug "Parsed ${cmd} to ${result.inspect()}"
        result = zwaveEvent(cmd)
        map = createEvent(result)
    } else {
        log.debug "Non-parsed event. Perhaps wrong version is being handled?: ${description}"
        return null
    }

    if (map) {
        log.debug "Parsed ${description} to command ${cmd} to result ${result.inspect()} map=${map}"
        // If the update was a change in the device's fan speed
        if (map.name == "thermostatFanMode" && map.isStateChange) {
            // store the new fan speed
            updateState("lastTriedFanMode", map.value)
        }
        return [map]
    } else {
       return null
    }
}


//***** Event Handlers */
//Handle events coming from the device

// Battery Level event
def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    log.debug "BatteryReport cmd:$cmd"
    def map = [:]
    map.name = "battery"
    map.value = cmd.batteryLevel > 0 ? cmd.batteryLevel.toString() : 1
    map.unit = "%"
    map.displayed = false
    log.debug "Battery Level Reported=$map.value"
    map
}

// - Sensor Multilevel Report
// The device is reporting temperature readings
def zwaveEvent(hubitat.zwave.commands.sensormultilevelv1.SensorMultilevelReport cmd)
{
    log.debug "SensorMultilevelReport reporting...cmd=$cmd"
    // Determine the temperature the device is reporting
    def map = [:]
    switch (cmd.sensorType) {
        case 1:
            // temperature
            def cmdScale = cmd.scale == 1 ? "F" : "C"
            log.debug "cmd.scale=$cmd.scale"
            log.debug "cmd.scaledSensorValue=$cmd.scaledSensorValue"
            // converTemp returns string with two decimal places
            // convert to double then to int to drop the decimal
            Integer temp = (int) convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale).toDouble()
            map.value = temp
            map.unit = getTemperatureScale()
            map.name = "temperature"
            // Send event to set ShortName + Temp tile
            def shortNameVal = shortName == null ? "ZXT-120" : shortName
            def tempName = shortNameVal + " " + map.value + "°"
            log.debug "Sensor Reporting temperatureName $tempName map.value=$map.value, cmdScale=$cmdScale"
            sendEvent("name":"temperatureName", "value":tempName)
            // Pass value converted to Fahrenheit and Unit of 1 which means Fahrenheit
            sendEvent("name":"temperature", "value":map.value, "isStateChange":true, unit:1, displayed:true)
            //sendEvent("name":"temperature", "value":map.value, "isStateChange":true, displayed:true)
            break;
        default:
            log.warn "Unknown sensorType reading from device"
            break;
    }
}

// - Thermostat Mode Report
// The device is reporting its heating/cooling Mode
def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
    def map = [:]

    // Determine the mode the device is reporting, based on its ZWave id
    map.value = modeMap.find {it.value == cmd.mode}?.key
    map.name = "thermostatMode"
    log.debug "Thermostat Mode reported : $map.value"
    // Return the interpreted report
    map
}

// - Thermostat Fan Mode Report
// The device is reporting its current fan speed
def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev2.ThermostatFanModeReport cmd) {
    def map = [:]

    // Determine the fan speed the device is reporting, based on its ZWave id
    map.value = fanModeMap.find {it.value == cmd.fanMode}?.key
    map.name = "thermostatFanMode"
    map.displayed = false
    log.debug "Fan Mode Report=$value"
    // Return the interpreted report
    map
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    def map = [:]

    switch (cmd.parameterNumber) {
    // If the device is reporting its remote code
        case commandParameters["remoteCode"]:
            map.name = "remoteCode"
            map.displayed = false

            def short remoteCodeLow = cmd.configurationValue[1]
            def short remoteCodeHigh = cmd.configurationValue[0]
            map.value = (remoteCodeHigh << 8) + remoteCodeLow

            // Display configured code in tile
            log.debug "reported currentConfigCode=$map.value"
            sendEvent("name":"currentConfigCode", "value":map.value)

            break

    // If the device is reporting its remote code
        case commandParameters["tempOffsetParam"]:
            map.name = "tempOffset"
            map.displayed = false

            def short offset = cmd.configurationValue[0]
            if (offset >= 0xFB) {
                // Hex FB-FF represent negative offsets FF=-1 - FB=-5
                offset = offset - 256
            }
            map.value = offset
            log.debug "reported offset=$map.value"
            // Display temp offset in tile
            sendEvent("name":"currentTempOffset", "value":map.value)

            break
    // If the device is reporting its oscillate mode
        case commandParameters["oscillateSetting"]:
            // determine if the device is oscillating
            def oscillateMode = (cmd.configurationValue[0] == 0) ? "off" : "on"

            //log.debug "Updated: Oscillate " + oscillateMode
            map.name = "swingMode"
            map.value = oscillateMode
            map.displayed = false

            map.isStateChange = oscillateMode != getDataByName("swingMode")

            log.debug "reported swing mode = oscillateMode"
            // Store and report the oscillate mode
            updateState("swingMode", oscillateMode)

            break
        default:
            log.warn "Unknown configuration report cmd.parameterNumber"
            break;
    }

    map
}

// - Thermostat Supported Modes Report
// The device is reporting heating/cooling modes it supports
def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeSupportedReport cmd) {
    // Create a string with mode names for each available mode
    def supportedModes = ""
    if(cmd.off) { supportedModes += "off " }
    if(cmd.heat) { supportedModes += "heat " }
    //if(cmd.auxiliaryemergencyHeat) { supportedModes += "emergencyHeat " }
    if(cmd.cool) { supportedModes += "cool " }
    //if(cmd.auto) { supportedModes += "auto " }
    if(cmd.dryAir) { supportedModes += "dry " }
    //if(cmd.autoChangeover) { supportedModes += "autoChangeover " }

    // Report and save available modes
    log.debug "Supported Modes: ${supportedModes}"
    updateState("supportedModes", supportedModes)
}

// - Thermostat Fan Supported Modes Report
// The device is reporting fan speeds it supports
def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev2.ThermostatFanModeSupportedReport cmd) {
    // Create a string with mode names for each available mode
    def supportedFanModes = ""
    if(cmd.auto) { supportedFanModes += "fanAuto " }
    if(cmd.low) { supportedFanModes += "fanLow " }
    if(cmd.medium) { supportedFanModes += "fanMedium " }
    if(cmd.high) { supportedFanModes += "fanHigh " }

    // Report and save available speeds
    log.debug "Supported Fan Modes: ${supportedFanModes}"
    updateState("supportedFanModes", supportedFanModes)
}

// - Basic Report
// The device is sending standard ZWave updates
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    log.debug "Zwave event received: $cmd"
}

// - Command Report
// The device is reporting parameter settings
def zwaveEvent(hubitat.zwave.Command cmd) {
    // simply report it
    log.warn "Unexpected zwave command $cmd"
}

// Update State
// Store mode and settings
def updateState(String name, String value) {
    state[name] = value
    device.updateDataValue(name, value)
}

def ping() {
	log.debug "ping called"
	poll()
}

// Command Implementations
// Ask the device for its current state
def poll() {
    def now=new Date()
    def tz = location.timeZone
    def nowString = now.format("MMM/dd HH:mm",tz)

    sendEvent("name":"lastPoll", "value":nowString)

    log.debug "Polling now $nowString"
    // create a list of requests to send
    def commands = []

    commands <<	zwave.sensorMultilevelV1.sensorMultilevelGet().format()		// current temperature
    commands <<	zwave.batteryV1.batteryGet().format()                       // current battery level
    commands <<	zwave.thermostatModeV2.thermostatModeGet().format()     	// thermostat mode
    commands <<	zwave.thermostatFanModeV2.thermostatFanModeGet().format()	// fan speed
    commands <<	zwave.configurationV1.configurationGet(parameterNumber: commandParameters["remoteCode"]).format()		// remote code
    commands <<	zwave.configurationV1.configurationGet(parameterNumber: commandParameters["tempOffsetParam"]).format()  // temp offset
    commands <<	zwave.configurationV1.configurationGet(parameterNumber: commandParameters["oscillateSetting"]).format()	// oscillate setting

    // add requests for each thermostat setpoint available on the device
    def supportedModes = getDataByName("supportedModes")
    for (setpoint in setpointMap) {
        // This code doesn't work correctly....Need to fix later for now only implemented supported modes for myself
        //if (supportedModes.tokenize()?.contains(setpoint.key)) {
        log.debug "Requesting setpoint $setpoint.value"
        commands << [zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: setpoint.value).format()]
        //} else {
        //    log.debug "Skipping unsupported mode $setpoint.key"
        //}
    }

    // send the requests
    delayBetween(commands, 2300)
}

def setHeatingSetpoint(degrees) {
    def degreesInteger = degrees as Integer
    def temperatureScale = getTemperatureScale()

    if (temperatureScale == "C") {
        // ZXT-120 lowest settings is 19 C
        if (degreesInteger < 19) {
            degreesInteger = 19;
        }
        // ZXT-120 highest setting is 28 C
        if (degreesInteger > 28) {
            degreesInteger = 28;
        }
    } else {
        // ZXT-120 lowest settings is 67 F
        if (degreesInteger < 67) {
            degreesInteger = 67;
        }
        // ZXT-120 highest setting is 84
        if (degreesInteger > 84) {
            degreesInteger = 84;
        }
    }
    log.debug "setHeatingSetpoint({$degreesInteger} ${temperatureScale})"
    sendEvent("name":"heatingSetpoint", "value":degreesInteger)
    //def celsius = (temperatureScale == "C") ? degreesInteger : (fahrenheitToCelsius(degreesInteger) as Double).round(2)
    //"st wattr 0x${device.deviceNetworkId} 1 0x201 0x12 0x29 {" + hex(celsius*100) + "}"
    //def setpointMode = hubitat.zwave.commands.thermostatsetpointv1.ThermostatSetpointSet.SETPOINT_TYPE_HEATING_1
    //setThermostatSetpointForMode(degreesInteger.toDouble(), setpointMode)
}

def setCoolingSetpoint(degrees) {
    def degreesInteger = degrees as Integer
    def temperatureScale = getTemperatureScale()

    if (temperatureScale == "C") {
        // ZXT-120 lowest settings is 19 C
        if (degreesInteger < 19) {
            degreesInteger = 19;
        }
        // ZXT-120 highest setting is 28 C
        if (degreesInteger > 28) {
            degreesInteger = 28;
        }
    } else {
        // ZXT-120 lowest settings is 67 F
        if (degreesInteger < 67) {
            degreesInteger = 67;
        }
        // ZXT-120 highest setting is 28
        if (degreesInteger > 84) {
            degreesInteger = 84;
        }
    }
    log.debug "setCoolingSetpoint({$degreesInteger} ${temperatureScale})"
    sendEvent("name":"coolingSetpoint", "value":degreesInteger)
    // Sending temp to zxt-120
    //def celsius = (temperatureScale == "C") ? degreesInteger : (fahrenheitToCelsius(degreesInteger) as Double).round(2)
    //"st wattr 0x${device.deviceNetworkId} 1 0x201 0x11 0x29 {" + hex(celsius*100) + "}"
    //def setpointMode = hubitat.zwave.commands.thermostatsetpointv1.ThermostatSetpointSet.SETPOINT_TYPE_COOLING_1
    //setThermostatSetpointForMode(degreesInteger.toDouble(), setpointMode)
}

def setLearningPosition(position) {
    log.debug "Setting learning postition: $position"
    sendEvent("name":"learningPosition", "value":learningPosition)
    def ctemp = 0
    if (position < 12) {
        ctemp=position+17
    } else {
        ctemp=position+7
    }
    def ftempLow=(Math.ceil(((ctemp*9)/5)+32)).toInteger()
    def ftempHigh=ftempLow+1
    def positionTemp = "not set"
    switch (position) {
        case 0:
            positionTemp = 'Off'
            break
        case 1:
            positionTemp = 'On(resume)'
            break
        case [3,4,5,6,8,9,10,11]:
            positionTemp = "cool ${ctemp}C ${ftempLow}-${ftempHigh}F"
            break
        case [2,7]:
            positionTemp = "cool ${ctemp}C ${ftempLow}F"
            break
        case [13,14,15,16,18,19,20,21]:
            positionTemp = "heat ${ctemp}C ${ftempLow}-${ftempHigh}F"
            break
        case [12,17]:
            positionTemp = "heat ${ctemp}C ${ftempLow}F"
            break
        case 22:
            positionTemp = 'Dry mode'
            break
        default:
            positionTemp = 'Invalid'
            break
    }
    sendEvent("name":"learningPositionTemp", "value":positionTemp)
}

def issueLearningCommand() {
    def position = device.currentValue("learningPosition").toInteger()
    log.debug "Issue Learning Command pressed Position Currently: $position"

    def positionConfigArray = [position]

    log.debug "Position Config Array: ${positionConfigArray}"

    delayBetween ([
            // Send the new remote code
            zwave.configurationV1.configurationSet(configurationValue: positionConfigArray,
                    parameterNumber: commandParameters["learningMode"], size: 1).format()
    ])
}

//***** Set the thermostat */
def setThermostatSetpoint(degrees) {
    log.debug "setThermostatSetpoint called.....want to get rid of that"
    // convert the temperature to a number and execute
    setThermostatSetpoint(degrees.toDouble())
}

// Configure
// Syncronize the device capabilities with those that the UI provides
def configure() {
    delayBetween([
            // update the device's remote code to ensure it provides proper mode info
            setRemoteCode(),
            setTempOffset(),
            // Request the device's current heating/cooling mode
            zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
            // Request the device's current fan speed
            zwave.thermostatFanModeV2.thermostatFanModeSupportedGet().format(),
            // Assign the device to ZWave group 1
            zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]).format()
    ], 2300)
}

// Switch Fan Mode
// Switch to the next available fan speed
def switchFanMode() {
    // Determine the current fan speed setting
    def currentMode = device.currentState("thermostatFanMode")?.value
    def lastTriedMode = getDataByName("lastTriedFanMode") ?: currentMode.value ?: "off"

    // Determine what fan speeds are available
    def supportedModes = getDataByName("supportedFanModes") ?: "fanAuto fanLow"
    def modeOrder = fanModes()
    //log.info modeOrder

    // Determine what the next fan speed should be
    def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
    def nextMode = next(lastTriedMode)
    while (!supportedModes?.contains(nextMode) && nextMode != "fanAuto") {
        nextMode = next(nextMode)
    }

    // Make it so
    switchToFanMode(nextMode)
}

// Switch to Fan Mode
// Given the name of a fan mode, make it happen
def switchToFanMode(nextMode) {
    def supportedFanModes = getDataByName("supportedFanModes")
    if(supportedFanModes && !supportedFanModes.tokenize()?.contains(nextMode)) log.warn "thermostat mode '$nextMode' is not supported"

    // If the mode is even possible
    if (nextMode in fanModes()) {
        // Try to switch to the mode
        updateState("lastTriedFanMode", nextMode)
        return "$nextMode"()  // Call the function perform the mode switch
    } else {
        // Otherwise, bail
        log.debug("no fan mode method '$nextMode'")
    }
}

// Get Data By Name
// Given the name of a setting/attribute, lookup the setting's value
def getDataByName(String name) {
    state[name] ?: device.getDataValue(name)
}


// - Thermostat Setpoint Report
// The device is telling us what temperatures it is set to for a particular mode
def zwaveEvent(hubitat.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd)
{
    log.info "RRG V1 ThermostatSetpointReport cmd=$cmd"
    //NOTE:  When temp is sent to device in Fahrenheit and returned in celsius
    //       1 degree difference is normal.  Device only has 1 degree celsius granularity
    //       issuing 80F for example returns 26C, which converts to 79F
    //       Maybe I should lie to user and report current set temp rather than reported temp
    //       to avoid confusion and false bug reports....needs to be considered.
    def cmdScale = cmd.scale == 1 ? "F" : "C"
    log.debug "cmd.scale=$cmd.scale"
    log.debug "cmd.scaledValue=$cmd.scaledValue"
    // converTemp returns string with two decimal places
    // convert to double then to int to drop the decimal
    Integer reportedTemp = (int) convertTemperatureIfNeeded(cmd.scaledValue, cmdScale).toDouble()

    // Determine what mode the setpoint is for, if the mode is not valid, bail out
    def name = setpointReportingMap.find {it.value == cmd.setpointType}?.key
    if (name == null) {
        log.warn "Setpoint Report for Unknown Type $cmd.setpointType"
        return
    }

    // Return the interpretation of the report
    log.debug "Thermostat Setpoint Report for $name = $reportedTemp forcing state change true"
    sendEvent("name":name, "value":reportedTemp, "isStateChange":true)
}

// Set Thermostat Mode
// Set the device to the named mode
def setThermostatMode(String value) {
    def commands = []
    def degrees=0
    def setpointMode=null
    log.debug("setThermostatMode value:$value")

    if (value == "cool") {
        log.debug("Cool requested, sending setpoint cooling")
        degrees = device.currentValue("coolingSetpoint")
        setpointMode = hubitat.zwave.commands.thermostatsetpointv1.ThermostatSetpointSet.SETPOINT_TYPE_COOLING_1
    } else if (value == "heat") {
        log.debug("heat requested, sending setpoint heating")
        degrees = device.currentValue("heatingSetpoint")
        setpointMode = hubitat.zwave.commands.thermostatsetpointv1.ThermostatSetpointSet.SETPOINT_TYPE_HEATING_1
    } else if (value == "dry" || value == "off" || value == "resume") {
        log.debug("Dry Mode or Off no need to send temp")
    } else {
        log.warn("Unknown thermostat mode set:$value")
    }

    // Send temp if degrees set
    if (degrees != 0 && setpointMode != null) {
        log.debug "state.scale=${state.scale}"
        def deviceScale = state.scale ?: 1
        log.debug "deviceScale=${deviceScale}"
        def deviceScaleString = deviceScale == 2 ? "C" : "F"
        log.debug "deviceScaleString=${deviceScaleString}"
        def locationScale = getTemperatureScale()
        log.debug "state.precision=${state.precision}"
        def p = (state.precision == null) ? 1 : state.precision
        log.debug "p=${p}"

        def convertedDegrees
        if (locationScale == "C" && deviceScaleString == "F") {
            log.debug "Converting celsius to fahrenheit"
            convertedDegrees = Math.ceil(celsiusToFahrenheit(degrees))
        } else if (locationScale == "F" && deviceScaleString == "C") {
            log.debug "Converting fahrenheit to celsius"
            convertedDegrees = fahrenheitToCelsius(degrees)
        } else {
            log.debug "No Conversion needed"
            convertedDegrees = degrees
        }
        log.debug "convertedDegrees=${convertedDegrees}, degrees=${degrees}"

        // Report the new temperature being set
        log.debug "new temp ${degrees}"
        log.debug("Sending Temp [$convertedDegrees] for $value mode before enabling mode")
        // Send the new temperature from the thermostat and request confirmation
        commands << zwave.thermostatSetpointV2.thermostatSetpointSet(setpointType: setpointMode, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format()
        commands << zwave.thermostatSetpointV2.thermostatSetpointGet(setpointType: setpointMode).format()
    }

    // Set thermostat mode and request confirmation
    commands << zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[value]).format()
    commands << zwave.thermostatModeV2.thermostatModeGet().format()

    // send the requests
    delayBetween(commands, 2300)
}

// Set Thermostat Fan Mode
// Set the device to the named fan speed
def setThermostatFanMode(String value) {
    log.debug "setThermostatFanMode to ${value} fanModeMap:${fanModeMap[value]}"
    delayBetween([
            // Command the device to change the fan speed
            zwave.thermostatFanModeV2.thermostatFanModeSet(fanMode: fanModeMap[value]).format(),
            // Request an update to make sure it worked
            zwave.thermostatFanModeV2.thermostatFanModeGet().format()
    ])
}

// Mode Commands
// provide simple access to mode changes

// public interface commands for Thermostat
def cool() {
    switchModeCool()
}

def heat() {
    switchModeHeat()
}

def dry() {
    switchModeDry()
}

def off() {
    log.debug "${device.name} received off request"
    switchModeOff()
}

def on() {
    def onCommandVal = onCommand == null ? "on(resume)" : onCommand
    log.debug "${device.name} received on request onCommandVal=${onCommandVal}"

    switch (onCommandVal) {
        case "on(resume)":
            log.debug "issuing setThermostatMode:on"
            setThermostatMode("resume")
            break;
        case ['cool','heat','dry']:
            log.debug "issuing setThermostatMode:${onCommandVal}"
            setThermostatMode(onCommandVal)
            break;
        default:
            log.warn "Configuration Error: unknown onCommandVal: ${onCommandVal}"
            break;
    }
}

// switchModeCommands
def switchModeOff() {
    setThermostatMode("off")
}

def switchModeHeat() {
    setThermostatMode("heat")
}

def emergencyHeat() {
    setThermostatMode("emergencyHeat")
}

def switchModeDry() {
    setThermostatMode("dry")
}

def switchModeCool() {
    setThermostatMode("cool")
}

def switchModeAuto() {
    setThermostatMode("auto")
}

def autoChangeover() {
    setThermostatMode("autoChangeover")
}

def switchFanLow() {
    log.debug "setting fan mode low"
    setThermostatFanMode("fanLow")
}

def switchFanMed() {
    log.debug "setting fan mode med"
    setThermostatFanMode("fanMedium")
}

def switchFanHigh() {
    log.debug "setting fan mode high"
    setThermostatFanMode("fanHigh")
}

def switchFanAuto() {
    log.debug "setting fan mode auto"
    setThermostatFanMode("fanAuto")
}

// Set Remote Code
// tell the ZXT-120 what remote code to use when communicating with the A/C
def setRemoteCode() {
    // Load the user's remote code setting
    def remoteCodeVal = remoteCode.toInteger()

    // Divide the remote code into a 2 byte value
    def short remoteCodeLow = remoteCodeVal & 0xFF
    def short remoteCodeHigh = (remoteCodeVal >> 8) & 0xFF
    def remoteBytes = [remoteCodeHigh, remoteCodeLow]

    log.debug "New Remote Code: ${remoteBytes}"

    delayBetween ([
            // Send the new remote code
            zwave.configurationV1.configurationSet(configurationValue: remoteBytes,
                    parameterNumber: commandParameters["remoteCode"], size: 2).format(),
            // Request the device's remote code to make sure the new setting worked
            zwave.configurationV1.configurationGet(parameterNumber: commandParameters["remoteCode"]).format()
    ])
}
def setTempOffset() {
    // Load the user's remote code setting
    def tempOffsetVal = tempOffset == null ? 0 : tempOffset.toInteger()
    // Convert negative values into hex value for this param -1 = 0xFF -5 = 0xFB
    if (tempOffsetVal < 0) {
        tempOffsetVal = 256 + tempOffsetVal
    }

    def configArray = [tempOffsetVal]

    log.debug "TempOffset: ${tempOffsetVal}"

    delayBetween ([
            // Send the new remote code
            zwave.configurationV1.configurationSet(configurationValue: configArray,
                    parameterNumber: commandParameters["tempOffsetParam"], size: 1).format(),
            // Request the device's remote code to make sure the new setting worked
            zwave.configurationV1.configurationGet(parameterNumber: commandParameters["tempOffsetParam"]).format()
    ])
}

// Switch Fan Oscillate
// Toggle fan oscillation on and off
def switchFanOscillate() {
    // Load the current swingmode and invert it (Off becomes true, On becomes false)
    def swingMode = (getDataByName("swingMode") == "off")

    // Make the new swingMode happen
    setFanOscillate(swingMode)
}

def swingModeOn() {
    log.debug "Setting Swing mode On"
    setFanOscillate(true)
}

def swingModeOff() {
    log.debug "Setting Swing mode Off"
    setFanOscillate(false)
}

// Set Fan Oscillate
// Set the fan oscillation to On (swingMode == true) or Off (swingMode == false)
def setFanOscillate(swingMode) {
    // Convert the swing mode requested to 1 for on, 0 for off
    def swingValue = swingMode ? 1 : 0
    log.debug "Sending Swing Mode swingValue=$swingValue"
    delayBetween ([
            // Command the new Swing Mode
            zwave.configurationV1.configurationSet(configurationValue: [swingValue],
                    parameterNumber: commandParameters["oscillateSetting"], size: 1).format(),
            // Request the device's swing mode to make sure the new setting was accepted
            zwave.configurationV1.configurationGet(parameterNumber: commandParameters["oscillateSetting"]).format()
    ])
}

