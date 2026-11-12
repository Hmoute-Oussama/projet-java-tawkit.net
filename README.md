# Tawkit Desktop — Java Swing Prayer Times App

A free, offline-first **prayer times display app for mosques and homes**, built in
**pure Java + Swing** (no JEE, no external libraries). Inspired by
[tawkit.net](https://tawkit.net) — same features, desktop kiosk style.

> وَأَقِيمُوا الصَّلَاةَ — «Et accomplissez la prière»

## Features

| Area | What it does |
|---|---|
| **Prayer times** | 5 prayers + Shuruq (sunrise), computed **inside the app** (offline) with the PrayTimes.org astronomical algorithm; 7 calculation methods; Standard/Hanafi Asr; ±30 min manual adjustment per prayer |
| **Iqama** | Countdown to iqama with circular progress ring; per-prayer offset (minutes after adhan) or fixed clock times |
| **Next prayer** | Large countdown (HH:MM:SS) + progress ring; next prayer highlighted in green; past prayers shown in a softer color |
| **Clock & dates** | Big digital clock (12/24h, seconds on/off), Hijri + Gregorian dates, Hijri day offset (±3), Ramadan countdown, Monday/Thursday fasting reminders |
| **Adhan & alerts** | Plays bundled WAV files (adhan, Fajr adhan, iqama, alert) — or synthesized chimes if files are missing, so sound always works; volume control; pre-Fajr alert |
| **Reminders** | Tawkit keyword syntax: `15.BEFORE.FAJR.(message)`, `10.AFTER.MAGHRIB.(msg)`, `NISF.(msg)` (midnight), `TULT.(msg)` (last third of night). 12-second on-screen message + beep |
| **Content** | Rotating Qur'an verses, hadith and adhkar (Arabic + translation + reference); azkar shown after each prayer |
| **Slides** | Your own slides (text with `\|` line breaks, `.jpg/.png` files, URLs) shown between prayers — folder: `~/.tawkit/slides/` |
| **Ticker** | Scrolling announcements bar (click to pause/resume); custom announcements + auto Ramadan countdown |
| **Weather** | Current conditions + 7-day forecast from Open-Meteo.com (no API key); refreshes at startup and at midnight; keeps last data offline |
| **City database** | 130+ built-in cities (Morocco, Algeria, Saudi, Europe, Americas, Africa, Asia…), country → city picker, coordinates editable |
| **Display** | Full-screen kiosk (F11), background themes (night / green / blue) or your own image, screen dimming during prayer |
| **Languages** | English, Français, العربية (RTL) — property bundles in `i18n/` |
| **Settings backup** | Export/import settings to a file (`settings.properties`) to copy to another screen |
| **Persistence** | All settings saved to `~/.tawkit/settings.properties` and restored on startup |

## Run

```bash
# Run directly
mvn compile exec:java

# Or build and run the JAR
mvn package
java -jar target/tawkit-desktop-1.0-SNAPSHOT.jar
```

Requires **Java 21+** (tested with Maven 3.9).

## Controls

- **F11** — full screen / **ESC** — exit full screen
- **Ctrl+Q** — quit
- Menu **Settings → Preferences…** — everything is configurable
- Click the **next-prayer ring** to enlarge it
- Click the **ticker** to pause/resume scrolling

## Put your own adhan sounds (optional)

16-bit PCM WAV files in `src/main/resources/assets/audio/`:

```
assets/audio/adhan/adhan.wav         full adhan
assets/audio/adhan/adhan_fajr.wav    Fajr adhan (optional)
assets/audio/adhan/short_adhan.wav   short adhan (optional)
assets/audio/iqama/short_iqama.wav   iqama call
assets/audio/alerts/alert.wav        alert sound
```

If a file is missing, a synthesized chime plays instead — sound always works.

## Add your own slides

In `~/.tawkit/slides/`:
- `slides.txt` — one slide per line: text (use `|` for line breaks), a URL, or an image filename
- drop `.jpg/.png` files directly in the folder — they are picked up automatically

## Project structure (MVC)

```
src/main/java/com/tawkit/
├── Main.java                     entry point
├── controller/AppController      wires everything together
├── model/                        plain data classes (prayer, settings, content, weather)
├── view/                         Swing UI (MainFrame, ClockPanel, PrayerTimesPanel, NextPrayerPanel,
│                                 ContentPanel, SlidesPanel, TickerPanel, WeatherPanel, SettingsDialog…)
├── service/
│   ├── prayer/                   PrayerTimeCalculator (astronomy), PrayerScheduler, HijriCalendarService
│   ├── audio/                    AudioService (WAV + synth fallback)
│   ├── weather/                  WeatherService (Open-Meteo, hand-rolled JSON parsing)
│   ├── content/                  ContentService (verses/hadith/azkar/slides)
│   ├── settings/                 SettingsService (persistence + export/import)
│   ├── scheduler/                ReminderService (custom alerts)
│   ├── city/                     CityService (built-in city database)
│   └── localization/             Messages (i18n EN/FR/AR + RTL)
└── infrastructure/               ToneGenerator (synthesized sounds)
```

Bundled data lives in `src/main/resources/`: `data/` (Qur'an, hadith, azkar, announcements, cities),
`i18n/` (language bundles), `assets/audio/` (sound folders).

## Team

Oussama, Amine, Oumayma & Karim — Java Swing course project.

Free as in free. Not for sale, like Tawkit.
