package dev.drobek.geeflow.presentation.feature.device.settings.main

import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.core.presentation.launch
import dev.drobek.geeflow.data.device.model.DeviceCapability
import dev.drobek.geeflow.data.device.model.DeviceCapability.BrewBoiler
import dev.drobek.geeflow.data.device.model.DeviceCapability.CleaningMode
import dev.drobek.geeflow.data.device.model.DeviceCapability.CleaningSettings
import dev.drobek.geeflow.data.device.model.DeviceCapability.CommercialGrinderConnectivity
import dev.drobek.geeflow.data.device.model.DeviceCapability.HeatingMode
import dev.drobek.geeflow.data.device.model.DeviceCapability.SingleDoseGrinderConnectivity
import dev.drobek.geeflow.data.device.model.DeviceCapability.SmartScaleConnectivity
import dev.drobek.geeflow.data.device.model.DeviceCapability.SteamBoiler
import dev.drobek.geeflow.data.device.model.DeviceCapability.WaterAlarm
import dev.drobek.geeflow.domain.device.usecase.GetDeviceCapabilitiesUseCase
import dev.drobek.geeflow.domain.device.usecase.GetDeviceUseCase
import dev.drobek.geeflow.navigation.destination.DeviceSettings
import dev.drobek.geeflow.navigation.destination.DeviceSettings.EntryPoint
import dev.drobek.geeflow.presentation.feature.device.settings.BrewingSettings
import dev.drobek.geeflow.presentation.feature.device.settings.ConnectivitySettings
import dev.drobek.geeflow.presentation.feature.device.settings.MaintenanceSettings
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.ItemClicked
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsViewState.Item
import geeflow.feature.device.settings.generated.resources.Res
import geeflow.feature.device.settings.generated.resources.device_settings_brewing
import geeflow.feature.device.settings.generated.resources.device_settings_brewing_description
import geeflow.feature.device.settings.generated.resources.device_settings_connectivity
import geeflow.feature.device.settings.generated.resources.device_settings_connectivity_description
import geeflow.feature.device.settings.generated.resources.device_settings_maintenance
import geeflow.feature.device.settings.generated.resources.device_settings_maintenance_description
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class DeviceSettingsViewModel(
    @InjectedParam val args: DeviceSettings,
    getDeviceUseCase: GetDeviceUseCase,
    private val getDeviceCapabilitiesUseCase: GetDeviceCapabilitiesUseCase
) : BaseViewModel<DeviceSettingsViewState, DeviceLitViewModelEvent>(DeviceSettingsViewState()) {

    private val connectivityCapabilities = setOf(
        SmartScaleConnectivity,
        SingleDoseGrinderConnectivity,
        CommercialGrinderConnectivity
    )
    private val brewingCapabilities = setOf(SteamBoiler, BrewBoiler, HeatingMode)
    private val maintenanceCapabilities = setOf(CleaningMode, CleaningSettings, WaterAlarm)

    init {
        modify { copy(deviceName = getDeviceUseCase(args.deviceId)?.name.orEmpty()) }
        buildOptions()

        when (args.entryPoint) {
            EntryPoint.Brewing -> navigateTo(BrewingSettings(args.deviceId))
            EntryPoint.Connectivity -> navigateTo(ConnectivitySettings(args.deviceId))
            EntryPoint.Maintenance -> navigateTo(MaintenanceSettings(args.deviceId))
            null -> Unit
        }
    }

    fun handleEvent(event: DeviceSettingsEvent) = when (event) {
        is BackClicked -> popTo(args, true)
        is ItemClicked -> onItemClicked(event.item)
    }

    private fun onItemClicked(item: Item) {
        popTo(args, false)
        when (item) {
            is Item.Brewing -> navigateTo(BrewingSettings(args.deviceId))
            is Item.Maintenance -> navigateTo(MaintenanceSettings(args.deviceId))
            is Item.Connectivity -> navigateTo(ConnectivitySettings(args.deviceId))
        }
    }

    private fun buildOptions() = launch {
        val capabilities = getDeviceCapabilitiesUseCase(args.deviceId)
        val items = mutableListOf<Item>()

        if (capabilities.hasBrewingCapabilities()) {
            items.add(
                Item.Brewing(
                    name = getString(Res.string.device_settings_brewing),
                    description = getString(Res.string.device_settings_brewing_description)
                )
            )
        }

        if (capabilities.hasMaintenanceCapabilities()) {
            items.add(
                Item.Maintenance(
                    name = getString(Res.string.device_settings_maintenance),
                    description = getString(Res.string.device_settings_maintenance_description)
                )
            )
        }

        if (capabilities.hasConnectivityCapabilities()) {
            items.add(
                Item.Connectivity(
                    name = getString(Res.string.device_settings_connectivity),
                    description = getString(Res.string.device_settings_connectivity_description)
                )
            )
        }
        modify { copy(items = items) }
    }

    private fun Set<DeviceCapability>.hasMaintenanceCapabilities() = (this intersect maintenanceCapabilities).isNotEmpty()

    private fun Set<DeviceCapability>.hasConnectivityCapabilities() = (this intersect connectivityCapabilities).isNotEmpty()

    private fun Set<DeviceCapability>.hasBrewingCapabilities() = (this intersect brewingCapabilities).isNotEmpty()
}
