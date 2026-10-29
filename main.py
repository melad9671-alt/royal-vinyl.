#!/usr/bin/env python3
# Royal Vinyl — Python GUI for Termux using native Android views via Termux:GUI.
# Downloads are attempted only for items explicitly labelled Creative Commons/public domain.

import os
import threading
import traceback

try:
    import termuxgui as tg
except ImportError:
    raise SystemExit(
        "Missing Termux:GUI Python bindings.\n"
        "Install the Termux:GUI Android add-on and run: pip install termuxgui"
    )

try:
    import yt_dlp
except ImportError:
    yt_dlp = None

BG = 0xFF090610
SURFACE = 0xFF171020
PURPLE = 0xFF8155D9
LAVENDER = 0xFFD6C4FF
TEXT = 0xFFF5F1FC
MUTED = 0xFFB3A8C4
GREEN = 0xFF71D9B0
RED = 0xFFFF7188

SAVE_DIR = "/sdcard/app/Music"


def safe_text(value, limit=95):
    value = str(value or "").replace("\n", " ").strip()
    return value if len(value) <= limit else value[:limit - 1] + "…"


class RoyalVinyl:
    def __init__(self):
        self.connection = tg.Connection()
        self.activity = tg.Activity(self.connection)
        self.activity.setorientation("portrait")
        self.activity.settheme(BG, PURPLE, BG, TEXT, PURPLE)
        self.page = "Music"
        self.results = []
        self.busy = False
        self.path = SAVE_DIR
        self.current_query = ""
        self.build_shell()
        self.show_music()

    def text(self, parent, value, size=15, color=TEXT):
        item = tg.TextView(self.activity, value, parent)
        item.settextsize(size)
        item.settextcolor(color)
        item.setmargin(5)
        item.setheight(tg.View.WRAP_CONTENT)
        return item

    def button(self, parent, value):
        item = tg.Button(self.activity, value, parent)
        item.settextsize(14)
        item.setbackgroundcolor(PURPLE)
        item.settextcolor(TEXT)
        item.setmargin(4)
        item.setheight(48)
        return item

    def build_shell(self):
        self.root = tg.LinearLayout(self.activity)
        self.root.setbackgroundcolor(BG)
        self.header = tg.LinearLayout(self.activity, self.root)
        self.header.setbackgroundcolor(BG)
        title = self.text(self.header, "ROYAL  VINYL", 25, LAVENDER)
        title.setmargin(10)
        self.text(self.header, "YOUR SOUND. YOUR SPACE.", 10, MUTED)
        self.content = tg.LinearLayout(self.activity, self.root)
        self.content.setlinearlayoutparams(1)
        self.dock = tg.LinearLayout(self.activity, self.root, False)
        self.dock.setbackgroundcolor(SURFACE)
        self.dock.setheight(62)
        self.music_tab = self.button(self.dock, "♫  Music")
        self.movies_tab = self.button(self.dock, "▶  Movies")
        self.settings_tab = self.button(self.dock, "⚙  Settings")

    def clear_content(self):
        self.content.clearchildren()

    def show_music(self):
        self.page = "Music"
        self.clear_content()
        self.text(self.content, "FIND YOUR SOUND", 22, TEXT)
        self.text(self.content, "Search an artist or track", 13, MUTED)
        self.query = tg.EditText(self.activity, "BLACKPINK", self.content, singleline=True)
        self.query.setmargin(8)
        self.query.setheight(52)
        self.search_btn = self.button(self.content, "EXECUTE SMART SEARCH")
        self.status = self.text(self.content, "◉  READY WHEN YOU ARE", 12, LAVENDER)
        self.vinyl = self.text(self.content, "      ◉", 56, PURPLE)
        self.vinyl.setgravity(1, 1)
        self.text(self.content, "Search results appear below. Downloads are limited to media marked CC/public domain.", 11, MUTED)
        self.results_view = tg.NestedScrollView(self.activity, self.content)
        self.results_view.setlinearlayoutparams(1)
        self.results_layout = tg.LinearLayout(self.activity, self.results_view)
        self.results_layout.setbackgroundcolor(BG)
        self.results_layout.setlinearlayoutparams(1)

    def show_movies(self):
        self.page = "Movies"
        self.clear_content()
        self.text(self.content, "MOVIES", 24, TEXT)
        self.text(self.content, "▶", 54, PURPLE)
        self.text(self.content, "COMING SOON", 22, LAVENDER)
        self.text(self.content, "A new experience is on the way.", 13, MUTED)

    def show_settings(self):
        self.page = "Settings"
        self.clear_content()
        self.text(self.content, "SETTINGS", 24, TEXT)
        self.text(self.content, "STORAGE LOCATION", 12, MUTED)
        self.path_input = tg.EditText(self.activity, self.path, self.content, singleline=True)
        self.path_input.setmargin(8)
        self.path_input.setheight(52)
        self.save_path_btn = self.button(self.content, "SAVE STORAGE PATH")
        self.text(self.content, "Current target folder:", 12, MUTED)
        self.text(self.content, self.path, 12, LAVENDER)
        self.text(self.content, "MP3 conversion requires FFmpeg. Only save music you own or have permission to download.", 12, MUTED)

    def set_status(self, message, color=LAVENDER):
        try:
            self.status.settext(safe_text(message, 180))
            self.status.settextcolor(color)
        except Exception:
            pass

    def do_search(self):
        query = self.query.gettext().strip()
        if not query:
            self.set_status("Enter an artist or track name.", RED)
            return
        self.current_query = query  # حفظ كلمة البحث الحالية لإنشاء فولدر الفنان بها
        if yt_dlp is None:
            self.set_status("yt-dlp is missing. Install it with: pip install yt-dlp", RED)
            return
        if self.busy:
            return
        self.busy = True
        self.set_status("◉  SEARCHING…", LAVENDER)
        threading.Thread(target=self.search_worker, args=(query,), daemon=True).start()

    def search_worker(self, query):
        try:
            opts = {"quiet": True, "no_warnings": True, "skip_download": True,
                    "extract_flat": True, "noplaylist": True}
            with yt_dlp.YoutubeDL(opts) as ydl:
                info = ydl.extract_info("ytsearch8:" + query, download=False)
            items = []
            for e in (info or {}).get("entries", []) or []:
                if not e:
                    continue
                url = e.get("webpage_url") or e.get("url") or ""
                if url and not str(url).startswith(("http://", "https://")):
                    url = "https://www.youtube.com/watch?v=" + str(url)
                items.append({
                    "title": safe_text(e.get("title", "Untitled"), 72),
                    "channel": safe_text(e.get("channel") or e.get("uploader") or "Unknown creator", 48),
                    "url": url,
                    "id": e.get("id", "")
                })
            self.pending_results = items
            self.pending_error = None
        except Exception as exc:
            self.pending_results = []
            self.pending_error = safe_text(exc, 180)
        finally:
            self.busy = False
        try:
            self.render_results()
        except Exception:
            pass

    def render_results(self):
        self.results_layout.clearchildren()
        if getattr(self, "pending_error", None):
            self.set_status("SEARCH FAILED: " + self.pending_error, RED)
            return
        self.results = getattr(self, "pending_results", [])
        if not self.results:
            self.set_status("No results. Try another spelling.", RED)
            return
        self.set_status(f"RESULTS READY · {len(self.results)} FOUND", GREEN)
        for index, item in enumerate(self.results):
            self.text(self.results_layout, item["title"], 13, TEXT)
            self.text(self.results_layout, item["channel"], 10, MUTED)
            b = self.button(self.results_layout, "SAVE MP3  ·  " + str(index + 1))
            self.download_buttons[b] = item

    def download_worker(self, item):
        try:
            # 1. إنشاء فولدر خاص بالفنان داخل مسار Music بناءً على كلمة البحث (مثل Lisa أو Blackpink)
            artist_folder_name = self.current_query.strip().title() if self.current_query else "Unknown_Artist"
            target_artist_dir = os.path.join(self.path, artist_folder_name)
            os.makedirs(target_artist_dir, exist_ok=True)

            url = item["url"]
            if not url:
                raise RuntimeError("This result has no direct URL.")

            # استخراج الميتافيتا لمعرفة اسم الأغنية المتوقع قبل التحميل لمنع التكرار
            with yt_dlp.YoutubeDL({"quiet": True, "no_warnings": True, "skip_download": True, "noplaylist": True}) as ydl:
                info = ydl.extract_info(url, download=False)
            
            clean_title = safe_text(info.get("title", "Untitled"), 150)
            # إزالة الرموز الممنوعة في الأسماء عشان ما تحصلش مشاكل في النظام
            for char in ['/', '\\', '?', '%', '*', ':', '|', '"', '<', '>']:
                clean_title = clean_title.replace(char, '')
            
            expected_file_path = os.path.join(target_artist_dir, f"{clean_title}.mp3")

            # 2. فحص ما إذا كانت الأغنية موجودة مسبقاً لمنع تحميلها مرة أخرى
            if os.path.exists(expected_file_path):
                self.pending_download_message = f"Skipped (Already Exists): {clean_title}"
                self.pending_download_success = True
                return

            license_name = str(info.get("license") or "").lower()
            if not any(word in license_name for word in
                       ("creative commons", "creativecommons", "public domain")):
                self.pending_download_message = (
                    "Skipped: this item is not clearly labelled Creative Commons/public domain."
                )
                self.pending_download_success = False
                return

            opts = {
                "format": "bestaudio/best",
                "outtmpl": os.path.join(target_artist_dir, "%(title).150B.%(ext)s"),
                "noplaylist": True,
                "quiet": True,
                "no_warnings": True,
                "postprocessors": [{
                    "key": "FFmpegExtractAudio",
                    "preferredcodec": "mp3",
                    "preferredquality": "192",
                }],
            }
            with yt_dlp.YoutubeDL(opts) as ydl:
                ydl.download([url])
            
            self.pending_download_message = f"Saved to: {artist_folder_name}"
            self.pending_download_success = True
        except Exception as exc:
            self.pending_download_message = safe_text(exc, 180)
            self.pending_download_success = False
        finally:
            self.busy = False
        try:
            self.set_status(
                getattr(self, "pending_download_message", "Finished"),
                GREEN if getattr(self, "pending_download_success", False) else RED
            )
        except Exception:
            pass

    def save_item(self, item):
        if self.busy:
            return
        self.busy = True
        self.set_status("◉  CHECKING & DOWNLOADING…", LAVENDER)
        threading.Thread(target=self.download_worker, args=(item,), daemon=True).start()

    def save_path(self):
        value = self.path_input.gettext().strip()
        if not value:
            self.text(self.content, "Path cannot be empty.", 12, RED)
            return
        try:
            os.makedirs(value, exist_ok=True)
            self.path = value
            self.set_status("Storage path saved: " + value, GREEN)
        except Exception as exc:
            self.text(self.content, "Could not create folder: " + safe_text(exc), 12, RED)

    def run(self):
        self.download_buttons = {}
        self.pending_search_update = False
        self.pending_download_update = False

        for event in self.connection.events():
            if event.type == tg.Event.destroy and event.value.get("finishing"):
                break

            if event.type == tg.Event.click:
                view_id = event.value.get("id")
                if view_id == self.music_tab:
                    self.show_music()
                elif view_id == self.movies_tab:
                    self.show_movies()
                elif view_id == self.settings_tab:
                    self.show_settings()
                elif self.page == "Music" and view_id == self.search_btn:
                    self.do_search()
                elif self.page == "Settings" and view_id == self.save_path_btn:
                    self.save_path()
                elif view_id in self.download_buttons:
                    self.save_item(self.download_buttons[view_id])

            if self.page == "Music" and getattr(self, "pending_search_update", False):
                self.pending_search_update = False
                self.render_results()
            if getattr(self, "pending_download_update", False):
                self.pending_download_update = False
                self.set_status(
                    getattr(self, "pending_download_message", "Finished"),
                    GREEN if getattr(self, "pending_download_success", False) else RED
                )

        self.connection.close()


if __name__ == "__main__":
    try:
        RoyalVinyl().run()
    except KeyboardInterrupt:
        pass
