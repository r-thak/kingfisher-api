# Kingfisher API

A REST interface for UIUC course data powered by Spring Boot. This README is for setting up your own Kingfisher API and is not designed to help you navigate the website. Inspired by Madgrades.
[Web UI](https://kf.rthak.com/)
[Web UI Repo](https://github.com/r-thak/kingfisher-web)

## Setup

By default, the provided example .env should work, but should be changed.
```bash
cp .env.example .env
```

Run with docker compose:
```bash
docker compose up -d --build
```

OR run separately (requires Java 21 and Gradle):
```bash
docker compose up -d db
gradle bootRun
```

## Update DB
Once running, upload a relevant UIUC grades CSV (`uiuc-gpa-dataset.csv`) through the `/v1/admin/ingest` end-point. Supply the API Key located in your `.env`:

```bash
curl -X POST http://localhost:5902/v1/admin/ingest \
  -H "Authorization: Bearer your-api-key-here" \
  -F "file=@uiuc-gpa-dataset.csv"
```

## Live section schedules
`/v1/admin/ingest-sections` pulls live section schedules (CRN, meeting times/rooms,
instructors) for a given term directly from the public UIUC Course Explorer, rather
than from a CSV. Runs in the background (it's a long walk of the Course Explorer's
own subject/course/section catalog with a deliberate delay between requests, so it
respects their rate limits) — the response comes back immediately, check server logs
for progress:

```bash
curl -X POST "http://localhost:5902/v1/admin/ingest-sections?year=2026&season=fall" \
  -H "Authorization: Bearer your-api-key-here"
```

Safe to re-trigger for the same term at any point (e.g. periodically during
registration) — it upserts by CRN and prunes sections that disappeared from the
schedule. Only one ingestion runs at a time; a second call while one is in progress
returns an error instead of running concurrently. Seat/enrollment counts aren't
included — the public Course Explorer API doesn't expose those, only schedule data.

Read the ingested data back via:
```
GET /v1/courses/{id}/scheduled-sections?term=2026-fa
```

## Docs
[https://kingfisherapi.rthak.com/swagger-ui/index.html](https://kingfisherapi.rthak.com/swagger-ui/index.html)

TO DO:
- Automate messages to professors to request copies of past syllabi
