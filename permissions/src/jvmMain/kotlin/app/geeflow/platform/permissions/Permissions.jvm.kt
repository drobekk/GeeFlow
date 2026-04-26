package app.geeflow.platform.permissions

import androidx.compose.runtime.Composable

/** Stubs for JVM because Moko does not provide them. No runtime permissions are required for this platform. */
actual interface PermissionsController {
    actual suspend fun providePermission(permission: Permission)
    actual fun openAppSettings()
}

actual interface PermissionsControllerFactory {
    actual fun createPermissionsController(): PermissionsController
}

private class PermissionsControllerImpl : PermissionsController {
    override suspend fun providePermission(permission: Permission) {
        // no-op on JVM
    }

    override fun openAppSettings() {
        // no-op on JVM
    }
}

private class PermissionsControllerFactoryImpl : PermissionsControllerFactory {
    override fun createPermissionsController(): PermissionsController = PermissionsControllerImpl()
}

@Composable
actual fun rememberPermissionsControllerFactory(): PermissionsControllerFactory = PermissionsControllerFactoryImpl()

@Composable
actual fun BindEffect(permissionsController: PermissionsController) {
    // no-op on JVM
}

actual interface Permission

private class PermissionImpl : Permission

actual val PermissionBluetoothScan: Permission get() = PermissionImpl()
actual val PermissionBluetoothConnect: Permission get() = PermissionImpl()

actual class DeniedException(actual val permission: Permission) : Exception()
