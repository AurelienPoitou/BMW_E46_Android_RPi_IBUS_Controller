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
)

data class InstrumentClusterState(
    var speed: Int = 0,
    var rpm: Int = 0,
    var fuelLevel: Int = 0,
    var coolantTemp: Int = 0,
    var outsideTemp: Int = 0,
    var odometer: Int = 0,
    var tripOdometer: Int = 0,
    var serviceInterval: Int = 0,
    var gear: Int = 0,
    var checkControlMessages: List<String> = emptyList()
)

data class LightControlModuleState(
    var headlights: LightStatus = LightStatus.OFF,
    var fogLights: Boolean = false,
    var turnSignalLeft: Boolean = false,
    var turnSignalRight: Boolean = false,
    var hazardLights: Boolean = false,
    var interiorLights: Boolean = false,
    var brakeLights: Boolean = false
)

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
)

data class HvacState(
    var mode: HvacMode = HvacMode.OFF,
    var fanSpeed: HvacFanSpeed = HvacFanSpeed.OFF,
    var temperatureDriver: Int = 0,
    var temperaturePassenger: Int = 0,
    var airDistribution: HvacAirDistribution = HvacAirDistribution.FACE,
    var ac: Boolean = false,
    var recirculate: Boolean = false,
    var defrost: Boolean = false
)

data class MultiFunctionSteeringWheelState(
    var buttonPressed: SteeringWheelButton = SteeringWheelButton.NONE
)