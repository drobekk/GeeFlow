# Tablet dashboard v02

This customization continues `custom/dashboard-redesign` from commit `6b4c41e`, based on upstream GeeFlow v1.0.8 at `c4db82c`.

## Display changes

- Use the platform sans-serif family consistently, with semibold headings and 36 sp primary values.
- Keep machine temperatures, selected profile, and reading context in one compact header.
- Give Pressure, Water flow, Time, and Output equal 104 dp cards. Use warm red and blue measurement accents.
- Place chart quantity names and the flow range control in left rails. Share the Actual/Target legend and time axis, align chart grids, and label phases once.
- Keep both primary charts visible in the compact tablet dashboard. Profiles remain on the labeled Profiles tab below 1100 dp.
- Use a pale selected-profile background, shorter descriptions, and denser cards. Keep the filter row on one horizontally scrolling line.
- Open labeled secondary statistics through Details in the bottom control row.
- Show N/A when weight cannot be read, and identify disconnected live readings explicitly. Completed readings use shot data consistently.

## Favorites

Favorites appear immediately after All in the filter row. A filled star identifies a favorite. The star button toggles the preference independently of profile selection. Favorites and text search intersect, and filtering preserves profile order.

The preference is stored per user in DataStore under `favorite_profiles_<userId>` as a set of profile ID strings. Removing a user clears their favorites. Coffee profile serialization, database profile records, and machine communication formats are unchanged.

## App integration

The normal Android entry point is `app.geeflow.MainActivity`. The redesign uses the existing dashboard ViewModels, navigation, and brewing events. The temporary screenshot review activity is not part of this source build. Manual brew, profile brew, stop, hot water, and device availability rules retain their existing command paths.

## Validation

Relevant unit suites contain 37 tests, including favorites persistence, account isolation, removing favorites, and clearing settings. The Android debug APK and common-source Detekt checks for dashboard, data user, domain user, and core UI are checked together.

Manual checks use the built-in Demo device at 1280 x 800 and 1024 x 640 logical viewport sizes. They cover favorite marking, filtering, removing, restart persistence, search intersection, device navigation, missing scale, Start/Stop, completed shot context, and compact chart visibility. No physical machine is used by these checks.

## Future upstream updates

Keep upstream changes separate from the customization branch. Fetch `upstream`, inspect its new commits and diffs, and merge only after review. The main customization areas are dashboard presentation, the shared typography/color definitions, and the per-user favorites preference. There is no personal GitHub remote or public push associated with this local work.
