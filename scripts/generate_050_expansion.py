from __future__ import annotations

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DRUGS_FILE = ROOT / "src" / "main" / "resources" / "drugs.yml"
START = "  # BEGIN GENERATED 0.5.0 EXPANSION"
END = "  # END GENERATED 0.5.0 EXPANSION"

# ID, display name, category, fallback material, color, visual description.
ITEMS = (
    ("cannabis_oil", "Cannabis Oil", "cannabis", "HONEY_BOTTLE", "dark_green", "A vial of emerald fantasy herb oil."),
    ("edible", "Herbal Edible", "cannabis", "COOKIE", "green", "A glazed cookie infused with fantasy herbs."),
    ("shatter", "Shatter", "cannabis", "HONEYCOMB", "gold", "A brittle amber alchemical shard."),
    ("wax", "Herbal Wax", "cannabis", "HONEYCOMB", "yellow", "A small tin of aromatic fantasy wax."),
    ("peyote", "Peyote", "psychedelic", "CACTUS", "green", "A star-shaped desert cactus button."),
    ("ibogaine", "Ibogaine", "psychedelic", "BROWN_DYE", "gold", "A carved bottle of amber root extract."),
    ("psilocybin", "Psilocybin", "psychedelic", "RED_MUSHROOM", "light_purple", "A glowing violet mushroom capsule."),
    ("amanita", "Amanita", "psychedelic", "RED_MUSHROOM", "red", "A bright spotted mushroom specimen."),
    ("morning_glory", "Morning Glory", "psychedelic", "BLUE_ORCHID", "blue", "A pouch of enchanted blue flower seeds."),
    ("fly_agaric", "Fly Agaric", "psychedelic", "BROWN_MUSHROOM", "dark_red", "A dried crimson woodland mushroom."),
    ("ecstasy", "Ecstasy", "stimulant", "MAGENTA_DYE", "light_purple", "A pressed neon fantasy tablet."),
    ("molly", "Molly", "stimulant", "PINK_DYE", "aqua", "A tiny pouch of shimmering crystals."),
    ("bath_salts", "Bath Salts", "stimulant", "SUGAR", "white", "A packet of iridescent alchemical salt."),
    ("flakka", "Flakka", "stimulant", "QUARTZ", "yellow", "Jagged pale-yellow fantasy crystals."),
    ("alpha_pvp", "Alpha-PVP", "stimulant", "PRISMARINE_CRYSTALS", "dark_aqua", "A sealed vial of teal crystalline flakes."),
    ("mdpv", "MDPV", "stimulant", "GLOWSTONE_DUST", "gold", "A dark bottle of glowing fantasy powder."),
    ("bk_mdma", "BK-MDMA", "stimulant", "PURPLE_DYE", "light_purple", "A purple alchemical capsule."),
    ("dexamphetamine", "Dexamphetamine", "stimulant", "ORANGE_DYE", "gold", "A compact orange tablet bottle."),
    ("lisdexamfetamine", "Lisdexamfetamine", "stimulant", "CYAN_DYE", "aqua", "A cyan vial of focus capsules."),
    ("modafinil", "Modafinil", "stimulant", "LIGHT_BLUE_DYE", "blue", "A silver-blue wakefulness tablet."),
    ("caffeine", "Caffeine", "stimulant", "COCOA_BEANS", "gold", "A steaming dark-roast fantasy tonic."),
    ("nicotine", "Nicotine", "stimulant", "DRIED_KELP", "dark_green", "A small green leaf pouch."),
    ("alcohol", "Alcohol", "sedative", "POTION", "gold", "A plain bottle of strong village brew."),
    ("absinthe", "Absinthe", "sedative", "LIME_DYE", "green", "An ornate bottle of emerald spirit."),
    ("moonshine", "Moonshine", "sedative", "GLASS_BOTTLE", "white", "A corked jar of clear cave brew."),
    ("chloroform", "Chloroform", "inhalant", "GLASS_BOTTLE", "aqua", "A dark glass bottle with a pale label."),
    ("butane", "Butane", "inhalant", "FIRE_CHARGE", "red", "A red fantasy pressure canister."),
    ("glue", "Glue", "inhalant", "SLIME_BALL", "yellow", "A dented tube of enchanted adhesive."),
    ("aerosol", "Aerosol", "inhalant", "IRON_NUGGET", "gray", "A bright spray can with warning stripes."),
    ("amyl_nitrite", "Amyl Nitrite", "inhalant", "PINK_DYE", "light_purple", "A tiny purple glass ampoule."),
    ("hydrocodone", "Hydrocodone", "opioid", "WHITE_DYE", "white", "A white fantasy medicine bottle."),
    ("hydromorphone", "Hydromorphone", "opioid", "LIGHT_BLUE_DYE", "aqua", "A blue vial with a silver stopper."),
    ("methadone", "Methadone", "opioid", "ORANGE_DYE", "gold", "An amber bottle of fantasy syrup."),
    ("buprenorphine", "Buprenorphine", "opioid", "CYAN_DYE", "dark_aqua", "A teal tablet blister pack."),
    ("tramadol", "Tramadol", "opioid", "GREEN_DYE", "green", "A green jar of round fantasy tablets."),
    ("tapentadol", "Tapentadol", "opioid", "MAGENTA_DYE", "light_purple", "A magenta capsule bottle."),
    ("carfentanil", "Carfentanil", "opioid", "BLACK_DYE", "dark_red", "A black hazard vial with a red seal."),
    ("sufentanil", "Sufentanil", "opioid", "GRAY_DYE", "gray", "A charcoal vial with a bright stopper."),
    ("lorazepam", "Lorazepam", "sedative", "LIGHT_GRAY_DYE", "white", "A pale bottle of calm tablets."),
    ("temazepam", "Temazepam", "sedative", "YELLOW_DYE", "yellow", "A golden jar marked with a moon."),
    ("midazolam", "Midazolam", "sedative", "BLUE_DYE", "blue", "A dark-blue fantasy ampoule."),
    ("phenobarbital", "Phenobarbital", "sedative", "RED_DYE", "dark_red", "A vintage red apothecary bottle."),
    ("quaaludes", "Quaaludes", "sedative", "PINK_DYE", "light_purple", "A retro jar of speckled tablets."),
    ("pregabalin", "Pregabalin", "sedative", "PURPLE_DYE", "light_purple", "A violet bottle of fantasy capsules."),
    ("gabapentin", "Gabapentin", "sedative", "CYAN_DYE", "aqua", "A turquoise medicine pouch."),
    ("desomorphine", "Desomorphine", "opioid", "BROWN_DYE", "dark_red", "A weathered brown hazard bottle."),
    ("etizolam", "Etizolam", "sedative", "BLUE_DYE", "blue", "A cobalt blister strip."),
    ("phenibut", "Phenibut", "sedative", "QUARTZ", "white", "A white jar of crystalline fantasy powder."),
    ("coca_leaf", "Coca Leaf", "stimulant", "AZALEA_LEAVES", "green", "A tied bundle of glossy green leaves."),
    ("opium", "Opium", "opioid", "BLACK_DYE", "dark_purple", "A dark resin ball in a tiny bronze case."),
)

EFFECTS = {
    "cannabis": (
        ("slow_falling", 34, 0), ("resistance", 28, 0), ("slowness", 22, 0),
        ("hunger", 28, 1),
    ),
    "psychedelic": (
        ("night_vision", 38, 0), ("nausea", 28, 1), ("slow_falling", 25, 0),
        ("weakness", 30, 1),
    ),
    "stimulant": (
        ("speed", 32, 2), ("haste", 30, 1),
        ("hunger", 38, 2), ("weakness", 28, 1),
    ),
    "sedative": (
        ("resistance", 38, 1), ("slowness", 40, 2),
        ("weakness", 42, 2), ("mining_fatigue", 30, 1),
    ),
    "inhalant": (
        ("slow_falling", 20, 1), ("nausea", 28, 2),
        ("blindness", 8, 0), ("weakness", 34, 2),
    ),
    "opioid": (
        ("resistance", 42, 2), ("absorption", 32, 1), ("slowness", 40, 2),
        ("weakness", 48, 2), ("hunger", 38, 2),
    ),
}

DOSE = {"cannabis": 3, "psychedelic": 4, "stimulant": 4, "sedative": 5, "inhalant": 5, "opioid": 6}
COOLDOWN = {"cannabis": 9, "psychedelic": 10, "stimulant": 8, "sedative": 12, "inhalant": 11, "opioid": 13}


def effect_line(effect: tuple[str, int, int]) -> str:
    effect_id, seconds, amplifier = effect
    return f'        - {{type: "{effect_id}", duration-seconds: {seconds}, amplifier: {amplifier}, particles: true}}'


def render_entry(item: tuple[str, str, str, str, str, str]) -> str:
    item_id, name, category, material, color, description = item
    immediate_and_after = EFFECTS[category]
    split = 3 if category in {"cannabis", "psychedelic", "opioid"} else 2
    immediate = "\n".join(effect_line(effect) for effect in immediate_and_after[:split])
    after = "\n".join(effect_line(effect) for effect in immediate_and_after[split:])
    return f'''  {item_id}:
    enabled: true
    consumable: true
    category: "{category}"
    display-name: "<{color}><bold>{name}</bold></{color}>"
    lore:
      - "<gray>{description}</gray>"
      - "<dark_gray>Fictional Minecraft gameplay item</dark_gray>"
    item: {{fallback-material: "{material}", catitems-id: "catdrugs:{item_id}"}}
    consumption: {{cooldown-seconds: {COOLDOWN[category]}, dose-points: {DOSE[category]}}}
    effects:
      after-delay-ticks: 480
      immediate:
{immediate}
      after:
{after}
'''


def main() -> None:
    if len(ITEMS) != 50 or len({item[0] for item in ITEMS}) != 50:
        raise ValueError("The expansion must contain exactly 50 unique IDs")
    text = DRUGS_FILE.read_text(encoding="utf-8")
    if START in text:
        before, remainder = text.split(START, 1)
        _, after = remainder.split(END, 1)
        text = before + after.lstrip("\r\n")
    marker = "  hemp_seed:"
    if marker not in text:
        raise ValueError("Could not find the ingredient insertion point")
    generated = START + "\n\n" + "\n".join(render_entry(item) for item in ITEMS) + "\n  " + END.strip() + "\n\n"
    DRUGS_FILE.write_text(text.replace(marker, generated + marker, 1), encoding="utf-8", newline="\n")
    print(f"Wrote {len(ITEMS)} generated expansion entries to {DRUGS_FILE}")


if __name__ == "__main__":
    main()
