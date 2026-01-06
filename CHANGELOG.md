## [1.0.2.26-snapshot] - 2026-01-03

### What's New
```diff
+ Updated to Minecraft 26.1-snapshot-1
+ Updated to Java 25
+ Updated Fabric Loader to 0.18.4
+ Updated Fabric API to 0.140.2+26.1 (Cannot bump until ModMenu updates, ClientCommandManager -> ClientCommands for >=0.140.3+26.1)
+ Updated ModMenu to 18.0.0-alpha.3
- Removed Cloth Config dependency and replaced with custom config system
```

## [1.0.1] - 2025-12-30

### What's New
```diff
+ Enhanced ChatNotifications with intelligent text matching:
  - Auto-detects plain text vs legacy text (color codes) based on unescaped & characters in regex patterns
  - Added \$ escape sequences for literal dollar signs in replacement text
+ New web-based regex tester tool (regex-tester/index.html):
  - Test patterns against Minecraft chat messages with § color codes
  - Visual color code preview
  - Capture group display with replacement simulation
  - Handles all escape sequences like the mod
+ Updated README with escape sequence guide
```

## [1.0.0] - 2025-12-28

### What's New
```diff
+ Chat notifications feature with regex/string matching, sound playback, title setting, and command execution
+ Fixed ClothConfig [Issue #300](https://github.com/shedaniel/cloth-config/issues/300) with a mixin
+ First release
```

## [1.0.0-beta.3] - 2025-12-20

### Added
```diff
+ Implemented MC-122477 Fix (Double Chat Fix) for Linux systems
```
### Changed
```diff
- Removed `//` to `/` command alias handling
```

## [1.0.0-beta.2] - 2025-12-19

### Fixed
```diff
- Removed artifacts from project refactor
+ Fixed config file path
```

### Technical
- Fabric mod for Minecraft 1.21.10
- Uses Cloth Config for settings (require)
- Compatible with ModMenu

## [1.0.0-beta.1] - 2025-12-19

### Added
```diff
+ Initial beta release of CursedAddons
+ Chat keybinding macros feature
+ Command aliases functionality
+ Enhanced chat preview features
+ View onClick action for chat messages
+ ModMenu integration with configuration screen
```
