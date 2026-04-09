package dev.drobek.geeflow.platform.permissions

import androidx.compose.runtime.Composable
import dev.icerock.moko.permissions.bluetooth.BluetoothConnectPermission
import dev.icerock.moko.permissions.bluetooth.BluetoothScanPermission
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.DeniedException as MokoDeniedException
import dev.icerock.moko.permissions.Permission as MokoPermission
import dev.icerock.moko.permissions.PermissionsController as MokoPermissionController
import dev.icerock.moko.permissions.compose.BindEffect as MokoBindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory as MokoPermissionsControllerFactory

actual typealias Permission = MokoPermission
actual typealias PermissionsController = MokoPermissionController
actual typealias PermissionsControllerFactory = MokoPermissionsControllerFactory
actual typealias DeniedException = MokoDeniedException

@Composable
actual fun rememberPermissionsControllerFactory(): PermissionsControllerFactory = rememberPermissionsControllerFactory()

@Composable
actual fun BindEffect(permissionsController: MokoPermissionController) = MokoBindEffect(permissionsController)

actual val PermissionBluetoothScan: MokoPermission get() = BluetoothScanPermission
actual val PermissionBluetoothConnect: MokoPermission get() = BluetoothConnectPermission
