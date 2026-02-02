## [1.0.2] - 2026-02-01

### What's New
```diff
+ Image Hover Preview: Preview images directly in chat by hovering over URLs
  - Supports PNG, JPG, JPEG, GIF, and WebP formats
  - Domain whitelist for security (only shows images from trusted domains)
  - GIF animation support
+ New fakechat command: Send formatted chat messages with tellraw syntax
  - Supports colors, click events, hover events, and selectors
  - Usage: `/cursedaddons fakechat <json>`
    - Example: `/cursedaddons fakechat {"text":"Hello","color":"red","click_event":{"action":"open_url","url":"https://example.com"}}`
```

### Changed
```diff
- Removed ClothConfig dependency - no longer required for installation
- Replaced with custom configuration system
  - New custom GUI screens for all config features
  - Improved field editors and list management
```

### Technical
- Complete rewrite of configuration system using native Minecraft GUI components

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
+ Implemented MC-122477 Fix (Double Chat Fix) for Unix systems
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
