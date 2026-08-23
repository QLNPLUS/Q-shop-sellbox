# QShop Sell Box

Addon for [QShop](https://www.curseforge.com/minecraft/mc-mods/q-shop).

## Short Description

An automatic selling container addon for QShop that sells stored items on a schedule or when its GUI closes and pays the configured owner.

## Full Description

QShop Sell Box is a Forge 1.20.1 addon for [QShop](https://www.curseforge.com/minecraft/mc-mods/q-shop) that provides an automatic sell box container.
Items placed inside the container can be sold on a timer or when the GUI is closed, with earnings
paid to the configured owner through QShop's currency system.

## Features

- 27-slot sell box container.
- Vanilla-style item and owner/settings tabs.
- Owner selection from every player who has joined the server.
- Owner UUID storage with online/offline status display.
- Interval selling with seconds, minutes, hours, and game days.
- Sell-on-GUI-close mode.
- Offline earnings written directly to the owner's QShop wallet through the UUID currency API.
- Configurable Action Bar and chat notifications.
- Configurable item price tooltips.
- Configurable price and currency rules.
- Generic NBT multipliers that can match multiple NBT fields on any item.
- KubeJS dynamic pricing with full item NBT access.
- Replaceable GUI, block, and container model textures.
- Optional F8 layout debugging tool, disabled by default and intended for development only.

## Requirements

- Minecraft 1.20.1
- Forge 47.x
- [QShop 1.1.0 or newer, before 2.0](https://www.curseforge.com/minecraft/mc-mods/q-shop)
- KubeJS is optional and only required for KubeJS price scripts.

## Configuration

The common configuration file is generated at:

```text
config/qshop_sellbox-common.toml
```

Example:

```toml
defaultCurrency = "coins"
priceRules = [
  "minecraft:iron_ingot|2|coins",
  "minecraft:diamond|100|coins",
  "minecraft:diamond|120|coins|{rarity:rare}"
]
nbtMultipliers = [
  "{rarity:rare}|1.2",
  "{quality:refined}|1.1",
  "minecraft:diamond|{rarity:rare,quality:refined}|1.15"
]
showPriceTooltip = true
enableLayoutDebug = false
```

`priceRules` uses `item_id|price|currency_id[|nbt]`.
`nbtMultipliers` can use `nbt|multiplier` for all items or
`item_id|nbt|multiplier` for one item. Multiple matching NBT rules are multiplied together.

## KubeJS Pricing

Use `SellBox.price` to calculate a final price from the complete item stack:

```js
SellBox.price(event => {
  const item = event.item
  let price = 1000
  let currency = 'coins'
  const nbt = item.nbt

  if (nbt && nbt.rarity == 'rare') {
    price *= 1.2
  }

  if (nbt && nbt.quality == 'refined') {
    price *= 1.1
  }

  return {
    price: price,
    currency: currency
  }
})
```

`event.item` is a script-friendly item view with `id`, `count`, `damage`, `maxDamage`, and recursively converted `nbt` fields, including nested compounds and arrays.

For troubleshooting, temporarily enable `debugDynamicPrice = true` in the common config to log the raw item NBT, the values exposed to the script, and the returned price.

The callback runs on the server. Its returned `currency` value must be a currency ID registered by
QShop. A dynamic price function takes priority over static configuration rules.

## Assets

Textures can be replaced through the resource pack or by editing the project assets:

```text
src/main/resources/assets/qshop_sellbox/textures/
```

The GUI textures are under `textures/gui/`; the sell box block textures are under `textures/block/`.

## License

All rights reserved. See [LICENSE.md](LICENSE.md).
