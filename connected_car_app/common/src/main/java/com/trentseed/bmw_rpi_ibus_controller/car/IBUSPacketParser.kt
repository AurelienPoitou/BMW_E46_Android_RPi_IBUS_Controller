package com.trentseed.bmw_rpi_ibus_controller.car

import com.trentseed.bmw_rpi_ibus_controller.common.IBUSPacket
import java.util.logging.Logger
import com.trentseed.bmw_rpi_ibus_controller.common.LogConfig

class IBUSPacketParser() {
    private val logger: Logger = LogConfig.getLogger()
    private val carState = CarState.getInstance()

    fun parsePackets(packets: List<IBUSPacket>) {
        for (packet in packets) {
            parsePacket(packet)
        }
        logger.info(carState.getCarState())
    }

    private fun parsePacket(packet: IBUSPacket) {
        try {
            when {
                packet.sourceId == 0xD0 -> parseLightControlModuleExtendedPacket(packet) // LCM Extended
                packet.destinationId == 0x18 -> parseCdChangerPacket(packet) // CD Changer
                packet.sourceId == 0x64 || packet.sourceId == 0xBF -> parseInstrumentClusterExtendedPacket(packet) // IKE Extended
                packet.sourceId == 0x00 -> parseGeneralModuleExtendedPacket(packet) // GM Extended
                packet.sourceId == 0x60 -> parsePdcPacket(packet) // PDC
                packet.destinationId == 0x68 -> parseBmButtonPacket(packet) // BM Button
                packet.sourceId == 0x50 -> parseStwButtonPacket(packet) // STW Button
                //packet.destinationId == 0x3B || packet.destinationId == 0xE7 -> parseGtPacket(packet) // GT
                packet.sourceId == 0x68 -> parseRadioPacket(packet) // Radio
                packet.sourceId == 0x80 -> parseInstrumentClusterPacket(packet) // Instrument Cluster
                packet.sourceId == 0xBF -> parseLightControlModulePacket(packet) // Light Control Module
                packet.sourceId == 0x08 -> parseGeneralModulePacket(packet) // General Module
                packet.sourceId == 0x72 -> parseHvacPacket(packet) // HVAC
                packet.sourceId == 0x66 -> parseMultiFunctionSteeringWheelPacket(packet) // Multi Function Steering Wheel
                packet.destinationId == 0x80 || packet.destinationId == 0xBF -> parseInstrumentClusterPacketExtended(packet)
                packet.sourceId == 0x3B || packet.sourceId == 0xE7 -> {
                    parseGtPacketExtended(packet)
                }
                else -> logger.warning("Unknown source address: ${packet.sourceId}")
            }
        } catch (e: Exception) {
            logger.severe("Error parsing packet: ${packet.raw}")
            logger.severe(e.stackTraceToString())
        }
    }

    private fun parseRadioPacket(packet: IBUSPacket) {
        // Example: Check if the packet is a power on/off command
        if (packet.data.size >= 2 && packet.data[0] == 0x43 && packet.data[1] == 0x01) {
            carState.radio.power = true
            logger.info("Radio power on")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x43 && packet.data[1] == 0x00) {
            carState.radio.power = false
            logger.info("Radio power off")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x44) {
            carState.radio.volume = packet.data[1]
            logger.info("Radio volume: ${carState.radio.volume}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x42) {
            carState.radio.band = when (packet.data[1]) {
                0x00 -> RadioBand.AM
                0x01 -> RadioBand.FM
                0x02 -> RadioBand.DAB
                else -> RadioBand.FM
            }
            logger.info("Radio band: ${carState.radio.band}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x45) {
            carState.radio.mute = packet.data[1] == 0x01
            logger.info("Radio mute: ${carState.radio.mute}")
        } else if (packet.data.size >= 3 && packet.data[0] == 0x46) {
            carState.radio.cdChangerDisc = packet.data[1]
            carState.radio.cdChangerTrack = packet.data[2]
            logger.info("Radio CD Changer: Disc ${carState.radio.cdChangerDisc}, Track ${carState.radio.cdChangerTrack}")
        } else {
            logger.info("Radio packet: ${packet.raw}")
        }
    }

    private fun parseInstrumentClusterPacket(packet: IBUSPacket) {
        // Example: Extract speed from the packet
        if (packet.data.size >= 2 && packet.data[0] == 0x3B) {
            carState.instrumentCluster.speed = packet.data[1]
            logger.info("Instrument Cluster speed: ${carState.instrumentCluster.speed}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x3C) {
            carState.instrumentCluster.rpm = packet.data[1] * 100
            logger.info("Instrument Cluster RPM: ${carState.instrumentCluster.rpm}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x3D) {
            carState.instrumentCluster.fuelLevel = packet.data[1]
            logger.info("Instrument Cluster Fuel Level: ${carState.instrumentCluster.fuelLevel}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x3E) {
            carState.instrumentCluster.coolantTemp = packet.data[1]
            logger.info("Instrument Cluster Coolant Temp: ${carState.instrumentCluster.coolantTemp}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x3F) {
            carState.instrumentCluster.outsideTemp = packet.data[1]
            logger.info("Instrument Cluster Outside Temp: ${carState.instrumentCluster.outsideTemp}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x40) {
            carState.instrumentCluster.odometer = packet.data[1]
            logger.info("Instrument Cluster Odometer: ${carState.instrumentCluster.odometer}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x41) {
            carState.instrumentCluster.tripOdometer = packet.data[1]
            logger.info("Instrument Cluster Trip Odometer: ${carState.instrumentCluster.tripOdometer}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x42) {
            carState.instrumentCluster.serviceInterval = packet.data[1]
            logger.info("Instrument Cluster Service Interval: ${carState.instrumentCluster.serviceInterval}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x43) {
            carState.instrumentCluster.gear = packet.data[1]
            logger.info("Instrument Cluster Gear: ${carState.instrumentCluster.gear}")
        } else {
            logger.info("Instrument Cluster packet: ${packet.raw}")
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
            logger.info("Light Control Module headlights: ${carState.lightControlModule.headlights}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x03) {
            carState.lightControlModule.fogLights = packet.data[1] == 0x01
            logger.info("Light Control Module fog lights: ${carState.lightControlModule.fogLights}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x04) {
            carState.lightControlModule.turnSignalLeft = packet.data[1] == 0x01
            logger.info("Light Control Module turn signal left: ${carState.lightControlModule.turnSignalLeft}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x05) {
            carState.lightControlModule.turnSignalRight = packet.data[1] == 0x01
            logger.info("Light Control Module turn signal right: ${carState.lightControlModule.turnSignalRight}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x06) {
            carState.lightControlModule.hazardLights = packet.data[1] == 0x01
            logger.info("Light Control Module hazard lights: ${carState.lightControlModule.hazardLights}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x07) {
            carState.lightControlModule.interiorLights = packet.data[1] == 0x01
            logger.info("Light Control Module interior lights: ${carState.lightControlModule.interiorLights}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x08) {
            carState.lightControlModule.brakeLights = packet.data[1] == 0x01
            logger.info("Light Control Module brake lights: ${carState.lightControlModule.brakeLights}")
        } else {
            logger.info("Light Control Module packet: ${packet.raw}")
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
            logger.info("General Module door driver: ${carState.generalModule.doorDriver}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02) {
            carState.generalModule.doorPassenger = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
            logger.info("General Module door passenger: ${carState.generalModule.doorPassenger}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x03) {
            carState.generalModule.doorRearLeft = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
            logger.info("General Module door rear left: ${carState.generalModule.doorRearLeft}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x04) {
            carState.generalModule.doorRearRight = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
            logger.info("General Module door rear right: ${carState.generalModule.doorRearRight}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x05) {
            carState.generalModule.trunk = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
            logger.info("General Module trunk: ${carState.generalModule.trunk}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x06) {
            carState.generalModule.hood = when (packet.data[1]) {
                0x00 -> DoorStatus.CLOSED
                0x01 -> DoorStatus.OPEN
                else -> DoorStatus.CLOSED
            }
            logger.info("General Module hood: ${carState.generalModule.hood}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x07) {
            carState.generalModule.windowDriver = when (packet.data[1]) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
            logger.info("General Module window driver: ${carState.generalModule.windowDriver}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x08) {
            carState.generalModule.windowPassenger = when (packet.data[1]) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
            logger.info("General Module window passenger: ${carState.generalModule.windowPassenger}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x09) {
            carState.generalModule.windowRearLeft = when (packet.data[1]) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
            logger.info("General Module window rear left: ${carState.generalModule.windowRearLeft}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x0A) {
            carState.generalModule.windowRearRight = when (packet.data[1]) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
            logger.info("General Module window rear right: ${carState.generalModule.windowRearRight}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x0B) {
            carState.generalModule.lockStatus = when (packet.data[1]) {
                0x00 -> LockStatus.LOCKED
                0x01 -> LockStatus.UNLOCKED
                else -> LockStatus.LOCKED
            }
            logger.info("General Module lock status: ${carState.generalModule.lockStatus}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x0C) {
            carState.generalModule.sunroof = when (packet.data[1]) {
                0x00 -> WindowStatus.CLOSED
                0x01 -> WindowStatus.OPEN
                0x02 -> WindowStatus.VENT
                else -> WindowStatus.CLOSED
            }
            logger.info("General Module sunroof: ${carState.generalModule.sunroof}")
        } else {
            logger.info("General Module packet: ${packet.raw}")
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
            logger.info("HVAC mode: ${carState.hvac.mode}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02) {
            carState.hvac.fanSpeed = when (packet.data[1]) {
                0x00 -> HvacFanSpeed.OFF
                0x01 -> HvacFanSpeed.LOW
                0x02 -> HvacFanSpeed.MEDIUM
                0x03 -> HvacFanSpeed.HIGH
                else -> HvacFanSpeed.OFF
            }
            logger.info("HVAC fan speed: ${carState.hvac.fanSpeed}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x03) {
            carState.hvac.temperatureDriver = packet.data[1]
            logger.info("HVAC temperature driver: ${carState.hvac.temperatureDriver}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x04) {
            carState.hvac.temperaturePassenger = packet.data[1]
            logger.info("HVAC temperature passenger: ${carState.hvac.temperaturePassenger}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x05) {
            carState.hvac.airDistribution = when (packet.data[1]) {
                0x00 -> HvacAirDistribution.FACE
                0x01 -> HvacAirDistribution.FEET
                0x02 -> HvacAirDistribution.DEFROST
                else -> HvacAirDistribution.FACE
            }
            logger.info("HVAC air distribution: ${carState.hvac.airDistribution}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x06) {
            carState.hvac.ac = packet.data[1] == 0x01
            logger.info("HVAC AC: ${carState.hvac.ac}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x07) {
            carState.hvac.recirculate = packet.data[1] == 0x01
            logger.info("HVAC recirculate: ${carState.hvac.recirculate}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x08) {
            carState.hvac.defrost = packet.data[1] == 0x01
            logger.info("HVAC defrost: ${carState.hvac.defrost}")
        } else {
            logger.info("HVAC packet: ${packet.raw}")
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
            logger.info("Multi Function Steering Wheel packet: ${packet.raw}")
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
            logger.info("Light Control Module Extended packet: ${packet.raw}")
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
            logger.info("CD Changer Play")
        } else if (packet.data == listOf(0x38, 0x02, 0x00)) {
            carState.cdChanger.isPaused = true
            carState.cdChanger.isPlaying = false
            carState.cdChanger.isStopped = false
            logger.info("CD Changer Pause")
        } else if (packet.data == listOf(0x38, 0x07, 0x00)) {
            carState.cdChanger.isScanning = false
            logger.info("CD Changer Scan Off")
        } else if (packet.data == listOf(0x38, 0x07, 0x01)) {
            carState.cdChanger.isScanning = true
            logger.info("CD Changer Scan On")
        } else if (packet.data == listOf(0x38, 0x01, 0x00)) {
            carState.cdChanger.isStopped = true
            carState.cdChanger.isPlaying = false
            carState.cdChanger.isPaused = false
            logger.info("CD Changer Stop")
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
            logger.info("CD Changer Disc Change")
        } else if (packet.data == listOf(0x38, 0x08, 0x00)) {
            carState.cdChanger.isRandom = false
            logger.info("CD Changer Random Off")
        } else if (packet.data == listOf(0x38, 0x08, 0x01)) {
            carState.cdChanger.isRandom = true
            logger.info("CD Changer Random On")
        } else {
            logger.info("CD Changer packet: ${packet.raw}")
        }
    }

    private fun parseInstrumentClusterExtendedPacket(packet: IBUSPacket) {
        if (packet.data.size >= 2 && packet.data[0] == 0x19) {
            logger.info("IKE State")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x15) {
            carState.instrumentClusterExtended.coolantTemp = packet.data[1]
            logger.info("IKE Coolant Temp: ${carState.instrumentClusterExtended.coolantTemp}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x16) {
            carState.instrumentClusterExtended.speed = packet.data[1]
            carState.instrumentClusterExtended.rpm = packet.data[2]
            logger.info("IKE Speed: ${carState.instrumentClusterExtended.speed} RPM: ${carState.instrumentClusterExtended.rpm}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x17) {
            carState.instrumentClusterExtended.ignitionState = when (packet.data[1]) {
                0 -> IgnitionState.OFF
                1 -> IgnitionState.ACC
                3 -> IgnitionState.ON
                7 -> IgnitionState.START
                else -> IgnitionState.OFF
            }
            logger.info("IKE Ignition State: ${carState.instrumentClusterExtended.ignitionState}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x1B) {
            carState.instrumentClusterExtended.odometer = packet.data[1]
            logger.info("IKE Odometer: ${carState.instrumentClusterExtended.odometer}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x14) {
            logger.info("IKE Country Coding")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x54) {
            logger.info("IKE VIN")
        } else {
            logger.info("Instrument Cluster Extended packet: ${packet.raw}")
        }
    }

    private fun parseGeneralModuleExtendedPacket(packet: IBUSPacket) {
        if (packet.data.size >= 2 && packet.data[0] == 0x7A) {
            carState.generalModuleExtended.state = "GM State"
            logger.info("GM State")
        } else {
            logger.info("General Module Extended packet: ${packet.raw}")
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
            logger.info("PDC packet: ${packet.raw}")
        }
    }

    private fun parseBmButtonPacket(packet: IBUSPacket) {
        if (packet.data == listOf(0x48, 0x40)) {
            carState.bmButton.nextHold = true
            logger.info("BM_NEXT_HOLD")
        } else if (packet.data == listOf(0x48, 0x80)) {
            carState.bmButton.nextRel = true
            logger.info("BM_NEXT_REL")
        } else if (packet.data == listOf(0x48, 0x50)) {
            carState.bmButton.prevHold = true
            logger.info("BM_PREV_HOLD")
        } else if (packet.data == listOf(0x48, 0x90)) {
            carState.bmButton.prevRel = true
            logger.info("BM_PREV_REL")
        } else if (packet.data == listOf(0x48, 0x54)) {
            carState.bmButton.reverseHold = true
            logger.info("BM_REVERSE_HOLD")
        } else if (packet.data == listOf(0x48, 0x94)) {
            carState.bmButton.reverseRel = true
            logger.info("BM_REVERSE_REL")
        } else if (packet.data == listOf(0x48, 0x33)) {
            carState.bmButton.dolbyPres = true
            logger.info("BM_DOLBY_PRES")
        } else if (packet.data == listOf(0x48, 0x73)) {
            carState.bmButton.dolbyHold = true
            logger.info("BM_DOLBY_HOLD")
        } else if (packet.data == listOf(0x48, 0xB3)) {
            carState.bmButton.dolbyRel = true
            logger.info("BM_DOLBY_REL")
        } else if (packet.data == listOf(0x48, 0x22)) {
            carState.bmButton.rdsPres = true
            logger.info("BM_RDS_PRES")
        } else if (packet.data == listOf(0x48, 0x62)) {
            carState.bmButton.rdsHold = true
            logger.info("BM_RDS_HOLD")
        } else if (packet.data == listOf(0x48, 0xA2)) {
            carState.bmButton.rdsRel = true
            logger.info("BM_RDS_REL")
        } else if (packet.data == listOf(0x48, 0x24)) {
            carState.bmButton.ejectPres = true
            logger.info("BM_EJECT_PRES")
        } else if (packet.data == listOf(0x48, 0x64)) {
            carState.bmButton.ejectHold = true
            logger.info("BM_EJECT_HOLD")
        } else if (packet.data == listOf(0x48, 0xA4)) {
            carState.bmButton.ejectRel = true
            logger.info("BM_EJECT_REL")
        } else if (packet.data == listOf(0x48, 0x04)) {
            carState.bmButton.tonePres = true
            logger.info("BM_TONE_PRES")
        } else if (packet.data == listOf(0x48, 0x44)) {
            carState.bmButton.toneHold = true
            logger.info("BM_TONE_HOLD")
        } else if (packet.data == listOf(0x48, 0x84)) {
            carState.bmButton.toneRel = true
            logger.info("BM_TONE_REL")
        } else if (packet.data == listOf(0x48, 0x20)) {
            carState.bmButton.selectPres = true
            logger.info("BM_SEL_PRES")
        } else if (packet.data == listOf(0x48, 0x60)) {
            carState.bmButton.selectHold = true
            logger.info("BM_SEL_HOLD")
        } else if (packet.data == listOf(0x48, 0xA0)) {
            carState.bmButton.selectRel = true
            logger.info("BM_SEL_REL")
        } else if (packet.data == listOf(0x48, 0x51)) {
            carState.bmButton.button1Hold = true
            logger.info("BM_BTN1_HOLD")
        } else if (packet.data == listOf(0x48, 0x91)) {
            carState.bmButton.button1Rel = true
            logger.info("BM_BTN1_REL")
        } else if (packet.data == listOf(0x48, 0x41)) {
            carState.bmButton.button2Hold = true
            logger.info("BM_BTN2_HOLD")
        } else if (packet.data == listOf(0x48, 0x81)) {
            carState.bmButton.button2Rel = true
            logger.info("BM_BTN2_REL")
        } else if (packet.data == listOf(0x48, 0x52)) {
            carState.bmButton.button3Hold = true
            logger.info("BM_BTN3_HOLD")
        } else if (packet.data == listOf(0x48, 0x92)) {
            carState.bmButton.button3Rel = true
            logger.info("BM_BTN3_REL")
        } else if (packet.data == listOf(0x48, 0x42)) {
            carState.bmButton.button4Hold = true
            logger.info("BM_BTN4_HOLD")
        } else if (packet.data == listOf(0x48, 0x82)) {
            carState.bmButton.button4Rel = true
            logger.info("BM_BTN4_REL")
        } else if (packet.data == listOf(0x48, 0x53)) {
            carState.bmButton.button5Hold = true
            logger.info("BM_BTN5_HOLD")
        } else if (packet.data == listOf(0x48, 0x93)) {
            carState.bmButton.button5Rel = true
            logger.info("BM_BTN5_REL")
        } else if (packet.data == listOf(0x48, 0x43)) {
            carState.bmButton.button6Hold = true
            logger.info("BM_BTN6_HOLD")
        } else if (packet.data == listOf(0x48, 0x83)) {
            carState.bmButton.button6Rel = true
            logger.info("BM_BTN6_REL")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x32 && (packet.data[1] and 0x0F) == 0x01) {
            carState.bmButton.volRight = (packet.data[1] shr 4) and 0x0F
            logger.info("BM_VOL_KNOB_RIGHT -steps: ${carState.bmButton.volRight}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x32 && (packet.data[1] and 0x0F) == 0x00) {
            carState.bmButton.volLeft = (packet.data[1] shr 4) and 0x0F
            logger.info("BM_VOL_KNOB_LEFT -steps: ${carState.bmButton.volLeft}")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x32 && (packet.data[1] and 0x0F) == 0x02) {
            carState.bmButton.volRel = true
            logger.info("BM_VOL_REL")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x32 && (packet.data[1] and 0x0F) == 0x03) {
            carState.bmButton.volHold = true
            logger.info("BM_VOL_HOLD")
        } else if (packet.data == listOf(0x48, 0x00)) {
            carState.bmButton.modePres = true
            logger.info("BM_MODE_PRES")
        } else if (packet.data == listOf(0x48, 0x40)) {
            carState.bmButton.modeHold = true
            logger.info("BM_MODE_HOLD")
        } else if (packet.data == listOf(0x48, 0x80)) {
            carState.bmButton.modeRel = true
            logger.info("BM_MODE_REL")
        } else if (packet.data == listOf(0x48, 0x01)) {
            carState.bmButton.fmPres = true
            logger.info("BM_FM_PRES")
        } else if (packet.data == listOf(0x48, 0x41)) {
            carState.bmButton.fmHold = true
            logger.info("BM_FM_HOLD")
        } else if (packet.data == listOf(0x48, 0x81)) {
            carState.bmButton.fmRel = true
            logger.info("BM_FM_REL")
        } else if (packet.data == listOf(0x48, 0x02)) {
            carState.bmButton.amPres = true
            logger.info("BM_AM_PRES")
        } else if (packet.data == listOf(0x48, 0x42)) {
            carState.bmButton.amHold = true
            logger.info("BM_AM_HOLD")
        } else if (packet.data == listOf(0x48, 0x82)) {
            carState.bmButton.amRel = true
            logger.info("BM_AM_REL")
        } else if (packet.data == listOf(0x48, 0x03)) {
            carState.bmButton.screenPres = true
            logger.info("BM_SCREEN_PRES")
        } else if (packet.data == listOf(0x48, 0x43)) {
            carState.bmButton.screenHold = true
            logger.info("BM_SCREEN_HOLD")
        } else if (packet.data == listOf(0x48, 0x83)) {
            carState.bmButton.screenRel = true
            logger.info("BM_SCREEN_REL")
        } else {
            logger.info("BM Button packet: ${packet.raw}")
        }
    }

    private fun parseStwButtonPacket(packet: IBUSPacket) {
        if (packet.data == listOf(0x01)) {
            carState.stwButton.volUp = true
            logger.info("STW_VOL_UP")
        } else if (packet.data == listOf(0x02)) {
            carState.stwButton.volDown = true
            logger.info("STW_VOL_DOWN")
        } else if (packet.data == listOf(0x03)) {
            carState.stwButton.upPres = true
            logger.info("STW_UP_PRES")
        } else if (packet.data == listOf(0x04)) {
            carState.stwButton.upHold = true
            logger.info("STW_UP_HOLD")
        } else if (packet.data == listOf(0x05)) {
            carState.stwButton.upRel = true
            logger.info("STW_UP_REL")
        } else if (packet.data == listOf(0x06)) {
            carState.stwButton.downPres = true
            logger.info("STW_DOWN_PRES")
        } else if (packet.data == listOf(0x07)) {
            carState.stwButton.downHold = true
            logger.info("STW_DOWN_HOLD")
        } else if (packet.data == listOf(0x08)) {
            carState.stwButton.downRel = true
            logger.info("STW_DOWN_REL")
        } else if (packet.data == listOf(0x09)) {
            carState.stwButton.rtHold = true
            logger.info("STW_RT_HOLD")
        } else if (packet.data == listOf(0x0A)) {
            carState.stwButton.rtRel = true
            logger.info("STW_RT_REL")
        } else if (packet.data == listOf(0x0B)) {
            carState.stwButton.rtOn = true
            logger.info("STW_RT_ON")
        } else if (packet.data == listOf(0x0C)) {
            carState.stwButton.rtOff = true
            logger.info("STW_RT_OFF")
        } else if (packet.data == listOf(0x0D)) {
            carState.stwButton.speakPres = true
            logger.info("STW_SPEAK_PRES")
        } else if (packet.data == listOf(0x0E)) {
            carState.stwButton.speakHold = true
            logger.info("STW_SPEAK_HOLD")
        } else if (packet.data == listOf(0x0F)) {
            carState.stwButton.speakRel = true
            logger.info("STW_SPEAK_REL")
        } else {
            logger.info("STW packet: ${packet.raw}")
        }
    }

    private fun parseInstrumentClusterPacketExtended(packet: IBUSPacket) {
        if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x00) {
            logger.info("Instrument Cluster Extended: Check Control OK")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x01) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x02) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x03) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x04) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x05) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x06) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x07) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x08) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x09) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x0A) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x0B) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x0C) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x0D) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x0E) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x0F) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x10) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x11) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x12) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x13) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x14) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x15) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x16) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x17) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x18) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x19) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x1A) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x1B) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x1C) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x1D) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x1E) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else if (packet.data.size >= 2 && packet.data[0] == 0x02 && packet.data[1] == 0x1F) {
            logger.info("Instrument Cluster Extended: Check Control Failure")
        } else {
            logger.info("Instrument Cluster Extended packet: ${packet.raw}")
        }
    }

    private fun parseGtPacketExtended(packet: IBUSPacket) {
        if (packet.data == listOf(0x02, 0x00)) {
            carState.gt.mode = GtMode.CD
            logger.info("GT CD")
        } else if (packet.data == listOf(0x02, 0x01)) {
            carState.gt.mode = GtMode.CIS
            logger.info("GT CIS")
        } else if (packet.data == listOf(0x02, 0x02)) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT TAPE")
        } else if (packet.data == listOf(0x02, 0x03)) {
            carState.gt.mode = GtMode.AUX
            logger.info("GT AUX")
        } else if (packet.data == listOf(0x01, 0x00)) {
            carState.gt.isTvOn = true
            logger.info("GT TV ON")
        } else if (packet.data == listOf(0x01, 0x01)) {
            carState.gt.isTvOn = false
            logger.info("GT TV OFF")
        } else if (packet.data == listOf(0xA5, 0x62, 0x01, 0x41, 0x43, 0x44, 0x37)) {
            carState.gt.mode = GtMode.CD
            logger.info("GT BMBT TEXT: CD7")
        } else if (packet.data.size >= 7 && packet.data.subList(0, 7) == listOf(0xA5, 0x62, 0x01, 0x41, 0x43, 0x44, 0x37)) {
            carState.gt.mode = GtMode.CD
            logger.info("GT BMBT TEXT: CD7")
        } else if (packet.data.size >= 7 && packet.data.subList(0, 7) == listOf(0xA5, 0x62, 0x01, 0x41, 0x43, 0x44, 0x36)) {
            carState.gt.mode = GtMode.CIS
            logger.info("GT BMBT TEXT: CD6 CIS")
        } else if (packet.data == listOf(0xA5, 0x62, 0x01, 0x41, 0x43, 0x44)) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT BMBT TEXT: CD E46 BM-CD")
        } else if (packet.data.size >= 9 && packet.data.subList(0, 9) == listOf(0x23, 0xC4, 0x20, 0x43, 0x44, 0x20, 0x37, 0x2D, 0x39)) {
            carState.gt.mode = GtMode.CD
            logger.info("GT BMBT TEXT: CD 7-9...")
        } else if (packet.data.size >= 7 && packet.data.subList(0, 7) == listOf(0x23, 0x50, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT BMBT TEXT: TAPE")
        } else if (packet.data.size >= 10 && packet.data.subList(0, 10) == listOf(0x23, 0x50, 0x20, 0x4E, 0x4F, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT BMBT TEXT: NO TAPE")
        } else if (packet.data.size >= 3 && (packet.data.subList(0, 3) == listOf(0x23, 0x40, 0x20) || packet.data.subList(0, 3) == listOf(0x23, 0x50, 0x20))) {
            carState.gt.mode = GtMode.RADIO
            logger.info("GT BMBT TEXT: what ever")
        } else if (packet.data.size >= 18 && packet.data.subList(0, 18) == listOf(0x23, 0x62, 0x30, 0x20, 0x20, 0x07, 0x20, 0x20, 0x20, 0x20, 0x20, 0x08, 0x43, 0x44, 0x20, 0x37, 0x2D, 0x39)) {
            carState.gt.mode = GtMode.CD
            logger.info("GT BMBT TEXT: CD 7-9...")
        } else if (packet.data.size >= 9 && packet.data.subList(0, 9) == listOf(0x23, 0xC4, 0x30, 0x43, 0x44, 0x20, 0x37, 0x2D, 0x39)) {
            carState.gt.mode = GtMode.CD
            logger.info("GT BMBT TEXT: CD 7-9... BM24 E38")
        } else if (packet.data.size >= 9 && packet.data.subList(0, 9) == listOf(0x23, 0xCB, 0x20, 0x43, 0x44, 0x20, 0x37, 0x2D, 0x39)) {
            carState.gt.mode = GtMode.CD
            logger.info("GT BMBT TEXT: CD 7-9... BM24 E38")
        } else if (packet.data.size >= 10 && packet.data.subList(0, 10) == listOf(0x23, 0xC2, 0x30, 0x4E, 0x4F, 0x20, 0x44, 0x49, 0x53, 0x43)) {
            carState.gt.mode = GtMode.CD
            logger.info("GT BMBT TEXT: NO DISC... BM24 E38")
        } else if (packet.data.size >= 16 && packet.data.subList(0, 16) == listOf(0x23, 0x62, 0x30, 0x20, 0x20, 0x07, 0x20, 0x20, 0x20, 0x20, 0x20, 0x08, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT BMBT TEXT: TAPE")
        } else if (packet.data.size >= 7 && packet.data.subList(0, 7) == listOf(0x23, 0x40, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT BMBT TEXT: TAPE")
        } else if (packet.data.size >= 19 && packet.data.subList(0, 19) == listOf(0x23, 0x62, 0x30, 0x20, 0x20, 0x07, 0x20, 0x20, 0x20, 0x20, 0x20, 0x08, 0x4E, 0x4F, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT BMBT TEXT: NO TAPE")
        } else if (packet.data.size >= 10 && packet.data.subList(0, 10) == listOf(0x23, 0x40, 0x20, 0x4E, 0x4F, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT BMBT TEXT: NO TAPE")
        } else if (packet.data.size >= 7 && (packet.data.subList(0, 7) == listOf(0x23, 0x82, 0x30, 0x54, 0x41, 0x50, 0x45) || packet.data.subList(0, 7) == listOf(0x23, 0x92, 0x30, 0x54, 0x41, 0x50, 0x45))) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT BMBT TEXT: TAPE BM24 E38")
        } else if (packet.data.size >= 10 && packet.data.subList(0, 10) == listOf(0x23, 0x40, 0x30, 0x4E, 0x4F, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT BMBT TEXT: NO TAPE BM24 E38")
        } else if (packet.data.size >= 19 && packet.data.subList(0, 19) == listOf(0x23, 0x62, 0x30, 0x07, 0x20, 0x20, 0x20, 0x20, 0x20, 0x08, 0x20, 0x20, 0x4E, 0x4F, 0x20, 0x54, 0x41, 0x50, 0x45)) {
            carState.gt.mode = GtMode.TAPE
            logger.info("GT BMBT TEXT: NO TAPE")
        } else if (packet.data.size >= 5 && (packet.data.subList(0, 5) == listOf(0x23, 0x40, 0x30, 0x46, 0x4D) || packet.data.subList(0, 5) == listOf(0x23, 0x48, 0x30, 0x46, 0x4D) || packet.data.subList(0, 5) == listOf(0x23, 0x40, 0x20, 0x46, 0x4D))) {
            carState.gt.mode = GtMode.RADIO
            logger.info("GT BMBT TEXT: FM BM24 E38")
        } else if (packet.data.size >= 5 && packet.data.subList(0, 5) == listOf(0x23, 0x40, 0x20, 0x41, 0x4D)) {
            carState.gt.mode = GtMode.RADIO
            logger.info("GT BMBT TEXT: AM BM24 E38")
        } else if (packet.data.size >= 5 && packet.data.subList(0, 5) == listOf(0x23, 0x40, 0x30, 0x4D, 0x57)) {
            carState.gt.mode = GtMode.RADIO
            logger.info("GT BMBT TEXT: MW BM24 E38")
        } else if (packet.data.size >= 5 && packet.data.subList(0, 5) == listOf(0x23, 0x40, 0x30, 0x4C, 0x57)) {
            carState.gt.mode = GtMode.RADIO
            logger.info("GT BMBT TEXT: LW BM24 E38")
        } else if (packet.data.size >= 6 && packet.data.subList(0, 6) == listOf(0x23, 0xC4, 0x30, 0x43, 0x44, 0x20) && packet.data.size >= 8 && packet.data[7] == 0x2D && carState.gt.modebm23) {
            carState.gt.mode = GtMode.CD
            logger.info("GT BMBT TEXT: CD X-XX.. BM23 with P-CDCD")
        } else {
            logger.info("GT Extended packet: ${packet.raw}")
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
}