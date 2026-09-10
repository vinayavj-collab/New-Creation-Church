import urllib.request, re

urls = [
    "https://www.wordproject.org/bibles/audio/11_hindi/19/140.htm",
    "https://www.wordproject.org/bibles/audio/11_hindi/b19.htm",
    "https://www.wordproject.org/bibles/audio/11_hindi/index.htm"
]

for url in urls:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req, timeout=8) as resp:
            html = resp.read().decode("utf-8", errors="ignore")
            print("URL:", url, "HTML length:", len(html))
            all_mp3s = re.findall(r"[\"']([^\"']+\.mp3)[\"']", html)
            print("All mp3s:", all_mp3s[:10])
            links = re.findall(r"href=[\"']([^\"']+)[\"']", html)
            print("Some links:", links[:10])
    except Exception as e:
        print("URL:", url, "Error:", e)
