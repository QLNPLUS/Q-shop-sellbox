# QShop Sell Box — 多版本开发约定

本仓库是**一个 git 仓库、两条版本分支**，每条分支各有独立 worktree。改动默认为"先落一条分支、测试通过后再迁移到另一条"。

**本文件在两条分支上内容完全相同。** 修改时必须两条分支同步提交同一内容（在一条分支提交后用 `git cherry-pick -x` 搬到另一条），否则各分支上的 AI 会读到不同约定。

## 分支矩阵

| 分支 | worktree 路径 | 加载器 | MC | JDK | Gradle | 构建插件 |
|---|---|---|---|---|---|---|
| `forge-1.20.1` | `D:\projects\q_shop_sellbox\forge-1.20.1` | Forge | 1.20.1 | **17** | 8.1.1 | ForgeGradle `[6.0.16,6.2)` |
| `neoforge-1.21.1` | `D:\projects\q_shop_sellbox\neoforge-1.21.1` | NeoForge | 1.21.1 | **21** | 8.8 | ModDevGradle 2.0.141 |

- 主工作树（持有 `.git` **目录**）是 `D:\projects\q_shop_sellbox\forge-1.20.1`；`neoforge-1.21.1` 是它的 linked worktree（`.git` 是**文件**，指向 `forge-1.20.1\.git\worktrees\neoforge-1.21.1`）。两者共享同一个对象库，因此在一个 worktree 里 commit 的提交，可直接在另一个 worktree 里 `cherry-pick`，无需 fetch。
- 唯一远程是 `origin`（`https://github.com/QLNPLUS/Q-shop-sellbox.git`）。本仓库**没有 fork 远程**，推送目标就是 `origin`。
- 两条分支均 tracking **同名**远程分支，推送用裸 `git push` 即可。`origin/HEAD` 与 GitHub 默认分支都是 **`forge-1.20.1`**（2026-09-13 由 `main` 改名而来；旧名 `main`、遗留名 `neoforge` 已不再使用）。
- 目录名、本地分支名、远程分支名三者一致。但仍建议用 `git rev-parse --abbrev-ref HEAD` 判定当前分支，不要相信目录名。
- 两条分支已漂移：分家于 `3fb0101 (Release QShop Sell Box 1.0.1)`，此后 `forge-1.20.1` 独有 13 个提交、`neoforge-1.21.1` 独有 11 个提交。

## 工作流：加新功能（默认流程，不必每次询问）

1. **只改一条分支。** 默认 `forge-1.20.1`（主分支）；用户指定了另一条就用指定的那条。
2. **改动必须先提交（commit）**，再谈迁移。
3. **触发迁移的说法**：用户说"测试通过 / 可以了 / 同步到其他版本 / 另一个版本也加上"等，即为迁移信号 —— 此时**立即**对另一条分支执行 `git cherry-pick -x <sha>`，不要等用户再次点名 `cherry-pick`。
4. **迁移前逐分支判定适用性，并明确说明结论**（见下节）。
5. **禁止把一个改动在另一条分支上手工重写一遍。**

## 迁移纪律（硬规则）

**禁止手工重写。** 跨版本搬运只能用：

```powershell
git -C D:\projects\q_shop_sellbox\<目标worktree> cherry-pick -x <源分支SHA>
```

`-x` 会在提交信息里记录来源 SHA，建立可追溯链接。手工重写的后果不是"多打一遍字"，而是产出**互不相关、无法追溯**的提交 —— 本仓库 1.0.1 之后的历史正是这样来的：同一件事在两条分支上是两个不同 SHA、两个不同父提交，提交信息里没有 `(cherry picked from commit ...)`。**从 `68ca799`（forge）/ `7992cbd`（neoforge）这一对开始才恢复溯源，此后新增改动必须保持。**

**冲突是信息，不是麻烦。** cherry-pick 冲突明确指给你"这里已与源分支分叉"，那正是需要知道的位置。**不要通过重新实现来"解决"冲突** —— 那等于丢掉这条信息。已知会冲突的位置：`build.gradle` 的依赖块与插件块、`gradle.properties`、`settings.gradle`（两条分支的平台配置本就不同）。

**缺陷修复是双向的。** 若在 `neoforge-1.21.1` 上定位并修好了 bug，要**先 cherry-pick 回 `forge-1.20.1`**，再流向其他分支；否则该修复只存在于一条分支，又出现两个真相源。

**首次迁移会有冲突。** 这些冲突是既有漂移的暴露，不是 cherry-pick 的缺陷。

## 迁移前的适用性判定

对目标分支给出结论，三类之一：

- **适用**（与加载器无关的逻辑改动，如定价规则、GUI 布局算法、KubeJS 数据模型）→ `git cherry-pick -x`
- **不适用**（平台相关：网络层、配置系统、loader metadata、数据包目录名、构建插件）→ **跳过并说明原因**，不要为了"保持一致"硬塞
- **需适配**（API 改名或包名变更，如 `net.minecraftforge.*` → `net.neoforged.neoforge.*`、`stack.getTag()` → `ItemStackData.getCustomTag(stack)`）→ **先 cherry-pick，再显式处理冲突**；绝不预先重写

## 已知平台鸿沟（真实差异，不要试图消除）

| | Forge 1.20.1 | NeoForge 1.21.1 |
|---|---|---|
| 网络 | `SimpleChannel` / `NetworkRegistry` | `CustomPacketPayload` + `PayloadRegistrar` |
| 配置 | `ForgeConfigSpec` | `ModConfigSpec` |
| 元数据 | `META-INF/mods.toml` | `META-INF/neoforge.mods.toml` |
| 物品数据 | `ItemStack.getTag()` | `com.qshop.util.ItemStackData.getCustomTag(stack)` |
| 数据包目录 | `data/<ns>/recipes`、`loot_tables` | `data/<ns>/recipe`、`loot_table` |
| 构建插件 | ForgeGradle，依赖要 `fg.deobf(...)` | ModDevGradle，依赖用裸 `files(...)` |
| 注册 | `DeferredRegister`（`net.minecraftforge.*`） | `DeferredRegister`（`net.neoforged.neoforge.*`） |
| KubeJS | `kubejs-forge 2001.6.5-build.14` + rhino/architectury/fabric-loader runtime | `kubejs-neoforge 2101.7.2-build.374` + rhino 2101.2.7-build.85 |

**物品数据这条是不对称的，最容易照抄出错**：`ItemStackData` 是 QShop 提供的 shim，但**只有 QShop 的 NeoForge 侧有**（`qshop-neoforge-1.21.1-1.7.0.jar` 内含 `com/qshop/util/ItemStackData.class`，`qshop-forge-1.20.1-1.7.0.jar` 内**没有**）。所以 NeoForge 分支写 `ItemStackData.getCustomTag(stack)`，Forge 分支只能写 `stack.getTag()`。搬运这段代码时不要直接复制，也不要为了对称去改 QShop——那属于另一个仓库的改动。

## 依赖：QShop

- 两条分支都 `compileOnly` 依赖 QShop 的构建产物，路径由 **`gradle.properties` 的 `qshop_jar`** 决定，缺失时构建立即失败并提示（不会退化成"一堆类找不到"）。
- 本地默认值指向用户级项目文件夹里的 QShop 工作树：

  ```
  forge-1.20.1    : ../../q_shop/forge-1.20.1/build/libs/qshop-forge-1.20.1-1.7.0.jar
  neoforge-1.21.1 : ../../q_shop/neoforge-1.21.1/build/libs/qshop-neoforge-1.21.1-1.7.0.jar
  ```

- **升级 QShop 只改 `qshop_jar` 这一行**，不要在 `build.gradle` 里重新硬编码文件名（历史上正是硬编码 `...-1.4.0.jar` 导致本地路径失效、CI 与 QShop 实际版本漂移）。
- CI 会先构建 Q-shop，再动态取 jar 并以 `-Pqshop_jar="../<checkout 路径>/build/libs/<jar>"` 覆盖，因此 CI 永远跟随 Q-shop 实际版本。三处必须保持一致：`gradle.properties` 默认值、`run/mods/` 里的运行时 jar、AGENTS.md 上表。
- 运行时：`run/mods/` 里放同一个 QShop jar，冒烟测试加载的就是它。

## Release Tag

格式：**`v<version>-<loader>-<mcversion>`**，前缀统一用 `v`：

```
v1.5.1-forge-1.20.1
v1.5.1-neoforge-1.21.1
```

git tag 是仓库级唯一的，而本仓库是锁步发布 —— 只打一个 `v1.5.1` 无法指认是哪个加载器/版本。

**已存在的无版本维度 tag（`v1.3.0`、`v1.4.0`、`v1.5.0`）不追溯改名**，保持现状（已有下载链接与 GitHub Release 指向它们）。它们都落在 Forge 分支的历史上；只对后续新 tag 应用上述格式。

## 构建

- **JDK 必须对上目标版本**（17 / 21），否则出现 `Unsupported class file major version`。本机路径：`C:\Program Files\Java\jdk-17`、`C:\Program Files\Java\jdk-21`。两条分支的 `gradle.properties` 都已 pin `org.gradle.java.home`，本地直接用各自 wrapper 即可。
- 两棵树**串行**构建，不要并行 —— 会争用 Gradle 缓存与内存。
- 使用各自的 Gradle wrapper（`.\gradlew.bat`），不要用系统 gradle，也不要用 `java -version` 默认的那个 JDK。
- 发布产物名：`qshop-sellbox-forge-1.20.1-<ver>.jar` / `qshop_sellbox-neoforge-1.21.1-<ver>.jar`。
- 构建前确认被引用的 QShop jar 已存在（缺失会 fail-fast 报错，不是静默跳过）。
- `pack.mcmeta` 的 `pack_format` 必须对目标 MC 的**资源包**格式：1.20.1 = **15**，1.21.1 = **34**。不要写成数据包格式（1.21.1 的数据包格式是 48 —— 声明过高会让包被判为 incompatible；本仓库 1.21.1 分支曾误用 48，已在 `1561c92` 修正）。`tools\verify-release-jars.ps1` 会核验这一项。
- **本机 `piston-meta.mojang.com` / `libraries.minecraft.net` 不可达**：Gradle 依赖解析可用 `--offline` 走缓存，但 ForgeGradle 的 `downloadMCMeta` / `downloadAssets` 不走 Gradle 离线开关，会直接连超时。见「已知遗留问题」第 4 条。

## 冒烟测试

- `.\gradlew.bat runServer`（NeoForge 用 ModDevGradle 的 `server` run，Forge 用 `minecraft` 的 `server` run），两棵树各有一份 `run/eula.txt`，**只改自己那棵树的**。
- 判定通过看日志出现 `Done (x.xxs)!`，并确认加载的是预期的 QShop 版本、无致命错误与缺依赖。
- 强杀 java 会让 Gradle 报 `> Task :runServer FAILED` 且退出码非零 —— 那是**清理副作用**，不是构建失败，报告时要分开写。
- 冒烟测试只覆盖注册期与启动，不验证功能（GUI、网络包、权限、配置项都不在其中）。

## CI

`.github/workflows/curseforge-publish.yml` **在两条分支上内容不同**，因此**不是 cherry-pick 的对象**，各自维护：

- `forge-1.20.1` 分支：双 job（`publish-forge` + `publish-neoforge`），默认 ref 分别是 `forge-1.20.1` 与 `neoforge-1.21.1`。
- `neoforge-1.21.1` 分支：单 job（仅 NeoForge），构建当前 ref。
- 两条分支的 QShop 依赖 checkout 分别是 `QLNPLUS/Q-shop@master`（Forge 侧 Q-shop 的默认分支仍叫 `master`）与 `QLNPLUS/Q-shop@neoforge-1.21.1`。

**因此分支名是 CI 的契约**：再次改名分支（`forge-1.20.1` / `neoforge-1.21.1`）必须同步更新 workflow 里的默认 ref 与 checkout 目录名，否则发布任务会去 checkout 一个不存在的 ref。workflow 里的构建/发布路径用的是 CI 内部 checkout 目录名（`sellbox-forge-1.20.1` / `sellbox-neoforge-1.21.1`），与本地 worktree 名一致只是便于对照，不影响本地构建。

## 推送

本仓库只有 `origin`，没有 fork 远程。**未经用户明确要求不要推送**，尤其不要 force-push 或推送分支改名。

## 沙箱注意

在 linked worktree（`neoforge-1.21.1`）里工作时，项目根的判定是**该 worktree 自身** —— 不会上溯到父目录 `D:\projects\q_shop_sellbox\`。因此 `AGENTS.md` 必须**每条分支各提交一份**，放在项目文件夹根（那里没有 `.git` 标记）是读不到的。

## 已知遗留问题（不要当成已完成）

1. **历史提交是手工重写的遗产**：`3fb0101` 之后的 13 / 11 个提交在两条分支间没有 patch 级对应关系，跨分支比对只能靠内容核对，不能靠 `git cherry`。
2. **两条分支功能面尚未逐条核对**：抽查（如"归属声明"功能）未发现缺失，但未做完整清单比对。搬运新功能前先确认目标分支是否已有该功能的平行实现。
3. **共享率已超抽 `common/` 的阈值**：同名 Java 文件 27/27，其中字节相同 8、差异 ≤3 行 4、4–15 行 4 → `(8+4+4)/27 = 59% > 50%`。按 skill 的量化判据，本项目**应当**抽 `common/` 共享模块（模型 B：聚合 `main` 分支）。这是一次结构性重构（需先把平台调用点收敛到 shim 文件，物品数据这条已经被 QShop 的 `ItemStackData` 部分解决了），属于独立任务，未在本轮整理中执行。
4. **Forge 的 `runServer` 加载不了 QShop 的 Forge 生产 jar（既有问题，与 QShop 版本无关）**：`run\mods\` 里放 QShop 的 forge 产物（1.4.0、1.7.0 均实测）会在 `common_setup` 抛 `NoSuchMethodError`，一次一个方法（1.7.0 是 `SoundEvent.m_262824_`，1.4.0 是 `Commands.m_82127_`）。原因是 Forge 生产 jar 用 SRG 名、dev 运行时按官方名解析，而这些方法的 SRG id 随 Forge 版本重新分配。**NeoForge 侧不受影响**（其生产 jar 不做 SRG 重映射），所以 `neoforge-1.21.1` 的服务端冒烟能通过。要让 Forge 冒烟也通过，需要 QShop 提供未 reobf 的 dev 产物，或把 QShop 的 classes 直接放进 run classpath。
5. **本机 Mojang 主机不可达**：`piston-meta.mojang.com` / `libraries.minecraft.net` 连不通 → ForgeGradle 的 `downloadMCMeta`、`downloadAssets`、`extractNatives` 会失败或挂起，且 `--offline` 对它们无效（它们不走 Gradle 的离线开关）。绕过方式：`-x downloadMCMeta -x downloadAssets`，并先把 `%USERPROFILE%\.gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1\version.json` 复制到 `build\downloadMCMeta\version.json` 满足 `extractNatives` 的输入校验。NeoForge 侧用 neoformruntime 缓存，`--offline` 即可跑通。
