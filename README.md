# QShop Sell Box

QShop 1.1.0 的 Forge 1.20.1 附属模组。`qshop-sellbox-forge-1.20.1-1.1.1.jar` 需要和 QShop 一起放入 `mods`。

## 功能

- 自动售货箱有 27 个物品槽，第一页用于放入待售物品。
- 第二页显示归属玩家头像、名字和 UUID，可从所有登录过服务器的玩家中选择归属。
- 出售模式和间隔保存在每个容器中：`自动间隔` 支持输入数值并切换秒、分、小时、游戏日，`关闭 GUI 时出售` 在容器 GUI 关闭时出售。
- 归属使用 UUID 保存，并记录所有登录过服务器的玩家 UUID 和最新名字。
- 归属者离线时收益通过 QShop 的 UUID 货币 API 直接写入对应玩家的钱包数据，登录后由 QShop 正常读取并同步。
- 物品 tooltip 会显示服务端同步的售价和货币 ID。
- GUI 使用可替换的 PNG 材质：`src/main/resources/assets/qshop_sellbox/textures/gui/`。其中 `tabs.png` 按原版创造模式图集布局生成，单个页签为 `26x32`，状态行依次为顶部未选中、顶部选中、底部未选中、底部选中；`input.png` 和 `input_focus.png` 分别是输入框普通与激活材质。

## 配置

这是 Forge `COMMON` 配置，首次启动后生成：

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
```

`priceRules` 格式为 `物品 ID|价格|货币 ID[|NBT]`。`nbtMultipliers` 的 `NBT|倍率` 是通用规则，会匹配所有带有这组 NBT 的物品；`物品 ID|NBT|倍率` 只匹配指定物品。NBT 支持一条规则中写多个字段，使用子集匹配。多个匹配规则会相乘，例如 `rarity:rare` 和 `quality:refined` 同时命中时会得到 `1.2 * 1.1`。

物品 NBT 的键和值使用 Minecraft SNBT 写法，例如 `{rarity:'rare',quality:'refined'}`。配置中的 `|` 是字段分隔符，因此不要在 NBT 值中使用未转义的 `|`。

## KubeJS 价格

推荐使用一个通用价格函数。函数在服务端执行，`event.item` 是当前待出售的物品，函数返回值就是最终单价；不需要为每种 NBT 单独注册静态规则：

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

`event.item` 是脚本友好的物品视图，`event.item.id`、`event.item.count`、`event.item.damage`、`event.item.maxDamage` 和 `event.item.nbt` 可直接读取；NBT 会递归转换为 JavaScript 对象和数组，因此可以按任意多个字段组合计算价格。也可以使用 `SellBox.priceFunction(callback)` 或 `SellBox.dynamicPrice(callback)`，它们与上面的写法相同。需要指定回调使用的货币时，可传第二个参数，例如 `SellBox.priceFunction(callback, 'coins')`；省略时使用配置中的 `defaultCurrency`。

排查价格脚本时，可以在 `config/qshop_sellbox-common.toml` 临时开启 `debugDynamicPrice = true`，日志会记录原始物品 NBT、脚本看到的 `rarity`/`Damage` 和最终返回值。调试完成后建议关闭。

回调应返回 `{ price, currency }`，其中 `price` 是最终单价，`currency` 是 QShop 使用的货币 ID。为兼容简单写法，也可以只返回数字，此时使用配置中的 `defaultCurrency`。

动态函数的结果在服务端计算，容器出售时直接使用该结果。客户端显示价格时会把物品和完整 NBT 发回服务端查询，再显示服务端返回的价格，不会在客户端执行 KubeJS 脚本。动态函数存在时，它是唯一价格来源；没有动态函数时才使用配置文件的 `priceRules` 和 `nbtMultipliers`。

修改脚本后使用 KubeJS 的服务器重载流程，Java 插件首次安装或升级仍需要重启。
