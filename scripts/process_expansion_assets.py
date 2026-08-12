from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "docs" / "assets"
OUTPUT = ROOT / "catitems-addon" / "assets" / "catdrugs" / "textures" / "item"
VILLAGER_OUTPUT = (
    ROOT
    / "catitems-addon"
    / "assets"
    / "minecraft"
    / "textures"
    / "entity"
    / "villager"
    / "profession"
    / "nitwit.png"
)

ITEM_IDS = (
    "mescaline",
    "ayahuasca",
    "kratom",
    "kava",
    "poppers",
    "ether",
    "lean",
    "morphine",
    "codeine",
    "diazepam",
    "alprazolam",
    "zolpidem",
    "two_cb",
    "two_ci",
    "cathinone",
    "mephedrone",
    "methylphenidate",
    "clonazepam",
    "barbiturates",
    "dxm",
)

EXPANSION_050_ATLASES = (
    (
        "catdrugs-expansion-050-atlas-a-source.png",
        (
            "cannabis_oil", "edible", "shatter", "wax", "peyote",
            "ibogaine", "psilocybin", "amanita", "morning_glory", "fly_agaric",
            "ecstasy", "molly", "bath_salts", "flakka", "alpha_pvp",
            "mdpv", "bk_mdma", "dexamphetamine", "lisdexamfetamine", "modafinil",
            "caffeine", "nicotine", "alcohol", "absinthe", "moonshine",
        ),
    ),
    (
        "catdrugs-expansion-050-atlas-b-source.png",
        (
            "chloroform", "butane", "glue", "aerosol", "amyl_nitrite",
            "hydrocodone", "hydromorphone", "methadone", "buprenorphine", "tramadol",
            "tapentadol", "carfentanil", "sufentanil", "lorazepam", "temazepam",
            "midazolam", "phenobarbital", "quaaludes", "pregabalin", "gabapentin",
            "desomorphine", "etizolam", "phenibut", "coca_leaf", "opium",
        ),
    ),
)

UTENSIL_ATLAS = (
    "catdrugs-utensils-atlas-source.png",
    (
        "joint", "rolling_paper", "lighter",
        "herbal_pipe", "filter_straw", "medicine_cup",
        "ritual_bowl", "sterile_applicator", "vaporizer",
    ),
)


def remove_connected_black(image: Image.Image, threshold: int = 28) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    queue: deque[tuple[int, int]] = deque()
    visited: set[tuple[int, int]] = set()

    for x in range(width):
        queue.append((x, 0))
        queue.append((x, height - 1))
    for y in range(height):
        queue.append((0, y))
        queue.append((width - 1, y))

    while queue:
        x, y = queue.popleft()
        if (x, y) in visited:
            continue
        visited.add((x, y))
        red, green, blue, alpha = pixels[x, y]
        if alpha == 0 or max(red, green, blue) <= threshold:
            pixels[x, y] = (red, green, blue, 0)
            if x > 0:
                queue.append((x - 1, y))
            if x + 1 < width:
                queue.append((x + 1, y))
            if y > 0:
                queue.append((x, y - 1))
            if y + 1 < height:
                queue.append((x, y + 1))
    return rgba


def fit_icon(image: Image.Image, canvas_size: int = 64, icon_size: int = 54) -> Image.Image:
    alpha = image.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError("Generated atlas cell is empty")
    icon = image.crop(bounds)
    ratio = min(icon_size / icon.width, icon_size / icon.height)
    width = max(1, round(icon.width * ratio))
    height = max(1, round(icon.height * ratio))
    icon = icon.resize((width, height), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    canvas.alpha_composite(icon, ((canvas_size - width) // 2, (canvas_size - height) // 2))
    return canvas


def process_item_atlas() -> None:
    atlas = Image.open(ASSETS / "catdrugs-expansion-item-atlas-source.png").convert("RGBA")
    cell_width = atlas.width // 5
    cell_height = atlas.height // 4
    OUTPUT.mkdir(parents=True, exist_ok=True)

    for index, item_id in enumerate(ITEM_IDS):
        column = index % 5
        row = index // 5
        left = column * cell_width + 7
        top = row * cell_height + 7
        right = (column + 1) * cell_width - 7
        bottom = (row + 1) * cell_height - 7
        cell = atlas.crop((left, top, right, bottom))
        fit_icon(remove_connected_black(cell)).save(OUTPUT / f"{item_id}.png")


def process_square_atlas(filename: str, item_ids: tuple[str, ...], columns: int = 5) -> None:
    atlas = Image.open(ASSETS / filename).convert("RGBA")
    rows = (len(item_ids) + columns - 1) // columns
    cell_width = atlas.width // columns
    cell_height = atlas.height // rows
    OUTPUT.mkdir(parents=True, exist_ok=True)

    for index, item_id in enumerate(item_ids):
        column = index % columns
        row = index // columns
        inset = max(5, min(cell_width, cell_height) // 40)
        cell = atlas.crop((
            column * cell_width + inset,
            row * cell_height + inset,
            (column + 1) * cell_width - inset,
            (row + 1) * cell_height - inset,
        ))
        fit_icon(remove_connected_black(cell)).save(OUTPUT / f"{item_id}.png")


def process_villager_overlay() -> None:
    source = Image.open(ASSETS / "apothecary-villager-overlay-v2-alpha.png").convert("RGBA")
    overlay = source.resize((64, 64), Image.Resampling.LANCZOS)
    VILLAGER_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    overlay.save(VILLAGER_OUTPUT)


if __name__ == "__main__":
    process_item_atlas()
    for atlas_filename, atlas_item_ids in EXPANSION_050_ATLASES:
        process_square_atlas(atlas_filename, atlas_item_ids)
    process_square_atlas(UTENSIL_ATLAS[0], UTENSIL_ATLAS[1], columns=3)
    process_villager_overlay()
    total = len(ITEM_IDS) + sum(len(item_ids) for _, item_ids in EXPANSION_050_ATLASES) + len(UTENSIL_ATLAS[1])
    print(f"Wrote {total} expansion item textures and {VILLAGER_OUTPUT}")
