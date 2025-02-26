package com.trentseed.bmw_rpi_ibus_controller.car

import com.trentseed.bmw_rpi_ibus_controller.common.IBUSPacket
import java.util.logging.Logger
import com.trentseed.bmw_rpi_ibus_controller.common.LogConfig

class IBUSPacketParser() {
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
                packet.sourceId == 0xD0 -> parseLightControlModuleExtendedPacket(packet) // LCM Extended
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
        if (packet.data.size >= 2 && packet.data[0] == 0x11) {
            val ignitionState = if (packet.data[4] < 2) packet.data[4] else (0x02 and packet.data[4])
            carState.instrumentCluster.ignitionState = when (ignitionState) {
                0 -> IgnitionState.OFF
                1 -> IgnitionState.ACC
                3 -> IgnitionState.ON
                7 -> IgnitionState.START
                else -> IgnitionState.OFF
            }
            logger.info("IKE Country Coding")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x14) {
            logger.info("IKE Country Coding")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x15) {
            carState.instrumentCluster.coolantTemp = packet.data[1]
            logger.info("IKE Coolant Temp: ${carState.instrumentCluster.coolantTemp}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x16) {
            carState.instrumentCluster.speed = packet.data[1]
            carState.instrumentCluster.rpm = packet.data[2]
            logger.info("IKE Speed: ${carState.instrumentCluster.speed} RPM: ${carState.instrumentCluster.rpm}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x17) {
            carState.instrumentCluster.odometer = packet.data[1] + packet.data[2] shl 8 + packet.data[3] shl 16
            carState.instrumentCluster.serviceInterval = (packet.data[4] + packet.data[5]) * 50
            carState.instrumentCluster.serviceIntervalType = packet.data[6]
            carState.instrumentCluster.serviceIntervalDays = packet.data[7]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x18) {
            carState.instrumentCluster.speed = packet.data[1]
            carState.instrumentCluster.rpm = packet.data[2] * 100
        } else if (packet.data.size >= 2 && packet.data[0] == 0x19) {
            carState.instrumentCluster.coolantTemp = packet.data[2]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x1B) {
            carState.  instrumentCluster.odometer = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x3B) {
            carState.instrumentCluster.speed = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x3C) {
            carState.instrumentCluster.rpm = packet.data[1] * 100
        } else if (packet.data.size >= 2 && packet.data[0] == 0x3D) {
            carState.instrumentCluster.fuelLevel = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x3E) {
            carState.instrumentCluster.coolantTemp = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x3F) {
            carState.instrumentCluster.outsideTemp = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x40) {
            carState.instrumentCluster.odometer = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x41) {
            carState.instrumentCluster.tripOdometer = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x42) {
            carState.instrumentCluster.serviceInterval = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x43) {
            carState.instrumentCluster.gear = packet.data[1]
        } else if (packet.data.size >= 2 && packet.data[0] == 0x54) {
            logger.info("IKE VIN")
        } else {
            logger.info("Instrument Cluster packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseLightControlModulePacket(packet: IBUSPacket) {
        // Example: Extract headlight status from the packet
        if (packet.data.size >= 2 && packet.data[0] == 0x02) {
            carState.lightControlModule.headlights = when (packet.data[1]) {
                0x00 -> LightStatus.OFF
                0x01 -> LightStatus.PARKING
                0x02 -> LightStatus.LOW_BEAM
                0x03 -> LightStatus.HIGH_BEAM
                else -> LightStatus.OFF
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x03) {
            carState.lightControlModule.fogLights = packet.data[1] == 0x01
        } else if (packet.data.size >= 2 && packet.data[0] == 0x04) {
            carState.lightControlModule.turnSignalLeft = packet.data[1] == 0x01
        } else if (packet.data.size >= 2 && packet.data[0] == 0x05) {
            carState.lightControlModule.turnSignalRight = packet.data[1] == 0x01
        } else if (packet.data.size >= 2 && packet.data[0] == 0x06) {
            carState.lightControlModule.hazardLights = packet.data[1] == 0x01
        } else if (packet.data.size >= 2 && packet.data[0] == 0x07) {
            carState.lightControlModule.interiorLights = packet.data[1] == 0x01
        } else if (packet.data.size >= 2 && packet.data[0] == 0x08) {
            carState.lightControlModule.brakeLights = packet.data[1] == 0x01
        } else {
            logger.info("Light Control Module packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseGeneralModulePacket(packet: IBUSPacket) {
        // Example: Extract door status from the packet
        if (packet.data.size >= 2 && packet.data[0] == 0x01) {
            carState.generalModule.doorDriver = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02) {
            carState.generalModule.doorPassenger = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x03) {
            carState.generalModule.doorRearLeft = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x04) {
            carState.generalModule.doorRearRight = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x05) {
            carState.generalModule.trunk = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x06) {
            carState.generalModule.hood = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x07) {
            carState.generalModule.windowDriver = when (packet.data[1]) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x08) {
            carState.generalModule.windowPassenger = when (packet.data[1]) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x09) {
            carState.generalModule.windowRearLeft = when (packet.data[1]) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x0A) {
            carState.generalModule.windowRearRight = when (packet.data[1]) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x0B) {
            carState.generalModule.lockStatus = when (packet.data[1]) {
                0x00 -> LockStatus.LOCKED
                0x01 -> LockStatus.UNLOCKED
                else -> LockStatus.LOCKED
            }
        } else if (packet.data.size >= 2 && packet.data[0] == 0x0C) {
            carState.generalModule.sunroof = when (packet.data[1]) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
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

    private fun parseLightControlModuleExtendedPacket(packet: IBUSPacket) {
        if (packet.data.size >= 2 && packet.data[0] == 0x5B) {
            logger.info("LCM State: ${packet.data.drop(1).joinToString(" ")}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x5C) {
            logger.info("LCM DIM State")
        } else if (packet.destinationId == 0x3F && packet.data.size >= 2 && packet.data[0] == 0xA0) {
            logger.info("LCM Diag State")
        } else {
            logger.info("Light Control Module Extended packet: ${printIntListAsHex(packet.raw)}")
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
        if (packet.data.size >= 2 && packet.data[0] == 0x90) {
            carState.pdc.isTurnedOn = true
            logger.info("PDC turned on")
        } else if (packet.data.size >= 2 && packet.data[0] == 0xA0) {
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
        if (packet.data == listOf(0x01)) {
            carState.stwButton.volUp = true
        } else if (packet.data == listOf(0x02)) {
            carState.stwButton.volDown = true
        } else if (packet.data == listOf(0x03)) {
            carState.stwButton.upPres = true
        } else if (packet.data == listOf(0x04)) {
            carState.stwButton.upHold = true
        } else if (packet.data == listOf(0x05)) {
            carState.stwButton.upRel = true
        } else if (packet.data == listOf(0x06)) {
            carState.stwButton.downPres = true
        } else if (packet.data == listOf(0x07)) {
            carState.stwButton.downHold = true
        } else if (packet.data == listOf(0x08)) {
            carState.stwButton.downRel = true
        } else if (packet.data == listOf(0x09)) {
            carState.stwButton.rtHold = true
        } else if (packet.data == listOf(0x0A)) {
            carState.stwButton.rtRel = true
        } else if (packet.data == listOf(0x0B)) {
            carState.stwButton.rtOn = true
        } else if (packet.data == listOf(0x0C)) {
            carState.stwButton.rtOff = true
        } else if (packet.data == listOf(0x0D)) {
            carState.stwButton.speakPres = true
        } else if (packet.data == listOf(0x0E)) {
            carState.stwButton.speakHold = true
        } else if (packet.data == listOf(0x0F)) {
            carState.stwButton.speakRel = true
        } else {
            logger.info("STW packet: ${printIntListAsHex(packet.raw)}")
        }
    }

    private fun parseInstrumentClusterPacketExtended(packet: IBUSPacket) {
        if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x00) {
            logger.info("Instrument Cluster Extended: Check Control OK")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02) {
            logger.info("Instrument Cluster Extended: Check Control Failure: " + packet.data[1])
        } else {
            logger.info("Instrument Cluster Extended packet: ${printIntListAsHex(packet.raw)}")
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
        return changes.toString()
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
        if (oldState.vin != newState.vin) changes.appendLine("  Instrument Cluster Extended VIN: ${oldState.vin} -> ${newState.vin}")
        if (oldState.countryCoding != newState.countryCoding) changes.appendLine("  Instrument Cluster Extended Country Coding: ${oldState.countryCoding} -> ${newState.countryCoding}")
        if (oldState.ignitionState != newState.ignitionState) changes.appendLine("  Instrument Cluster Extended Ignition State: ${oldState.ignitionState} -> ${newState.ignitionState}")
        if (oldState.speed != newState.speed) changes.appendLine("  Instrument Cluster Speed: ${oldState.speed} -> ${newState.speed}")
        if (oldState.rpm != newState.rpm) changes.appendLine("  Instrument Cluster RPM: ${oldState.rpm} -> ${newState.rpm}")
        if (oldState.fuelLevel != newState.fuelLevel) changes.appendLine("  Instrument Cluster Fuel Level: ${oldState.fuelLevel} -> ${newState.fuelLevel}")
        if (oldState.coolantTemp != newState.coolantTemp) changes.appendLine("  Instrument Cluster Coolant Temp: ${oldState.coolantTemp} -> ${newState.coolantTemp}")
        if (oldState.outsideTemp != newState.outsideTemp) changes.appendLine("  Instrument Cluster Outside Temp: ${oldState.outsideTemp} -> ${newState.outsideTemp}")
        if (oldState.odometer != newState.odometer) changes.appendLine("  Instrument Cluster Odometer: ${oldState.odometer} -> ${newState.odometer}")
        if (oldState.tripOdometer != newState.tripOdometer) changes.appendLine("  Instrument Cluster Trip Odometer: ${oldState.tripOdometer} -> ${newState.tripOdometer}")
        if (oldState.serviceInterval != newState.serviceInterval) changes.appendLine("  Instrument Cluster Service Interval: ${oldState.serviceInterval} -> ${newState.serviceInterval}")
        if (oldState.serviceInterval != newState.serviceInterval) changes.appendLine("  Instrument Cluster Service Interval Days: ${oldState.serviceIntervalDays} -> ${newState.serviceIntervalDays}")
        if (oldState.serviceInterval != newState.serviceInterval) changes.appendLine("  Instrument Cluster Service Interval Type: ${oldState.serviceIntervalType} -> ${newState.serviceIntervalType}")
        if (oldState.gear != newState.gear) changes.appendLine("  Instrument Cluster Gear: ${oldState.gear} -> ${newState.gear}")
        if (oldState.checkControlMessages != newState.checkControlMessages) changes.appendLine("  Instrument Cluster Check Control Messages: ${oldState.checkControlMessages} -> ${newState.checkControlMessages}")
    }

    private fun compareLightControlModuleState(oldState: LightControlModuleState, newState: LightControlModuleState, changes: StringBuilder) {
        if (oldState.headlights != newState.headlights) changes.appendLine("  Light Control Module Headlights: ${oldState.headlights} -> ${newState.headlights}")
        if (oldState.fogLights != newState.fogLights) changes.appendLine("  Light Control Module Fog Lights: ${oldState.fogLights} -> ${newState.fogLights}")
        if (oldState.turnSignalLeft != newState.turnSignalLeft) changes.appendLine("  Light Control Module Turn Signal Left: ${oldState.turnSignalLeft} -> ${newState.turnSignalLeft}")
        if (oldState.turnSignalRight != newState.turnSignalRight) changes.appendLine("  Light Control Module Turn Signal Right: ${oldState.turnSignalRight} -> ${newState.turnSignalRight}")
        if (oldState.hazardLights != newState.hazardLights) changes.appendLine("  Light Control Module Hazard Lights: ${oldState.hazardLights} -> ${newState.hazardLights}")
        if (oldState.interiorLights != newState.interiorLights) changes.appendLine("  Light Control Module Interior Lights: ${oldState.interiorLights} -> ${newState.interiorLights}")
        if (oldState.brakeLights != newState.brakeLights) changes.appendLine("  Light Control Module Brake Lights: ${oldState.brakeLights} -> ${newState.brakeLights}")
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