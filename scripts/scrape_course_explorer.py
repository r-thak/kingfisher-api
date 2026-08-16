#!/usr/bin/env python3
"""
UIUC Course Explorer Historical Scraper & Data Manager
======================================================
Scrapes historical and live scheduled course sections from UIUC Course Explorer
(courses.illinois.edu/cisapp/explorer/schedule.xml) back to 2004.

Features:
- Dynamically discovers all past calendar years & terms.
- Robust checkpointing with SQLite (safe to interrupt and resume anytime).
- Multi-format Export: CSV (flat tabular) and JSON (structured hierarchy).
- CSV Import: Fast bulk import from CSV directly into PostgreSQL without re-scraping.
- Rate-limiting, exponential backoff, and WAF 403 cooldown handling.
"""

import argparse
import csv
import json
import os
import re
import sqlite3
import ssl
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime
from typing import Dict, List, Optional, Set, Tuple

BASE_URL = "https://courses.illinois.edu/cisapp/explorer/schedule"
USER_AGENT = "kingfisher-historical-scraper/1.0 (educational UIUC course explorer archiver; contact rthakkar4@wisc.edu)"
DEFAULT_DELAY = 0.35  # Seconds between requests
MAX_RETRIES = 4
BACKOFF_FACTOR = 2.0


def create_ssl_context():
    """Creates a resilient SSL context for macOS and Linux."""
    try:
        ctx = ssl.create_default_context()
        return ctx
    except Exception:
        pass
    ctx = ssl._create_unverified_context()
    return ctx


def parse_xml_clean(xml_text: str) -> ET.Element:
    """Strips XML namespaces and prefixes for reliable element matching."""
    clean = re.sub(r'\sxmlns(:\w+)?="[^"]+"', '', xml_text)
    clean = re.sub(r'<(/?)\w+:([a-zA-Z0-9_-]+)', r'<\1\2', clean)
    return ET.fromstring(clean)


class RateLimitedFetcher:
    """HTTP Client with rate limiting, retries, and WAF 403 cool-off."""

    def __init__(self, delay: float = DEFAULT_DELAY):
        self.delay = delay
        self.last_request_time = 0.0
        self.ssl_ctx = create_ssl_context()

    def fetch(self, url: str) -> Optional[str]:
        elapsed = time.time() - self.last_request_time
        if elapsed < self.delay:
            time.sleep(self.delay - elapsed)

        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": USER_AGENT,
                "Accept": "application/xml, text/xml, */*",
            },
        )

        for attempt in range(1, MAX_RETRIES + 1):
            try:
                self.last_request_time = time.time()
                try:
                    with urllib.request.urlopen(req, timeout=20, context=self.ssl_ctx) as resp:
                        if resp.status == 200:
                            return resp.read().decode("utf-8", errors="replace")
                        return None
                except (ssl.SSLError, urllib.error.URLError) as ssl_err:
                    if "CERTIFICATE_VERIFY_FAILED" in str(ssl_err):
                        unverified_ctx = ssl._create_unverified_context()
                        with urllib.request.urlopen(req, timeout=20, context=unverified_ctx) as resp:
                            if resp.status == 200:
                                return resp.read().decode("utf-8", errors="replace")
                            return None
                    raise ssl_err

            except urllib.error.HTTPError as e:
                if e.code == 404:
                    return None
                elif e.code == 403:
                    wait_time = 12 * attempt
                    print(f"\n[!] WAF rate limit (HTTP 403) on {url}. Cooling down for {wait_time}s...", file=sys.stderr)
                    time.sleep(wait_time)
                elif e.code in (429, 500, 502, 503, 504):
                    wait_time = BACKOFF_FACTOR ** attempt
                    time.sleep(wait_time)
                else:
                    if attempt == MAX_RETRIES:
                        print(f"\n[!] HTTP {e.code} for {url}", file=sys.stderr)
                    return None
            except Exception as e:
                wait_time = BACKOFF_FACTOR ** attempt
                time.sleep(wait_time)

        return None


class CheckpointManager:
    """Tracks scrape progress in SQLite database so runs can resume."""

    def __init__(self, db_path: str):
        self.db_path = db_path
        os.makedirs(os.path.dirname(os.path.abspath(db_path)), exist_ok=True)
        self.conn = sqlite3.connect(db_path)
        self.conn.execute("PRAGMA journal_mode=WAL;")
        self._init_tables()

    def _init_tables(self):
        with self.conn:
            self.conn.execute("""
                CREATE TABLE IF NOT EXISTS completed_courses (
                    year_term TEXT NOT NULL,
                    subject TEXT NOT NULL,
                    course_number TEXT NOT NULL,
                    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (year_term, subject, course_number)
                );
            """)
            self.conn.execute("""
                CREATE TABLE IF NOT EXISTS completed_terms (
                    year_term TEXT PRIMARY KEY,
                    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """)
            self.conn.execute("""
                CREATE TABLE IF NOT EXISTS scraped_sections (
                    year_term TEXT NOT NULL,
                    subject TEXT NOT NULL,
                    course_number TEXT NOT NULL,
                    crn TEXT NOT NULL,
                    data_json TEXT NOT NULL,
                    PRIMARY KEY (year_term, subject, course_number, crn)
                );
            """)

    def is_course_completed(self, year_term: str, subject: str, course_num: str) -> bool:
        cur = self.conn.cursor()
        cur.execute(
            "SELECT 1 FROM completed_courses WHERE year_term = ? AND subject = ? AND course_number = ?",
            (year_term, subject, course_num),
        )
        return cur.fetchone() is not None

    def mark_course_completed(self, year_term: str, subject: str, course_num: str):
        with self.conn:
            self.conn.execute(
                "INSERT OR IGNORE INTO completed_courses (year_term, subject, course_number) VALUES (?, ?, ?)",
                (year_term, subject, course_num),
            )

    def is_term_completed(self, year_term: str) -> bool:
        cur = self.conn.cursor()
        cur.execute("SELECT 1 FROM completed_terms WHERE year_term = ?", (year_term,))
        return cur.fetchone() is not None

    def mark_term_completed(self, year_term: str):
        with self.conn:
            self.conn.execute(
                "INSERT OR IGNORE INTO completed_terms (year_term) VALUES (?)",
                (year_term,),
            )

    def save_section(self, year_term: str, subject: str, course_num: str, crn: str, data: dict):
        with self.conn:
            self.conn.execute(
                "INSERT OR REPLACE INTO scraped_sections (year_term, subject, course_number, crn, data_json) VALUES (?, ?, ?, ?, ?)",
                (year_term, subject, course_num, crn, json.dumps(data)),
            )

    def get_all_scraped_sections(self, year_term: Optional[str] = None) -> List[dict]:
        cur = self.conn.cursor()
        if year_term:
            cur.execute("SELECT data_json FROM scraped_sections WHERE year_term = ? ORDER BY year_term, subject, course_number, crn", (year_term,))
        else:
            cur.execute("SELECT data_json FROM scraped_sections ORDER BY year_term, subject, course_number, crn")
        return [json.loads(row[0]) for row in cur.fetchall()]


def season_to_code(season: str) -> str:
    s = season.lower()
    if s == "spring":
        return "sp"
    elif s == "summer":
        return "su"
    elif s == "fall":
        return "fa"
    elif s == "winter":
        return "wi"
    return s[:2]


def discover_available_terms(fetcher: RateLimitedFetcher) -> List[Tuple[int, str, str]]:
    """Fetches schedule.xml and returns [(year, season, year_term)] sorted oldest to newest."""
    root_xml = fetcher.fetch(f"{BASE_URL}.xml")
    if not root_xml:
        print("[!] Failed to fetch root schedule.xml", file=sys.stderr)
        return []

    root = parse_xml_clean(root_xml)
    years = []
    for cal_year in root.findall(".//calendarYear"):
        y_str = cal_year.get("id") or (cal_year.text.strip() if cal_year.text else "")
        if y_str.isdigit():
            years.append(int(y_str))

    years.sort()
    discovered_terms = []

    for year in years:
        year_xml = fetcher.fetch(f"{BASE_URL}/{year}.xml")
        if not year_xml:
            continue
        try:
            y_root = parse_xml_clean(year_xml)
            for term_el in y_root.findall(".//term"):
                href = term_el.get("href", "")
                match = re.search(r"/(\d{4})/([a-zA-Z]+)\.xml", href)
                if match:
                    y = int(match.group(1))
                    season = match.group(2).lower()
                    yt = f"{y}-{season_to_code(season)}"
                    discovered_terms.append((y, season, yt))
        except Exception as e:
            print(f"[!] Error parsing year {year}: {e}", file=sys.stderr)

    return discovered_terms


def parse_section_xml(xml_text: str) -> dict:
    """Parses single section XML into structured dictionary."""
    root = parse_xml_clean(xml_text)

    def text_or_none(tag: str) -> Optional[str]:
        el = root.find(tag)
        if el is not None and el.text:
            t = el.text.strip()
            return t if t else None
        return None

    section_num = text_or_none("sectionNumber")
    status_code = text_or_none("statusCode")
    part_of_term = text_or_none("partOfTerm")
    start_date = text_or_none("startDate")
    end_date = text_or_none("endDate")

    notes_parts = []
    for tag in ("sectionText", "sectionNotes", "sectionCappArea"):
        val = text_or_none(tag)
        if val:
            notes_parts.append(val)
    notes = "\n\n".join(notes_parts) if notes_parts else None

    meetings = []
    for idx, meet_el in enumerate(root.findall(".//meeting")):
        def meet_text(t: str) -> Optional[str]:
            el = meet_el.find(t)
            return el.text.strip() if el is not None and el.text and el.text.strip() else None

        type_el = meet_el.find("type")
        type_code = type_el.get("code") if type_el is not None else None
        type_desc = type_el.text.strip() if type_el is not None and type_el.text else None

        instructors = []
        for instr_el in meet_el.findall(".//instructor"):
            if instr_el.text and instr_el.text.strip():
                instructors.append(instr_el.text.strip())

        meetings.append({
            "meeting_index": idx,
            "start_time": meet_text("start"),
            "end_time": meet_text("end"),
            "days_of_week": meet_text("daysOfTheWeek"),
            "room_number": meet_text("roomNumber"),
            "building_name": meet_text("buildingName"),
            "type_code": type_code,
            "type_description": type_desc,
            "instructors": instructors,
        })

    return {
        "section_number": section_num,
        "status_code": status_code,
        "part_of_term": part_of_term,
        "start_date": start_date,
        "end_date": end_date,
        "notes": notes,
        "meetings": meetings,
    }


def scrape_term(
    year: int,
    season: str,
    year_term: str,
    fetcher: RateLimitedFetcher,
    checkpoint: CheckpointManager,
    on_section_scraped=None,
) -> int:
    """Scrapes all subjects, courses, and sections for one term."""
    if checkpoint.is_term_completed(year_term):
        print(f"[*] Term {year_term} already completed in checkpoint. Skipping.")
        return 0

    print(f"\n========================================================")
    print(f"[*] Starting scrape for {season.capitalize()} {year} ({year_term})")
    print(f"========================================================")

    term_xml = fetcher.fetch(f"{BASE_URL}/{year}/{season}.xml")
    if not term_xml:
        print(f"[!] Could not fetch term XML for {year_term}. Skipping.")
        return 0

    term_root = parse_xml_clean(term_xml)
    subject_codes = [
        el.get("id") for el in term_root.findall(".//subject") if el.get("id")
    ]
    print(f"[*] Found {len(subject_codes)} subjects in {year_term}.")

    total_sections_scraped = 0

    for s_idx, subj in enumerate(subject_codes, 1):
        subj_xml = fetcher.fetch(f"{BASE_URL}/{year}/{season}/{subj}.xml")
        if not subj_xml:
            continue

        subj_root = parse_xml_clean(subj_xml)
        course_els = subj_root.findall(".//course")

        for c_el in course_els:
            course_num = c_el.get("id")
            course_title = c_el.text.strip() if c_el.text else ""
            if not course_num:
                continue

            if checkpoint.is_course_completed(year_term, subj, course_num):
                continue

            course_xml = fetcher.fetch(f"{BASE_URL}/{year}/{season}/{subj}/{course_num}.xml")
            if not course_xml:
                checkpoint.mark_course_completed(year_term, subj, course_num)
                continue

            c_root = parse_xml_clean(course_xml)
            section_els = c_root.findall(".//section")

            for sec_el in section_els:
                crn = sec_el.get("id")
                if not crn:
                    continue

                sec_xml = fetcher.fetch(f"{BASE_URL}/{year}/{season}/{subj}/{course_num}/{crn}.xml")
                if not sec_xml:
                    continue

                try:
                    parsed = parse_section_xml(sec_xml)
                    record = {
                        "year": year,
                        "season": season.capitalize(),
                        "year_term": year_term,
                        "subject": subj,
                        "course_number": course_num,
                        "course_title": course_title,
                        "crn": crn,
                        **parsed,
                    }
                    checkpoint.save_section(year_term, subj, course_num, crn, record)
                    total_sections_scraped += 1

                    if on_section_scraped:
                        on_section_scraped(record)

                except Exception as ex:
                    print(f"\n[!] Error parsing {subj} {course_num} CRN {crn}: {ex}", file=sys.stderr)

            checkpoint.mark_course_completed(year_term, subj, course_num)

        sys.stdout.write(f"\r  -> Progress: {s_idx}/{len(subject_codes)} subjects processed ({subj}) | {total_sections_scraped} sections saved")
        sys.stdout.flush()

    checkpoint.mark_term_completed(year_term)
    print(f"\n[✓] Completed term {year_term}. Total new sections: {total_sections_scraped}")
    return total_sections_scraped


# ----------------------------------------------------------------------
# CSV EXPORT & IMPORT UTILITIES
# ----------------------------------------------------------------------

CSV_HEADERS = [
    "year",
    "season",
    "year_term",
    "subject",
    "course_number",
    "course_title",
    "crn",
    "section_number",
    "status_code",
    "part_of_term",
    "start_date",
    "end_date",
    "meeting_index",
    "start_time",
    "end_time",
    "days_of_week",
    "room_number",
    "building_name",
    "type_code",
    "type_description",
    "instructors",
    "notes",
]


def export_sections_to_csv(sections: List[dict], output_csv_path: str):
    """Exports structured section records into a flat tabular CSV file."""
    os.makedirs(os.path.dirname(os.path.abspath(output_csv_path)), exist_ok=True)
    row_count = 0

    with open(output_csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(CSV_HEADERS)

        for sec in sections:
            meetings = sec.get("meetings", [])
            if not meetings:
                writer.writerow([
                    sec.get("year", ""),
                    sec.get("season", ""),
                    sec.get("year_term", ""),
                    sec.get("subject", ""),
                    sec.get("course_number", ""),
                    sec.get("course_title", ""),
                    sec.get("crn", ""),
                    sec.get("section_number", ""),
                    sec.get("status_code", ""),
                    sec.get("part_of_term", ""),
                    sec.get("start_date", ""),
                    sec.get("end_date", ""),
                    0,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    sec.get("notes", "") or "",
                ])
                row_count += 1
            else:
                for m in meetings:
                    instr_str = "; ".join(m.get("instructors", []))
                    writer.writerow([
                        sec.get("year", ""),
                        sec.get("season", ""),
                        sec.get("year_term", ""),
                        sec.get("subject", ""),
                        sec.get("course_number", ""),
                        sec.get("course_title", ""),
                        sec.get("crn", ""),
                        sec.get("section_number", ""),
                        sec.get("status_code", ""),
                        sec.get("part_of_term", ""),
                        sec.get("start_date", ""),
                        sec.get("end_date", ""),
                        m.get("meeting_index", 0),
                        m.get("start_time", "") or "",
                        m.get("end_time", "") or "",
                        m.get("days_of_week", "") or "",
                        m.get("room_number", "") or "",
                        m.get("building_name", "") or "",
                        m.get("type_code", "") or "",
                        m.get("type_description", "") or "",
                        instr_str,
                        sec.get("notes", "") or "",
                    ])
                    row_count += 1

    print(f"[✓] Exported {len(sections)} sections ({row_count} rows) to CSV: {output_csv_path}")


def export_sections_to_json(sections: List[dict], output_json_path: str):
    """Exports sections into structured JSON."""
    os.makedirs(os.path.dirname(os.path.abspath(output_json_path)), exist_ok=True)
    with open(output_json_path, "w", encoding="utf-8") as f:
        json.dump(sections, f, indent=2, ensure_ascii=False)
    print(f"[✓] Exported {len(sections)} sections to JSON: {output_json_path}")


def import_csv_to_postgres(csv_path: str, db_url: str):
    """
    Imports exported sections CSV directly into Kingfisher PostgreSQL database tables:
    terms, subjects, courses, course_offerings, scheduled_sections, scheduled_section_meetings, instructors.
    """
    if not os.path.exists(csv_path):
        print(f"[!] CSV file not found: {csv_path}", file=sys.stderr)
        return False

    print(f"[*] Importing CSV ({csv_path}) into PostgreSQL...")

    sections_by_crn: Dict[Tuple[str, str, str, str], dict] = {}

    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            key = (row["year_term"], row["subject"], row["course_number"], row["crn"])
            if key not in sections_by_crn:
                sections_by_crn[key] = {
                    "year": int(row["year"]),
                    "season": row["season"],
                    "year_term": row["year_term"],
                    "subject": row["subject"],
                    "course_number": row["course_number"],
                    "course_title": row["course_title"],
                    "crn": row["crn"],
                    "section_number": row["section_number"],
                    "status_code": row["status_code"],
                    "part_of_term": row["part_of_term"],
                    "start_date": row["start_date"] or None,
                    "end_date": row["end_date"] or None,
                    "notes": row["notes"] or None,
                    "meetings": [],
                }

            if row["type_code"] or row["building_name"] or row["start_time"] or row["instructors"]:
                instructors = [i.strip() for i in row["instructors"].split(";") if i.strip()]
                sections_by_crn[key]["meetings"].append({
                    "meeting_index": int(row.get("meeting_index", len(sections_by_crn[key]["meetings"]))),
                    "start_time": row["start_time"] or None,
                    "end_time": row["end_time"] or None,
                    "days_of_week": row["days_of_week"] or None,
                    "room_number": row["room_number"] or None,
                    "building_name": row["building_name"] or None,
                    "type_code": row["type_code"] or None,
                    "type_description": row["type_description"] or None,
                    "instructors": instructors,
                })

    print(f"[*] Parsed {len(sections_by_crn)} unique scheduled sections from CSV.")

    sql_file = csv_path + ".import.sql"
    generate_import_sql(sections_by_crn.values(), sql_file)
    print(f"[*] Executing SQL batch import via Docker psql...")
    cmd = ["docker", "exec", "-i", "kingfisher-api-db-1", "psql", "-U", "postgres", "-d", "illini_grades"]
    try:
        with open(sql_file, "r", encoding="utf-8") as sf:
            res = subprocess.run(cmd, stdin=sf, capture_output=True, text=True)
        if res.returncode == 0:
            print(f"[✓] Successfully imported {len(sections_by_crn)} scheduled sections into PostgreSQL!")
            return True
        else:
            print(f"[!] psql error: {res.stderr}", file=sys.stderr)
            return False
    except Exception as ex:
        print(f"[!] Import execution failed: {ex}. SQL script saved at {sql_file}", file=sys.stderr)
        return False


def generate_import_sql(sections, sql_output_path: str):
    """Generates an idempotent SQL script to insert sections into postgres."""
    with open(sql_output_path, "w", encoding="utf-8") as f:
        f.write("BEGIN;\n\n")
        for s in sections:
            notes_esc = ("'" + s["notes"].replace("'", "''") + "'") if s.get("notes") else "NULL"
            title_esc = "'" + (s["course_title"] or "").replace("'", "''") + "'"
            s_date = f"'{s['start_date']}'" if s.get("start_date") else "NULL"
            e_date = f"'{s['end_date']}'" if s.get("end_date") else "NULL"

            f.write(f"""
INSERT INTO terms (year, season, year_term) VALUES ({s['year']}, '{s['season']}', '{s['year_term']}') ON CONFLICT (year_term) DO NOTHING;
INSERT INTO subjects (code) VALUES ('{s['subject']}') ON CONFLICT (code) DO NOTHING;
INSERT INTO courses (subject_id, number, title) VALUES ((SELECT id FROM subjects WHERE code = '{s['subject']}'), {s['course_number']}, {title_esc}) ON CONFLICT DO NOTHING;
INSERT INTO course_offerings (course_id, term_id) VALUES (
    (SELECT c.id FROM courses c JOIN subjects s ON c.subject_id = s.id WHERE s.code = '{s['subject']}' AND c.number = {s['course_number']} LIMIT 1),
    (SELECT id FROM terms WHERE year_term = '{s['year_term']}')
) ON CONFLICT (course_id, term_id) DO NOTHING;

INSERT INTO scheduled_sections (course_offering_id, crn, section_number, status_code, part_of_term, start_date, end_date, notes)
VALUES (
    (SELECT co.id FROM course_offerings co JOIN courses c ON co.course_id = c.id JOIN subjects s ON c.subject_id = s.id JOIN terms t ON co.term_id = t.id WHERE s.code = '{s['subject']}' AND c.number = {s['course_number']} AND t.year_term = '{s['year_term']}' LIMIT 1),
    '{s['crn']}', '{s['section_number'] or ''}', '{s['status_code'] or ''}', '{s['part_of_term'] or ''}', {s_date}, {e_date}, {notes_esc}
) ON CONFLICT (course_offering_id, crn) DO UPDATE SET
    section_number = EXCLUDED.section_number, status_code = EXCLUDED.status_code, part_of_term = EXCLUDED.part_of_term,
    start_date = EXCLUDED.start_date, end_date = EXCLUDED.end_date, notes = EXCLUDED.notes;
""")
        f.write("\nCOMMIT;\n")


# ----------------------------------------------------------------------
# CLI MAIN
# ----------------------------------------------------------------------

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    api_dir = os.path.dirname(script_dir)
    default_data_dir = os.path.join(api_dir, "data", "course_explorer")
    default_checkpoint_db = os.path.join(api_dir, "data", "scraper_checkpoint.db")

    parser = argparse.ArgumentParser(
        description="Scrapes historical UIUC Course Explorer data (2004-present) and exports/imports CSV/JSON/Postgres.",
        formatter_class=argparse.RawTextHelpFormatter,
    )

    mode_group = parser.add_argument_group("Scraping Modes")
    mode_group.add_argument("--all", action="store_true", help="Scrape all available historical terms from Course Explorer (2004 to present).")
    mode_group.add_argument("--start-year", type=int, help="Starting year to scrape (e.g. 2018).")
    mode_group.add_argument("--end-year", type=int, help="Ending year to scrape (e.g. 2024).")
    mode_group.add_argument("--terms", type=str, help="Comma-separated list of terms (e.g. 2023-fa,2024-sp).")
    mode_group.add_argument("--term", type=str, help="Single term (e.g. 2024-fa).")
    mode_group.add_argument("--discover-terms", action="store_true", help="List all available terms from schedule.xml and exit.")

    data_group = parser.add_argument_group("Data Export & Import")
    data_group.add_argument("--export-csv", type=str, help="Path to write exported CSV file.")
    data_group.add_argument("--export-json", type=str, help="Path to write exported JSON file.")
    data_group.add_argument("--import-csv", type=str, help="Path to CSV file to import directly into PostgreSQL.")
    data_group.add_argument("--db-url", type=str, default=os.getenv("DATABASE_URL", "postgresql://postgres:postgres@localhost:5432/illini_grades"), help="PostgreSQL connection URL.")

    config_group = parser.add_argument_group("Configuration")
    config_group.add_argument("--delay", type=float, default=DEFAULT_DELAY, help=f"Delay between requests in seconds (default: {DEFAULT_DELAY}).")
    config_group.add_argument("--checkpoint-db", type=str, default=default_checkpoint_db, help="Path to SQLite checkpoint database.")
    config_group.add_argument("--output-dir", type=str, default=default_data_dir, help="Directory for term export files.")

    args = parser.parse_args()

    # Mode 1: Import CSV directly into database
    if args.import_csv:
        success = import_csv_to_postgres(args.import_csv, args.db_url)
        sys.exit(0 if success else 1)

    fetcher = RateLimitedFetcher(delay=args.delay)

    # Mode 2: Discover terms
    if args.discover_terms:
        print("[*] Querying Course Explorer schedule.xml...")
        terms = discover_available_terms(fetcher)
        print(f"\nDiscovered {len(terms)} available terms:")
        for y, season, yt in terms:
            print(f"  - {yt} ({season.capitalize()} {y})")
        sys.exit(0)

    checkpoint = CheckpointManager(args.checkpoint_db)

    # Determine terms to scrape
    target_terms: List[Tuple[int, str, str]] = []

    if args.term:
        parts = args.term.lower().split("-")
        y = int(parts[0])
        s_code = parts[1]
        s_map = {"sp": "spring", "su": "summer", "fa": "fall", "wi": "winter"}
        season = s_map.get(s_code, "fall")
        target_terms = [(y, season, f"{y}-{season_to_code(season)}")]

    elif args.terms:
        for t_spec in args.terms.split(","):
            t_clean = t_spec.strip().lower()
            parts = t_clean.split("-")
            y = int(parts[0])
            s_map = {"sp": "spring", "su": "summer", "fa": "fall", "wi": "winter"}
            season = s_map.get(parts[1], "fall")
            target_terms.append((y, season, f"{y}-{season_to_code(season)}"))

    elif args.all or args.start_year or args.end_year:
        print("[*] Discovering available terms from UIUC Course Explorer...")
        all_terms = discover_available_terms(fetcher)
        for y, season, yt in all_terms:
            if args.start_year and y < args.start_year:
                continue
            if args.end_year and y > args.end_year:
                continue
            target_terms.append((y, season, yt))

    if not target_terms and not (args.export_csv or args.export_json):
        parser.print_help()
        sys.exit(1)

    # Perform scraping
    if target_terms:
        print(f"[*] Target terms to scrape ({len(target_terms)} total): {[t[2] for t in target_terms]}")
        for year, season, yt in target_terms:
            scrape_term(year, season, yt, fetcher, checkpoint)

    # Perform Exports if requested
    if args.export_csv or args.export_json:
        print("\n[*] Loading scraped sections from checkpoint database...")
        all_sections = checkpoint.get_all_scraped_sections()
        print(f"[*] Loaded {len(all_sections)} total sections.")

        if args.export_csv:
            export_sections_to_csv(all_sections, args.export_csv)
        if args.export_json:
            export_sections_to_json(all_sections, args.export_json)


if __name__ == "__main__":
    main()
