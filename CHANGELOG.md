# Changelog

All notable changes to QShop Sell Box are documented here.

Release notes are grouped by Minecraft loader when a release supports both Forge and NeoForge.

## [1.1.1] - 2026-08-31

### NeoForge 1.21.1

#### Added

- Added a shaped crafting recipe using an iron ingot, a vanilla barrel, and an iron ingot.

### Forge 1.20.1

#### Added

- Added a shaped crafting recipe using an iron ingot, a vanilla barrel, and an iron ingot.

#### Changed

- Updated the Forge build to use the available QShop Forge 1.20.1 1.2.4 artifact.

## [1.0.1] - 2026-08-23

### Added

- Added a 27-slot automatic sell box with interval selling and sell-on-GUI-close mode.
- Added vanilla-style inventory and owner/settings tabs.
- Added owner selection from players who have joined the server, with UUID-based ownership.
- Added offline earnings through QShop's UUID currency API.
- Added configurable Action Bar and chat sale notifications.
- Added configurable price tooltips with server-synchronized prices and currency display names.
- Added static price rules and generic multi-field NBT multipliers.
- Added KubeJS dynamic pricing through `SellBox.price(event => { ... })`.
- Added recursive item NBT access for KubeJS dynamic price callbacks.
- Added EMI integration to prevent hidden EMI items from intercepting the sell box settings screen.
- Added replaceable GUI, block, and container model textures.
- Added an optional F8 layout debugging tool with JSON layout persistence.
- Added an English CurseForge description with the QShop dependency link.

### Changed

- Updated the sell box block properties to match the vanilla barrel: wood map color, wood sounds,
  bass drum instrument, barrel-strength hardness, and lava ignition.
- Added the axe mining tag for the sell box block.
- Improved container item drops when the sell box is broken.
- Improved dynamic pricing diagnostics with optional NBT and result logging.

### Fixed

- Fixed dynamic KubeJS price callbacks not being able to read nested NBT values.
- Fixed dynamic prices and tooltips using currency IDs instead of registered currency display names.
- Fixed item tooltips and stack counts being rendered over the settings tab.
- Fixed middle-click item cloning inside the sell box inventory.
- Fixed sell-mode and interval settings being applied at the wrong GUI-close timing.
