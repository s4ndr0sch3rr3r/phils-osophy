# Android backup and restore verification

Phil's-osophy is already configured to back up its Room database through Android Backup.

## Current configuration

The application manifest enables backup and references both Android backup rule formats:

- `android:allowBackup="true"`
- `android:fullBackupContent="@xml/backup_rules"` for Android 11 and lower
- `android:dataExtractionRules="@xml/data_extraction_rules"` for Android 12 and higher

Both rule files include the complete `database` domain for cloud backup. The Android 12+ rules also include it for device-to-device transfer.

The Room database is named `phils_osophy.db`. Because all saved Movies, Series, Books, Games, Recipes, watched episodes, comments, favorites, personal ratings, reading progress, completion dates, and profile statistics are stored in that database, they are all in backup scope.

Relevant files:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/java/com/example/phils_osophy/data/local/PhilsOsophyDatabase.kt`

## Deterministic uninstall and restore test

Android's normal cloud backup schedule is controlled by the operating system, so simply waiting and uninstalling is not a reliable developer test. The repository includes a PowerShell script that uses Android's local backup transport to force a backup and restore cycle.

The script:

1. Requires exactly one authorized `adb` device.
2. Detects the current Android user and confirms that `com.example.phils_osophy` is installed for that user.
3. Pulls the exact installed base and split APKs to a temporary directory.
4. Saves the current backup transport and local-transport test setting.
5. Selects Android's deterministic local backup transport.
6. Runs `bmgr backupnow` and requires this exact success result before deleting anything:

   ```text
   Package com.example.phils_osophy with result: Success
   ```

7. Uninstalls the package for the current Android user.
8. Reinstalls the same signed APK set, which triggers Android restore before the first launch.
9. Restores the previous backup transport and test setting.
10. Keeps the saved APKs instead of deleting them if reinstall does not complete.

### Prepare sentinel data

Before running the test, add records whose restoration is easy to identify. A useful set is:

- one Movie with a favorite, rating, and comment
- one Series with watched episodes, a rating, and a comment
- one Book with reading progress, status, favorite, rating, and comment
- one Game
- one Recipe

Take screenshots or write down the exact values.

### Requirements

- Android SDK Platform Tools, with `adb` available in `PATH`
- USB debugging enabled and authorized
- exactly one connected Android phone or emulator
- Phil's-osophy already installed and populated with the sentinel data

### Run the test from the repository root

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-android-backup-restore.ps1 -IUnderstandThisUninstallsTheApp
```

The explicit switch is required because the test uninstalls the app after backup succeeds.

After the script reports `Restore installation completed`, open Phil's-osophy and verify every sentinel value. If all values are present, the Room database backup rules and restore path work for that installed build and Android device.

This local-transport test validates the app's backup configuration. It does not prove that Google cloud backup has uploaded a current dataset for the phone.

If the connected device does not expose `com.android.localtransport/.LocalTransport`, run the deterministic test on an Android emulator or use the real phone-change test below.

## Real phone-change test

Use this test to verify the actual Google account and device migration path.

1. On the old phone, confirm Android backup is enabled and the intended Google account is selected. Settings names vary by device, but the controls are normally under **Settings > Google > Backup** or **Settings > System > Backup**.
2. Open Phil's-osophy and create or update the sentinel data.
3. Use **Back up now** in Android settings when the device provides it, then wait until the device backup shows a current completion time.
4. Set up the new or factory-reset phone with the same Google account.
5. During the setup wizard, select the old device backup or use the offered device-to-device transfer.
6. Install or restore Phil's-osophy as part of setup. Use the same application ID and a build signed with the compatible signing certificate. Use the same or a newer app version so Room migrations can run normally.
7. Open the app only after installation and restore have completed, then verify all sentinel values.

Android performs application-data restore during package installation, before the app is first launched. Clearing app storage afterward does not automatically replay the backup. For a clean repeat, uninstall and reinstall the compatible build or repeat the device setup flow.

## Development-build caution

Debug builds from different development computers can use different debug signing certificates. For the deterministic test, the script avoids that problem by pulling and reinstalling the exact APK set already installed on the phone. For a real phone migration, use builds signed with the same stable release or upload signing identity.

Do not enable `android:restoreAnyVersion` merely to make a test pass. Keeping its default behavior prevents a backup created by a newer schema from being restored into an older app build. Test with the same or a newer version and retain all required Room migrations.

## Official Android references

- Auto Backup: https://developer.android.com/identity/data/autobackup
- Test backup and restore: https://developer.android.com/identity/data/testingbackup
- App signing: https://developer.android.com/studio/publish/app-signing
