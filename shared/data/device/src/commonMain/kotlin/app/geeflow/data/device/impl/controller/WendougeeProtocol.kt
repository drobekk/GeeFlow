package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceConstraints

internal const val TAG = "WendougeeController"
internal const val BLE_TRACE_TAG = "WendougeeBle"

internal const val DATA_UUID_SUFFIX = "2b10"
internal const val CTRL_UUID_SUFFIX = "2c10"

internal const val POLLING_INTERVAL_MS = 200L
internal const val LIVE_POLLING_INTERVAL_MS = 70L
internal const val HEARTBEAT_TIMEOUT_MS = 10_000L

internal const val MTU_SIZE = 512
internal const val MTU_TIMEOUT_MS = 4_000L
internal const val POST_MTU_DELAY_MS = 500L
internal const val CHARS_READY_TIMEOUT_MS = 10_000L
internal const val CONNECT_TIMEOUT_MS = 10_000L
internal const val CONNECT_RETRY_DELAY_MS = 100L
internal const val POST_CONNECT_SETTLE_MS = 500L
internal const val CHARS_RETRY_DELAY_MS = 200L
internal const val STALE_GATT_SETTLE_MS = 300L
internal const val CCCD_SETTLE_DELAY_MS = 200L
internal const val SCALE_LIST_DELAY_MS = 200L
internal const val POST_INIT_DELAY_MS = 300L
internal const val NOTIFY_RESET_DELAY_MS = 100L
internal const val POLL_BETWEEN_DELAY_MS = 30L
internal const val POLL_TIMEOUT_MS = 800L
internal const val INIT_POLL_TIMEOUT_MS = 3000L
internal const val SCAN_TIMEOUT_MS = 12_000L
internal const val WATCHDOG_INTERVAL_MS = 1_000L
internal const val MODBUS_UNIT_ID: Byte = 0x01
internal const val CONFIG_REGISTER_COUNT = 37
internal const val TELEMETRY_REGISTER_BASE = 1404
internal const val TELEMETRY_REGISTER_COUNT = 20
internal const val STATUS_COIL_BASE = 182
internal const val STATUS_COIL_COUNT = 24
internal const val SENSOR_SCALE_FACTOR = 10
internal const val BREW_MAX_TEMP = 110
internal const val STEAM_MAX_TEMP = 140
internal const val PADDLE_PRESSURE_MAX_INT = 120
internal const val PADDLE_TIME_MAX = 60
internal const val FREE_BREW_PRESSURE_MAX = 12f
internal const val FREE_BREW_FLOW_MAX = 12f
internal const val CLEANING_TIME_MAX = 60
internal const val CLEANING_REST_MAX = 60
internal const val CLEANING_COUNT_MAX = 10
internal const val SCALE_SEARCH_AFTER_ENABLE_DELAY_MS = 200L
internal const val SCALE_SETTINGS_TIMEOUT_MS = 2000L
internal const val COIL_MANUAL_BREW = 0x9A
internal const val COIL_FREE_VAR_BREW = 0x9D
internal const val FREE_VAR_PREPARE_VALUE = 4
internal const val COIL_SHORT_PRESS = 0x96
internal const val COIL_CLEANING = 0x9B
internal const val TRACE_BYTE_MASK = 0xFF
internal const val HEX_RADIX = 16
internal const val SCALE_CMD_CONNECT = 0x80
internal const val SCALE_CMD_DISCONNECT = 0x87

internal const val WRITE_TYPE_NO_RESPONSE = 1

internal fun toHexString(arr: ByteArray): String = arr.joinToString("") {
    (it.toInt() and TRACE_BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
}

internal val wendougeeConstraints: DeviceConstraints = DeviceConstraints(
    brewTempRange = 0..BREW_MAX_TEMP,
    steamTempRange = 0..STEAM_MAX_TEMP,
    manualBrewPressureRange = 1..PADDLE_PRESSURE_MAX_INT,
    manualBrewTimeRange = 0..PADDLE_TIME_MAX,
    cleaningTimeRange = 1..CLEANING_TIME_MAX,
    cleaningRestRange = 1..CLEANING_REST_MAX,
    cleaningCountRange = 1..CLEANING_COUNT_MAX,
    pressureRange = 0f..FREE_BREW_PRESSURE_MAX,
    flowRange = 0f..FREE_BREW_FLOW_MAX,
)

internal val wendougeeCapabilities: Set<DeviceCapability> = setOf(
    DeviceCapability.SteamBoiler,
    DeviceCapability.BrewBoiler,
    DeviceCapability.WaterAlarm,
    DeviceCapability.CleaningSettings,
    DeviceCapability.ManualBrewing,
    DeviceCapability.ProfileBrewing,
    DeviceCapability.CleaningMode,
    DeviceCapability.HeatingMode,
    DeviceCapability.SmartScaleConnectivity,
    DeviceCapability.SingleDoseGrinderConnectivity,
    DeviceCapability.CommercialGrinderConnectivity,
    DeviceCapability.PressureProfiling,
    DeviceCapability.FlowProfiling,
)
