# Kingfisher API

A REST interface for UIUC course data powered by Spring Boot. This README is for setting up your own Kingfisher API and is not designed to help you navigate the website. Inspired by Madgrades.

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
  -F "file=@../uiuc-gpa-dataset.csv"
```

## Docs
[http://localhost:5902/swagger-ui.html](http://localhost:5902/swagger-ui.html)

TO DO:
- Automate messages to professors to request copies of past syllabi