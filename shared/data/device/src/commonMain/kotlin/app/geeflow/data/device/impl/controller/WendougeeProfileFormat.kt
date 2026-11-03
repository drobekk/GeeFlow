package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.model.SupportedDevice

enum class WendougeeProfileFormat { Legacy, Packed }

internal fun resolveWendougeeProfileFormat(model: SupportedDevice, firmware: Int?): WendougeeProfileFormat =
    if (model == SupportedDevice.WendougeeDataS && firmware != null && firmware >= DATA_S_PACKED_PROFILE_MIN_FIRMWARE) {
        WendougeeProfileFormat.Packed
    } else {
        WendougeeProfileFormat.Legacy
    }

// Confirmed for Data-S 18706 (funcCode 100229); compatibility with later boards is assumed.
private const val DATA_S_PACKED_PROFILE_MIN_FIRMWARE = 18706
