# CursedAddons

## Description
CursedAddons is a Minecraft Fabric mod that enhances chat functionality with keybindings, aliases, chat notifiers, and the ability to view onClick actions for rich messages.

## Features
*   **Command Keybindings**: Define custom hotkeys and key combinations (e.g., Shift+F1) to instantly send predefined chat messages or commands.
*   **Command Aliases**: Create shortcuts for frequently used commands or messages (e.g., `/s` → `/say` or `/about` → `Hello, my name is John Smith`).
*   **Click Events Preview**: When hovering over clickable text in chat, preview what actions will occur (opening URLs, running commands, copying to clipboard, etc.) before clicking.
*   **Image Hover Preview**: Preview images directly in chat by hovering over image URLs. Supports PNG, JPG, GIF, and WebP with domain whitelisting.
*   **Chat Notifications**: Set up custom notifications for specific chat messages using string matching or regex patterns. Notifications can play sounds, set window titles, or execute commands when triggered.
*   **Fake Chat Messages**: Send custom formatted messages to chat using tellraw JSON syntax, including colors, links, and interactive elements.
*   **Fixes MC-122477**: Fixes a bug on Unix systems where the key to open commands and/or chat can sometimes be doubled when pressed.

## Installation

### Prerequisites
*   Minecraft Java Edition `1.21.10` (currently supported; future versions may be supported in updates)
*   Fabric Loader `0.18.2` or newer
*   Fabric API `0.138.3+1.21.10` or newer

### Steps
1.  **Install Fabric Loader:** If you haven't already, download and install the Fabric Loader for Minecraft `1.21.10` from the [Fabric website](https://fabricmc.net/use/).
2.  **Download Fabric API:** Download the Fabric API for `1.21.10` from its [Modrinth page](https://modrinth.com/mod/fabric-api).
3.  **Download CursedAddons**
4.  **Place Mods in `mods` folder:**
    *   Locate your Minecraft installation directory.
    *   Navigate to the `mods` folder. If it doesn't exist, create one.
    *   Place the downloaded `fabric-api-*.jar` and `cursedaddons-*.jar` files into the `mods` folder.
5.  **Launch Minecraft:** Start the Minecraft launcher, select the Fabric profile, and launch the game.

## Usage

CursedAddons can be configured through its configuration screen, accessible in two ways:
- **Via ModMenu**: If you have ModMenu installed, go to Mods → CursedAddons → Config
- **Via Command**: Type `/cursedaddons` in chat to open the configuration screen

For per-feature setup instructions, see the **[Feature Guide](docs/FEATURES.md)**.

### Regex Tester
For debugging ChatNotifications regex patterns, use the [online regex tester](https://cursedatom.github.io/CursedAddons/docs/regex-tester/). It allows you to:
- Test patterns against Minecraft chat messages with § color codes
- See capture groups in the same format used by the mod ($0, $1, etc.)
- Preview how color codes will appear visually
- Convert & color codes to § automatically (like the mod does)

## Building from Source

### Prerequisites
*   Java Development Kit (JDK) `21`
*   Git

### Steps
1.  **Clone the repository:**
    ```bash
    git clone https://github.com/CursedAtom/CursedAddons.git
    cd CursedAddons
    ```
2.  **Build the project:**
    ```bash
    ./gradlew build
    ```
    The compiled mod `.jar` file will be located in the `build/libs/` directory.

## Contributing
Contributions are welcome! If you have suggestions, bug reports, or want to contribute code, please feel free to open an issue or pull request!

## License
This project is licensed under the GNU General Public License v3.0. See the `LICENSE` file for more details.

#### Big shout out to 70CentsApple for the [original concept](https://github.com/70CentsApple/ChatTools)!
