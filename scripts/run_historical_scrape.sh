#!/usr/bin/env bash
# ==============================================================================
# UIUC Course Explorer Historical Scraper & Data Manager Runner
# ==============================================================================
#
# Usage Examples (run from kingfisher-api/):
#
# 1. Discover all available semesters on Course Explorer:
#    ./scripts/run_historical_scrape.sh discover
#
# 2. Scrape a single semester to CSV:
#    ./scripts/run_historical_scrape.sh scrape-term 2024-fa
#
# 3. Scrape a range of years (e.g. 2018 to 2025) to CSV:
#    ./scripts/run_historical_scrape.sh scrape-range 2018 2025
#
# 4. Scrape ALL historical semesters (2004 to present) to CSV:
#    ./scripts/run_historical_scrape.sh scrape-all
#
# 5. Import previously scraped CSV into PostgreSQL:
#    ./scripts/run_historical_scrape.sh import-csv data/course_explorer/sections_2024-fa.csv
#
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PYTHON_SCRIPT="$SCRIPT_DIR/scrape_course_explorer.py"
DATA_DIR="$API_ROOT/data/course_explorer"
DB_URL="${DATABASE_URL:-postgresql://postgres:postgres@localhost:5432/illini_grades}"

mkdir -p "$DATA_DIR"

cmd="${1:-help}"

case "$cmd" in
  discover)
    echo "Querying UIUC Course Explorer for all available terms..."
    python3 "$PYTHON_SCRIPT" --discover-terms
    ;;

  scrape-term)
    term="${2:?Error: Specify term (e.g. 2024-fa)}"
    csv_file="$DATA_DIR/sections_${term}.csv"
    echo "Scraping term $term to $csv_file..."
    python3 "$PYTHON_SCRIPT" --term "$term" --export-csv "$csv_file"
    ;;

  scrape-range)
    start_y="${2:?Error: Specify start year (e.g. 2020)}"
    end_y="${3:?Error: Specify end year (e.g. 2024)}"
    csv_file="$DATA_DIR/sections_${start_y}_${end_y}.csv"
    echo "Scraping years $start_y to $end_y to $csv_file..."
    python3 "$PYTHON_SCRIPT" --start-year "$start_y" --end-year "$end_y" --export-csv "$csv_file"
    ;;

  scrape-all)
    csv_file="$DATA_DIR/all_historical_sections.csv"
    json_file="$DATA_DIR/all_historical_sections.json"
    echo "Scraping ALL historical terms (2004-present) from Course Explorer..."
    echo "Data will be exported to: $csv_file and $json_file"
    echo "Checkpoint database enables safe stopping (Ctrl+C) and resuming anytime."
    python3 "$PYTHON_SCRIPT" --all --export-csv "$csv_file" --export-json "$json_file"
    ;;

  import-csv)
    csv_file="${2:?Error: Specify path to CSV file}"
    echo "Importing CSV ($csv_file) into PostgreSQL ($DB_URL)..."
    python3 "$PYTHON_SCRIPT" --import-csv "$csv_file" --db-url "$DB_URL"
    ;;

  *)
    echo "UIUC Course Explorer Scraper & Data Manager"
    echo ""
    echo "Commands:"
    echo "  discover                         List all available terms from schedule.xml"
    echo "  scrape-term <term>               Scrape one term (e.g. 2024-fa) to CSV"
    echo "  scrape-range <start> <end>       Scrape year range (e.g. 2020 2024) to CSV"
    echo "  scrape-all                       Scrape all past terms (2004-present) to CSV"
    echo "  import-csv <file.csv>            Import CSV into PostgreSQL database"
    echo ""
    ;;
esac
