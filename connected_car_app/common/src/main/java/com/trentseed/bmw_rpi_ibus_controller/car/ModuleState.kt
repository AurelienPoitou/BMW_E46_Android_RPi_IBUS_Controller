package com.trentseed.bmw_rpi_ibus_controller.car

data class RadioState(
    var power: Boolean = false,
    var band: RadioBand = RadioBand.FM,
    var frequency: Double = 0.0,
    var volume: Int = 0,
    var stationName: String = "",
    var mute: Boolean = false,
    var cdChangerTrack: Int = 0,
    var cdChangerDisc: Int = 0
) {
    fun copy(): RadioState {
        return RadioState(
            power,
            band,
            frequency,
            volume,
            stationName,
            mute,
            cdChangerTrack,
            cdChangerDisc
        )
    }
}

data class InstrumentClusterState(
    var speed: Int = 0,
    var rpm: Int = 0,
    var fuelLevel: Int = 0,
    var coolantTemp: Int = 0,
    var outsideTemp: Int = 0,
    var odometer: Int = 0,
    var tripOdometer: Int = 0,
    var gear: Int = 0,
    var engineRunning: Boolean = true,
    var handbrakeOn: Boolean = true,
    var vin: String = "",
    var countryCoding: String = "",
    var ignitionState: IgnitionState = IgnitionState.UNKNOWN,
    var serviceInterval: Int = 0,
    var serviceIntervalType: Int = 0,
    var serviceIntervalDays: Int = 0,
    var checkControlMessages: List<String> = emptyList()
) {

    fun copy(): InstrumentClusterState {
        return InstrumentClusterState(
            speed,
            rpm,
            fuelLevel,
            coolantTemp,
            outsideTemp,
            odometer,
            tripOdometer,
            gear,
            engineRunning,
            handbrakeOn,
            vin,
            countryCoding,
            ignitionState,
            serviceInterval,
            serviceIntervalDays,
            serviceIntervalType,
            checkControlMessages
        )
    }
}

data class LightControlModuleState(
    // Vehicle Data
    var vin: String = "XX12345",
    var odometer: Int = 0,
    var sinceServiceDays: Int = 0,
    var sinceServiceLiters: Int = 0,
    // Lights
    var frontPositionLights: Boolean = false,
    var lowBeamLights: Boolean = false,
    var highBeamLights: Boolean = false,
    var frontFogLights: Boolean = false,
    var rearFogLights: Boolean = false,
    var leftTurnSignal: Boolean = false,
    var rightTurnSignal: Boolean = false,
    var turnSignalFast: Boolean = false,
    var brakeLights: Boolean = false,
    var turnSignalSync: Boolean = false,
    var rearPositionLights: Boolean = false,
    var trailerPositionLights: Boolean = false,
    var reverseLights: Boolean = false,
    var trailerReverseLights: Boolean = false,
    var hazardLights: Boolean = false,
    // Faults
    var frontPositionFault: Boolean = false,
    var lowBeamFault: Boolean = false,
    var highBeamFault: Boolean = false,
    var frontFogFault: Boolean = false,
    var rearFogFault: Boolean = false,
    var leftTurnSignalFault: Boolean = false,
    var rightTurnSignalFault: Boolean = false,
    var licencePlateFault: Boolean = false,
    var brakeLightRightFault: Boolean = false,
    var brakeLightLeftFault: Boolean = false,
    var rearRightPositionFault: Boolean = false,
    var rearLeftPositionFault: Boolean = false,
    var rightLowBeamFault: Boolean = false,
    var leftLowBeamFault: Boolean = false,
    // Switches
    var positionSwitch: Boolean = false,
    var lowBeam1Switch: Boolean = false,
    var lowBeam2Switch: Boolean = false,
    var highBeamSwitch: Boolean = false,
    var frontFogSwitch: Boolean = false,
    var rearFogSwitch: Boolean = false,
    var leftTurnSignalSwitch: Boolean = false,
    var rightTurnSignalSwitch: Boolean = false,
    var brakeLightSwitch: Boolean = false,
    var highBeamFlashSwitch: Boolean = false,
    var hazardSwitch: Boolean = false,
    // Inputs
    var fireExtinguisher: Boolean = false,
    var preHeatingFuelInjection: Boolean = false,
    var carb: Boolean = false,
    var keyInIgnition: Boolean = false,
    var seatBeltsLock: Boolean = false,
    var kfn: Boolean = false,
    var armouredDoor: Boolean = false,
    var brakeFluidLevel: Boolean = false,
    var airSuspension: Boolean = false,
    var holdUpAlarm: Boolean = false,
    var washerFluidLevel: Boolean = false,
    var engineFailSafe: Boolean = false,
    var tyreDefect: Boolean = false,
    var verticalAim: Boolean = false,
    var failsafeMode: Boolean = false,
    var sleepMode: Boolean = false,
    // Outputs
    var frontLeftPosition: Boolean = false,
    var frontRightPosition: Boolean = false,
    var leftLowBeam: Boolean = false,
    var rightLowBeam: Boolean = false,
    var leftHighBeam: Boolean = false,
    var rightHighBeam: Boolean = false,
    var frontLeftFog: Boolean = false,
    var frontRightFog: Boolean = false,
    var frontLeftTurnSignal: Boolean = false,
    var frontRightTurnSignal: Boolean = false,
    var rearLeftPosition: Boolean = false,
    var rearRightPosition: Boolean = false,
    var rearLeftInnerPosition: Boolean = false,
    var rearRightInnerPosition: Boolean = false,
    var rearLeftFog: Boolean = false,
    var rearRightFog: Boolean = false,
    var leftReverse: Boolean = false,
    var rightReverse: Boolean = false,
    var rearLeftTurnSignal: Boolean = false,
    var rearRightTurnSignal: Boolean = false,
    var leftBrake: Boolean = false,
    var rightBrake: Boolean = false,
    var centerBrake: Boolean = false,
    var rearLeftLicence: Boolean = false,
    var rightLicence: Boolean = false,
    var trailerReverse: Boolean = false,
    var trailerFog: Boolean = false,
    var sideLeftTurnSignal: Boolean = false,
    var sideRightTurnSignal: Boolean = false
) {


    fun copy(): LightControlModuleState {
        return LightControlModuleState(
            vin,
            odometer,
            sinceServiceDays,
            sinceServiceLiters,
            frontPositionLights,
            lowBeamLights,
            highBeamLights,
            frontFogLights,
            rearFogLights,
            leftTurnSignal,
            rightTurnSignal,
            turnSignalFast,
            brakeLights,
            turnSignalSync,
            rearPositionLights,
            trailerPositionLights,
            reverseLights,
            trailerReverseLights,
            hazardLights,
            frontPositionFault,
            lowBeamFault,
            highBeamFault,
            frontFogFault,
            rearFogFault,
            leftTurnSignalFault,
            rightTurnSignalFault,
            licencePlateFault,
            brakeLightRightFault,
            brakeLightLeftFault,
            rearRightPositionFault,
            rearLeftPositionFault,
            rightLowBeamFault,
            leftLowBeamFault,
            positionSwitch,
            lowBeam1Switch,
            lowBeam2Switch,
            highBeamSwitch,
            frontFogSwitch,
            rearFogSwitch,
            leftTurnSignalSwitch,
            rightTurnSignalSwitch,
            brakeLightSwitch,
            highBeamFlashSwitch,
            hazardSwitch,
            fireExtinguisher,
            preHeatingFuelInjection,
            carb,
            keyInIgnition,
            seatBeltsLock,
            kfn,
            armouredDoor,
            brakeFluidLevel,
            airSuspension,
            holdUpAlarm,
            washerFluidLevel,
            engineFailSafe,
            tyreDefect,
            verticalAim,
            failsafeMode,
            sleepMode,
            frontLeftPosition,
            frontRightPosition,
            leftLowBeam,
            rightLowBeam,
            leftHighBeam,
            rightHighBeam,
            frontLeftFog,
            frontRightFog,
            frontLeftTurnSignal,
            frontRightTurnSignal,
            rearLeftPosition,
            rearRightPosition,
            rearLeftInnerPosition,
            rearRightInnerPosition,
            rearLeftFog,
            rearRightFog,
            leftReverse,
            rightReverse,
            rearLeftTurnSignal,
            rearRightTurnSignal,
            leftBrake,
            rightBrake,
            centerBrake,
            rearLeftLicence,
            rightLicence,
            trailerReverse,
            trailerFog,
            sideLeftTurnSignal,
            sideRightTurnSignal
        )
    }
}

data class GeneralModuleState(
    var doorDriver: DoorStatus = DoorStatus.CLOSED,
    var doorPassenger: DoorStatus = DoorStatus.CLOSED,
    var doorRearLeft: DoorStatus = DoorStatus.CLOSED,
    var doorRearRight: DoorStatus = DoorStatus.CLOSED,
    var trunk: DoorStatus = DoorStatus.CLOSED,
    var hood: DoorStatus = DoorStatus.CLOSED,
    var windowDriver: WindowStatus = WindowStatus.CLOSED,
    var windowPassenger: WindowStatus = WindowStatus.CLOSED,
    var windowRearLeft: WindowStatus = WindowStatus.CLOSED,
    var windowRearRight: WindowStatus = WindowStatus.CLOSED,
    var lockStatus: LockStatus = LockStatus.LOCKED,
    var sunroof: WindowStatus = WindowStatus.CLOSED
) {
    fun copy(): GeneralModuleState {
        return GeneralModuleState(
            doorDriver,
            doorPassenger,
            doorRearLeft,
            doorRearRight,
            trunk,
            hood,
            windowDriver,
            windowPassenger,
            windowRearLeft,
            windowRearRight,
            lockStatus,
            sunroof
        )
    }
}

data class HvacState(
    var mode: HvacMode = HvacMode.OFF,
    var fanSpeed: HvacFanSpeed = HvacFanSpeed.OFF,
    var temperatureDriver: Int = 0,
    var temperaturePassenger: Int = 0,
    var airDistribution: HvacAirDistribution = HvacAirDistribution.FACE,
    var ac: Boolean = false,
    var recirculate: Boolean = false,
    var defrost: Boolean = false
) {
    fun copy(): HvacState {
        return HvacState(
            mode,
            fanSpeed,
            temperatureDriver,
            temperaturePassenger,
            airDistribution,
            ac,
            recirculate,
            defrost
        )
    }
}

data class MultiFunctionSteeringWheelState(
    var buttonPressed: SteeringWheelButton = SteeringWheelButton.NONE
) {
    fun copy(): MultiFunctionSteeringWheelState {
        return MultiFunctionSteeringWheelState(
            buttonPressed
        )
    }
}