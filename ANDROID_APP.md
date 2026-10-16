# Chess Arena 171 for Android

This Android app wraps the live Chess Arena 171 website in a WebView. It uses the existing `wallpaper.jpg` artwork as the launcher icon, not the default Android robot icon.

## Build
A GitHub Actions workflow builds a debug APK on pushes that change Android app files, or when manually triggered from the Actions tab. Download the `Chess-Arena-171-APK` artifact from a successful workflow run.

## Notes
- Requires Android 6.0 (API 23) or newer.
- An internet connection is required because the game and Firebase services are hosted online.
- Google sign-in can be restricted by Google when used inside embedded WebViews; test this flow on a real device. If Google blocks it, a native Android OAuth flow will be needed.
- This is a debug-signed APK intended for testing and direct installation. Play Store publishing would require a release signing setup.
