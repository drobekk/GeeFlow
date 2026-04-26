package app.geeflow.core.presentation

import app.geeflow.domain.exception.DeviceNotConnectedException
import app.geeflow.domain.exception.ScaleNotConnectedException
import geeflow.core.ui.generated.resources.Res
import geeflow.core.ui.generated.resources.error_connect_device
import geeflow.core.ui.generated.resources.error_generic
import geeflow.core.ui.generated.resources.error_scale_not_connected
import org.jetbrains.compose.resources.getString

suspend fun Throwable.toUserMessage(): String = when (this) {
    is DeviceNotConnectedException -> getString(Res.string.error_connect_device)
    is ScaleNotConnectedException -> getString(Res.string.error_scale_not_connected)
    else -> getString(Res.string.error_generic)
}
