# Manual Bluetooth setup v07

The QR parser requires `data` with a separate `blemac`. Its `deviceid` is not used to resolve Bluetooth addresses. A printed serial cannot therefore replace the QR payload or be converted into a MAC address. This revision adds a manual setup route without a manufacturer/cloud lookup.

## User flow

Add device has permanent Nearby, QR code and Manual entry selectors. Manual entry defaults to LITA BA, accepts a printed serial and optional BLE/MAC address, and starts a bounded 20-second Bluetooth search after permission checks. Labels and errors are in English and Simplified Chinese.

Serial matches in advertising names appear first and are marked. Other recognized Wendougee machines remain visible because not every firmware broadcasts its serial. An optional MAC address narrows results to that address. The user must select the result, confirm the model/address and pass the existing Wendougee warning before the app uses the existing add/dashboard/connect path. No result is auto-selected and no serial is treated as a credential or verified ownership claim.

Known incompatible model results cannot be selected. Changing an input or model cancels pending work and clears stale results. Cancellation generations prevent an older request from stopping a newer scan. Denied permissions, no result and timeout leave a retryable page. Desktop camera availability remains unchanged.

## Discovery and persistence

Legacy Wendougee hardware-MAC name suffixes are retained. Recognized LITA/Wendougee names without a suffix may use the actual Android BLE peripheral MAC. Only an explicit manual search may additionally recognize an otherwise unknown advertising name containing the entered serial as a whole token. That path requires a real Android MAC. It does not invent one from a serial or an iOS peripheral UUID.

Hexadecimal serials are tested so they cannot become the connection address. Reconnect matches the actual peripheral address as well as the legacy advertised hardware address. GATT characteristics, initialization and Modbus/brew commands are unchanged.

A manually confirmed device uses `modelSource=manual-entry` and keeps `model · serial` in its existing name field. Existing MAC deduplication remains in place. No database migration or machine communication format change is needed.

## Verification

358 JVM tests pass across device-add, domain-device, data-device and dashboard, including 20 new tests. Android assembleDebug and CommonMain Detekt for all three changed modules pass. Device-add has 36 resource keys with complete Chinese coverage. Emulator checks cover tablet and phone layouts, input validation, denied permission, bounded scan completion, discovery of the project virtual Wendougee advertiser, and the confirmation dialog. Model selection and no-result recovery are covered by JVM tests. These checks do not prove connection to the physical LITA BA.

The APK retains `app.geeflow.debug` and the existing development signing certificate for in-place updates. It remains local on `custom/dashboard-redesign`; no push or public fork is required.
