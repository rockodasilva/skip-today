# Skip Today

Android alarm app with a key differentiator: **silence an entire group of alarms for today only**. Tomorrow, they all ring again automatically.

## The Problem

You set 6 morning alarms to make sure you wake up. You finally get up at the 3rd one — but the remaining 3 still go off while you're already awake. You could disable them manually, but then you'd have to remember to re-enable them for tomorrow.

## The Solution

**Skip Today** lets you group your alarms and silence an entire group with one tap. The silencing only lasts for today — no reset needed, no remembering to turn them back on.

## Features

- **Alarm Groups**: Organize alarms by purpose (e.g., "Wake Up", "Medication", "Gym")
- **Silence Group for Today**: One tap to silence all remaining alarms in a group — just for today
- **Three actions when an alarm rings**:
  - **Apagar** — Dismiss this alarm
  - **Snooze (5 min)** — Postpone this alarm
  - **Basta por hoy** — Silence the entire group for the rest of the day
- **Full-screen alarm** over lock screen with notification actions
- **Per-alarm configuration**: Time, repeat days, system sound, label
- **Toggle alarms** on/off individually or silence groups from the home screen
- **Survives reboots**: Alarms are rescheduled after device restart

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3 with dynamic colors
- Room (SQLite) for local storage
- Hilt for dependency injection
- AlarmManager with `setAlarmClock()` for reliable exact alarms

## How "Silence for Today" Works

When you tap "Basta por hoy", the app stores today's date (`2025-02-27`) in the alarm group. Before any alarm rings, the system checks: does the stored date match today? If yes, the alarm is silenced. Tomorrow the date won't match, so all alarms ring normally. No background jobs or timers needed.

## Building

1. Open the project in Android Studio
2. Sync Gradle
3. Run on a device or emulator (API 26+, tested on API 34)

## Permissions

- `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` — Exact alarm scheduling
- `POST_NOTIFICATIONS` — Alarm notifications
- `USE_FULL_SCREEN_INTENT` — Full-screen alarm over lock screen
- `FOREGROUND_SERVICE` — Keep alarm sound playing
- `RECEIVE_BOOT_COMPLETED` — Reschedule alarms after reboot
- `VIBRATE` — Alarm vibration
