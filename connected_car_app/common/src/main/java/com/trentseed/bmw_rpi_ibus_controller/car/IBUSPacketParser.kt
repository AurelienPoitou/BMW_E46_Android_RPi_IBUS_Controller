package com.trentseed.bmw_rpi_ibus_controller.car

import com.trentseed.bmw_rpi_ibus_controller.common.IBUSPacket
import com.trentseed.bmw_rpi_ibus_controller.common.LogConfig
import java.util.logging.Logger

class IBUSPacketParser {
    private val logger: Logger = LogConfig.getLogger()
    private val carState: CarState = CarState.getInstance()

    fun parsePackets(packets: List<IBUSPacket>) {
        val previousCarState = carState.copy()
        for (packet in packets) {
            parsePacket(packet)
        }
        val changes = compareCarStates(previousCarState, carState)
        if (changes.isNotEmpty()) {
            logger.info("Car State Changes:\n$changes")
        }
    }

    private fun parsePacket(packet: IBUSPacket) {
        try {
            when {
                packet.data.size <= 2 -> {}
                packet.destinationId == 0x18 -> parseCdChangerPacket(packet) // CD Changer
                packet.sourceId == 0x60 -> parsePdcPacket(packet) // PDC
                packet.destinationId == 0x68 -> parseBmButtonPacket(packet) // BM Button
                packet.sourceId == 0x50 -> parseStwButtonPacket(packet) // STW Button
                //packet.destinationId == 0x3B || packet.destinationId == 0xE7 -> parseGtPacket(packet) // GT
                packet.sourceId == 0x68 -> parseRadioPacket(packet) // Radio
                packet.sourceId == 0x80 -> parseInstrumentClusterPacket(packet) // Instrument Cluster
                packet.sourceId == 0xBF -> parseLightControlModulePacket(packet) // Light Control Module
                packet.sourceId == 0x00 || packet.sourceId == 0x08 -> parseGeneralModulePacket(packet) // General Module
                packet.sourceId == 0x72 -> parseHvacPacket(packet) // HVAC
                packet.sourceId == 0x66 -> parseMultiFunctionSteeringWheelPacket(packet) // Multi Function Steering Wheel
                packet.sourceId == 0x3B || packet.sourceId == 0xE7 -> {
                    parseGtPacketExtended(packet)
                }
                else -> logger.warning("Unknown source address: ${packet.sourceId}")
            }
        } catch (e: Exception) {
            logger.severe("Error parsing packet: ${printIntListAsHex(packet.raw)}")
            logger.severe(e.stackTraceToString())
        }
    }

    private fun parseRadioPacket(packet: IBUSPacket) {
        // Example: Check if the packet is a power on/off command
        if (packet.data.size >= 2 && packet.data[0] == 0x43 && packet.data[1] == 0x01) {
            carState.radio.power = true
        } else if (packet.data.size >= 2 && packet.data[0] == 0x43 && packet.data[1] == 0x00) {
            carState.radio.power = false
        } else if (packet.data.size >= 2 && packet.data[0] == 0x44) {
            carState.radio.volume = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x42) {
            carState.radio.band = when (packet.data[1]) {
                0x00 -> RadioBand.AM
                0x01 -> RadioBand.FM
                0x02 -> RadioBand.DAB
                else -> RadioBand.FM
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x45) {
            carState.radio.mute = packet.data[1] == 0x01
        } else if (packet.data.size >= 3 && packet.data[0] == 0x46) {
            carState.radio.cdChangerDisc = packet.data[1]
            carState.radio.cdChangerTrack = packet.data[2]
        } else {
            logger.info("Radio packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseInstrumentClusterPacket(packet: IBUSPacket) {
        // Example: Extract speed from the packet
        if (packet.data[0] == 0x11) {
            val ignitionState = if (packet.data[4] < 2) packet.data[4] else (0x02 and packet.data[4])
            carState.instrumentCluster.ignitionState = when (ignitionState) {
                0 -> IgnitionState.OFF
                1 -> IgnitionState.ACC
                3 -> IgnitionState.ON
                7 -> IgnitionState.START
                else -> IgnitionState.UNKNOWN
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x13) {
            carState.instrumentCluster.handbrakeOn = packet.data[1] and 0x01 == 0x01
            carState.instrumentCluster.engineRunning = packet.data[2] and 0x01 == 0x01
            carState.instrumentCluster.gear = if (packet.data[2] and 0x10 == 0x01) -1 else packet.data[2] and 0xF0
            carState.instrumentCluster.fuelLevel = packet.data[7]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x15) {
        } else if (packet.data.size >= 2 && packet.data[0] == 0x17) {
            carState.instrumentCluster.odometer = packet.data[1] + packet.data[2] shl 8 + packet.data[3] shl 16
            carState.instrumentCluster.serviceInterval = (packet.data[4] + packet.data[5]) * 50
            carState.instrumentCluster.serviceIntervalType = packet.data[6]
            carState.instrumentCluster.serviceIntervalDays = packet.data[7]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x18) {
            carState.instrumentCluster.speed = packet.data[1]
            carState.instrumentCluster.rpm = packet.data[2] * 100
        } else if (packet.data.size >= 2 && packet.data[0] == 0x19) {
            carState.instrumentCluster.outsideTemp = when{
                packet.data[1] > 128 -> packet.data[1] - 256
                else -> packet.data[1]
            }
            carState.instrumentCluster.coolantTemp = when{
                packet.data[2] > 128 -> packet.data[2] - 256
                else -> packet.data[2]
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x24) {
            val text = IBUSPacket.convertASCIIHexToString(packet.data.subList(3, packet.data.size))
            logger.info("Display: " + text)
        } else {
            logger.info("Instrument Cluster packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseLightControlModulePacket(packet: IBUSPacket) {
        // Example: Extract headlight status from the packet
        if (packet.data[0] == 0x54) {
            carState.lightControlModule.vin = IBUSPacket.convertASCIIHexToString(packet.data.subList(1, 6))
            carState.lightControlModule.odometer = (packet.data[6] shl 8 + packet.data[7]) * 100
            carState.lightControlModule.sinceServiceDays = packet.data[10] shl 8 + packet.data[11]
            carState.lightControlModule.sinceServiceLiters = ((packet.data[8] shl 8 + packet.data[9]) and 0x7ff) * 10
        } else if (packet.data[0] == 0x5B) {
            // Lights
            carState.lightControlModule.frontPositionLights = packet.data[1] and 0x01 == 1
            carState.lightControlModule.lowBeamLights = packet.data[1] and 0x02 == 1
            carState.lightControlModule.highBeamLights = packet.data[1] and 0x04 == 1
            carState.lightControlModule.frontFogLights = packet.data[1] and 0x08 == 1
            carState.lightControlModule.rearFogLights = packet.data[1] and 0x10 == 1
            carState.lightControlModule.leftTurnSignal = packet.data[1] and 0x20 == 1
            carState.lightControlModule.rightTurnSignal = packet.data[1] and 0x40 == 1
            carState.lightControlModule.turnSignalFast = packet.data[1] and 0x80 == 1
            carState.lightControlModule.brakeLights = packet.data[3] and 0x02 == 1
            carState.lightControlModule.turnSignalSync = packet.data[3] and 0x04 == 1
            carState.lightControlModule.rearPositionLights = packet.data[3] and 0x08 == 1
            carState.lightControlModule.trailerPositionLights = packet.data[3] and 0x10 == 1
            carState.lightControlModule.reverseLights = packet.data[3] and 0x20 == 1
            carState.lightControlModule.trailerReverseLights = packet.data[3] and 0x40 == 1
            carState.lightControlModule.hazardLights = packet.data[3] and 0x80 == 1
            // Faults
            carState.lightControlModule.frontPositionFault = packet.data[2] and 0x01 == 1
            carState.lightControlModule.lowBeamFault = packet.data[2] and 0x02 == 1
            carState.lightControlModule.highBeamFault = packet.data[2] and 0x04 == 1
            carState.lightControlModule.frontFogFault = packet.data[2] and 0x08 == 1
            carState.lightControlModule.rearFogFault = packet.data[2] and 0x10 == 1
            carState.lightControlModule.leftTurnSignalFault = packet.data[2] and 0x20 == 1
            carState.lightControlModule.rightTurnSignalFault = packet.data[2] and 0x40 == 1
            carState.lightControlModule.licencePlateFault = packet.data[2] and 0x80 == 1
            carState.lightControlModule.brakeLightRightFault = packet.data[4] and 0x01 == 1
            carState.lightControlModule.brakeLightLeftFault = packet.data[4] and 0x02 == 1
            carState.lightControlModule.rearRightPositionFault = packet.data[4] and 0x04 == 1
            carState.lightControlModule.rearLeftPositionFault = packet.data[4] and 0x08 == 1
            carState.lightControlModule.rightLowBeamFault = packet.data[4] and 0x10 == 1
            carState.lightControlModule.leftLowBeamFault = packet.data[4] and 0x20 == 1
        } else if (packet.data[0] == 0xA0) {
            // Inputs
            carState.lightControlModule.fireExtinguisher = packet.data[1] and 0x02 == 1
            carState.lightControlModule.preHeatingFuelInjection = packet.data[1] and 0x04 == 1
            carState.lightControlModule.carb = packet.data[1] and 0x10 == 1
            carState.lightControlModule.keyInIgnition = packet.data[2] and 0x01 == 1
            carState.lightControlModule.seatBeltsLock = packet.data[2] and 0x02 == 1
            carState.lightControlModule.kfn = packet.data[2] and 0x20 == 1
            carState.lightControlModule.armouredDoor = packet.data[2] and 0x40 == 1
            carState.lightControlModule.brakeFluidLevel = packet.data[2] and 0x80 == 1
            carState.lightControlModule.airSuspension = packet.data[4] and 0x01 == 1
            carState.lightControlModule.holdUpAlarm = packet.data[4] and 0x02 == 1
            carState.lightControlModule.washerFluidLevel = packet.data[4] and 0x04 == 1
            carState.lightControlModule.engineFailSafe = packet.data[4] and 0x40 == 1
            carState.lightControlModule.tyreDefect = packet.data[4] and 0x80 == 1
            carState.lightControlModule.verticalAim = packet.data[7] and 0x02 == 1
            carState.lightControlModule.failsafeMode = packet.data[9] and 0x01 == 1
            carState.lightControlModule.sleepMode = packet.data[9] and 0x40 == 1
            // Outputs
            carState.lightControlModule.rearLeftLicence = packet.data[5] and 0x04 == 1
            carState.lightControlModule.leftBrake = packet.data[5] and 0x08 == 1
            carState.lightControlModule.rightBrake = packet.data[5] and 0x10 == 1
            carState.lightControlModule.rightHighBeam = packet.data[5] and 0x20 == 1
            carState.lightControlModule.leftHighBeam = packet.data[5] and 0x40 == 1
            carState.lightControlModule.frontLeftPosition = packet.data[6] and 0x01 == 1
            carState.lightControlModule.rearLeftInnerPosition = packet.data[6] and 0x02 == 1
            carState.lightControlModule.frontLeftFog = packet.data[6] and 0x04 == 1
            carState.lightControlModule.leftReverse = packet.data[6] and 0x08 == 1
            carState.lightControlModule.leftLowBeam = packet.data[6] and 0x10 == 1
            carState.lightControlModule.rightLowBeam = packet.data[6] and 0x20 == 1
            carState.lightControlModule.frontRightFog = packet.data[6] and 0x40 == 1
            carState.lightControlModule.rearRightFog = packet.data[6] and 0x80 == 1
            carState.lightControlModule.rightLicence = packet.data[7] and 0x04 == 1
            carState.lightControlModule.rearLeftPosition = packet.data[7] and 0x08 == 1
            carState.lightControlModule.centerBrake = packet.data[7] and 0x10 == 1
            carState.lightControlModule.frontRightPosition = packet.data[7] and 0x20 == 1
            carState.lightControlModule.frontRightTurnSignal = packet.data[7] and 0x40 == 1
            carState.lightControlModule.rearLeftTurnSignal = packet.data[7] and 0x80 == 1
            carState.lightControlModule.rearRightTurnSignal = packet.data[8] and 0x02 == 1
            carState.lightControlModule.rearLeftFog = packet.data[8] and 0x04 == 1
            carState.lightControlModule.rearRightInnerPosition = packet.data[8] and 0x08 == 1
            carState.lightControlModule.rearRightPosition = packet.data[8] and 0x10 == 1
            carState.lightControlModule.sideLeftTurnSignal = packet.data[8] and 0x20 == 1
            carState.lightControlModule.frontLeftTurnSignal = packet.data[8] and 0x40 == 1
            carState.lightControlModule.rightReverse = packet.data[8] and 0x80 == 1
            carState.lightControlModule.trailerFog = packet.data[9] and 0x10 == 1
            // Switches
            carState.lightControlModule.hazardSwitch = packet.data[2] and 0x10 == 1
            carState.lightControlModule.highBeamFlashSwitch = packet.data[2] and 0x04 == 1
            carState.lightControlModule.brakeLightSwitch = packet.data[3] and 0x01 == 1
            carState.lightControlModule.highBeamSwitch = packet.data[3] and 0x02 == 1
            carState.lightControlModule.frontFogSwitch = packet.data[3] and 0x08 == 1
            carState.lightControlModule.rearFogSwitch = packet.data[3] and 0x10 == 1
            carState.lightControlModule.positionSwitch = packet.data[3] and 0x20 == 1
            carState.lightControlModule.rightTurnSignalSwitch = packet.data[3] and 0x40 == 1
            carState.lightControlModule.leftTurnSignalSwitch = packet.data[3] and 0x80 == 1
            carState.lightControlModule.lowBeam1Switch = packet.data[4] and 0x02 == 1
            carState.lightControlModule.lowBeam2Switch = packet.data[4] and 0x04 == 1
        } else {
            logger.info("Light Control Module packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseGeneralModulePacket(packet: IBUSPacket) {
        // Example: Extract door status from the packet
        if (packet.data.size >= 2 && packet.data[0] == 0x72) {
            carState.keyFob.keyNumber = packet.data[1] and 0x03
            carState.keyFob.lowBattery = packet.data[1] and 0x01 == 1
            carState.keyFob.lockButtonPressed = packet.data[1] and 0x10 == 1
            carState.keyFob.unlockButtonPressed = packet.data[1] and 0x20 == 1
            carState.keyFob.trunkButtonPressed = packet.data[1] and 0x40 == 1
        } else if (packet.data.size >= 2 && packet.data[0] == 0x76) {

        } else if (packet.data.size >= 2 && packet.data[0] == 0x77) {

        } else if (packet.data.size >= 2 && packet.data[0] == 0x78) {

        } else if (packet.data.size >= 2 && packet.data[0] == 0x7A) {
            carState.generalModule.doorDriver = when (packet.data[1] and 0x01) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
            carState.generalModule.doorPassenger = when (packet.data[1] and 0x02) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
            carState.generalModule.doorRearLeft = when (packet.data[1] and 0x04) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
            carState.generalModule.doorRearRight = when (packet.data[1] and 0x08) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
            carState.generalModule.lockStatus = when (packet.data[1] and 0x20) {
                0x00 -> LockStatus.LOCKED
                0x01 -> LockStatus.UNLOCKED
                else -> LockStatus.LOCKED
            }
            carState.generalModule.windowDriver = when (packet.data[2] and 0x01) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
            carState.generalModule.windowPassenger = when (packet.data[2] and 0x02) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
            carState.generalModule.windowRearLeft = when (packet.data[2] and 0x04) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
            carState.generalModule.windowRearRight = when (packet.data[2] and 0x08) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
            carState.generalModule.sunroof = when (packet.data[2] and 0x10) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
            carState.generalModule.trunk = when (packet.data[2] and 0x20) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
            carState.generalModule.hood = when (packet.data[2] and 0x40) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x7D) {

        } else {
            logger.info("General Module packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseHvacPacket(packet: IBUSPacket) {
        if (packet.data.size >= 2 && packet.data[0] == 0x01) {
            carState.hvac.mode = when (packet.data[1]) {
                0x00 -> HvacMode.OFF
                0x01 -> HvacMode.AUTO
                0x02 -> HvacMode.MANUAL
                else -> HvacMode.OFF
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02) {
            carState.hvac.fanSpeed = when (packet.data[1]) {
                0x00 -> HvacFanSpeed.OFF
                0x01 -> HvacFanSpeed.LOW
                0x02 -> HvacFanSpeed.MEDIUM
                0x03 -> HvacFanSpeed.HIGH
                else -> HvacFanSpeed.OFF
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x03) {
            carState.hvac.temperatureDriver = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x04) {
            carState.hvac.temperaturePassenger = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x05) {
            carState.hvac.airDistribution = when (packet.data[1]) {
                0x00 -> HvacAirDistribution.FACE
                0x01 -> HvacAirDistribution.FEET
                0x02 -> HvacAirDistribution.DEFROST
                else -> HvacAirDistribution.FACE
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x06) {
            carState.hvac.ac = packet.data[1] == 0x01
        } else if (packet.data.size >= 2 && packet.data[0] == 0x07) {
            carState.hvac.recirculate = packet.data[1] == 0x01
        } else if (packet.data.size >= 2 && packet.data[0] == 0x08) {
            carState.hvac.defrost = packet.data[1] == 0x01
        } else {
            logger.info("HVAC packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseMultiFunctionSteeringWheelPacket(packet: IBUSPacket) {
        if (packet.data.size >= 2 && packet.data[0] == 0x01) {
            carState.multiFunctionSteeringWheel.buttonPressed = when (packet.data[1]) {
                0x01 -> SteeringWheelButton.VOLUME_UP
                0x02 -> SteeringWheelButton.VOLUME_DOWN
                0x03 -> SteeringWheelButton.SEEK_UP
                0x04 -> SteeringWheelButton.SEEK_DOWN
                0x05 -> SteeringWheelButton.MODE
                else -> SteeringWheelButton.NONE
            }
            logger.info("Multi Function Steering Wheel button pressed: ${carState.multiFunctionSteeringWheel.buttonPressed}")
        } else {
            logger.info("Multi Function Steering Wheel packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseCdChangerPacket(packet: IBUSPacket) {
        if (packet.data == listOf(0x01)) {
            carState.cdChanger.isAlive = true
            logger.info("CD Changer Alive")
        } else if (packet.data == listOf(0x38, 0x00, 0x00)) {
            logger.info("CD Changer State")
        } else if (packet.data == listOf(0x38, 0x03, 0x00)) {
            carState.cdChanger.isPlaying = true
            carState.cdChanger.isPaused = false
            carState.cdChanger.isStopped = false
        } else if (packet.data == listOf(0x38, 0x02, 0x00)) {
            carState.cdChanger.isPaused = true
            carState.cdChanger.isPlaying = false
            carState.cdChanger.isStopped = false
        } else if (packet.data == listOf(0x38, 0x07, 0x00)) {
            carState.cdChanger.isScanning = false
        } else if (packet.data == listOf(0x38, 0x07, 0x01)) {
            carState.cdChanger.isScanning = true
        } else if (packet.data == listOf(0x38, 0x01, 0x00)) {
            carState.cdChanger.isStopped = true
            carState.cdChanger.isPlaying = false
            carState.cdChanger.isPaused = false
        } else if (packet.data == listOf(0x38, 0x04, 0x01)) {
            logger.info("CD Changer FFWD")
        } else if (packet.data == listOf(0x38, 0x04, 0x00)) {
            logger.info("CD Changer FRWD")
        } else if (packet.data == listOf(0x38, 0x0A, 0x00)) {
            logger.info("CD Changer Next")
        } else if (packet.data == listOf(0x38, 0x0A, 0x01)) {
            logger.info("CD Changer Prev")
        } else if (packet.data.size >= 3 && packet.data[0] == 0x38 && packet.data[1] == 0x06) {
            carState.cdChanger.currentDisc = packet.data[2]
        } else if (packet.data == listOf(0x38, 0x08, 0x00)) {
            carState.cdChanger.isRandom = false
        } else if (packet.data == listOf(0x38, 0x08, 0x01)) {
            carState.cdChanger.isRandom = true
        } else {
            logger.info("CD Changer packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parsePdcPacket(packet: IBUSPacket) {
        if (packet.data[0] == 0x07) {
            carState.pdc.values = packet.data.drop(1).map { it }
        } else if (packet.data[0] == 0x90) {
            carState.pdc.isTurnedOn = true
            logger.info("PDC turned on")
        } else if (packet.data[0] == 0xA0) {
            carState.pdc.values = packet.data.drop(1).map { it }
            logger.info("PDC value reply: ${carState.pdc.values.joinToString(" ")}")
        } else {
            logger.info("PDC packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseBmButtonPacket(packet: IBUSPacket) {
        if (packet.data == listOf(0x48, 0x40)) {
            carState.bmButton.nextHold = true
        } else if (packet.data == listOf(0x48, 0x80)) {
            carState.bmButton.nextRel = true
        } else if (packet.data == listOf(0x48, 0x50)) {
            carState.bmButton.prevHold = true
        } else if (packet.data == listOf(0x48, 0x90)) {
            carState.bmButton.prevRel = true
        } else if (packet.data == listOf(0x48, 0x54)) {
            carState.bmButton.reverseHold = true
        } else if (packet.data == listOf(0x48, 0x94)) {
            carState.bmButton.reverseRel = true
        } else if (packet.data == listOf(0x48, 0x33)) {
            carState.bmButton.dolbyPres = true
        } else if (packet.data == listOf(0x48, 0x73)) {
            carState.bmButton.dolbyHold = true
        } else if (packet.data == listOf(0x48, 0xB3)) {
            carState.bmButton.dolbyRel = true
        } else if (packet.data == listOf(0x48, 0x22)) {
            carState.bmButton.rdsPres = true
        } else if (packet.data == listOf(0x48, 0x62)) {
            carState.bmButton.rdsHold = true
        } else if (packet.data == listOf(0x48, 0xA2)) {
            carState.bmButton.rdsRel = true
        } else if (packet.data == listOf(0x48, 0x24)) {
            carState.bmButton.ejectPres = true
        } else if (packet.data == listOf(0x48, 0x64)) {
            carState.bmButton.ejectHold = true
        } else if (packet.data == listOf(0x48, 0xA4)) {
            carState.bmButton.ejectRel = true
        } else if (packet.data == listOf(0x48, 0x04)) {
            carState.bmButton.tonePres = true
        } else if (packet.data == listOf(0x48, 0x44)) {
            carState.bmButton.toneHold = true
        } else if (packet.data == listOf(0x48, 0x84)) {
            carState.bmButton.toneRel = true
        } else if (packet.data == listOf(0x48, 0x20)) {
            carState.bmButton.selectPres = true
        } else if (packet.data == listOf(0x48, 0x60)) {
            carState.bmButton.selectHold = true
        } else if (packet.data == listOf(0x48, 0xA0)) {
            carState.bmButton.selectRel = true
        } else if (packet.data == listOf(0x48, 0x51)) {
            carState.bmButton.button1Hold = true
        } else if (packet.data == listOf(0x48, 0x91)) {
            carState.bmButton.button1Rel = true
        } else if (packet.data == listOf(0x48, 0x41)) {
            carState.bmButton.button2Hold = true
        } else if (packet.data == listOf(0x48, 0x81)) {
            carState.bmButton.button2Rel = true
        } else if (packet.data == listOf(0x48, 0x52)) {
            carState.bmButton.button3Hold = true
        } else if (packet.data == listOf(0x48, 0x92)) {
            carState.bmButton.button3Rel = true
        } else if (packet.data == listOf(0x48, 0x42)) {
            carState.bmButton.button4Hold = true
        } else if (packet.data == listOf(0x48, 0x82)) {
            carState.bmButton.button4Rel = true
        } else if (packet.data == listOf(0x48, 0x53)) {
            carState.bmButton.button5Hold = true
        } else if (packet.data == listOf(0x48, 0x93)) {
            carState.bmButton.button5Rel = true
        } else if (packet.data == listOf(0x48, 0x43)) {
            carState.bmButton.button6Hold = true
        } else if (packet.data == listOf(0x48, 0x83)) {
            carState.bmButton.button6Rel = true
        } else if (packet.data.size >= 2 && packet.data[0] == 0x32 && (packet.data[1] and 0x0F) == 0x01) {
            carState.bmButton.volRight = (packet.data[1] shr 4) and 0x0F
            logger.info("BM_VOL_KNOB_RIGHT -steps: ${carState.bmButton.volRight}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x32 && (packet.data[1] and 0x0F) == 0x00) {
            carState.bmButton.volLeft = (packet.data[1] shr 4) and 0x0F
            logger.info("BM_VOL_KNOB_LEFT -steps: ${carState.bmButton.volLeft}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x32 && (packet.data[1] and 0x0F) == 0x02) {
            carState.bmButton.volRel = true
        } else if (packet.data.size >= 2 && packet.data[0] == 0x32 && (packet.data[1] and 0x0F) == 0x03) {
            carState.bmButton.volHold = true
        } else if (packet.data == listOf(0x48, 0x00)) {
            carState.bmButton.modePres = true
        } else if (packet.data == listOf(0x48, 0x40)) {
            carState.bmButton.modeHold = true
        } else if (packet.data == listOf(0x48, 0x80)) {
            carState.bmButton.modeRel = true
        } else if (packet.data == listOf(0x48, 0x01)) {
            carState.bmButton.fmPres = true
        } else if (packet.data == listOf(0x48, 0x41)) {
            carState.bmButton.fmHold = true
        } else if (packet.data == listOf(0x48, 0x81)) {
            carState.bmButton.fmRel = true
        } else if (packet.data == listOf(0x48, 0x02)) {
            carState.bmButton.amPres = true
        } else if (packet.data == listOf(0x48, 0x42)) {
            carState.bmButton.amHold = true
        } else if (packet.data == listOf(0x48, 0x82)) {
            carState.bmButton.amRel = true
        } else if (packet.data == listOf(0x48, 0x03)) {
            carState.bmButton.screenPres = true
        } else if (packet.data == listOf(0x48, 0x43)) {
            carState.bmButton.screenHold = true
        } else if (packet.data == listOf(0x48, 0x83)) {
            carState.bmButton.screenRel = true
        } else {
            logger.info("BM Button packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseStwButtonPacket(packet: IBUSPacket) {
        when (packet.data) {
            listOf(0x01) -> {
                carState.stwButton.volUp = true
            }
            listOf(0x02) -> {
                carState.stwButton.volDown = true
            }
            listOf(0x03) -> {
                carState.stwButton.upPres = true
            }
            listOf(0x04) -> {
                carState.stwButton.upHold = true
            }
            listOf(0x05) -> {
                carState.stwButton.upRel = true
            }
            listOf(0x06) -> {
                carState.stwButton.downPres = true
            }
            listOf(0x07) -> {
                carState.stwButton.downHold = true
            }
            listOf(0x08) -> {
                carState.stwButton.downRel = true
            }
            listOf(0x09) -> {
                carState.stwButton.rtHold = true
            }
            listOf(0x0A) -> {
                carState.stwButton.rtRel = true
            }
            listOf(0x0B) -> {
                carState.stwButton.rtOn = true
            }
            listOf(0x0C) -> {
                carState.stwButton.rtOff = true
            }
            listOf(0x0D) -> {
                carState.stwButton.speakPres = true
            }
            listOf(0x0E) -> {
                carState.stwButton.speakHold = true
            }
            listOf(0x0F) -> {
                carState.stwButton.speakRel = true
            }
            else -> {
                logger.info("STW packet: ${printIntListAsHex(packet.raw)}")
            }
        }
    }

    private fun parseGtPacketExtended(packet: IBUSPacket) {
        if (packet.data == listOf(0x02, 0x00)) {
            carState.gt.mode = GtMode.CD
        } else if (packet.data == listOf(0x02, 0x01)) {
            carState.gt.mode = GtMode.CIS
        } else if (packet.data == listOf(0x02, 0x02)) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data == listOf(0x02, 0x03)) {
            carState.gt.mode = GtMode.AUX
        } else if (packet.data == listOf(0x01, 0x00)) {
            carState.gt.isTvOn = true
        } else if (packet.data == listOf(0x01, 0x01)) {
            carState.gt.isTvOn = false
        } else if (packet.data == listOf(0xA5, 0x62, 0x01, 0x41, 0x43, 0x44, 0x37)) {
            carState.gt.mode = GtMode.CD
        } else if (packet.data.size >= 7 && packet.data.subList(0, 7) == listOf(0xA5, 0x62, 0x01, 0x41, 0x43, 0x44, 0x37)) {
            carState.gt.mode = GtMode.CD
        } else if (packet.data.size >= 7 && packet.data.subList(0, 7) == listOf(0xA5, 0x62, 0x01, 0x41, 0x43, 0x44, 0x36)) {
            carState.gt.mode = GtMode.CIS
        } else if (packet.data == listOf(0xA5, 0x62, 0x01, 0x41, 0x43, 0x44)) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data.size >= 9 && packet.data.subList(0, 9) == listOf(0x23, 0xC4, 0x20, 0x43, 0x44, 0x20, 0x37, 0x2D, 0x39)) {
            carState.gt.mode = GtMode.CD
        } else if (packet.data.size >= 7 && packet.data.subList(0, 7) == listOf(0x23, 0x50, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data.size >= 10 && packet.data.subList(0, 10) == listOf(0x23, 0x50, 0x20, 0x4E, 0x4F, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data.size >= 3 && (packet.data.subList(0, 3) == listOf(0x23, 0x40, 0x20) || packet.data.subList(0, 3) == listOf(0x23, 0x50, 0x20))) {
            carState.gt.mode = GtMode.RADIO
        } else if (packet.data.size >= 18 && packet.data.subList(0, 18) == listOf(0x23, 0x62, 0x30, 0x20, 0x20, 0x07, 0x20, 0x20, 0x20, 0x20, 0x20, 0x08, 0x43, 0x44, 0x20, 0x37, 0x2D, 0x39)) {
            carState.gt.mode = GtMode.CD
        } else if (packet.data.size >= 9 && packet.data.subList(0, 9) == listOf(0x23, 0xC4, 0x30, 0x43, 0x44, 0x20, 0x37, 0x2D, 0x39)) {
            carState.gt.mode = GtMode.CD
        } else if (packet.data.size >= 9 && packet.data.subList(0, 9) == listOf(0x23, 0xCB, 0x20, 0x43, 0x44, 0x20, 0x37, 0x2D, 0x39)) {
            carState.gt.mode = GtMode.CD
        } else if (packet.data.size >= 10 && packet.data.subList(0, 10) == listOf(0x23, 0xC2, 0x30, 0x4E, 0x4F, 0x20, 0x44, 0x49, 0x53, 0x43)) {
            carState.gt.mode = GtMode.CD
        } else if (packet.data.size >= 16 && packet.data.subList(0, 16) == listOf(0x23, 0x62, 0x30, 0x20, 0x20, 0x07, 0x20, 0x20, 0x20, 0x20, 0x20, 0x08, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data.size >= 7 && packet.data.subList(0, 7) == listOf(0x23, 0x40, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data.size >= 19 && packet.data.subList(0, 19) == listOf(0x23, 0x62, 0x30, 0x20, 0x20, 0x07, 0x20, 0x20, 0x20, 0x20, 0x20, 0x08, 0x4E, 0x4F, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data.size >= 10 && packet.data.subList(0, 10) == listOf(0x23, 0x40, 0x20, 0x4E, 0x4F, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data.size >= 7 && (packet.data.subList(0, 7) == listOf(0x23, 0x82, 0x30, 0x54, 0x41, 0x50, 0x45) || packet.data.subList(0, 7) == listOf(0x23, 0x92, 0x30, 0x54, 0x41, 0x50, 0x45))) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data.size >= 10 && packet.data.subList(0, 10) == listOf(0x23, 0x40, 0x30, 0x4E, 0x4F, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data.size >= 19 && packet.data.subList(0, 19) == listOf(0x23, 0x62, 0x30, 0x07, 0x20, 0x20, 0x20, 0x20, 0x20, 0x08, 0x20, 0x20, 0x4E, 0x4F, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
        } else if (packet.data.size >= 5 && (packet.data.subList(0, 5) == listOf(0x23, 0x40, 0x30, 0x46, 0x4D) || packet.data.subList(0, 5) == listOf(0x23, 0x48, 0x30, 0x46, 0x4D) || packet.data.subList(0, 5) == listOf(0x23, 0x40, 0x20, 0x46, 0x4D))) {
            carState.gt.mode = GtMode.RADIO
        } else if (packet.data.size >= 5 && packet.data.subList(0, 5) == listOf(0x23, 0x40, 0x20, 0x41, 0x4D)) {
            carState.gt.mode = GtMode.RADIO
        } else if (packet.data.size >= 5 && packet.data.subList(0, 5) == listOf(0x23, 0x40, 0x30, 0x4D, 0x57)) {
            carState.gt.mode = GtMode.RADIO
        } else if (packet.data.size >= 5 && packet.data.subList(0, 5) == listOf(0x23, 0x40, 0x30, 0x4C, 0x57)) {
            carState.gt.mode = GtMode.RADIO
        } else if (packet.data.size >= 6 && packet.data.subList(0, 6) == listOf(0x23, 0xC4, 0x30, 0x43, 0x44, 0x20) && packet.data.size >= 8 && packet.data[7] == 0x2D && carState.gt.modebm23) {
            carState.gt.mode = GtMode.CD
        } else {
            logger.info("GT Extended packet: ${printIntListAsHex(packet.raw)}")
            // TODO: Implement the remaining logic from the Python 'manage' method:
            // - Handling of TUNER_MODES, TP_MESSAGES, NAV menus, TONE menus, etc.
            // - Handling of BM_NAV_KNOB, MID buttons, STW_SPEAK, NAV position/location, RADIO LED, OBC, PHONE, EWS, DSP, etc.
            // - This will likely involve creating new methods and potentially new data structures in CarState.
            // - Example:
            // if (packet.data in carState.TUNER_MODES) { ... }
            // if (packet.data in carState.TP_MESSAGES) { ... }
            // if (packet.data == listOf(0x46, 0x01)) { ... }
            // ... and so on
        }
    }

    private fun compareCarStates(oldState: CarState, newState: CarState): String {
        val changes = StringBuilder()
        compareRadioState(oldState.radio, newState.radio, changes)
        compareInstrumentClusterState(oldState.instrumentCluster, newState.instrumentCluster, changes)
        compareLightControlModuleState(oldState.lightControlModule, newState.lightControlModule, changes)
        compareGeneralModuleState(oldState.generalModule, newState.generalModule, changes)
        compareHvacState(oldState.hvac, newState.hvac, changes)
        compareMultiFunctionSteeringWheelState(oldState.multiFunctionSteeringWheel, newState.multiFunctionSteeringWheel, changes)
        compareCdChangerState(oldState.cdChanger, newState.cdChanger, changes)
        comparePdcState(oldState.pdc, newState.pdc, changes)
        compareBmButtonState(oldState.bmButton, newState.bmButton, changes)
        compareStwButtonState(oldState.stwButton, newState.stwButton, changes)
        compareGtState(oldState.gt, newState.gt, changes)
        compareKeyfobState(oldState.keyFob, newState.keyFob, changes)
        return changes.toString()
    }

    private fun compareKeyfobState(oldState: KeyfobState, newState: KeyfobState, changes: StringBuilder) {
        if (oldState.lowBattery != newState.lowBattery) changes.appendLine("  Keyfob Low Battery: ${oldState.lowBattery} -> ${newState.lowBattery}")
        if (oldState.lockButtonPressed != newState.lockButtonPressed) changes.appendLine("  Keyfob Lock Button Pressed: ${oldState.lockButtonPressed} -> ${newState.lockButtonPressed}")
        if (oldState.unlockButtonPressed != newState.unlockButtonPressed) changes.appendLine("  Keyfob Unlock Button Pressed: ${oldState.unlockButtonPressed} -> ${newState.unlockButtonPressed}")
        if (oldState.trunkButtonPressed != newState.trunkButtonPressed) changes.appendLine("  Keyfob Trunk Button Pressed: ${oldState.trunkButtonPressed} -> ${newState.trunkButtonPressed}")
        if (oldState.keyNumber != newState.keyNumber) changes.appendLine("  Keyfob Key Number: ${oldState.keyNumber} -> ${newState.keyNumber}")
    }

    private fun compareRadioState(oldState: RadioState, newState: RadioState, changes: StringBuilder) {
        if (oldState.power != newState.power) changes.appendLine("  Radio Power: ${oldState.power} -> ${newState.power}")
        if (oldState.band != newState.band) changes.appendLine("  Radio Band: ${oldState.band} -> ${newState.band}")
        if (oldState.frequency != newState.frequency) changes.appendLine("  Radio Frequency: ${oldState.frequency} -> ${newState.frequency}")
        if (oldState.volume != newState.volume) changes.appendLine("  Radio Volume: ${oldState.volume} -> ${newState.volume}")
        if (oldState.stationName != newState.stationName) changes.appendLine("  Radio Station Name: ${oldState.stationName} -> ${newState.stationName}")
        if (oldState.mute != newState.mute) changes.appendLine("  Radio Mute: ${oldState.mute} -> ${newState.mute}")
        if (oldState.cdChangerTrack != newState.cdChangerTrack) changes.appendLine("  Radio CD Changer Track: ${oldState.cdChangerTrack} -> ${newState.cdChangerTrack}")
        if (oldState.cdChangerDisc != newState.cdChangerDisc) changes.appendLine("  Radio CD Changer Disc: ${oldState.cdChangerDisc} -> ${newState.cdChangerDisc}")
    }

    private fun compareInstrumentClusterState(oldState: InstrumentClusterState, newState: InstrumentClusterState, changes: StringBuilder) {
        if (oldState.speed != newState.speed) changes.appendLine("  Instrument Cluster Speed: ${oldState.speed} -> ${newState.speed}")
        if (oldState.rpm != newState.rpm) changes.appendLine("  Instrument Cluster RPM: ${oldState.rpm} -> ${newState.rpm}")
        if (oldState.fuelLevel != newState.fuelLevel) changes.appendLine("  Instrument Cluster Fuel Level: ${oldState.fuelLevel} -> ${newState.fuelLevel}")
        if (oldState.coolantTemp != newState.coolantTemp) changes.appendLine("  Instrument Cluster Coolant Temp: ${oldState.coolantTemp} -> ${newState.coolantTemp}")
        if (oldState.outsideTemp != newState.outsideTemp) changes.appendLine("  Instrument Cluster Outside Temp: ${oldState.outsideTemp} -> ${newState.outsideTemp}")
        if (oldState.odometer != newState.odometer) changes.appendLine("  Instrument Cluster Odometer: ${oldState.odometer} -> ${newState.odometer}")
        if (oldState.tripOdometer != newState.tripOdometer) changes.appendLine("  Instrument Cluster Trip Odometer: ${oldState.tripOdometer} -> ${newState.tripOdometer}")
        if (oldState.gear != newState.gear) changes.appendLine("  Instrument Cluster Gear: ${oldState.gear} -> ${newState.gear}")
        if (oldState.engineRunning != newState.engineRunning) changes.appendLine("  Instrument Cluster Gear: ${oldState.engineRunning} -> ${newState.engineRunning}")
        if (oldState.handbrakeOn != newState.handbrakeOn) changes.appendLine("  Instrument Cluster Gear: ${oldState.handbrakeOn} -> ${newState.handbrakeOn}")
        if (oldState.vin != newState.vin) changes.appendLine("  Instrument Cluster VIN: ${oldState.vin} -> ${newState.vin}")
        if (oldState.countryCoding != newState.countryCoding) changes.appendLine("  Instrument Cluster Country Coding: ${oldState.countryCoding} -> ${newState.countryCoding}")
        if (oldState.ignitionState != newState.ignitionState) changes.appendLine("  Instrument Cluster Ignition State: ${oldState.ignitionState} -> ${newState.ignitionState}")
        if (oldState.serviceInterval != newState.serviceInterval) changes.appendLine("  Instrument Cluster Service Interval: ${oldState.serviceInterval} -> ${newState.serviceInterval}")
        if (oldState.serviceInterval != newState.serviceInterval) changes.appendLine("  Instrument Cluster Service Interval Days: ${oldState.serviceIntervalDays} -> ${newState.serviceIntervalDays}")
        if (oldState.serviceInterval != newState.serviceInterval) changes.appendLine("  Instrument Cluster Service Interval Type: ${oldState.serviceIntervalType} -> ${newState.serviceIntervalType}")
        if (oldState.checkControlMessages != newState.checkControlMessages) changes.appendLine("  Instrument Cluster Check Control Messages: ${oldState.checkControlMessages} -> ${newState.checkControlMessages}")
    }

    private fun compareLightControlModuleState(oldState: LightControlModuleState, newState: LightControlModuleState, changes: StringBuilder) {
        // Vehicle Data
        if (oldState.vin != newState.vin) changes.appendLine("  Light Control Module VIN: ${oldState.vin} -> ${newState.vin}")
        if (oldState.odometer != newState.odometer) changes.appendLine("  Light Control Module Odometer: ${oldState.odometer} -> ${newState.odometer}")
        if (oldState.sinceServiceDays != newState.sinceServiceDays) changes.appendLine("  Light Control Module Since Service Days: ${oldState.sinceServiceDays} -> ${newState.sinceServiceDays}")
        if (oldState.sinceServiceLiters != newState.sinceServiceLiters) changes.appendLine("  Light Control Module Since Service Liters: ${oldState.sinceServiceLiters} -> ${newState.sinceServiceLiters}")
        // Lights
        if (oldState.frontPositionLights != newState.frontPositionLights) changes.appendLine("  Light Control Module Front Position Lights: ${oldState.frontPositionLights} -> ${newState.frontPositionLights}")
        if (oldState.lowBeamLights != newState.lowBeamLights) changes.appendLine("  Light Control Module Low Beam Lights: ${oldState.lowBeamLights} -> ${newState.lowBeamLights}")
        if (oldState.highBeamLights != newState.highBeamLights) changes.appendLine("  Light Control Module High Beam Lights: ${oldState.highBeamLights} -> ${newState.highBeamLights}")
        if (oldState.frontFogLights != newState.frontFogLights) changes.appendLine("  Light Control Module Front Fog Lights: ${oldState.frontFogLights} -> ${newState.frontFogLights}")
        if (oldState.rearFogLights != newState.rearFogLights) changes.appendLine("  Light Control Module Rear Fog Lights: ${oldState.rearFogLights} -> ${newState.rearFogLights}")
        if (oldState.leftTurnSignal != newState.leftTurnSignal) changes.appendLine("  Light Control Module Left Turn Signal: ${oldState.leftTurnSignal} -> ${newState.leftTurnSignal}")
        if (oldState.rightTurnSignal != newState.rightTurnSignal) changes.appendLine("  Light Control Module Right Turn Signal: ${oldState.rightTurnSignal} -> ${newState.rightTurnSignal}")
        if (oldState.turnSignalFast != newState.turnSignalFast) changes.appendLine("  Light Control Module Turn Signal Fast: ${oldState.turnSignalFast} -> ${newState.turnSignalFast}")
        if (oldState.brakeLights != newState.brakeLights) changes.appendLine("  Light Control Module Brake Lights: ${oldState.brakeLights} -> ${newState.brakeLights}")
        if (oldState.turnSignalSync != newState.turnSignalSync) changes.appendLine("  Light Control Module Turn Signal Sync: ${oldState.turnSignalSync} -> ${newState.turnSignalSync}")
        if (oldState.rearPositionLights != newState.rearPositionLights) changes.appendLine("  Light Control Module Rear Position Lights: ${oldState.rearPositionLights} -> ${newState.rearPositionLights}")
        if (oldState.trailerPositionLights != newState.trailerPositionLights) changes.appendLine("  Light Control Module Trailer Position Lights: ${oldState.trailerPositionLights} -> ${newState.trailerPositionLights}")
        if (oldState.reverseLights != newState.reverseLights) changes.appendLine("  Light Control Module Reverse Lights: ${oldState.reverseLights} -> ${newState.reverseLights}")
        if (oldState.trailerReverseLights != newState.trailerReverseLights) changes.appendLine("  Light Control Module Trailer Reverse Lights: ${oldState.trailerReverseLights} -> ${newState.trailerReverseLights}")
        if (oldState.hazardLights != newState.hazardLights) changes.appendLine("  Light Control Module Hazard Lights: ${oldState.hazardLights} -> ${newState.hazardLights}")
        // Faults
        if (oldState.frontPositionFault != newState.frontPositionFault) changes.appendLine("  Light Control Module Front Position Fault: ${oldState.frontPositionFault} -> ${newState.frontPositionFault}")
        if (oldState.lowBeamFault != newState.lowBeamFault) changes.appendLine("  Light Control Module Low Beam Fault: ${oldState.lowBeamFault} -> ${newState.lowBeamFault}")
        if (oldState.highBeamFault != newState.highBeamFault) changes.appendLine("  Light Control Module High Beam Fault: ${oldState.highBeamFault} -> ${newState.highBeamFault}")
        if (oldState.frontFogFault != newState.frontFogFault) changes.appendLine("  Light Control Module Front Fog Fault: ${oldState.frontFogFault} -> ${newState.frontFogFault}")
        if (oldState.rearFogFault != newState.rearFogFault) changes.appendLine("  Light Control Module Rear Fog Fault: ${oldState.rearFogFault} -> ${newState.rearFogFault}")
        if (oldState.leftTurnSignalFault != newState.leftTurnSignalFault) changes.appendLine("  Light Control Module Left Turn Signal Fault: ${oldState.leftTurnSignalFault} -> ${newState.leftTurnSignalFault}")
        if (oldState.rightTurnSignalFault != newState.rightTurnSignalFault) changes.appendLine("  Light Control Module Right Turn Signal Fault: ${oldState.rightTurnSignalFault} -> ${newState.rightTurnSignalFault}")
        if (oldState.licencePlateFault != newState.licencePlateFault) changes.appendLine("  Light Control Module Licence Plate Fault: ${oldState.licencePlateFault} -> ${newState.licencePlateFault}")
        if (oldState.brakeLightRightFault != newState.brakeLightRightFault) changes.appendLine("  Light Control Module Brake Light Right Fault: ${oldState.brakeLightRightFault} -> ${newState.brakeLightRightFault}")
        if (oldState.brakeLightLeftFault != newState.brakeLightLeftFault) changes.appendLine("  Light Control Module Brake Light Left Fault: ${oldState.brakeLightLeftFault} -> ${newState.brakeLightLeftFault}")
        if (oldState.rearRightPositionFault != newState.rearRightPositionFault) changes.appendLine("  Light Control Module Rear Right Position Fault: ${oldState.rearRightPositionFault} -> ${newState.rearRightPositionFault}")
        if (oldState.rearLeftPositionFault != newState.rearLeftPositionFault) changes.appendLine("  Light Control Module Rear Left Position Fault: ${oldState.rearLeftPositionFault} -> ${newState.rearLeftPositionFault}")
        if (oldState.rightLowBeamFault != newState.rightLowBeamFault) changes.appendLine("  Light Control Module Right Low Beam Fault: ${oldState.rightLowBeamFault} -> ${newState.rightLowBeamFault}")
        if (oldState.leftLowBeamFault != newState.leftLowBeamFault) changes.appendLine("  Light Control Module Left Low Beam Fault: ${oldState.leftLowBeamFault} -> ${newState.leftLowBeamFault}")
        // Switches
        if (oldState.positionSwitch != newState.positionSwitch) changes.appendLine("  Light Control Module Position Switch: ${oldState.positionSwitch} -> ${newState.positionSwitch}")
        if (oldState.lowBeam1Switch != newState.lowBeam1Switch) changes.appendLine("  Light Control Module Low Beam 1 Switch: ${oldState.lowBeam1Switch} -> ${newState.lowBeam1Switch}")
        if (oldState.lowBeam2Switch != newState.lowBeam2Switch) changes.appendLine("  Light Control Module Low Beam 2 Switch: ${oldState.lowBeam2Switch} -> ${newState.lowBeam2Switch}")
        if (oldState.highBeamSwitch != newState.highBeamSwitch) changes.appendLine("  Light Control Module High Beam Switch: ${oldState.highBeamSwitch} -> ${newState.highBeamSwitch}")
        if (oldState.frontFogSwitch != newState.frontFogSwitch) changes.appendLine("  Light Control Module Front Fog Switch: ${oldState.frontFogSwitch} -> ${newState.frontFogSwitch}")
        if (oldState.rearFogSwitch != newState.rearFogSwitch) changes.appendLine("  Light Control Module Rear Fog Switch: ${oldState.rearFogSwitch} -> ${newState.rearFogSwitch}")
        if (oldState.leftTurnSignalSwitch != newState.leftTurnSignalSwitch) changes.appendLine("  Light Control Module Left Turn Signal Switch: ${oldState.leftTurnSignalSwitch} -> ${newState.leftTurnSignalSwitch}")
        if (oldState.rightTurnSignalSwitch != newState.rightTurnSignalSwitch) changes.appendLine("  Light Control Module Right Turn Signal Switch: ${oldState.rightTurnSignalSwitch} -> ${newState.rightTurnSignalSwitch}")
        if (oldState.brakeLightSwitch != newState.brakeLightSwitch) changes.appendLine("  Light Control Module Brake Light Switch: ${oldState.brakeLightSwitch} -> ${newState.brakeLightSwitch}")
        if (oldState.highBeamFlashSwitch != newState.highBeamFlashSwitch) changes.appendLine("  Light Control Module High Beam Flash Switch: ${oldState.highBeamFlashSwitch} -> ${newState.highBeamFlashSwitch}")
        if (oldState.hazardSwitch != newState.hazardSwitch) changes.appendLine("  Light Control Module Hazard Switch: ${oldState.hazardSwitch} -> ${newState.hazardSwitch}")
        // Inputs
        if (oldState.fireExtinguisher != newState.fireExtinguisher) changes.appendLine("  Light Control Module Fire Extinguisher: ${oldState.fireExtinguisher} -> ${newState.fireExtinguisher}")
        if (oldState.preHeatingFuelInjection != newState.preHeatingFuelInjection) changes.appendLine("  Light Control Module Pre-Heating Fuel Injection: ${oldState.preHeatingFuelInjection} -> ${newState.preHeatingFuelInjection}")
        if (oldState.carb != newState.carb) changes.appendLine("  Light Control Module Carb: ${oldState.carb} -> ${newState.carb}")
        if (oldState.keyInIgnition != newState.keyInIgnition) changes.appendLine("  Light Control Module Key In Ignition: ${oldState.keyInIgnition} -> ${newState.keyInIgnition}")
        if (oldState.seatBeltsLock != newState.seatBeltsLock) changes.appendLine("  Light Control Module Seat Belts Lock: ${oldState.seatBeltsLock} -> ${newState.seatBeltsLock}")
        if (oldState.kfn != newState.kfn) changes.appendLine("  Light Control Module KFN: ${oldState.kfn} -> ${newState.kfn}")
        if (oldState.armouredDoor != newState.armouredDoor) changes.appendLine("  Light Control Module Armoured Door: ${oldState.armouredDoor} -> ${newState.armouredDoor}")
        if (oldState.brakeFluidLevel != newState.brakeFluidLevel) changes.appendLine("  Light Control Module Brake Fluid Level: ${oldState.brakeFluidLevel} -> ${newState.brakeFluidLevel}")
        if (oldState.airSuspension != newState.airSuspension) changes.appendLine("  Light Control Module Air Suspension: ${oldState.airSuspension} -> ${newState.airSuspension}")
        if (oldState.holdUpAlarm != newState.holdUpAlarm) changes.appendLine("  Light Control Module Hold Up Alarm: ${oldState.holdUpAlarm} -> ${newState.holdUpAlarm}")
        if (oldState.washerFluidLevel != newState.washerFluidLevel) changes.appendLine("  Light Control Module Washer Fluid Level: ${oldState.washerFluidLevel} -> ${newState.washerFluidLevel}")
        if (oldState.engineFailSafe != newState.engineFailSafe) changes.appendLine("  Light Control Module Engine Fail Safe: ${oldState.engineFailSafe} -> ${newState.engineFailSafe}")
        if (oldState.tyreDefect != newState.tyreDefect) changes.appendLine("  Light Control Module Tyre Defect: ${oldState.tyreDefect} -> ${newState.tyreDefect}")
        if (oldState.verticalAim != newState.verticalAim) changes.appendLine("  Light Control Module Vertical Aim: ${oldState.verticalAim} -> ${newState.verticalAim}")
        if (oldState.failsafeMode != newState.failsafeMode) changes.appendLine("  Light Control Module Failsafe Mode: ${oldState.failsafeMode} -> ${newState.failsafeMode}")
        if (oldState.sleepMode != newState.sleepMode) changes.appendLine("  Light Control Module Sleep Mode: ${oldState.sleepMode} -> ${newState.sleepMode}")
        // Outputs
        if (oldState.frontLeftPosition != newState.frontLeftPosition) changes.appendLine("  Light Control Module Front Left Position: ${oldState.frontLeftPosition} -> ${newState.frontLeftPosition}")
        if (oldState.frontRightPosition != newState.frontRightPosition) changes.appendLine("  Light Control Module Front Right Position: ${oldState.frontRightPosition} -> ${newState.frontRightPosition}")
        if (oldState.leftLowBeam != newState.leftLowBeam) changes.appendLine("  Light Control Module Left Low Beam: ${oldState.leftLowBeam} -> ${newState.leftLowBeam}")
        if (oldState.rightLowBeam != newState.rightLowBeam) changes.appendLine("  Light Control Module Right Low Beam: ${oldState.rightLowBeam} -> ${newState.rightLowBeam}")
        if (oldState.leftHighBeam != newState.leftHighBeam) changes.appendLine("  Light Control Module Left High Beam: ${oldState.leftHighBeam} -> ${newState.leftHighBeam}")
        if (oldState.rightHighBeam != newState.rightHighBeam) changes.appendLine("  Light Control Module Right High Beam: ${oldState.rightHighBeam} -> ${newState.rightHighBeam}")
        if (oldState.frontLeftFog != newState.frontLeftFog) changes.appendLine("  Light Control Module Front Left Fog: ${oldState.frontLeftFog} -> ${newState.frontLeftFog}")
        if (oldState.frontRightFog != newState.frontRightFog) changes.appendLine("  Light Control Module Front Right Fog: ${oldState.frontRightFog} -> ${newState.frontRightFog}")
        if (oldState.frontLeftTurnSignal != newState.frontLeftTurnSignal) changes.appendLine("  Light Control Module Front Left Turn Signal: ${oldState.frontLeftTurnSignal} -> ${newState.frontLeftTurnSignal}")
        if (oldState.frontRightTurnSignal != newState.frontRightTurnSignal) changes.appendLine("  Light Control Module Front Right Turn Signal: ${oldState.frontRightTurnSignal} -> ${newState.frontRightTurnSignal}")
        if (oldState.rearLeftPosition != newState.rearLeftPosition) changes.appendLine("  Light Control Module Rear Left Position: ${oldState.rearLeftPosition} -> ${newState.rearLeftPosition}")
        if (oldState.rearRightPosition != newState.rearRightPosition) changes.appendLine("  Light Control Module Rear Right Position: ${oldState.rearRightPosition} -> ${newState.rearRightPosition}")
        if (oldState.rearLeftInnerPosition != newState.rearLeftInnerPosition) changes.appendLine("  Light Control Module Rear Left Inner Position: ${oldState.rearLeftInnerPosition} -> ${newState.rearLeftInnerPosition}")
        if (oldState.rearRightInnerPosition != newState.rearRightInnerPosition) changes.appendLine("  Light Control Module Rear Right Inner Position: ${oldState.rearRightInnerPosition} -> ${newState.rearRightInnerPosition}")
        if (oldState.rearLeftFog != newState.rearLeftFog) changes.appendLine("  Light Control Module Rear Left Fog: ${oldState.rearLeftFog} -> ${newState.rearLeftFog}")
        if (oldState.rearRightFog != newState.rearRightFog) changes.appendLine("  Light Control Module Rear Right Fog: ${oldState.rearRightFog} -> ${newState.rearRightFog}")
        if (oldState.leftReverse != newState.leftReverse) changes.appendLine("  Light Control Module Left Reverse: ${oldState.leftReverse} -> ${newState.leftReverse}")
        if (oldState.rightReverse != newState.rightReverse) changes.appendLine("  Light Control Module Right Reverse: ${oldState.rightReverse} -> ${newState.rightReverse}")
        if (oldState.rearLeftTurnSignal != newState.rearLeftTurnSignal) changes.appendLine("  Light Control Module Rear Left Turn Signal: ${oldState.rearLeftTurnSignal} -> ${newState.rearLeftTurnSignal}")
        if (oldState.rearRightTurnSignal != newState.rearRightTurnSignal) changes.appendLine("  Light Control Module Rear Right Turn Signal: ${oldState.rearRightTurnSignal} -> ${newState.rearRightTurnSignal}")
        if (oldState.leftBrake != newState.leftBrake) changes.appendLine("  Light Control Module Left Brake: ${oldState.leftBrake} -> ${newState.leftBrake}")
        if (oldState.rightBrake != newState.rightBrake) changes.appendLine("  Light Control Module Right Brake: ${oldState.rightBrake} -> ${newState.rightBrake}")
        if (oldState.centerBrake != newState.centerBrake) changes.appendLine("  Light Control Module Center Brake: ${oldState.centerBrake} -> ${newState.centerBrake}")
        if (oldState.rearLeftLicence != newState.rearLeftLicence) changes.appendLine("  Light Control Module Rear Left Licence: ${oldState.rearLeftLicence} -> ${newState.rearLeftLicence}")
        if (oldState.rightLicence != newState.rightLicence) changes.appendLine("  Light Control Module Right Licence: ${oldState.rightLicence} -> ${newState.rightLicence}")
        if (oldState.trailerReverse != newState.trailerReverse) changes.appendLine("  Light Control Module Trailer Reverse: ${oldState.trailerReverse} -> ${newState.trailerReverse}")
        if (oldState.trailerFog != newState.trailerFog) changes.appendLine("  Light Control Module Trailer Fog: ${oldState.trailerFog} -> ${newState.trailerFog}")
        if (oldState.sideLeftTurnSignal != newState.sideLeftTurnSignal) changes.appendLine("  Light Control Module Side Left Turn Signal: ${oldState.sideLeftTurnSignal} -> ${newState.sideLeftTurnSignal}")
        if (oldState.sideRightTurnSignal != newState.sideRightTurnSignal) changes.appendLine("  Light Control Module Side Right Turn Signal: ${oldState.sideRightTurnSignal} -> ${newState.sideRightTurnSignal}")
    }

    private fun compareGeneralModuleState(oldState: GeneralModuleState, newState: GeneralModuleState, changes: StringBuilder) {
        if (oldState.doorDriver != newState.doorDriver) changes.appendLine("  General Module Door Driver: ${oldState.doorDriver} -> ${newState.doorDriver}")
        if (oldState.doorPassenger != newState.doorPassenger) changes.appendLine("  General Module Door Passenger: ${oldState.doorPassenger} -> ${newState.doorPassenger}")
        if (oldState.doorRearLeft != newState.doorRearLeft) changes.appendLine("  General Module Door Rear Left: ${oldState.doorRearLeft} -> ${newState.doorRearLeft}")
        if (oldState.doorRearRight != newState.doorRearRight) changes.appendLine("  General Module Door Rear Right: ${oldState.doorRearRight} -> ${newState.doorRearRight}")
        if (oldState.trunk != newState.trunk) changes.appendLine("  General Module Trunk: ${oldState.trunk} -> ${newState.trunk}")
        if (oldState.hood != newState.hood) changes.appendLine("  General Module Hood: ${oldState.hood} -> ${newState.hood}")
        if (oldState.windowDriver != newState.windowDriver) changes.appendLine("  General Module Window Driver: ${oldState.windowDriver} -> ${newState.windowDriver}")
        if (oldState.windowPassenger != newState.windowPassenger) changes.appendLine("  General Module Window Passenger: ${oldState.windowPassenger} -> ${newState.windowPassenger}")
        if (oldState.windowRearLeft != newState.windowRearLeft) changes.appendLine("  General Module Window Rear Left: ${oldState.windowRearLeft} -> ${newState.windowRearLeft}")
        if (oldState.windowRearRight != newState.windowRearRight) changes.appendLine("  General Module Window Rear Right: ${oldState.windowRearRight} -> ${newState.windowRearRight}")
        if (oldState.lockStatus != newState.lockStatus) changes.appendLine("  General Module Lock Status: ${oldState.lockStatus} -> ${newState.lockStatus}")
        if (oldState.sunroof != newState.sunroof) changes.appendLine("  General Module Sunroof: ${oldState.sunroof} -> ${newState.sunroof}")
    }

    private fun compareHvacState(oldState: HvacState, newState: HvacState, changes: StringBuilder) {
        if (oldState.mode != newState.mode) changes.appendLine("  HVAC Mode: ${oldState.mode} -> ${newState.mode}")
        if (oldState.fanSpeed != newState.fanSpeed) changes.appendLine("  HVAC Fan Speed: ${oldState.fanSpeed} -> ${newState.fanSpeed}")
        if (oldState.temperatureDriver != newState.temperatureDriver) changes.appendLine("  HVAC Temperature Driver: ${oldState.temperatureDriver} -> ${newState.temperatureDriver}")
        if (oldState.temperaturePassenger != newState.temperaturePassenger) changes.appendLine("  HVAC Temperature Passenger: ${oldState.temperaturePassenger} -> ${newState.temperaturePassenger}")
        if (oldState.airDistribution != newState.airDistribution) changes.appendLine("  HVAC Air Distribution: ${oldState.airDistribution} -> ${newState.airDistribution}")
        if (oldState.ac != newState.ac) changes.appendLine("  HVAC AC: ${oldState.ac} -> ${newState.ac}")
        if (oldState.recirculate != newState.recirculate) changes.appendLine("  HVAC Recirculate: ${oldState.recirculate} -> ${newState.recirculate}")
        if (oldState.defrost != newState.defrost) changes.appendLine("  HVAC Defrost: ${oldState.defrost} -> ${newState.defrost}")
    }

    private fun compareMultiFunctionSteeringWheelState(oldState: MultiFunctionSteeringWheelState, newState: MultiFunctionSteeringWheelState, changes: StringBuilder) {
        if (oldState.buttonPressed != newState.buttonPressed) changes.appendLine("  Multi-Function Steering Wheel Button Pressed: ${oldState.buttonPressed} -> ${newState.buttonPressed}")
    }

    private fun compareCdChangerState(oldState: CdChangerState, newState: CdChangerState, changes: StringBuilder) {
        if (oldState.isAlive != newState.isAlive) changes.appendLine("  CD Changer isAlive: ${oldState.isAlive} -> ${newState.isAlive}")
        if (oldState.isPlaying != newState.isPlaying) changes.appendLine("  CD Changer isPlaying: ${oldState.isPlaying} -> ${newState.isPlaying}")
        if (oldState.isPaused != newState.isPaused) changes.appendLine("  CD Changer isPaused: ${oldState.isPaused} -> ${newState.isPaused}")
        if (oldState.isStopped != newState.isStopped) changes.appendLine("  CD Changer isStopped: ${oldState.isStopped} -> ${newState.isStopped}")
        if (oldState.isScanning != newState.isScanning) changes.appendLine("  CD Changer isScanning: ${oldState.isScanning} -> ${newState.isScanning}")
        if (oldState.isRandom != newState.isRandom) changes.appendLine("  CD Changer isRandom: ${oldState.isRandom} -> ${newState.isRandom}")
        if (oldState.currentDisc != newState.currentDisc) changes.appendLine("  CD Changer currentDisc: ${oldState.currentDisc} -> ${newState.currentDisc}")
        if (oldState.currentTrack != newState.currentTrack) changes.appendLine("  CD Changer currentTrack: ${oldState.currentTrack} -> ${newState.currentTrack}")
    }

    private fun comparePdcState(oldState: PdcState, newState: PdcState, changes: StringBuilder) {
        if (oldState.isTurnedOn != newState.isTurnedOn) changes.appendLine("  PDC isTurnedOn: ${oldState.isTurnedOn} -> ${newState.isTurnedOn}")
        if (oldState.values != newState.values) changes.appendLine("  PDC values: ${oldState.values} -> ${newState.values}")
    }

    private fun compareBmButtonState(oldState: BmButtonState, newState: BmButtonState, changes: StringBuilder) {
        if (oldState.nextHold != newState.nextHold) changes.appendLine("  BM Button nextHold: ${oldState.nextHold} -> ${newState.nextHold}")
        if (oldState.nextRel != newState.nextRel) changes.appendLine("  BM Button nextRel: ${oldState.nextRel} -> ${newState.nextRel}")
        if (oldState.prevHold != newState.prevHold) changes.appendLine("  BM Button prevHold: ${oldState.prevHold} -> ${newState.prevHold}")
        if (oldState.prevRel != newState.prevRel) changes.appendLine("  BM Button prevRel: ${oldState.prevRel} -> ${newState.prevRel}")
        if (oldState.reverseHold != newState.reverseHold) changes.appendLine("  BM Button reverseHold: ${oldState.reverseHold} -> ${newState.reverseHold}")
        if (oldState.reverseRel != newState.reverseRel) changes.appendLine("  BM Button reverseRel: ${oldState.reverseRel} -> ${newState.reverseRel}")
        if (oldState.dolbyPres != newState.dolbyPres) changes.appendLine("  BM Button dolbyPres: ${oldState.dolbyPres} -> ${newState.dolbyPres}")
        if (oldState.dolbyHold != newState.dolbyHold) changes.appendLine("  BM Button dolbyHold: ${oldState.dolbyHold} -> ${newState.dolbyHold}")
        if (oldState.dolbyRel != newState.dolbyRel) changes.appendLine("  BM Button dolbyRel: ${oldState.dolbyRel} -> ${newState.dolbyRel}")
        if (oldState.rdsPres != newState.rdsPres) changes.appendLine("  BM Button rdsPres: ${oldState.rdsPres} -> ${newState.rdsPres}")
        if (oldState.rdsHold != newState.rdsHold) changes.appendLine("  BM Button rdsHold: ${oldState.rdsHold} -> ${newState.rdsHold}")
        if (oldState.rdsRel != newState.rdsRel) changes.appendLine("  BM Button rdsRel: ${oldState.rdsRel} -> ${newState.rdsRel}")
        if (oldState.ejectPres != newState.ejectPres) changes.appendLine("  BM Button ejectPres: ${oldState.ejectPres} -> ${newState.ejectPres}")
        if (oldState.ejectHold != newState.ejectHold) changes.appendLine("  BM Button ejectHold: ${oldState.ejectHold} -> ${newState.ejectHold}")
        if (oldState.ejectRel != newState.ejectRel) changes.appendLine("  BM Button ejectRel: ${oldState.ejectRel} -> ${newState.ejectRel}")
        if (oldState.tonePres != newState.tonePres) changes.appendLine("  BM Button tonePres: ${oldState.tonePres} -> ${newState.tonePres}")
        if (oldState.toneHold != newState.toneHold) changes.appendLine("  BM Button toneHold: ${oldState.toneHold} -> ${newState.toneHold}")
        if (oldState.toneRel != newState.toneRel) changes.appendLine("  BM Button toneRel: ${oldState.toneRel} -> ${newState.toneRel}")
        if (oldState.selectPres != newState.selectPres) changes.appendLine("  BM Button selectPres: ${oldState.selectPres} -> ${newState.selectPres}")
        if (oldState.selectHold != newState.selectHold) changes.appendLine("  BM Button selectHold: ${oldState.selectHold} -> ${newState.selectHold}")
        if (oldState.selectRel != newState.selectRel) changes.appendLine("  BM Button selectRel: ${oldState.selectRel} -> ${newState.selectRel}")
        if (oldState.volRight != newState.volRight) changes.appendLine("  BM Button volRight: ${oldState.volRight} -> ${newState.volRight}")
        if (oldState.volLeft != newState.volLeft) changes.appendLine("  BM Button volLeft: ${oldState.volLeft} -> ${newState.volLeft}")
        if (oldState.volRel != newState.volRel) changes.appendLine("  BM Button volRel: ${oldState.volRel} -> ${newState.volRel}")
        if (oldState.volHold != newState.volHold) changes.appendLine("  BM Button volHold: ${oldState.volHold} -> ${newState.volHold}")
        if (oldState.modePres != newState.modePres) changes.appendLine("  BM Button modePres: ${oldState.modePres} -> ${newState.modePres}")
        if (oldState.modeHold != newState.modeHold) changes.appendLine("  BM Button modeHold: ${oldState.modeHold} -> ${newState.modeHold}")
        if (oldState.modeRel != newState.modeRel) changes.appendLine("  BM Button modeRel: ${oldState.modeRel} -> ${newState.modeRel}")
    }

    private fun compareStwButtonState(oldState: StwButtonState, newState: StwButtonState, changes: StringBuilder) {
        if (oldState.volUp != newState.volUp) changes.appendLine("  STW Button volUp: ${oldState.volUp} -> ${newState.volUp}")
        if (oldState.volDown != newState.volDown) changes.appendLine("  STW Button volDown: ${oldState.volDown} -> ${newState.volDown}")
        if (oldState.upPres != newState.upPres) changes.appendLine("  STW Button upPres: ${oldState.upPres} -> ${newState.upPres}")
        if (oldState.upHold != newState.upHold) changes.appendLine("  STW Button upHold: ${oldState.upHold} -> ${newState.upHold}")
        if (oldState.upRel != newState.upRel) changes.appendLine("  STW Button upRel: ${oldState.upRel} -> ${newState.upRel}")
        if (oldState.downPres != newState.downPres) changes.appendLine("  STW Button downPres: ${oldState.downPres} -> ${newState.downPres}")
        if (oldState.downHold != newState.downHold) changes.appendLine("  STW Button downHold: ${oldState.downHold} -> ${newState.downHold}")
        if (oldState.downRel != newState.downRel) changes.appendLine("  STW Button downRel: ${oldState.downRel} -> ${newState.downRel}")
        if (oldState.rtHold != newState.rtHold) changes.appendLine("  STW Button rtHold: ${oldState.rtHold} -> ${newState.rtHold}")
        if (oldState.rtRel != newState.rtRel) changes.appendLine("  STW Button rtRel: ${oldState.rtRel} -> ${newState.rtRel}")
        if (oldState.rtOn != newState.rtOn) changes.appendLine("  STW Button rtOn: ${oldState.rtOn} -> ${newState.rtOn}")
        if (oldState.rtOff != newState.rtOff) changes.appendLine("  STW Button rtOff: ${oldState.rtOff} -> ${newState.rtOff}")
        if (oldState.speakPres != newState.speakPres) changes.appendLine("  STW Button speakPres: ${oldState.speakPres} -> ${newState.speakPres}")
        if (oldState.speakHold != newState.speakHold) changes.appendLine("  STW Button speakHold: ${oldState.speakHold} -> ${newState.speakHold}")
        if (oldState.speakRel != newState.speakRel) changes.appendLine("  STW Button speakRel: ${oldState.speakRel} -> ${newState.speakRel}")
    }

    private fun compareGtState(oldState: GtState, newState: GtState, changes: StringBuilder) {
        if (oldState.mode != newState.mode) changes.appendLine("  GT Mode: ${oldState.mode} -> ${newState.mode}")
    }

    fun printIntListAsHex(intList: List<Int>): String {
        var hexString = ""
        for (number in intList) {
            hexString += number.toString(16).uppercase().padStart(2, '0') + " "
        }
        return hexString
    }
}