import os
import json
import sqlite3
import re

print("Starting Comprehensive Bible Dataset Audit & Sanitization Script...")

def sanitize_text(text):
    if not text:
        return ""
    text = text.replace("song reasons", "strong reasons")
    text = text.replace("song reason", "strong reason")
    
    text = re.sub(r'\[\s*(?:Greek|Hebrew|Aramaic|Latin|Septuagint|LXX|Vulgate|Masoretic|MT|TR|NU|WH|Codex|MSS|MS|Variant|Alt|Or|Lit|Literal|Literally|Meaning)[^\]]*\]', '', text, flags=re.IGNORECASE)
    text = re.sub(r'\(\s*(?:Greek|Hebrew|Aramaic|Latin|Septuagint|LXX|Vulgate|Masoretic|MT|TR|NU|WH|Codex|MSS|MS|Variant|Alt|Or|Lit|Literal|Literally|Meaning)[^\)]*\)', '', text, flags=re.IGNORECASE)
    
    text = re.sub(r'\[\s*\*\s*\]', '', text)
    text = re.sub(r'\[\s*\d+\s*\]', '', text)
    text = re.sub(r'\[\s*[a-zA-Z]\s*\]', '', text)
    
    text = text.replace("“", "\"").replace("”", "\"").replace("‘", "'").replace("’", "'")
    text = text.replace("—", "-").replace("–", "-")
    
    text = re.sub(r'\s+', ' ', text).strip()
    return text

bible_dir = '/app/applet/app/src/main/assets/bible'
if os.path.exists(bible_dir):
    for fname in os.listdir(bible_dir):
        fpath = os.path.join(bible_dir, fname)
        if fname.endswith('.json'):
            print(f"Auditing and cleansing JSON asset: {fname}")
            try:
                with open(fpath, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                modified = False
                if isinstance(data, list):
                    for item in data:
                        if isinstance(item, dict):
                            for k, v in item.items():
                                if isinstance(v, str):
                                    cleaned = sanitize_text(v)
                                    if cleaned != v:
                                        item[k] = cleaned
                                        modified = True
                elif isinstance(data, dict):
                    for k, v in data.items():
                        if isinstance(v, str):
                            cleaned = sanitize_text(v)
                            if cleaned != v:
                                data[k] = cleaned
                                modified = True
                        elif isinstance(v, list):
                            for item in v:
                                if isinstance(item, dict):
                                    for subk, subv in item.items():
                                        if isinstance(subv, str):
                                            c_sub = sanitize_text(subv)
                                            if c_sub != subv:
                                                item[subk] = c_sub
                                                modified = True
                if modified:
                    out_path = fpath.replace('.json', '_clean.json')
                    with open(out_path, 'w', encoding='utf-8') as f:
                        json.dump(data, f, ensure_ascii=False, indent=2)
                    print(f"Saved cleansed JSON to {out_path}")
            except Exception as e:
                print(f"Error processing {fname}: {e}")
        elif fname.endswith('.db') or fname.endswith('.sqlite') or fname.endswith('.sqlite3'):
            print(f"Auditing and cleansing SQLite database: {fname}")
            try:
                conn = sqlite3.connect(fpath)
                cursor = conn.cursor()
                cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
                tables = [row[0] for row in cursor.fetchall()]
                for tbl in tables:
                    cursor.execute(f"PRAGMA table_info({tbl})")
                    columns = [col[1] for col in cursor.fetchall()]
                    text_cols = [c for c in columns if any(k in c.lower() for k in ['text', 'content', 'verse', 'heading', 'title', 'note'])]
                    if text_cols:
                        for col in text_cols:
                            cursor.execute(f"SELECT rowid, {col} FROM {tbl}")
                            rows = cursor.fetchall()
                            for rowid, val in rows:
                                if isinstance(val, str):
                                    cleaned = sanitize_text(val)
                                    if cleaned != val:
                                        cursor.execute(f"UPDATE {tbl} SET {col} = ? WHERE rowid = ?", (cleaned, rowid))
                conn.commit()
                conn.close()
                print(f"Successfully sanitized database {fname}")
            except Exception as e:
                print(f"Error sanitizing DB {fname}: {e}")

print("Dataset Audit & Sanitization completed successfully.")
