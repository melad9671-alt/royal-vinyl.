[app]
title = Royal Vinyl
package.name = royalvinyl
package.domain = com.example
source.dir = .
source.include_exts = py,png,jpg,kv,atlas
version = 1.0.0
requirements = python3,kivy,yt-dlp
orientation = portrait
fullscreen = 0
android.permissions = INTERNET,READ_EXTERNAL_STORAGE,WRITE_EXTERNAL_STORAGE
android.api = 35
android.minapi = 24
android.accept_sdk_license = True
p4a.bootstrap = sdl2
# NOTE: FFmpeg must be packaged/available for MP3 post-processing.
# yt-dlp's dependencies and YouTube site changes may require build adjustments.
