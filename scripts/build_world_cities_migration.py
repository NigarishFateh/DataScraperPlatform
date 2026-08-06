#!/usr/bin/env python3
"""Build Flyway V11 from GeoNames cities5000 + countryInfo capitals."""

from __future__ import annotations

import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GEONAMES = ROOT / "scripts" / "geonames"
OUT = ROOT / "location-service" / "src" / "main" / "resources" / "db" / "migration" / "V11__world_cities_geonames.sql"

SLUG_RE = re.compile(r"[^a-z0-9]+")


def slugify(value: str) -> str:
    text = unicodedata.normalize("NFKD", value)
    text = "".join(ch for ch in text if not unicodedata.combining(ch))
    text = text.lower().strip()
    text = SLUG_RE.sub("-", text).strip("-")
    return text or "city"


def sql_escape(value: str) -> str:
    return value.replace("'", "''")


def load_valid_countries() -> set[str]:
    # Prefer ISO list already seeded in V4 — parse from countryInfo (same ISO2 codes).
    codes: set[str] = set()
    path = GEONAMES / "countryInfo.txt"
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        parts = line.split("\t")
        if len(parts) < 5:
            continue
        code = parts[0].strip().upper()
        if len(code) == 2:
            codes.add(code)
    return codes


def load_capitals() -> dict[str, str]:
    capitals: dict[str, str] = {}
    path = GEONAMES / "countryInfo.txt"
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        parts = line.split("\t")
        if len(parts) < 6:
            continue
        code = parts[0].strip().upper()
        capital = parts[5].strip()
        if len(code) == 2 and capital:
            capitals[code] = capital
    return capitals


def main() -> None:
    countries = load_valid_countries()
    capitals = load_capitals()
    cities_path = GEONAMES / "cities5000.txt"

    # key: city_id -> (name, country)
    rows: dict[str, tuple[str, str]] = {}
    used_ids: set[str] = set()
    countries_with_city: set[str] = set()

    for line in cities_path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) < 15:
            continue
        geoname_id = parts[0].strip()
        name = parts[1].strip() or parts[2].strip()
        ascii_name = parts[2].strip() or name
        country = parts[8].strip().upper()
        if country not in countries:
            continue
        if not name:
            continue
        name = name[:200]
        base = slugify(ascii_name)[:40]
        city_id = f"{country.lower()}-{base}"
        if city_id in used_ids:
            city_id = f"{country.lower()}-{base}-{geoname_id}"
        if len(city_id) > 96:
            city_id = f"{country.lower()}-{geoname_id}"
        used_ids.add(city_id)
        rows[city_id] = (name, country)
        countries_with_city.add(country)

    # Capitals for countries still missing any city.
    for code, capital in capitals.items():
        if code not in countries or code in countries_with_city:
            continue
        name = capital[:200]
        base = slugify(capital)[:40]
        city_id = f"{code.lower()}-{base}"
        if city_id in used_ids:
            city_id = f"{code.lower()}-{base}-capital"
        used_ids.add(city_id)
        rows[city_id] = (name, code)
        countries_with_city.add(code)

    items = sorted(rows.items(), key=lambda item: (item[1][1], item[1][0].lower(), item[0]))

    lines: list[str] = [
        "-- GeoNames cities5000 (+ capitals for countries with no large cities).",
        "-- Source: https://download.geonames.org/export/dump/",
        "",
        "ALTER TABLE cities ALTER COLUMN id TYPE VARCHAR(96);",
        "ALTER TABLE cities ALTER COLUMN name TYPE VARCHAR(200);",
        "",
    ]

    batch_size = 400
    for i in range(0, len(items), batch_size):
        batch = items[i : i + batch_size]
        lines.append("INSERT INTO cities (id, name, country_code)")
        lines.append("SELECT v.id, v.name, v.country_code")
        lines.append("FROM (VALUES")
        value_lines = []
        for city_id, (name, country) in batch:
            value_lines.append(
                f"  ('{sql_escape(city_id)}', '{sql_escape(name)}', '{country}')"
            )
        lines.append(",\n".join(value_lines))
        lines.append(") AS v(id, name, country_code)")
        lines.append("WHERE EXISTS (SELECT 1 FROM countries c WHERE c.code = v.country_code)")
        lines.append("  AND NOT EXISTS (SELECT 1 FROM cities x WHERE x.id = v.id);")
        lines.append("")

    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {OUT}")
    print(f"cities={len(items)} countries_covered={len(countries_with_city)} countries_total={len(countries)}")


if __name__ == "__main__":
    main()
