package app.geeflow.data.device.impl.controller

object WendougeeRegisters {
    // Boiler & Heating States
    const val STEAM_BOILER_STATE = 0x0006
    const val BREW_BOILER_STATE = 0x0007
    const val STEAM_TEMPERATURE = 0x0008
    const val BREW_TEMPERATURE = 0x0009
    const val HEATING_MODE = 0x0016

    // Manual Brew Settings (button preset)
    const val MANUAL_BREW_TIME = 0x0011
    const val MANUAL_BREW_PRESSURE = 0x0013

    // Free Variable Brew - live target (2 registers: [pressure*10, 0] or [0, flow*10])
    const val FREE_VAR_TARGET_BASE = 0x058B
    const val FREE_VAR_PREPARE = 0x000F
    const val FREE_VAR_MODE = 0x05B3 // 0 = pressure, 1 = flow

    // Maintenance
    const val CLEANING_TIME = 0x0000
    const val CLEANING_STANDBY_TIME = 0x0001
    const val CLEANING_COUNT = 0x0002
    const val WATER_ALARM = 0x018C

    // Profiles & Binding
    const val BIND_PROFILE = 30

    // Profile Upload: Constant/Variable Modes (Base Register)
    const val CONSTANT_MODE_BASE = 2048
    const val CONSTANT_MODE_BOUND_BASE = 2560

    // Profile Upload: Free Variable Mode Flags
    const val FV_FINISH_CONDITION = 79
    const val FV_PROFILE_MODE = 87
    const val BOUND_PROFILE_MODE = 88
    const val FV_TARGET_VALUE = 358
    const val FV_CONTROL_MODE = 362 // Captures: 0 = pressure, 1 = flow
    const val FV_TABLE_ENDS = 356 // Captured value 0x7f7f in both control modes
    const val FV_AUTO_LINK = 366

    // Profile Upload: Free Variable Memory Regions
    val FV_MEMORY_REGIONS = listOf(
        760, // Pressure First Half
        824, // Pressure Second Half
        888, // Cumulative Volume First Half
        952, // Cumulative Volume Second Half
        1016, // Weight First Half
        1080, // Weight Second Half
        1500, // Absolute Flow First Half
        1564, // Absolute Flow Second Half
    )
}
