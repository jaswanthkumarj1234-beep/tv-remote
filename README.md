# TV Remote (prototype)

Control an Android TV from your phone's browser: type text, use a touchpad,
press Back/Home/OK/Volume, and speak commands.

## Layout

- `client/index.html` - phone controller page (copied into the TV app's assets)
- `tv-app/` - Android TV app (server side), builds with GitHub Actions
- `docs/index.html` - GitHub Pages download page
- `.github/workflows/build-apk.yml` - builds the APK; tags `v*` publish a Release

## Use it

1. Install the APK on the TV (download from the Pages site or the latest Release).
2. Open **TV Remote** on the TV. It shows `http://<TV_IP>:8080` and a 6-digit code.
3. Turn on **TV Remote** under Settings > Accessibility.
4. On the phone (same Wi-Fi), open the address, enter the code, tap Save.

## Limits

- Typing works in the focused text field of apps that allow it.
- Arrow keys are approximated with swipes; raw remote key codes need system permissions.
- No real mouse pointer; the touchpad sends taps and swipes.
- No play/pause (volume only).
- Browser voice may need HTTPS; the phone keyboard's mic always works in the text box.
- Not compiled or tested on a device yet.
