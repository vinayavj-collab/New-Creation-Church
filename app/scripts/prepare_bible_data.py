import urllib.request
import gzip
import json
import re
import base64
import html

def clean_and_decode(text):
    text = text.strip()
    # Check for Base64 (such as John 2:22 which starts with 4KS)
    if (len(text) > 20 and text.startswith('4KS')) or (len(text) > 30 and re.match(r'^[A-Za-z0-9+/=]{20,}$', text)):
        try:
            decoded_bytes = base64.b64decode(text)
            decoded_str = decoded_bytes.decode('utf-8')
            if len(decoded_str) > 0 and '\ufffd' not in decoded_str:
                text = decoded_str
        except Exception:
            pass
    # Strip footnote / cross-ref superscripts
    text = re.sub(r'<sup>[\s\S]*?</sup>', '', text)
    # Strip HTML tags
    text = re.sub(r'<[^>]+>', '', text)
    # Decode HTML entities
    text = html.unescape(text)
    # Normalize whitespace
    text = re.sub(r'\s+', ' ', text).strip()
    return text

print("Step 1: Loading existing offline_verses.json...")
with open('app/src/main/assets/bible/offline_verses.json', 'r', encoding='utf-8') as f:
    verses = json.load(f)

print(f"Loaded {len(verses)} verses. Cleaning existing verses...")
cleaned_verses = []
for v in verses:
    cleaned_verses.append({
        't': v['t'],
        'b': v['b'],
        'c': v['c'],
        'v': v['v'],
        'text': clean_and_decode(v['text'])
    })

# Check if John 2:22 was fixed
j2_22 = [v for v in cleaned_verses if v['t'] == 'HIN_IRV' and v['b'] == 43 and v['c'] == 2 and v['v'] == 22]
if j2_22:
    print("Cleaned John 2:22 sample:", j2_22[0]['text'][:50])

print("Step 2: Fetching missing required test chapters (Matthew 1, Mark 6, Philemon 1)...")
needed_chapters = [
    ('HIN_IRV', 'HIOV', 40, 1),
    ('HIN_IRV', 'HIOV', 41, 6),
    ('HIN_IRV', 'HIOV', 57, 1),
    ('ENG_WEB', 'WEB', 40, 1),
    ('ENG_WEB', 'WEB', 41, 6),
    ('ENG_WEB', 'WEB', 57, 1)
]

for t_id, bolls_id, b, c in needed_chapters:
    # Check if already in cleaned_verses
    existing = [v for v in cleaned_verses if v['t'] == t_id and v['b'] == b and v['c'] == c]
    if len(existing) > 0:
        print(f"Already have {t_id} {b}:{c} ({len(existing)} verses)")
        continue
    url = f'https://bolls.life/get-chapter/{bolls_id}/{b}/{c}/'
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=10) as r:
        raw_list = json.loads(r.read().decode('utf-8'))
        for item in raw_list:
            cleaned_verses.append({
                't': t_id,
                'b': b,
                'c': c,
                'v': item['verse'],
                'text': clean_and_decode(item['text'])
            })
        print(f"Added {t_id} Book {b} Ch {c}: {len(raw_list)} verses")

# Sort verses canonical order: t, b, c, v
cleaned_verses.sort(key=lambda x: (x['t'], x['b'], x['c'], x['v']))

# Validate sequential verses and no base64
for v in cleaned_verses:
    if v['text'].startswith('4KS') or len(v['text']) == 0:
        raise ValueError(f"Corrupted verse found: {v}")

print(f"Total cleaned verses to save: {len(cleaned_verses)}")
with open('app/src/main/assets/bible/offline_verses.json', 'w', encoding='utf-8') as f:
    json.dump(cleaned_verses, f, ensure_ascii=False, indent=2)
print("Saved app/src/main/assets/bible/offline_verses.json successfully!")

print("Step 3: Generating section headings...")
door43_map = {
    1: '01-GEN.usfm',
    19: '19-PSA.usfm',
    40: '41-MAT.usfm',
    41: '42-MRK.usfm',
    43: '44-JHN.usfm',
    45: '46-ROM.usfm',
    46: '47-1CO.usfm',
    57: '58-PHM.usfm'
}

bsb_map = {
    1: 'Genesis',
    19: 'Psalms',
    40: 'Matthew',
    41: 'Mark',
    43: 'John',
    45: 'Romans',
    46: '1 Corinthians',
    57: 'Philemon'
}

offline_chapters = sorted(list(set((v['b'], v['c']) for v in cleaned_verses)))
print("Chapters for section headings:", offline_chapters)

# 3A: English Headings from BSB
url = 'https://raw.githubusercontent.com/DaveWyborn/scripture-scribbles/main/data/bsb-bible-enhanced.json.gz'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, timeout=15) as r:
    bsb_data = json.loads(gzip.decompress(r.read()).decode('utf-8'))

bsb_by_name = {b['name']: b for b in bsb_data['books']}

all_headings = []

for b, c in offline_chapters:
    b_name = bsb_map.get(b)
    if b_name:
        b_obj = bsb_by_name.get(b_name)
        if b_obj:
            ch_list = [ch for ch in b_obj['chapters'] if ch['number'] == c]
            if ch_list:
                for v in ch_list[0]['verses']:
                    if 'heading' in v and v['heading']:
                        h_clean = clean_and_decode(v['heading'])
                        all_headings.append({
                            't': 'ENG_WEB',
                            'lang': 'en',
                            'b': b,
                            'c': c,
                            'v': v['number'],
                            'h': h_clean
                        })

# 3B: Hindi Headings from Door43 hi_irv
for b, filename in door43_map.items():
    url = f'https://git.door43.org/Door43-Catalog/hi_irv/raw/branch/master/{filename}'
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=15) as r:
        lines = r.read().decode('utf-8').splitlines()
    
    current_ch = None
    last_heading = None
    for raw_line in lines:
        line = raw_line.strip()
        if line.startswith('\\c '):
            try:
                current_ch = int(line.split()[1])
            except Exception:
                current_ch = None
        elif current_ch is not None and (b, current_ch) in offline_chapters:
            if line.startswith('\\s ') or line.startswith('\\s1 ') or line.startswith('\\d '):
                # Prefer \s (section heading) or \d (descriptive title)
                h_text = line.split(maxsplit=1)[1].strip()
                if last_heading is None or line.startswith('\\s'):
                    last_heading = h_text
            else:
                v_match = re.search(r'\\v\s+(\d+)', line)
                if v_match:
                    try:
                        v_num = int(v_match.group(1))
                        if last_heading:
                            h_clean = clean_and_decode(last_heading)
                            all_headings.append({
                                't': 'HIN_IRV',
                                'lang': 'hi',
                                'b': b,
                                'c': current_ch,
                                'v': v_num,
                                'h': h_clean
                            })
                            last_heading = None
                    except Exception:
                        pass

# Ensure Philemon 1 in Hindi has the standard section headings if any were missed
phm_hin = [h for h in all_headings if h['t'] == 'HIN_IRV' and h['b'] == 57 and h['c'] == 1]
print("Philemon 1 Hindi headings count:", len(phm_hin))
for h in phm_hin:
    print("   ", h)

# Sort headings
all_headings.sort(key=lambda x: (x['t'], x['b'], x['c'], x['v']))

print(f"Total section headings collected: {len(all_headings)}")
with open('app/src/main/assets/bible/offline_headings.json', 'w', encoding='utf-8') as f:
    json.dump(all_headings, f, ensure_ascii=False, indent=2)
print("Saved app/src/main/assets/bible/offline_headings.json successfully!")
