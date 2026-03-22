package dev.drobek.geeflow.presentation.feature.device.settings.main

import dev.drobek.geeflow.domain.device.model.DeviceCapability
import dev.drobek.geeflow.domain.device.model.DeviceCapability.BrewBoiler
import dev.drobek.geeflow.domain.device.model.DeviceCapability.CleaningMode
import dev.drobek.geeflow.domain.device.model.DeviceCapability.CleaningSettings
import dev.drobek.geeflow.domain.device.model.DeviceCapability.CommercialGrinderConnectivity
import dev.drobek.geeflow.domain.device.model.DeviceCapability.HeatingMode
import dev.drobek.geeflow.domain.device.model.DeviceCapability.SingleDoseGrinderConnectivity
import dev.drobek.geeflow.domain.device.model.DeviceCapability.SmartScaleConnectivity
import dev.drobek.geeflow.domain.device.model.DeviceCapability.SteamBoiler
import dev.drobek.geeflow.domain.device.model.DeviceCapability.WaterAlarm
import dev.drobek.geeflow.domain.device.usecase.GetDeviceCapabilitiesUseCase
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.ItemClicked
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsViewState.Item
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.DeviceSettings
import dev.drobek.geeflow.viewmodel.BaseViewModel
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.device_settings_brewing
import geeflow.composeapp.generated.resources.device_settings_brewing_description
import geeflow.composeapp.generated.resources.device_settings_connectivity
import geeflow.composeapp.generated.resources.device_settings_connectivity_description
import geeflow.composeapp.generated.resources.device_settings_maintenance
import geeflow.composeapp.generated.resources.device_settings_maintenance_description
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class DeviceSettingsViewModel(
    @InjectedParam val args: DeviceSettings,
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
        buildOptions()
    }

    fun handleEvent(event: DeviceSettingsEvent) = when (event) {
        is BackClicked -> emitEvent(Navigation.Back)
        is ItemClicked -> when (event.item) {
            is Item.Brewing -> emitEvent(Navigation.BrewingSettings(args.deviceId))
            is Item.Maintenance -> emitEvent(Navigation.MaintenanceSettings(args.deviceId))
            is Item.Connectivity -> emitEvent(Navigation.ConnectivitySettings(args.deviceId))
        }
    }

    private fun buildOptions() = launch {
        val capabilities = getDeviceCapabilitiesUseCase()
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
