package dev.drobek.geeflow.data.device.impl.controller

object WendougeeRegisters {
    // Boiler & Heating States
    const val STEAM_BOILER_STATE = 0x0006
    const val BREW_BOILER_STATE = 0x0007
    const val STEAM_TEMPERATURE = 0x0008
    const val BREW_TEMPERATURE = 0x0009
    const val HEATING_MODE = 0x0016

    // Manual Brew Settings
    const val MANUAL_BREW_TIME = 0x0011
    const val MANUAL_BREW_PRESSURE = 0x0013

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
    const val FV_OFFSET_ZEROING = 362
    const val FV_AUTO_LINK = 366

    // Profile Upload: Free Variable Memory Regions
    val FV_MEMORY_REGIONS = listOf(
        760,  // Pressure First Half
        824,  // Pressure Second Half
        888,  // Relative Flow First Half
        952,  // Relative Flow Second Half
        1016, // Weight First Half
        1080, // Weight Second Half
        1500, // Absolute Flow First Half
        1564  // Absolute Flow Second Half
    )
}
