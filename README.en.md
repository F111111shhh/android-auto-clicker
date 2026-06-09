# Android Auto Clicker

[中文](README.md) | [English](README.en.md)

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Release](https://img.shields.io/badge/release-v1.0.5-2563eb)
![Java](https://img.shields.io/badge/Java-Native%20Views-f97316)

A lightweight Android auto clicker with a clean Material You-inspired interface. It uses Android Accessibility Service to perform user-configured taps, while a floating overlay provides quick controls for start, pause, stop, region selection, and compact bubble mode.

The app is designed for repeated taps, fixed-point tapping, random taps inside a selected area, and click intervals with optional timing jitter. It does not require root access, does not read content from other apps, and does not upload user data.

[Download the latest APK](https://github.com/F111111shhh/android-auto-clicker/releases/latest)

## Screenshots

<table>
  <tr>
    <td align="center"><img src="docs/images/floating-panel.jpg" width="280" alt="Floating control panel"><br>Floating control panel</td>
    <td align="center"><img src="docs/images/main-settings.jpg" width="280" alt="Main settings"><br>Main settings</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/images/range-settings.jpg" width="280" alt="Click area settings"><br>Click area settings</td>
    <td align="center"><img src="docs/images/permissions.jpg" width="280" alt="Permissions and system settings"><br>Permissions and system settings</td>
  </tr>
</table>

## Features

- **Click count**: Run a fixed number of clicks or keep clicking until stopped manually.
- **Click interval**: Configure the click speed by setting the interval in milliseconds.
- **Click area**: Select either a draggable rectangle or a circular area with center and radius.
- **Random area clicks**: Generate random tap points inside the selected rectangle or circle without leaving the selected bounds.
- **Fixed tap point**: When random area tapping is disabled, choose one exact tap point.
- **Timing jitter**: Add randomized interval variation so taps are not triggered at identical intervals.
- **Floating controls**: Start, pause/resume, stop, select area, and return to settings from the overlay.
- **Bubble mode**: The floating panel collapses into a compact bubble after a period of inactivity.
- **Overlay opacity**: Adjust the floating window opacity to reduce visual obstruction.
- **Saved settings**: The last configuration is restored automatically the next time the app opens.

## Design

- Dynamic, wallpaper-aware color styling inspired by Material You.
- Smooth superellipse/squircle corners for cards, buttons, and input fields.
- A separate permissions screen keeps the main configuration page focused.
- Disabled options are greyed out and locked until the corresponding switch is enabled.
- Status bar spacing and overlay ergonomics are handled for daily phone use.

## How To Use

1. Download and install the APK from the Release page.
2. Open the app and enable the floating window permission.
3. Enable "连点器点击服务" in Android Accessibility settings.
4. Return to the app and configure click count, interval, area, and randomization.
5. Open the floating control panel and select a tap point or region on the target screen.
6. Tap start on the overlay to run the click task.

## Permissions And Privacy

- **Floating window permission**: Displays the control panel, bubble, and selection overlay.
- **Accessibility permission**: Uses Android gesture dispatch to perform the taps you configure.

The app does not request root access, does not inspect content inside other apps, and does not upload click settings or device data.

## Responsible Use

Use this tool only in apps and situations where you are allowed to automate taps. Some games, services, or platforms may forbid auto-clicking, so check their rules before using it.
