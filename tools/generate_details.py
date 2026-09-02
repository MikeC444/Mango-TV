#!/usr/bin/env python3
"""
Generates the detail-screen payload: cast lists, seasons and episodes.

This is kept in a separate asset from mock_catalog.json, and generated rather
than hand-authored, for two reasons.

The architectural one: a detail payload is an order of magnitude larger than
the browse projection and is only ever needed for the one title being looked
at. Splitting it out lets the application load it lazily, which is the
behaviour a real content service would force anyway - so the screens are built
against the shape they will actually have.

The practical one: episode metadata is placeholder text that a real provider
will replace wholesale. Episode titles are authored per series, because they
are what a viewer reads down a list. Synopses are composed from an authored
pool, which gives the volume needed to exercise scrolling and lazy loading
without pretending to be finished writing.

Run from the repository root:  python3 tools/generate_details.py
"""

import hashlib
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
CATALOG_PATH = os.path.join(ASSETS, "mock_catalog.json")
OUT_PATH = os.path.join(ASSETS, "mock_details.json")

SEASONS_PER_SERIES = 2
EPISODES_PER_SEASON = 6

# Invented names. Any resemblance to a working performer is unintended; these
# exist to give the cast row realistic string lengths to lay out.
FIRST_NAMES = [
    "Aoife", "Marcus", "Ingrid", "Tobias", "Nadia", "Rowan", "Celia", "Idris",
    "Marta", "Felix", "Yusuf", "Hanne", "Oscar", "Leila", "Bram", "Juno",
    "Cormac", "Sylvie", "Anton", "Mireille", "Kwame", "Elsa", "Rafael", "Nina",
]
LAST_NAMES = [
    "Vance", "Okonjo", "Halvorsen", "Marchetti", "Bexley", "Ferreira", "Lindqvist",
    "Ashworth", "Delacroix", "Nakamura", "OStrand", "Ruiz", "Whitlock", "Baptiste",
    "Sorensen", "Achebe", "Calloway", "Duarte", "Fairweather", "Novak",
]
ROLES = [
    "Eleanor Shaw", "The Surveyor", "Dr. Anselm", "Kit Mowbray", "The Registrar",
    "Vera Lund", "Thomas Fen", "The Harbourmaster", "Ada Crane", "Silas Redmond",
    "Margit", "The Apprentice", "Josephine Hale", "Captain Ivers", "Nell",
]

# Episode titles, authored per series so a list reads as one show's run.
EPISODE_TITLES = {
    "s01": ["Under Glass", "The Founder's Hand", "Propagation", "Nightblooming",
            "The Cold Frame", "Cuttings", "Dormancy", "The Long Border",
            "Grafting", "Seed Stock", "The Orangery", "Last Light"],
    "s02": ["Attribution", "The Signature", "Condition Report", "Reserve",
            "The Catalogue", "Hammer Price", "Provenance Gap", "The Consignor",
            "Restitution", "Lot 41", "Withdrawn", "Sold As Seen"],
    "s03": ["Spring Tide", "The Causeway", "House Call", "Saltmarsh",
            "The Long Round", "Neap", "Dressings", "The Ferry",
            "Night Visit", "Reed Cutter", "The Bore", "Slack Water"],
    "s04": ["Standing By", "Blocking", "The Note", "Half Hour",
            "Beginners", "Curtain Up", "The Understudy", "Dark Night",
            "Press Night", "Notices", "The Run", "Closing"],
    "s05": ["First Light", "The Reopening", "Statement", "Churring",
            "The Heath", "Ringing", "Cold Case", "The Witness",
            "Migration", "Last Sighting", "The Roost", "Dusk Chorus"],
    "s06": ["Pangaea", "The Rift", "Subduction", "Cratons",
            "The Argument", "Sea Floor", "Magnetism", "Reversal",
            "Deep Time", "The Boundary", "Uplift", "Consensus"],
    "s07": ["Legal Deposit", "The Stacks", "Uncatalogued", "Foxing",
            "The Reading Room", "Accession", "Deaccession", "The Backlog",
            "Marginalia", "Shelfmark", "Closed Access", "Last Request"],
    "s08": ["Year Four", "Pressure", "The Moon Pool", "Saturation",
            "Blackout", "The Surface", "Decompression", "Silt",
            "The Signal", "Bends", "Resupply", "Ascent"],
    "s09": ["Mirror Check", "Hill Start", "The Test", "Blind Spot",
            "Three-Point", "Motorway", "The Confession", "Emergency Stop",
            "Night Driving", "Pass Plus", "The Retest", "Full Licence"],
    "s10": ["The Line", "1919", "Crossing", "The Valley",
            "Requisition", "1947", "The Return", "Boundary Stone",
            "1989", "The Survey", "Redrawn", "Common Ground"],
    "s11": ["High Water", "The Flats", "Recovered", "Spring Ebb",
            "The Channel", "Mudlark", "Slipway", "The Dredger",
            "Silt Line", "Low Water", "The Wreck", "Turn of the Tide"],
    "s12": ["The Order", "Casting Pit", "The Mould", "Tuning",
            "Bell Metal", "The Pour", "Cooling", "The Clapper",
            "Hanging", "First Peal", "The Inscription", "Ringing Out"],
}

# Sentence frames, varied enough that a scrolled list does not read as one
# sentence repeated. Placeholder copy, plainly.
SYNOPSIS_FRAMES = [
    "An arrangement that held for years stops holding, and nobody says so out loud.",
    "A visit that was meant to take an afternoon runs into the following week.",
    "Someone produces a document that everyone had agreed was lost.",
    "The weather closes in, and a decision that could have waited cannot.",
    "An old debt is called in, politely, and in front of witnesses.",
    "Two people who have avoided each other for a decade share a car.",
    "A routine inspection turns up something that was not there last year.",
    "The safest option is also the one nobody in the room can defend.",
    "A promise made in confidence is repeated to exactly the wrong person.",
    "What looked like carelessness turns out to have taken considerable planning.",
    "A stranger arrives knowing more than they should, and says less than they could.",
    "The truth is established early, and gets harder to act on with every hour.",
]


def seeded(key, size):
    return int(hashlib.sha256(key.encode()).hexdigest()[:8], 16) % size


def cast_for(title_id, count=5):
    people = []
    for index in range(count):
        seed = f"{title_id}-cast-{index}"
        people.append({
            "name": (
                FIRST_NAMES[seeded(seed + "f", len(FIRST_NAMES))]
                + " "
                + LAST_NAMES[seeded(seed + "l", len(LAST_NAMES))]
            ),
            "role": ROLES[seeded(seed + "r", len(ROLES))],
        })
    return people


def episodes_for(series_id, base_runtime):
    titles = EPISODE_TITLES[series_id]
    episodes = []
    for season in range(1, SEASONS_PER_SERIES + 1):
        for number in range(1, EPISODES_PER_SEASON + 1):
            index = (season - 1) * EPISODES_PER_SEASON + (number - 1)
            key = f"{series_id}-s{season}e{number}"
            episodes.append({
                "id": key,
                "season": season,
                "number": number,
                "title": titles[index % len(titles)],
                # A little variation either side of the series' typical length.
                "runtimeMinutes": base_runtime + (seeded(key + "r", 11) - 5),
                "synopsis": SYNOPSIS_FRAMES[seeded(key, len(SYNOPSIS_FRAMES))],
            })
    return episodes


def main():
    with open(CATALOG_PATH, encoding="utf-8") as handle:
        catalog = json.load(handle)

    details = {}
    for title in catalog["titles"]:
        entry = {"cast": cast_for(title["id"])}
        if title["type"] == "SERIES":
            entry["episodes"] = episodes_for(title["id"], title["runtimeMinutes"])
        details[title["id"]] = entry

    with open(OUT_PATH, "w", encoding="utf-8") as handle:
        json.dump({"details": details}, handle, indent=1, ensure_ascii=False)
        handle.write("\n")

    episodes = sum(len(d.get("episodes", [])) for d in details.values())
    size = os.path.getsize(OUT_PATH) / 1024
    print(f"{len(details)} titles, {episodes} episodes, {size:.0f} KB")


if __name__ == "__main__":
    main()
