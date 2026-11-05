#!/usr/bin/env python3
"""
Gera dados MOCK com a forma das respostas da Lufthansa Open API.

  airports.json   -> estilo /references/airports   (coordenadas e fusos reais)
  schedules.json  -> estilo /operations/schedules  (horarios INVENTADOS, padrao de operacao)
  prices.json     -> NAO existe na API da Lufthansa; ficheiro separado com fetchedAt/source

Os aeroportos sao reais. As rotas sao plausiveis (hubs LH/LX/OS/SN) mas os numeros de voo,
horas, terminais e precos sao gerados. Nao usar como informacao de viagem.

Uso:  python3 generate_mock.py [--seed 42] [--price-from 2027-05-08] [--price-to 2027-05-24]
"""
import argparse
import json
import math
import random
from datetime import date, datetime, time, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

# code, nome, cityCode, pais, lat, lon, timezone
AIRPORTS = [
    ("FRA", "Frankfurt/Main", "FRA", "DE", 50.0379, 8.5622, "Europe/Berlin"),
    ("MUC", "Munich", "MUC", "DE", 48.3538, 11.7861, "Europe/Berlin"),
    ("ZRH", "Zurich", "ZRH", "CH", 47.4647, 8.5492, "Europe/Zurich"),
    ("VIE", "Vienna", "VIE", "AT", 48.1103, 16.5697, "Europe/Vienna"),
    ("BRU", "Brussels", "BRU", "BE", 50.9014, 4.4844, "Europe/Brussels"),
    ("LIS", "Lisbon", "LIS", "PT", 38.7813, -9.1359, "Europe/Lisbon"),
    ("OPO", "Porto", "OPO", "PT", 41.2481, -8.6814, "Europe/Lisbon"),
    ("FAO", "Faro", "FAO", "PT", 37.0144, -7.9659, "Europe/Lisbon"),
    ("MAD", "Madrid", "MAD", "ES", 40.4983, -3.5676, "Europe/Madrid"),
    ("BCN", "Barcelona", "BCN", "ES", 41.2974, 2.0833, "Europe/Madrid"),
    ("LHR", "London Heathrow", "LON", "GB", 51.4700, -0.4543, "Europe/London"),
    ("CDG", "Paris Charles de Gaulle", "PAR", "FR", 49.0097, 2.5479, "Europe/Paris"),
    ("AMS", "Amsterdam", "AMS", "NL", 52.3105, 4.7683, "Europe/Amsterdam"),
    ("FCO", "Rome Fiumicino", "ROM", "IT", 41.8003, 12.2389, "Europe/Rome"),
    ("ATH", "Athens", "ATH", "GR", 37.9364, 23.9445, "Europe/Athens"),
    ("IST", "Istanbul", "IST", "TR", 41.2753, 28.7519, "Europe/Istanbul"),
    ("CPH", "Copenhagen", "CPH", "DK", 55.6180, 12.6508, "Europe/Copenhagen"),
    ("WAW", "Warsaw Chopin", "WAW", "PL", 52.1657, 20.9671, "Europe/Warsaw"),
    ("DXB", "Dubai", "DXB", "AE", 25.2532, 55.3657, "Asia/Dubai"),
    ("BKK", "Bangkok Suvarnabhumi", "BKK", "TH", 13.6900, 100.7501, "Asia/Bangkok"),
    ("SIN", "Singapore Changi", "SIN", "SG", 1.3644, 103.9915, "Asia/Singapore"),
    ("HND", "Tokyo Haneda", "TYO", "JP", 35.5494, 139.7798, "Asia/Tokyo"),
    ("NRT", "Tokyo Narita", "TYO", "JP", 35.7720, 140.3929, "Asia/Tokyo"),
    ("ICN", "Seoul Incheon", "SEL", "KR", 37.4602, 126.4407, "Asia/Seoul"),
    ("DEL", "Delhi", "DEL", "IN", 28.5562, 77.1000, "Asia/Kolkata"),
    ("JFK", "New York JFK", "NYC", "US", 40.6413, -73.7781, "America/New_York"),
    ("EWR", "Newark Liberty", "NYC", "US", 40.6895, -74.1745, "America/New_York"),
    ("ORD", "Chicago O'Hare", "CHI", "US", 41.9742, -87.9073, "America/Chicago"),
    ("SFO", "San Francisco", "SFO", "US", 37.6213, -122.3790, "America/Los_Angeles"),
    ("LAX", "Los Angeles", "LAX", "US", 33.9416, -118.4085, "America/Los_Angeles"),
    ("YYZ", "Toronto Pearson", "YTO", "CA", 43.6777, -79.6248, "America/Toronto"),
    ("GRU", "Sao Paulo Guarulhos", "SAO", "BR", -23.4356, -46.4731, "America/Sao_Paulo"),
    ("JNB", "Johannesburg", "JNB", "ZA", -26.1337, 28.2420, "Africa/Johannesburg"),
]
AP = {a[0]: a for a in AIRPORTS}

HUB_CARRIER = {"FRA": "LH", "MUC": "LH", "ZRH": "LX", "VIE": "OS", "BRU": "SN"}
HUBS = set(HUB_CARRIER)

ASIA = {"BKK", "SIN", "HND", "NRT", "ICN", "DEL"}
MIDEAST = {"DXB"}
AMER = {"JFK", "EWR", "ORD", "SFO", "LAX", "YYZ"}
SOUTH = {"GRU", "JNB"}

# hub -> [(destino, voos_por_dia_em_cada_sentido)]
HUB_SPOKES = {
    "FRA": [("LIS", 3), ("OPO", 2), ("FAO", 1), ("MAD", 3), ("BCN", 3), ("LHR", 4), ("CDG", 3),
            ("AMS", 3), ("FCO", 3), ("ATH", 2), ("IST", 2), ("CPH", 3), ("WAW", 2),
            ("HND", 2), ("BKK", 2), ("SIN", 1), ("ICN", 1), ("DEL", 2), ("DXB", 2),
            ("JFK", 3), ("EWR", 1), ("ORD", 3), ("SFO", 2), ("LAX", 2), ("YYZ", 2),
            ("GRU", 1), ("JNB", 1)],
    "MUC": [("LIS", 2), ("OPO", 1), ("MAD", 2), ("BCN", 2), ("LHR", 3), ("CDG", 2), ("AMS", 2),
            ("FCO", 2), ("ATH", 2), ("IST", 2), ("CPH", 2), ("HND", 1), ("BKK", 1), ("SIN", 1),
            ("DXB", 1), ("JFK", 1), ("ORD", 1), ("SFO", 1), ("LAX", 1), ("YYZ", 1)],
    "ZRH": [("LIS", 2), ("OPO", 1), ("MAD", 2), ("BCN", 2), ("LHR", 3), ("CDG", 2), ("AMS", 2),
            ("FCO", 2), ("ATH", 1), ("IST", 1), ("CPH", 1), ("NRT", 1), ("BKK", 1), ("SIN", 1),
            ("JFK", 2), ("EWR", 1), ("ORD", 1), ("SFO", 1), ("LAX", 1), ("JNB", 1)],
    "VIE": [("LIS", 1), ("MAD", 1), ("BCN", 1), ("LHR", 2), ("CDG", 2), ("AMS", 1), ("FCO", 2),
            ("ATH", 2), ("IST", 1), ("CPH", 1), ("NRT", 1), ("BKK", 1), ("DXB", 1),
            ("JFK", 1), ("ORD", 1)],
    "BRU": [("LIS", 1), ("OPO", 1), ("MAD", 1), ("BCN", 1), ("LHR", 2), ("CDG", 2), ("FCO", 1),
            ("ATH", 1), ("IST", 1), ("JFK", 1), ("EWR", 1)],
}
# (a, b, companhia, voos_por_dia_em_cada_sentido)
HUB_HUB = [
    ("FRA", "MUC", "LH", 5), ("FRA", "ZRH", "LH", 3), ("FRA", "VIE", "LH", 3), ("FRA", "BRU", "LH", 2),
    ("MUC", "ZRH", "LX", 2), ("MUC", "VIE", "OS", 3), ("ZRH", "VIE", "LX", 2),
    ("ZRH", "BRU", "LX", 1), ("MUC", "BRU", "LH", 1), ("VIE", "BRU", "SN", 1),
]

TERMINALS = {
    "FRA": ["1", "2"], "MUC": ["2"], "ZRH": ["1", "2"], "VIE": ["3"], "BRU": ["B"],
    "LIS": ["1"], "OPO": ["1"], "FAO": ["1"], "MAD": ["4"], "BCN": ["1"], "LHR": ["2"],
    "CDG": ["1", "2"], "AMS": ["1"], "FCO": ["1"], "ATH": ["1"], "IST": ["1"], "CPH": ["3"],
    "WAW": ["1"], "DXB": ["1"], "BKK": ["1"], "SIN": ["1"], "HND": ["3"], "NRT": ["1"],
    "ICN": ["2"], "DEL": ["3"], "JFK": ["1"], "EWR": ["B"], "ORD": ["5"], "SFO": ["G"],
    "LAX": ["B"], "YYZ": ["1"], "GRU": ["3"], "JNB": ["A"],
}

SHORT_AC = ["320", "321", "32N", "32Q", "E90"]
LONG_AC = ["359", "333", "346", "74H", "789"]

# primeiro numero de voo por (companhia, longo_curso)
NUM_START = {
    ("LH", False): 1100, ("LH", True): 400,
    ("LX", False): 700, ("LX", True): 10,
    ("OS", False): 100, ("OS", True): 51,
    ("SN", False): 3000, ("SN", True): 500,
}

SEASON_START = date(2027, 5, 1)
SEASON_END = date(2027, 5, 31)
FETCHED_AT = "2026-10-25T09:00:00Z"


def dist_km(o, d):
    la1, lo1, la2, lo2 = map(math.radians, (AP[o][4], AP[o][5], AP[d][4], AP[d][5]))
    h = math.sin((la2 - la1) / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin((lo2 - lo1) / 2) ** 2
    return 6371 * 2 * math.asin(math.sqrt(h))


def r5(x):
    return int(round(x / 5.0) * 5)


def block_minutes(o, d, rng):
    km = dist_km(o, d)
    if km < 3500:
        m = 35 + km / 700 * 60
    else:
        dlon = AP[d][5] - AP[o][5]
        wind = 1.06 if dlon < -30 else (0.96 if dlon > 30 else 1.0)
        m = 30 + km / 860 * 60 * wind
    return r5(m + rng.randint(-5, 5))


def window(o, d):
    """Janela de partida (horas locais) consoante o tipo de rota."""
    km = dist_km(o, d)
    if km < 3500:
        return (6.25, 21.0)
    far = d if o in HUBS else o
    out = o in HUBS
    if far in ASIA:
        return (11.0, 22.5) if out else (21.0, 23.9)
    if far in MIDEAST:
        return (7.5, 21.5) if out else (8.0, 22.0)
    if far in AMER:
        return (9.0, 15.5) if out else (16.0, 23.0)
    return (20.0, 23.0) if out else (19.0, 22.5)


def slots(rng, f, win):
    start, end = win
    step = (end - start) / f
    out = []
    for i in range(f):
        t = start + (i + 0.5) * step + rng.uniform(-0.3, 0.3) * step
        m = min(1435, max(0, r5(t * 60)))
        out.append(m)
    return sorted(out)


def days_pattern(rng, i):
    if i == 0:
        return "1234567"
    if i == 1:
        return rng.choice(["1234567", "12345", "1234567"])
    return rng.choice(["12345", "1234567", "12345"])


def build_flights(rng):
    counters = dict(NUM_START)
    pairs = []  # (origem_hub, destino, companhia, f)
    for hub, spokes in HUB_SPOKES.items():
        for dest, f in spokes:
            pairs.append((hub, dest, HUB_CARRIER[hub], f))
    for a, b, car, f in HUB_HUB:
        pairs.append((a, b, car, f))

    flights = []
    for a, b, car, f in pairs:
        long_haul = dist_km(a, b) >= 3500
        out_slots = slots(rng, f, window(a, b))
        in_slots = slots(rng, f, window(b, a))
        for i in range(f):
            n = counters[(car, long_haul)]
            counters[(car, long_haul)] += 2
            for (o, d, num, dep) in ((a, b, n, out_slots[i]), (b, a, n + 1, in_slots[i])):
                flights.append({
                    "origin": o, "dest": d, "carrier": car, "number": num,
                    "dep_min": dep, "block": block_minutes(o, d, rng),
                    "days": days_pattern(rng, i),
                    "aircraft": rng.choice(LONG_AC if long_haul else SHORT_AC),
                    "dep_term": rng.choice(TERMINALS[o]),
                    "arr_term": rng.choice(TERMINALS[d]),
                    "freq": f, "long_haul": long_haul,
                })
    return flights


def first_operating_day(days):
    d = SEASON_START
    while str(d.isoweekday()) not in days:
        d += timedelta(days=1)
    return d


def iso_duration(minutes):
    return f"PT{minutes // 60}H{minutes % 60:02d}M"


def schedule_entry(fl):
    d0 = first_operating_day(fl["days"])
    tz_o, tz_d = ZoneInfo(AP[fl["origin"]][6]), ZoneInfo(AP[fl["dest"]][6])
    dep = datetime.combine(d0, time(fl["dep_min"] // 60, fl["dep_min"] % 60), tzinfo=tz_o)
    arr = (dep + timedelta(minutes=fl["block"])).astimezone(tz_d)
    return {
        "TotalJourney": {"Duration": iso_duration(fl["block"])},
        "Flight": {  # objeto (nao array) quando ha um unico voo, como na API real
            "Departure": {
                "AirportCode": fl["origin"],
                "ScheduledTimeLocal": {"DateTime": dep.strftime("%Y-%m-%dT%H:%M")},
                "Terminal": {"Name": fl["dep_term"]},
            },
            "Arrival": {
                "AirportCode": fl["dest"],
                "ScheduledTimeLocal": {"DateTime": arr.strftime("%Y-%m-%dT%H:%M")},
                "Terminal": {"Name": fl["arr_term"]},
            },
            "MarketingCarrier": {"AirlineID": fl["carrier"], "FlightNumber": str(fl["number"])},
            "Equipment": {"AircraftCode": fl["aircraft"]},
            "Details": {
                "Stops": {"StopQuantity": 0},
                "DaysOfOperation": fl["days"],
                "DatePeriod": {
                    "Effective": SEASON_START.isoformat(),
                    "Expiration": SEASON_END.isoformat(),
                },
            },
        },
    }


def build_airports():
    ref = date(2027, 5, 15)
    items = []
    for code, name, city, country, lat, lon, tz in AIRPORTS:
        off = datetime.combine(ref, time(12), tzinfo=ZoneInfo(tz)).utcoffset().total_seconds() / 3600
        items.append({
            "AirportCode": code,
            "Position": {"Coordinate": {"Latitude": lat, "Longitude": lon}},
            "CityCode": city,
            "CountryCode": country,
            "LocationType": "Airport",
            "Names": {"Name": [{"@LanguageCode": "EN", "$": name}]},
            "UtcOffset": off,
            "TimeZoneId": tz,
        })
    return {"AirportResource": {"Airports": {"Airport": items}}}


def build_schedules(flights):
    by_route = {}
    for fl in flights:
        by_route.setdefault((fl["origin"], fl["dest"]), []).append(fl)
    routes = []
    for (o, d), fls in sorted(by_route.items()):
        fls.sort(key=lambda x: x["dep_min"])
        routes.append({
            "origin": o,
            "destination": d,
            "ScheduleResource": {
                "Schedule": [schedule_entry(f) for f in fls],
                "Meta": {"Version": "1.0.0"},
            },
        })
    return {
        "_comment": "MOCK. Forma inspirada em GET /v2/operations/schedules/{origin}/{destination}/{date}. "
                    "Horarios, numeros de voo e terminais sao gerados.",
        "_season": {"Effective": SEASON_START.isoformat(), "Expiration": SEASON_END.isoformat()},
        "routes": routes,
    }


def build_prices(flights, rng, d_from, d_to):
    out = []
    ref_fetch = date.fromisoformat(FETCHED_AT[:10])
    for fl in flights:
        km = dist_km(fl["origin"], fl["dest"])
        base = 150 + 0.040 * km if fl["long_haul"] else 45 + 0.055 * km
        if fl["freq"] >= 3:
            base *= 0.92
        hour = fl["dep_min"] / 60
        base *= 0.95 if (hour < 7 or hour > 21) else 1.0
        d = d_from
        while d <= d_to:
            if str(d.isoweekday()) in fl["days"]:
                wd = {1: 1.04, 2: 0.92, 3: 0.92, 4: 1.0, 5: 1.12, 6: 0.97, 7: 1.12}[d.isoweekday()]
                lead = (d - ref_fetch).days
                lead_f = 1.0 if lead > 45 else 1.0 + (45 - lead) * 0.006
                amount = base * wd * lead_f * rng.uniform(0.92, 1.08)
                out.append({
                    "carrier": fl["carrier"], "flightNumber": str(fl["number"]),
                    "origin": fl["origin"], "destination": fl["dest"],
                    "departureDate": d.isoformat(), "cabin": "ECONOMY",
                    "amount": round(round(amount) - 0.1, 2), "currency": "EUR",
                    "source": "mock-pricing", "fetchedAt": FETCHED_AT,
                })
            d += timedelta(days=1)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--price-from", default="2027-05-08")
    ap.add_argument("--price-to", default="2027-05-24")
    ap.add_argument("--out", default=str(Path(__file__).parent))
    args = ap.parse_args()

    rng = random.Random(args.seed)
    out = Path(args.out)
    flights = build_flights(rng)

    (out / "airports.json").write_text(json.dumps(build_airports(), indent=2, ensure_ascii=False))
    (out / "schedules.json").write_text(json.dumps(build_schedules(flights), indent=2, ensure_ascii=False))

    prices = build_prices(flights, rng, date.fromisoformat(args.price_from), date.fromisoformat(args.price_to))
    header = {"_comment": "MOCK. A API da Lufthansa nao devolve precos; separado de proposito.",
              "currency": "EUR", "fetchedAt": FETCHED_AT}
    body = ",\n".join(json.dumps(p, ensure_ascii=False) for p in prices)
    (out / "prices.json").write_text(
        json.dumps(header, ensure_ascii=False)[:-1] + ',\n"prices": [\n' + body + "\n]}\n")

    print(f"airports: {len(AIRPORTS)} | voos (padroes): {len(flights)} | precos: {len(prices)}")


if __name__ == "__main__":
    main()
