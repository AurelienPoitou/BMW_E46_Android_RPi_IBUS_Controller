package com.trentseed.bmw_rpi_ibus_controller.car

import androidx.core.graphics.values

class CarState private constructor(
    val radio: RadioState,
    val instrumentCluster: InstrumentClusterState,
    val lightControlModule: LightControlModuleState,
    val generalModule: GeneralModuleState,
    val hvac: HvacState,
    val multiFunctionSteeringWheel: MultiFunctionSteeringWheelState,
    val cdChanger: CdChangerState,
    val pdc: PdcState,
    val bmButton: BmButtonState,
    val stwButton: StwButtonState,
    val gt: GtState
) {
    // Primary constructor for initial creation
    private constructor() : this(
        RadioState(),
        InstrumentClusterState(),
        LightControlModuleState(),
        GeneralModuleState(),
        HvacState(),
        MultiFunctionSteeringWheelState(),
        CdChangerState(),
        PdcState(),
        BmButtonState(),
        StwButtonState(),
        GtState()
    )
    companion object {
        @Volatile
        private var instance: CarState? = null

        fun getInstance(): CarState {
            return instance ?: synchronized(this) {
                instance ?: CarState().also { instance = it }
            }
        }
    }

    fun copy(): CarState {
        return CarState(
            radio.copy(),
            instrumentCluster.copy(),
            lightControlModule.copy(),
            generalModule.copy(),
            hvac.copy(),
            multiFunctionSteeringWheel.copy(),
            cdChanger.copy(),
            pdc.copy(),
            bmButton.copy(),
            stwButton.copy(),
            gt.copy()
        )
    }

    fun getCarState(): String {
        val sb = StringBuilder()
        sb.appendLine("--- Current Car State ---")

        sb.appendLine("Radio State:")
        sb.appendLine("  Power: ${radio.power}")
        sb.appendLine("  Band: ${radio.band}")
        sb.appendLine("  Frequency: ${radio.frequency}")
        sb.appendLine("  Volume: ${radio.volume}")
        sb.appendLine("  Station Name: ${radio.stationName}")
        sb.appendLine("  Mute: ${radio.mute}")
        sb.appendLine("  CD Changer Track: ${radio.cdChangerTrack}")
        sb.appendLine("  CD Changer Disc: ${radio.cdChangerDisc}")

        sb.appendLine("Instrument Cluster State:")
        sb.appendLine("  Speed: ${instrumentCluster.speed}")
        sb.appendLine("  RPM: ${instrumentCluster.rpm}")
        sb.appendLine("  Fuel Level: ${instrumentCluster.fuelLevel}")
        sb.appendLine("  Coolant Temp: ${instrumentCluster.coolantTemp}")
        sb.appendLine("  Outside Temp: ${instrumentCluster.outsideTemp}")
        sb.appendLine("  Odometer: ${instrumentCluster.odometer}")
        sb.appendLine("  Trip Odometer: ${instrumentCluster.tripOdometer}")
        sb.appendLine("  Service Interval: ${instrumentCluster.serviceInterval}")
        sb.appendLine("  Gear: ${instrumentCluster.gear}")
        sb.appendLine("  Check Control Messages: ${instrumentCluster.checkControlMessages}")

        sb.appendLine("Light Control Module State:")
        sb.appendLine("  Headlights: ${lightControlModule.headlights}")
        sb.appendLine("  Fog Lights: ${lightControlModule.fogLights}")
        sb.appendLine("  Turn Signal Left: ${lightControlModule.turnSignalLeft}")
        sb.appendLine("  Turn Signal Right: ${lightControlModule.turnSignalRight}")
        sb.appendLine("  Hazard Lights: ${lightControlModule.hazardLights}")
        sb.appendLine("  Interior Lights: ${lightControlModule.interiorLights}")
        sb.appendLine("  Brake Lights: ${lightControlModule.brakeLights}")

        sb.appendLine("General Module State:")
        sb.appendLine("  Door Driver: ${generalModule.doorDriver}")
        sb.appendLine("  Door Passenger: ${generalModule.doorPassenger}")
        sb.appendLine("  Door Rear Left: ${generalModule.doorRearLeft}")
        sb.appendLine("  Door Rear Right: ${generalModule.doorRearRight}")
        sb.appendLine("  Trunk: ${generalModule.trunk}")
        sb.appendLine("  Hood: ${generalModule.hood}")
        sb.appendLine("  Window Driver: ${generalModule.windowDriver}")
        sb.appendLine("  Window Passenger: ${generalModule.windowPassenger}")
        sb.appendLine("  Window Rear Left: ${generalModule.windowRearLeft}")
        sb.appendLine("  Window Rear Right: ${generalModule.windowRearRight}")
        sb.appendLine("  Lock Status: ${generalModule.lockStatus}")
        sb.appendLine("  Sunroof: ${generalModule.sunroof}")

        sb.appendLine("HVAC State:")
        sb.appendLine("  Mode: ${hvac.mode}")
        sb.appendLine("  Fan Speed: ${hvac.fanSpeed}")
        sb.appendLine("  Temperature Driver: ${hvac.temperatureDriver}")
        sb.appendLine("  Temperature Passenger: ${hvac.temperaturePassenger}")
        sb.appendLine("  Air Distribution: ${hvac.airDistribution}")
        sb.appendLine("  AC: ${hvac.ac}")
        sb.appendLine("  Recirculate: ${hvac.recirculate}")
        sb.appendLine("  Defrost: ${hvac.defrost}")

        sb.appendLine("Multi-Function Steering Wheel State:")
        sb.appendLine("  Button Pressed: ${multiFunctionSteeringWheel.buttonPressed}")

        sb.appendLine("CD Changer State:")
        sb.appendLine("  isAlive: ${cdChanger.isAlive}")
        sb.appendLine("  isPlaying: ${cdChanger.isPlaying}")
        sb.appendLine("  isPaused: ${cdChanger.isPaused}")
        sb.appendLine("  isStopped: ${cdChanger.isStopped}")
        sb.appendLine("  isScanning: ${cdChanger.isScanning}")
        sb.appendLine("  isRandom: ${cdChanger.isRandom}")
        sb.appendLine("  currentDisc: ${cdChanger.currentDisc}")
        sb.appendLine("  currentTrack: ${cdChanger.currentTrack}")

        sb.appendLine("PDC State:")
        sb.appendLine("  isTurnedOn: ${pdc.isTurnedOn}")
        sb.appendLine("  values: ${pdc.values}")

        sb.appendLine("BM Button State:")
        sb.appendLine("  nextHold: ${bmButton.nextHold}")
        sb.appendLine("  nextRel: ${bmButton.nextRel}")
        sb.appendLine("  prevHold: ${bmButton.prevHold}")
        sb.appendLine("  prevRel: ${bmButton.prevRel}")
        sb.appendLine("  reverseHold: ${bmButton.reverseHold}")
        sb.appendLine("  reverseRel: ${bmButton.reverseRel}")
        sb.appendLine("  dolbyPres: ${bmButton.dolbyPres}")
        sb.appendLine("  dolbyHold: ${bmButton.dolbyHold}")
        sb.appendLine("  dolbyRel: ${bmButton.dolbyRel}")
        sb.appendLine("  rdsPres: ${bmButton.rdsPres}")
        sb.appendLine("  rdsHold: ${bmButton.rdsHold}")
        sb.appendLine("  rdsRel: ${bmButton.rdsRel}")
        sb.appendLine("  ejectPres: ${bmButton.ejectPres}")
        sb.appendLine("  ejectHold: ${bmButton.ejectHold}")
        sb.appendLine("  ejectRel: ${bmButton.ejectRel}")
        sb.appendLine("  tonePres: ${bmButton.tonePres}")
        sb.appendLine("  toneHold: ${bmButton.toneHold}")
        sb.appendLine("  toneRel: ${bmButton.toneRel}")
        sb.appendLine("  selectPres: ${bmButton.selectPres}")
        sb.appendLine("  selectHold: ${bmButton.selectHold}")
        sb.appendLine("  selectRel: ${bmButton.selectRel}")
        sb.appendLine("  volRight: ${bmButton.volRight}")
        sb.appendLine("  volLeft: ${bmButton.volLeft}")
        sb.appendLine("  volRel: ${bmButton.volRel}")
        sb.appendLine("  volHold: ${bmButton.volHold}")
        sb.appendLine("  modePres: ${bmButton.modePres}")
        sb.appendLine("  modeHold: ${bmButton.modeHold}")
        sb.appendLine("  modeRel: ${bmButton.modeRel}")
        sb.appendLine("  fmPres: ${bmButton.fmPres}")
        sb.appendLine("  fmHold: ${bmButton.fmHold}")
        sb.appendLine("  fmRel: ${bmButton.fmRel}")
        sb.appendLine("  amPres: ${bmButton.amPres}")
        sb.appendLine("  amHold: ${bmButton.amHold}")
        sb.appendLine("  amRel: ${bmButton.amRel}")
        sb.appendLine("  screenPres: ${bmButton.screenPres}")
        sb.appendLine("  screenHold: ${bmButton.screenHold}")
        sb.appendLine("  screenRel: ${bmButton.screenRel}")
        sb.appendLine("  button1Hold: ${bmButton.button1Hold}")
        sb.appendLine("  button1Rel: ${bmButton.button1Rel}")
        sb.appendLine("  button2Hold: ${bmButton.button2Hold}")
        sb.appendLine("  button2Rel: ${bmButton.button2Rel}")
        sb.appendLine("  button3Hold: ${bmButton.button3Hold}")
        sb.appendLine("  button3Rel: ${bmButton.button3Rel}")
        sb.appendLine("  button4Hold: ${bmButton.button4Hold}")
        sb.appendLine("  button4Rel: ${bmButton.button4Rel}")
        sb.appendLine("  button5Hold: ${bmButton.button5Hold}")
        sb.appendLine("  button5Rel: ${bmButton.button5Rel}")
        sb.appendLine("  button6Hold: ${bmButton.button6Hold}")
        sb.appendLine("  button6Rel: ${bmButton.button6Rel}")

        sb.appendLine("STW Button State:")
        sb.appendLine("  volUp: ${stwButton.volUp}")
        sb.appendLine("  volDown: ${stwButton.volDown}")
        sb.appendLine("  upPres: ${stwButton.upPres}")
        sb.appendLine("  upHold: ${stwButton.upHold}")
        sb.appendLine("  upRel: ${stwButton.upRel}")
        sb.appendLine("  downPres: ${stwButton.downPres}")
        sb.appendLine("  downHold: ${stwButton.downHold}")
        sb.appendLine("  downRel: ${stwButton.downRel}")
        sb.appendLine("  rtHold: ${stwButton.rtHold}")
        sb.appendLine("  rtRel: ${stwButton.rtRel}")
        sb.appendLine("  rtOn: ${stwButton.rtOn}")
        sb.appendLine("  rtOff: ${stwButton.rtOff}")
        sb.appendLine("  speakPres: ${stwButton.speakPres}")
        sb.appendLine("  speakHold: ${stwButton.speakHold}")
        sb.appendLine("  speakRel: ${stwButton.speakRel}")

        sb.appendLine("GT State:")
        sb.appendLine("  isTvOn: ${gt.isTvOn}")
        sb.appendLine("  mode: ${gt.mode}")
        sb.appendLine("  modebm23: ${gt.modebm23}")

        sb.appendLine("--- End Car State ---")
        return sb.toString()
    }
}

data class CdChangerState(
    var isAlive: Boolean = false,
    var isPlaying: Boolean = false,
    var isPaused: Boolean = false,
    var isStopped: Boolean = false,
    var isScanning: Boolean = false,
    var isRandom: Boolean = false,
    var currentDisc: Int = 0,
    var currentTrack: Int = 0
) {
    fun copy(): CdChangerState {
        return CdChangerState(
            isAlive,
            isPlaying,
            isPaused,
            isStopped,
            isScanning,
            isRandom,
            currentDisc,
            currentTrack
        )
    }
}

enum class IgnitionState {
    UNKNOWN,
    OFF,
    ACC,
    ON,
    START
}

data class PdcState(
    var isTurnedOn: Boolean = false,
    var values: List<Int> = emptyList()
) {
    fun copy(): PdcState {
        return PdcState(
            isTurnedOn,
            values.toList()
        )
    }
}

data class BmButtonState(
    var nextHold: Boolean = false,
    var nextRel: Boolean = false,
    var prevHold: Boolean = false,
    var prevRel: Boolean = false,
    var reverseHold: Boolean = false,
    var reverseRel: Boolean = false,
    var dolbyPres: Boolean = false,
    var dolbyHold: Boolean = false,
    var dolbyRel: Boolean = false,
    var rdsPres: Boolean = false,
    var rdsHold: Boolean = false,
    var rdsRel: Boolean = false,
    var ejectPres: Boolean = false,
    var ejectHold: Boolean = false,
    var ejectRel: Boolean = false,
    var tonePres: Boolean = false,
    var toneHold: Boolean = false,
    var toneRel: Boolean = false,
    var selectPres: Boolean = false,
    var selectHold: Boolean = false,
    var selectRel: Boolean = false,
    var volRight: Int = 0,
    var volLeft: Int = 0,
    var volRel: Boolean = false,
    var volHold: Boolean = false,
    var modePres: Boolean = false,
    var modeHold: Boolean = false,
    var modeRel: Boolean = false,
    var fmPres: Boolean = false,
    var fmHold: Boolean = false,
    var fmRel: Boolean = false,
    var amPres: Boolean = false,
    var amHold: Boolean = false,
    var amRel: Boolean = false,
    var screenPres: Boolean = false,
    var screenHold: Boolean = false,
    var screenRel: Boolean = false,
    var button1Hold: Boolean = false,
    var button1Rel: Boolean = false,
    var button2Hold: Boolean = false,
    var button2Rel: Boolean = false,
    var button3Hold: Boolean = false,
    var button3Rel: Boolean = false,
    var button4Hold: Boolean = false,
    var button4Rel: Boolean = false,
    var button5Hold: Boolean = false,
    var button5Rel: Boolean = false,
    var button6Hold: Boolean = false,
    var button6Rel: Boolean = false,
) {
    fun copy(): BmButtonState {
        return BmButtonState(
            nextHold,
            nextRel,
            prevHold,
            prevRel,
            reverseHold,
            reverseRel,
            dolbyPres,
            dolbyHold,
            dolbyRel,
            rdsPres,
            rdsHold,
            rdsRel,
            ejectPres,
            ejectHold,
            ejectRel,
            tonePres,
            toneHold,
            toneRel,
            selectPres,
            selectHold,
            selectRel,
            volRight,
            volLeft,
            volRel,
            volHold,
            modePres,
            modeHold,
            modeRel,
            fmPres,
            fmHold,
            fmRel,
            amPres,
            amHold,
            amRel,
            screenPres,
            screenHold,
            screenRel,
            button1Hold,
            button1Rel,
            button2Hold,
            button2Rel,
            button3Hold,
            button3Rel,
            button4Hold,
            button4Rel,
            button5Hold,
            button5Rel,
            button6Hold,
            button6Rel
        )
    }
}

data class StwButtonState(
    var volUp: Boolean = false,
    var volDown: Boolean = false,
    var upPres: Boolean = false,
    var upHold: Boolean = false,
    var upRel: Boolean = false,
    var downPres: Boolean = false,
    var downHold: Boolean = false,
    var downRel: Boolean = false,
    var rtHold: Boolean = false,
    var rtRel: Boolean = false,
    var rtOn: Boolean = false,
    var rtOff: Boolean = false,
    var speakPres: Boolean = false,
    var speakHold: Boolean = false,
    var speakRel: Boolean = false
) {
    fun copy(): StwButtonState {
        return StwButtonState(
            volUp,
            volDown,
            upPres,
            upHold,
            upRel,
            downPres,
            downHold,
            downRel,
            rtHold,
            rtRel,
            rtOn,
            rtOff,
            speakPres,
            speakHold,
            speakRel
        )
    }
}

data class GtState(
    var isTvOn: Boolean = false,
    var mode: GtMode = GtMode.NONE,
    val modebm23: Boolean = false
) {
    fun copy(): GtState {
        return GtState(
            isTvOn,
            mode,
            modebm23
        )
    }
}

enum class GtMode {
    NONE,
    CD,
    CIS,
    TAPE,
    AUX,
    RADIO
}