import urllib.request
import re

url = "https://www.wordproject.org/bibles/audio/11_hindi/19/140.htm"
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
try:
    with urllib.request.urlopen(req, timeout=8) as resp:
        html = resp.read().decode("utf-8", errors="ignore")
        print("HTML length:", len(html))
        # Find audio src
        audio_srcs = re.findall(r"src=[\"']([^\"']+\.mp3)[\"']", html)
        print("Audio srcs:", audio_srcs)
        # Find any mp3 links
        all_mp3s = re.findall(r"[\"']([^\"']+\.mp3)[\"']", html)
        print("All mp3s:", all_mp3s)
except Exception as e:
    print("Error:", e)
