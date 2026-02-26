package dev.drobek.geeflow.platform.permissions

import androidx.compose.runtime.Composable
import dev.icerock.moko.permissions.bluetooth.BluetoothConnectPermission
import dev.icerock.moko.permissions.bluetooth.BluetoothScanPermission
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.ios.PermissionsControllerProtocol
import dev.icerock.moko.permissions.DeniedException as MokoDeniedException
import dev.icerock.moko.permissions.Permission as MokoPermission
import dev.icerock.moko.permissions.compose.BindEffect as MokoBindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory as MokoPermissionsControllerFactory

actual typealias Permission = MokoPermission
actual typealias PermissionsController = PermissionsControllerProtocol
actual typealias PermissionsControllerFactory = MokoPermissionsControllerFactory
actual typealias DeniedException = MokoDeniedException

@Composable
actual fun rememberPermissionsControllerFactory(): MokoPermissionsControllerFactory = rememberPermissionsControllerFactory()

@Composable
actual fun BindEffect(permissionsController: PermissionsController) = MokoBindEffect(permissionsController)

actual val PermissionBluetoothScan: MokoPermission get() = BluetoothScanPermission
actual val PermissionBluetoothConnect: MokoPermission get() = BluetoothConnectPermission

