# Kinetra — HTML → MP4, on your phone

Java + XML Android app. No third-party libraries (no AndroidX), so CI is fast and nothing can break on dependencies.

## Build the APK on GitHub (phone-friendly)
1. Create a new GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab → **Build Kinetra APK** → it runs on every push (or press **Run workflow**).
3. When it turns green, open the run → **Artifacts** → download **Kinetra-debug-apk** → unzip → install the APK.

No Gradle wrapper is needed; the workflow installs Gradle 8.7 itself. Needs Android 8.0+ (API 26).

## First run
Tap **Start recording** once and allow **All files access**. Videos are saved to:

`/storage/emulated/0/Kinetra/<Project>/<File>.mp4`  (poster PNG saved beside it)

## How recording works
The page runs in a WebView sized to your output resolution. Each frame is drawn to a bitmap, pushed into the hardware
H.264/H.265 encoder (MediaCodec) and written with MediaMuxer. Timing follows the real clock, so a slow phone drops frames
instead of slowing the video down. Lower resolutions capture faster.

## Page tips
- `window.KINETRA_DURATION = 12000;` in your HTML tells Kinetra how many ms to record.
- Use **Auto-click selector** (e.g. `#start`) for pages that wait for a tap.
- Page audio is not captured; add an AAC/M4A soundtrack in Create.
- Inline CSS/images/fonts (data: URIs) for reliable offline renders.

## Structure
`app/src/main/java/com/kinetra/app` — Cfg (settings), Store (paths/permissions), Recorder (capture+encode), Mux (audio),
Ui (toolkit), MainActivity + Home/Create/Studio/Library/More pages.
