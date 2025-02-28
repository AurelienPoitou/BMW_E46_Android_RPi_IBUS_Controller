package com.trentseed.bmw_rpi_ibus_controller.car

import java.time.LocalDateTime

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
    val gt: GtState,
    val keyFob: KeyfobState,
    val navigation: NavigationState,
    val immobilizer: ImmobilizerState
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
        GtState(),
        KeyfobState(),
        NavigationState(),
        ImmobilizerState()
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
            gt.copy(),
            keyFob.copy(),
            navigation.copy(),
            immobilizer.copy()
        )
    }
}

data class NavigationState(
    var gpsFix: Boolean = false,
    var latitude: String = "",
    var longitude: String = "",
    var altitude: Int = -1,
    var datetime: LocalDateTime = LocalDateTime.now(),
    var city: String = "",
    var street: String = "",
) {
    fun copy(): NavigationState {
        return NavigationState(
            gpsFix,
            latitude,
            longitude,
            altitude,
            datetime,
            city,
            street
        )
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

data class ImmobilizerState(
    var keyPosition: IgnitionState = IgnitionState.UNKNOWN,
    var keyNumber: Int = 0
) {
    fun copy(): ImmobilizerState {
        return ImmobilizerState(
            keyPosition,
            keyNumber
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

data class KeyfobState(
    var keyNumber: Int = 0,
    var lowBattery: Boolean = false,
    var lockButtonPressed: Boolean = false,
    var unlockButtonPressed: Boolean = false,
    var trunkButtonPressed: Boolean = false
) {
    fun copy(): KeyfobState {
        return KeyfobState(
            keyNumber,
            lowBattery,
            lockButtonPressed,
            unlockButtonPressed,
            trunkButtonPressed
        )
    }
}
