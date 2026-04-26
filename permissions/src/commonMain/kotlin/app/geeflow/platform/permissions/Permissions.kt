package app.geeflow.platform.permissions

import androidx.compose.runtime.Composable

expect interface PermissionsController {
    suspend fun providePermission(permission: Permission)
    fun openAppSettings()
}

expect interface PermissionsControllerFactory {
    fun createPermissionsController(): PermissionsController
}

@Composable
expect fun rememberPermissionsControllerFactory(): PermissionsControllerFactory

@Composable
expect fun BindEffect(permissionsController: PermissionsController)

expect interface Permission

expect val PermissionBluetoothScan: Permission
expect val PermissionBluetoothConnect: Permission

expect class DeniedException : Exception {
    val permission: Permission
}
