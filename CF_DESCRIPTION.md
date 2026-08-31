# QShop Sell Box

Addon for [QShop](https://www.curseforge.com/minecraft/mc-mods/q-shop).

## Short Description

An automatic selling container for QShop on Forge 1.20.1 and NeoForge 1.21.1 that sells stored items on a schedule or when its GUI closes and pays the configured owner.

## Full Description

QShop Sell Box is an addon for [QShop](https://www.curseforge.com/minecraft/mc-mods/q-shop) available for Forge 1.20.1 and NeoForge 1.21.1. It adds a dedicated automatic selling container for servers that use QShop as their currency system.

Place items in the sell box, select the player who should receive the earnings, and choose when the contents should be sold. The box can sell automatically at a configurable interval or sell its contents when the GUI is closed. Earnings are deposited into the selected owner's QShop wallet, including when that player is offline.

The mod is designed to follow the visual and interaction style of vanilla container screens.

## How It Works

1. Place a QShop Sell Box in the world.
2. Open the container and put sellable items into its 27 storage slots.
3. Open the settings tab and select an owner from the list of players who have joined the server.
4. Choose one of the selling modes:
   - **Automatic interval:** sell the stored items after the configured number of seconds, minutes, hours, or game days.
   - **Sell on GUI close:** sell the stored items when the container GUI is closed.
5. The resolved price for every item stack is calculated on the server and the earnings are deposited using the selected currency.

## Features

- A dedicated 27-slot automatic selling container.
- A vanilla-style inventory tab for storing items.
- A vanilla-style owner and settings tab.
- Owner selection from every player recorded as having joined the server.
- UUID-based ownership, so changing a player's name does not change the destination of future earnings.
- Online and offline owner status display.
- Per-container selling mode and interval settings.
- Interval units for seconds, minutes, hours, and game days.
- Sell-on-GUI-close mode for manual batch selling.
- Offline earnings stored through QShop's UUID currency API and available when the owner logs in.
- Optional Action Bar and chat notifications for sales and synchronized offline earnings.
- Optional item price tooltips.
- Server-side price calculation for consistent tooltip and transaction results.
- Configurable currencies using QShop currency IDs and registered display names.
- Static item prices and NBT-specific exact price rules.
- Generic NBT multipliers that can match multiple fields on any item.
- KubeJS dynamic pricing with access to item IDs, stack counts, durability values, complete nested NBT, and item tags.
- EMI compatibility that prevents hidden EMI entries from intercepting the sell box settings screen.
- Wood barrel-style block properties, sounds, hardness, lava ignition, and axe mining behavior.
- Container contents are dropped when the block is broken.

## Requirements

- Minecraft Forge 1.20.1 with Forge 47.x, or Minecraft NeoForge 1.21.1 with NeoForge 21.1.x
- [QShop 1.1.0 or newer, before 2.0](https://www.curseforge.com/minecraft/mc-mods/q-shop)
- KubeJS is optional. It is only required for KubeJS price scripts.
- EMI is optional. The compatibility layer is only enabled when EMI is present.

QShop Sell Box is an addon and does not provide an independent currency system. QShop must be installed for the mod to load and deposit earnings.

## Installation

1. Install Minecraft 1.20.1 with Forge 47.x.
2. Install the required [QShop](https://www.curseforge.com/minecraft/mc-mods/q-shop) version.
3. Put `qshop-sellbox-1.0.2.jar` into the server and client `mods` folders.
4. Start the game once to generate the common configuration file.

The mod should be installed on both the server and the clients that open the GUI. The server is authoritative for ownership, selling, prices, and currency deposits.

## Configuration

The Forge COMMON configuration is generated at:

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
debugDynamicPrice = false
```

### Static price rules

`priceRules` uses this format:

```text
item_id|price|currency_id[|nbt]
```

Examples:

```toml
priceRules = [
  "minecraft:iron_ingot|2|coins",
  "minecraft:diamond|100|coins",
  "minecraft:diamond|150|coins|{rarity:'rare'}"
]
```

An NBT condition is matched as a subset. The item must have the specified fields and values to use that rule.

### Generic NBT multipliers

`nbtMultipliers` supports a rule for every item or a rule for one item:

```text
nbt|multiplier
item_id|nbt|multiplier
```

Examples:

```toml
nbtMultipliers = [
  "{rarity:'rare'}|1.2",
  "{quality:'refined'}|1.1",
  "minecraft:diamond|{rarity:'rare',quality:'refined'}|1.15"
]
```

Multiple matching multipliers are applied together. For example, an item matching both `rarity:rare` and `quality:refined` receives `1.2 * 1.1`.

KubeJS dynamic pricing takes priority over static price rules when a dynamic price callback is registered.

## KubeJS Dynamic Pricing

KubeJS price callbacks run on the server and receive the complete item stack through `event.item`. The callback should return a single-item price and a QShop currency ID.

```js
SellBox.price(event => {
  const item = event.item
  const nbt = item.nbt
  let price = 1000

  if (nbt && nbt.rarity == 'rare') {
    price *= 1.2
  }

  if (item.maxDamage > 0) {
    price *= 1.1
  }

  if (item.hasTag('minecards:cards')) {
    price *= 2
  }

  return {
    price: price,
    currency: 'coins'
  }
})
```

The returned `price` is the unit price. The sell box multiplies it by the number of items removed from the stack, so the callback should not multiply the result by `event.item.count`.

### Available item properties

`event.item` provides:

- `id`: the namespaced item ID, such as `minecraft:diamond`.
- `count`: the current stack count.
- `damage`: the current durability damage value.
- `maxDamage`: the maximum durability value, or `0` for non-damageable items.
- `isDamaged`: whether the item currently has durability damage.
- `nbt`: the complete item NBT converted to JavaScript objects, arrays, strings, and numbers.
- `hasTag(tagId)`: checks an item tag using the server's current item tag registry.

The tag argument can be written with or without the `#` prefix:

```js
item.hasTag('minecards:cards')
item.hasTag('#forge:tools')
```

Nested NBT fields can be read directly:

```js
const rarity = event.item.nbt?.rarity
const quality = event.item.nbt?.quality_food?.quality
const attachment = event.item.nbt?.AttachmentEXTENDED_MAG?.tag?.AttachmentId
```

The callback can return only a number when the configured `defaultCurrency` should be used:

```js
SellBox.price(event => {
  return 100
})
```

For a custom currency, return an object:

```js
SellBox.price(event => {
  return {
    price: 100,
    currency: 'base_money'
  }
})
```

The returned currency must be registered by QShop. Tooltips, Action Bar messages, chat messages, and offline deposits use the currency's registered display name when one is available.

After changing a KubeJS price script, use KubeJS's server reload workflow. Installing or upgrading the Java mod still requires restarting the game or server.

## Notifications and Tooltips

The settings tab provides independent checkboxes for:

- Action Bar sale notifications.
- Chat sale notifications.

When the owner is offline, the earnings are written to the UUID-based wallet data. When the owner logs in, the pending earnings are deposited and the selected notification types are shown.

Price tooltips are controlled by `showPriceTooltip`. Dynamic prices are queried from the server, so the price shown for an NBT-customized item matches the price used by the sale transaction.

## 中文介绍

QShop Sell Box 是一个适用于 Minecraft 1.20.1 Forge 的 QShop 附属模组，为服务器添加可以自动出售物品的容器方块。

玩家可以将物品放入自动售货箱，选择收益归属玩家，并设置出售方式。容器支持按时间间隔自动出售，也支持在关闭容器 GUI 时立即出售。交易和价格均由服务端处理，出售收益会通过 QShop 的货币系统发放给归属玩家。

## 使用方式

1. 放置自动售货箱。
2. 打开容器，将需要出售的物品放入第一页的 27 个物品槽位。
3. 打开设置分页，在玩家列表中选择收益归属玩家。
4. 选择出售模式：
   - **自动间隔**：按照设置的秒、分钟、小时或游戏日间隔自动出售。
   - **关闭 GUI 时出售**：关闭容器界面后出售当前容器内的物品。
5. 出售后，收益会存入所选玩家对应的 QShop 货币账户。

## 主要功能

- 27 格自动售货容器。
- 物品栏分页和玩家设置分页。
- 从所有进入过服务器的玩家中选择归属玩家。
- 使用 UUID 保存归属关系，不受玩家改名影响。
- 显示玩家在线或离线状态。
- 每个容器独立保存出售模式和出售间隔。
- 支持秒、分钟、小时和游戏日作为出售间隔单位。
- 支持关闭 GUI 自动出售。
- 玩家离线时直接缓存收益，玩家登录后同步并发放增量。
- 可分别开启或关闭 Action Bar 提示和聊天栏提示。
- 可在配置中关闭物品价格 tooltip。
- 支持多个货币类型，使用 QShop 注册的货币 ID。
- 支持静态价格、NBT 精确价格和通用 NBT 倍率。
- 支持通过 KubeJS 根据物品 ID、数量、耐久、NBT 和物品 tag 动态计算价格。
- 破坏容器时会掉落容器本体和其中的物品。

## 配置文件

配置文件位于：

```text
config/qshop_sellbox-common.toml
```

示例：

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
debugDynamicPrice = false
```

`priceRules` 格式为：

```text
物品 ID|单价|货币 ID[|NBT]
```

`nbtMultipliers` 支持通用规则和指定物品规则：

```text
NBT|倍率
物品 ID|NBT|倍率
```

NBT 使用 Minecraft SNBT 格式，并且支持在同一条规则中读取多个字段。例如 `{rarity:'rare',quality:'refined'}` 可以同时匹配稀有度和品质。多个倍率同时匹配时会相乘。

如果注册了 KubeJS 动态价格函数，动态价格的优先级高于配置文件中的静态价格规则。

## KubeJS 动态价格

KubeJS 价格函数在服务端执行，`event.item` 表示当前正在出售的单件物品。返回的 `price` 是单价，Sell Box 会自动乘以实际出售的物品数量，因此脚本不需要再次乘以 `event.item.count`。

```js
SellBox.price(event => {
  const item = event.item
  const nbt = item.nbt
  let price = 1000

  if (nbt && nbt.rarity == 'rare') {
    price *= 1.2
  }

  if (item.maxDamage > 0) {
    price *= 1.1
  }

  if (item.hasTag('minecards:cards')) {
    price *= 2
  }

  return {
    price: price,
    currency: 'coins'
  }
})
```

`event.item` 支持以下属性：

- `id`：带命名空间的物品 ID，例如 `minecraft:diamond`。
- `count`：当前物品堆数量。
- `damage`：当前耐久损耗值。
- `maxDamage`：物品最大耐久，非耐久物品为 `0`。
- `isDamaged`：物品是否已经损耗耐久。
- `nbt`：完整物品 NBT，会递归转换为 JavaScript 对象、数组、字符串和数字。
- `hasTag(tagId)`：检查物品是否拥有指定物品 tag。

tag 可以使用带 `#` 或不带 `#` 的写法：

```js
item.hasTag('minecards:cards')
item.hasTag('#forge:tools')
```

也可以直接读取嵌套 NBT：

```js
const rarity = event.item.nbt?.rarity
const quality = event.item.nbt?.quality_food?.quality
const attachment = event.item.nbt?.AttachmentEXTENDED_MAG?.tag?.AttachmentId
```

如果使用配置中的默认货币，可以只返回数字：

```js
SellBox.price(event => {
  return 100
})
```

如果需要指定货币，则返回对象：

```js
SellBox.price(event => {
  return {
    price: 100,
    currency: 'base_money'
  }
})
```

返回的货币必须是 QShop 已注册的货币 ID。价格 tooltip、Action Bar、聊天栏和离线收益同步提示会优先显示货币注册时配置的显示名称。

修改 KubeJS 脚本后可以使用 KubeJS 的服务端重载功能；安装或升级 Java 模组仍然需要重启游戏或服务器。

## 提示和价格显示

设置分页中提供两个独立选项：

- 是否显示 Action Bar 出售提示。
- 是否显示聊天栏出售提示。

`showPriceTooltip` 可以控制是否在物品 tooltip 中显示出售价格。动态价格会由服务端查询，因此 tooltip 显示的价格与实际出售交易使用的价格一致。

QShop Sell Box 本身不提供独立的经济系统，必须安装 QShop 才能加载模组并发放货币。

## License

All rights reserved. See [LICENSE.md](LICENSE.md).
