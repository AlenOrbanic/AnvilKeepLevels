# AnvilKeepLevels

A small Paper plugin that makes anvils apply enchantments **at the exact level they have**, while making sure players can **never level an enchantment up by combining books**.

It is meant to be used alongside the [Enchantment Extractor](https://modrinth.com/datapack/enchantment-extractor) datapack, but it does not require it and works with any enchanted book or item.

## Why use it?

Enchantment Extractor lets players pull the enchantments off an item and put them on a book. If the item had over-leveled enchantments (for example Unbreaking 10 from commands or custom sources), the book carries those levels too.

The problem is the anvil. On servers where a plugin takes control of anvils and caps each enchantment at a maximum level (AdvancedEnchantments does this through its `levels` table), applying that book gets clamped back down. A level 10 book ends up as level 3 or 4 on the item, which defeats the point of extracting it.

Raising those caps would fix the clamping, but it creates a new problem: with higher caps, players can combine two books of the same level to get a higher one (7 + 7 = 8), and level up past what you intended.

AnvilKeepLevels solves both at once:

- A book's enchantments are applied at the book's levels, with no cap.
- If both slots have the same enchantment, vanilla rules apply: equal levels below the enchantment's max go up by one (Health 1 + 1 = 2), equal levels at the max stay at the max (Health 5 + 5 = 5), and different levels keep the higher one.

## What it does

The plugin listens to `PrepareAnvilEvent` at `HIGHEST` priority, so it runs after other anvil plugins have built their preview. Every time the anvil updates, it:

1. Does nothing if the left, right or result slot is empty, or if the right item has no vanilla enchantments (so plain repairs and renames are left alone).
2. Starts from the left item's enchantments.
3. For each enchantment on the right item:
   - If the left item already has it: equal levels below the enchantment's max level give +1, otherwise the result keeps the higher of the two levels. The max level comes from the enchantment itself, so datapack enchantments use their own `max_level`.
   - Otherwise it is applied at the right item's exact level, as long as it fits the item and (unless conflicts are allowed) does not conflict with an enchantment already chosen. Enchantments that fail those checks are skipped silently.
4. Rewrites the vanilla enchantments on the preview result with the chosen ones.

## Configuration

`plugins/AnvilKeepLevels/config.yml`:

```yaml
# If true, conflicting enchants (e.g. all four protections) can be applied together.
allow-conflicts: false
```

The config is read at startup, so changes need a server restart.

## Requirements

- Paper 1.21.11 (or a compatible 1.21.x version)
- Java 21 or newer

## Installation

1. Download the jar and place it in your server's `plugins/` folder.
2. Restart the server.

## Building

You need JDK 21+ and Maven.

```
mvn package
```

The jar is written to `target/AnvilKeepLevels-1.0.0.jar`.

If Maven 3.10 refuses to download the Paper API with a "prefixes ... NOT allowed" error, disable the repository prefix filter for the build:

```
mvn package -Daether.remoteRepositoryFilter.prefixes=false
```

## Things to know

- **Combining never goes past the enchantment's max level.** Two Sharpness 5 books stay at Sharpness 5. Books that already carry over-max levels (from commands or the extractor) keep them and are never lowered.
- **There is no level cap.** Whatever level a book carries is applied, so the limit is whoever can obtain the book.
- **The anvil cost is not recalculated.** It stays whatever vanilla or the other anvil plugin computed.
- **It does not touch** enchanting tables, loot, villager trades, grindstones, or items players already own.
- **Only the anvil preview is edited.** If another plugin changes the item again when it is taken from the result slot, the final item can differ from the preview.
- **Conflict handling:** with `allow-conflicts: false`, which of several conflicting enchantments wins depends on the order the server returns them in, so do not rely on a specific one.
- **Enchantments already on the left item are kept as they are**, even if they conflict with each other.
- No permissions or bypass: it applies to every player.

## Compatibility

Developed and tested with AdvancedEnchantments on Paper 1.21.11. Other anvil or custom-enchant plugins have not been tested. If another plugin edits the anvil result after this one, it can override the result.
