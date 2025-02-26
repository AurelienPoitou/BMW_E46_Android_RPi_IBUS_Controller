package com.trentseed.bmw_rpi_ibus_controller.car

enum class LightStatus {
    OFF,
    PARKING,
    LOW_BEAM,
    HIGH_BEAM
}

enum class DoorStatus {
    OPEN,
    CLOSED
}

enum class WindowStatus {
    OPEN,
    CLOSED,
    VENT
}

enum class LockStatus {
    LOCKED,
    UNLOCKED
}

enum class RadioBand {
    AM,
    FM,
    DAB
}

enum class HvacMode {
    OFF,
    AUTO,
    MANUAL
}

enum class HvacFanSpeed {
    OFF,
    LOW,
    MEDIUM,
    HIGH
}

enum class HvacAirDistribution {
    FACE,
    FEET,
    DEFROST
}

enum class SteeringWheelButton {
    NONE,
    VOLUME_UP,
    VOLUME_DOWN,
    SEEK_UP,
    SEEK_DOWN,
    MODE
}