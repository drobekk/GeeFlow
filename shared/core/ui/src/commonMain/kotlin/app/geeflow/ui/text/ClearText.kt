package app.geeflow.ui.text

import androidx.compose.runtime.Composable
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.clear_avg_cup_flow
import geeflow.shared.core.ui.generated.resources.clear_back
import geeflow.shared.core.ui.generated.resources.clear_bind
import geeflow.shared.core.ui.generated.resources.clear_boiler_temps
import geeflow.shared.core.ui.generated.resources.clear_bound
import geeflow.shared.core.ui.generated.resources.clear_brew_boiler
import geeflow.shared.core.ui.generated.resources.clear_busy
import geeflow.shared.core.ui.generated.resources.clear_charts
import geeflow.shared.core.ui.generated.resources.clear_clean
import geeflow.shared.core.ui.generated.resources.clear_clear_search
import geeflow.shared.core.ui.generated.resources.clear_close
import geeflow.shared.core.ui.generated.resources.clear_control
import geeflow.shared.core.ui.generated.resources.clear_copy
import geeflow.shared.core.ui.generated.resources.clear_cup_flow
import geeflow.shared.core.ui.generated.resources.clear_cup_flow_unit
import geeflow.shared.core.ui.generated.resources.clear_cup_output
import geeflow.shared.core.ui.generated.resources.clear_cup_weight_chart
import geeflow.shared.core.ui.generated.resources.clear_cup_weight_unit
import geeflow.shared.core.ui.generated.resources.clear_decrease_tenth
import geeflow.shared.core.ui.generated.resources.clear_delete
import geeflow.shared.core.ui.generated.resources.clear_details
import geeflow.shared.core.ui.generated.resources.clear_device_disconnected
import geeflow.shared.core.ui.generated.resources.clear_devices
import geeflow.shared.core.ui.generated.resources.clear_disconnected_controls
import geeflow.shared.core.ui.generated.resources.clear_disconnected_flush
import geeflow.shared.core.ui.generated.resources.clear_done
import geeflow.shared.core.ui.generated.resources.clear_drag_hold
import geeflow.shared.core.ui.generated.resources.clear_edit
import geeflow.shared.core.ui.generated.resources.clear_elapsed_seconds
import geeflow.shared.core.ui.generated.resources.clear_experimental
import geeflow.shared.core.ui.generated.resources.clear_fine
import geeflow.shared.core.ui.generated.resources.clear_fine_quarter
import geeflow.shared.core.ui.generated.resources.clear_flow_unit
import geeflow.shared.core.ui.generated.resources.clear_flush
import geeflow.shared.core.ui.generated.resources.clear_flush_charts_paused
import geeflow.shared.core.ui.generated.resources.clear_flush_duration
import geeflow.shared.core.ui.generated.resources.clear_flush_last_shot
import geeflow.shared.core.ui.generated.resources.clear_flush_manual
import geeflow.shared.core.ui.generated.resources.clear_flush_timed
import geeflow.shared.core.ui.generated.resources.clear_flush_untimed
import geeflow.shared.core.ui.generated.resources.clear_flushing
import geeflow.shared.core.ui.generated.resources.clear_hot_water
import geeflow.shared.core.ui.generated.resources.clear_increase_tenth
import geeflow.shared.core.ui.generated.resources.clear_lita_ba_demo
import geeflow.shared.core.ui.generated.resources.clear_live_last_shot
import geeflow.shared.core.ui.generated.resources.clear_live_recording
import geeflow.shared.core.ui.generated.resources.clear_manual
import geeflow.shared.core.ui.generated.resources.clear_manual_control
import geeflow.shared.core.ui.generated.resources.clear_menu
import geeflow.shared.core.ui.generated.resources.clear_name
import geeflow.shared.core.ui.generated.resources.clear_no_preview
import geeflow.shared.core.ui.generated.resources.clear_no_scale
import geeflow.shared.core.ui.generated.resources.clear_not_available
import geeflow.shared.core.ui.generated.resources.clear_open_devices
import geeflow.shared.core.ui.generated.resources.clear_output
import geeflow.shared.core.ui.generated.resources.clear_pad_decrease
import geeflow.shared.core.ui.generated.resources.clear_pad_description
import geeflow.shared.core.ui.generated.resources.clear_pad_increase
import geeflow.shared.core.ui.generated.resources.clear_pause_only
import geeflow.shared.core.ui.generated.resources.clear_pressure
import geeflow.shared.core.ui.generated.resources.clear_pressure_target_value
import geeflow.shared.core.ui.generated.resources.clear_pressure_unit
import geeflow.shared.core.ui.generated.resources.clear_profiles
import geeflow.shared.core.ui.generated.resources.clear_programmed_targets
import geeflow.shared.core.ui.generated.resources.clear_range_flag
import geeflow.shared.core.ui.generated.resources.clear_ready_record
import geeflow.shared.core.ui.generated.resources.clear_recipe_blooming
import geeflow.shared.core.ui.generated.resources.clear_recipe_cremina
import geeflow.shared.core.ui.generated.resources.clear_recipe_espresso_lungo
import geeflow.shared.core.ui.generated.resources.clear_recipe_filter
import geeflow.shared.core.ui.generated.resources.clear_recipe_gentle
import geeflow.shared.core.ui.generated.resources.clear_recipe_italiano
import geeflow.shared.core.ui.generated.resources.clear_recipe_londinium
import geeflow.shared.core.ui.generated.resources.clear_recipe_rao
import geeflow.shared.core.ui.generated.resources.clear_recipe_slayer
import geeflow.shared.core.ui.generated.resources.clear_recipe_turbo
import geeflow.shared.core.ui.generated.resources.clear_recipe_zuppa
import geeflow.shared.core.ui.generated.resources.clear_recipe_zuppa_lungo
import geeflow.shared.core.ui.generated.resources.clear_recorded_profile
import geeflow.shared.core.ui.generated.resources.clear_save_profile
import geeflow.shared.core.ui.generated.resources.clear_scale_needed
import geeflow.shared.core.ui.generated.resources.clear_set_grinder
import geeflow.shared.core.ui.generated.resources.clear_settings
import geeflow.shared.core.ui.generated.resources.clear_shot_charts
import geeflow.shared.core.ui.generated.resources.clear_shot_details
import geeflow.shared.core.ui.generated.resources.clear_start_brew
import geeflow.shared.core.ui.generated.resources.clear_starting
import geeflow.shared.core.ui.generated.resources.clear_steam_boiler
import geeflow.shared.core.ui.generated.resources.clear_step
import geeflow.shared.core.ui.generated.resources.clear_stop_brew
import geeflow.shared.core.ui.generated.resources.clear_stop_flush
import geeflow.shared.core.ui.generated.resources.clear_syncing
import geeflow.shared.core.ui.generated.resources.clear_target
import geeflow.shared.core.ui.generated.resources.clear_target_pressure
import geeflow.shared.core.ui.generated.resources.clear_target_temp
import geeflow.shared.core.ui.generated.resources.clear_target_water_flow
import geeflow.shared.core.ui.generated.resources.clear_time
import geeflow.shared.core.ui.generated.resources.clear_toggle_chart_hint
import geeflow.shared.core.ui.generated.resources.clear_toggle_steam
import geeflow.shared.core.ui.generated.resources.clear_untimed_flush
import geeflow.shared.core.ui.generated.resources.clear_visible_charts
import geeflow.shared.core.ui.generated.resources.clear_water_flow
import geeflow.shared.core.ui.generated.resources.clear_water_output_chart
import geeflow.shared.core.ui.generated.resources.clear_water_volume
import geeflow.shared.core.ui.generated.resources.clear_water_volume_unit
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Shared labels for the customized control screens; translations remain in Compose resources. */
enum class ClearText(val resource: StringResource) {
    Pressure(Res.string.clear_pressure),
    WaterFlow(Res.string.clear_water_flow),
    Time(Res.string.clear_time),
    Output(Res.string.clear_output),
    Control(Res.string.clear_control),
    ShotCharts(Res.string.clear_shot_charts),
    DisconnectedControls(Res.string.clear_disconnected_controls),
    LiveRecording(Res.string.clear_live_recording),
    LiveLastShot(Res.string.clear_live_last_shot),
    ReadyRecord(Res.string.clear_ready_record),
    TargetPressure(Res.string.clear_target_pressure),
    TargetWaterFlow(Res.string.clear_target_water_flow),
    Fine(Res.string.clear_fine),
    FineQuarter(Res.string.clear_fine_quarter),
    DecreaseTenth(Res.string.clear_decrease_tenth),
    IncreaseTenth(Res.string.clear_increase_tenth),
    DragHold(Res.string.clear_drag_hold),
    StopBrew(Res.string.clear_stop_brew),
    StartBrew(Res.string.clear_start_brew),
    Starting(Res.string.clear_starting),
    Back(Res.string.clear_back),
    ManualControl(Res.string.clear_manual_control),
    Charts(Res.string.clear_charts),
    Name(Res.string.clear_name),
    SaveProfile(Res.string.clear_save_profile),
    VisibleCharts(Res.string.clear_visible_charts),
    Done(Res.string.clear_done),
    PressureUnit(Res.string.clear_pressure_unit),
    FlowUnit(Res.string.clear_flow_unit),
    CupFlowUnit(Res.string.clear_cup_flow_unit),
    WaterVolumeUnit(Res.string.clear_water_volume_unit),
    CupWeightUnit(Res.string.clear_cup_weight_unit),
    Details(Res.string.clear_details),
    ShotDetails(Res.string.clear_shot_details),
    CupFlow(Res.string.clear_cup_flow),
    AvgCupFlow(Res.string.clear_avg_cup_flow),
    CupOutput(Res.string.clear_cup_output),
    WaterVolume(Res.string.clear_water_volume),
    ToggleChartHint(Res.string.clear_toggle_chart_hint),
    Close(Res.string.clear_close),
    Profiles(Res.string.clear_profiles),
    DeviceDisconnected(Res.string.clear_device_disconnected),
    NoScale(Res.string.clear_no_scale),
    FlushLastShot(Res.string.clear_flush_last_shot),
    FlushChartsPaused(Res.string.clear_flush_charts_paused),
    Menu(Res.string.clear_menu),
    Clean(Res.string.clear_clean),
    Devices(Res.string.clear_devices),
    Settings(Res.string.clear_settings),
    ToggleSteam(Res.string.clear_toggle_steam),
    BrewBoiler(Res.string.clear_brew_boiler),
    SteamBoiler(Res.string.clear_steam_boiler),
    Flush(Res.string.clear_flush),
    Manual(Res.string.clear_manual),
    HotWater(Res.string.clear_hot_water),
    Syncing(Res.string.clear_syncing),
    Busy(Res.string.clear_busy),
    StopFlush(Res.string.clear_stop_flush),
    Flushing(Res.string.clear_flushing),
    UntimedFlush(Res.string.clear_untimed_flush),
    DisconnectedFlush(Res.string.clear_disconnected_flush),
    OpenDevices(Res.string.clear_open_devices),
    BoilerTemps(Res.string.clear_boiler_temps),
    Target(Res.string.clear_target),
    TargetTemp(Res.string.clear_target_temp),
    FlushManual(Res.string.clear_flush_manual),
    PauseOnly(Res.string.clear_pause_only),
    ProgrammedTargets(Res.string.clear_programmed_targets),
    RecordedProfile(Res.string.clear_recorded_profile),
    NoPreview(Res.string.clear_no_preview),
    WaterOutputChart(Res.string.clear_water_output_chart),
    CupWeightChart(Res.string.clear_cup_weight_chart),
    Step(Res.string.clear_step),
    RangeFlag(Res.string.clear_range_flag),
    FlushTimed(Res.string.clear_flush_timed),
    FlushUntimed(Res.string.clear_flush_untimed),
    FlushDuration(Res.string.clear_flush_duration),
    PressureTargetValue(Res.string.clear_pressure_target_value),
    ElapsedSeconds(Res.string.clear_elapsed_seconds),
    PadDescription(Res.string.clear_pad_description),
    PadIncrease(Res.string.clear_pad_increase),
    PadDecrease(Res.string.clear_pad_decrease),
    LitaBaDemo(Res.string.clear_lita_ba_demo),
    NotAvailable(Res.string.clear_not_available),
    SetGrinder(Res.string.clear_set_grinder),
    ScaleNeeded(Res.string.clear_scale_needed),
    Bound(Res.string.clear_bound),
    Experimental(Res.string.clear_experimental),
    Delete(Res.string.clear_delete),
    Bind(Res.string.clear_bind),
    Copy(Res.string.clear_copy),
    Edit(Res.string.clear_edit),
    ClearSearch(Res.string.clear_clear_search),
    RecipeCremina(Res.string.clear_recipe_cremina),
    RecipeBlooming(Res.string.clear_recipe_blooming),
    RecipeSlayer(Res.string.clear_recipe_slayer),
    RecipeGentle(Res.string.clear_recipe_gentle),
    RecipeZuppa(Res.string.clear_recipe_zuppa),
    RecipeTurbo(Res.string.clear_recipe_turbo),
    RecipeLondinium(Res.string.clear_recipe_londinium),
    RecipeRao(Res.string.clear_recipe_rao),
    RecipeFilter(Res.string.clear_recipe_filter),
    RecipeZuppaLungo(Res.string.clear_recipe_zuppa_lungo),
    RecipeEspressoLungo(Res.string.clear_recipe_espresso_lungo),
    RecipeItaliano(Res.string.clear_recipe_italiano),
}

@Composable
fun clearText(text: ClearText, vararg arguments: Any): String = stringResource(text.resource, *arguments)
