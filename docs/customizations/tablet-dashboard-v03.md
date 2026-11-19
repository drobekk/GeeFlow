# Tablet dashboard v03

Continues v02 on `custom/dashboard-redesign`, based on GeeFlow v1.0.8. All work remains local.

## Header and profile cards

Three equal header regions keep the boiler readings in the horizontal center. Brew and steam each have one label and a 32 sp semibold value. The separate connection button is removed; the Devices action retains its existing navigation and the device identity retains a plain connection-status label. Clean, Settings, user/device navigation, alarms, and the steam-toggle long press keep their existing events.

Profile titles use 16 sp, descriptions 14 sp, with more padding and line spacing. Text occupies 60% and the favorite control plus programmed target preview occupy 40% of the card's inner width. The preview is 28 dp high. Existing selection, favorites, filters, search, swipe actions, and command availability are unchanged.

The chart content is keyed by the fixed Y maximum. This recreates Vico's chart state when the range preference loads or changes; otherwise an unchanged model can retain its initial axis while the selector already displays the saved range.

## LITA BA

The existing Wendougee controller declares pressure and flow profiling for BA. App hot-water dispensing is exposed only for LA and LR. Dashboard controls follow these capabilities; the visual layout is common to all models. No machine protocol or profile file format changes are included.

## Verification

Android debug build, common-source dashboard Detekt, 35 dashboard tests, and 6 existing LITA variant tests pass. Manual checks use the real MainActivity with the built-in Demo device at 1280 x 800 and 1024 x 640 logical viewport sizes. No physical LITA BA has been tested. Demo exposes hot water even though BA does not.

Versioned APK, captures, build logs, and manual check notes are in the workspace's `GeeFlow tablet refinement v03` folder. The earlier v02 artifacts remain available.
