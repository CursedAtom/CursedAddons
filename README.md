# CursedAddons

## Description
CursedAddons is a Minecraft Fabric mod that enhances chat functionality with keybindings, aliases, chat notifiers, and the ability to view onClick actions for rich messages.

## Features
*   **Command Keybindings**: Define custom hotkeys and key combinations (e.g., Shift+F1) to instantly send predefined chat messages or commands.
*   **Command Aliases**: Create shortcuts for frequently used commands or messages (e.g., `/s` → `/say` or `/about` → `Hello, my name is John Smith`).
*   **Click Events Preview**: When hovering over clickable text in chat, preview what actions will occur (opening URLs, running commands, copying to clipboard, etc.) before clicking.
*   **Chat Notifications**: Set up custom notifications for specific chat messages using string matching or regex patterns. Notifications can play sounds, set window titles, or execute commands when triggered.
*   **Fixes MC-122477**: Fixes a bug on Unix systems where the key to open commands and/or chat can sometimes be doubled when pressed.

## Installation

### Prerequisites
*   Minecraft Java Edition `26.1-snapshot-1` (currently supported; future versions may be supported in updates)
*   Fabric Loader `0.18.4` or newer
*   Fabric API `0.140.2+26.1`
*   Java 25

### Steps
1.  **Install Fabric Loader:** If you haven't already, download and install the Fabric Loader for Minecraft `26.1-snapshot-1` from the [Fabric website](https://fabricmc.net/use/).
2.  **Download Fabric API:** Download the Fabric API for `26.1-snapshot-1` from its [Modrinth page](https://modrinth.com/mod/fabric-api).
3.  **Download CursedAddons**
4.  **Place Mods in `mods` folder:**
    *   Locate your Minecraft installation directory.
    *   Navigate to the `mods` folder. If it doesn't exist, create one.
    *   Place the downloaded `fabric-api-*.jar` and `cursedaddons-*.jar` files into the `mods` folder.
5.  **Launch Minecraft:** Start the Minecraft launcher, select the Fabric profile, and launch the game.

## Usage

### Configuration
CursedAddons can be configured through its configuration screen, accessible in two ways:
- **Via ModMenu**: If you have ModMenu installed, go to Mods → CursedAddons → Config
- **Via Command**: Type `/cursedaddons` in chat to open the configuration screen

### Features Configuration

#### Command Keybindings
This feature allows you to create macros that send chat messages or commands when you press specific key combinations.

1. In the config screen, ensure "Enable Command Keybindings" is checked
2. Click "Command Keybindings" to manage your macros
3. Click "New Rule" to add a macro
4. Configure each macro:
   - **Key**: Choose the main key (e.g., F1, F2, Numpad 0, X, etc.)
   - **Combination Key**: Select a modifier (Shift, Ctrl, Alt) or "NONE" for single key press
   - **Command**: Enter the chat message or command to send (e.g., "/say Hello World!" or "Hello everyone!")
   - **Enabled**: Check to activate the macro
5. Save and exit the config screen

Example: Set Shift+F1 to send "/say Hello everyone!"

#### Command Aliases
This feature allows you to create shortcuts for commands or messages, expanding them when you type in chat.

1. In the config screen, ensure "Enable Command Aliases" is checked
2. Click "Command Aliases" to manage your aliases
3. Click "New Alias" to add an alias
4. Configure each alias:
   - **Alias**: The shortcut you type (e.g., `/s` )
   - **Command**: What it expands to (e.g., `/say`)
   - **Enabled**: Check to activate the alias
5. Save and exit the config screen

Examples:
- `/s hello` → `/say hello` (command expansion)
- `/about` → `this is a message about something` (text message)

#### Click Events Preview
This feature shows a preview of what will happen when you click on interactive text in chat.

1. In the config screen, check "Enable Click Events Preview"
2. When hovering over clickable text in chat, you'll see a tooltip showing:
   - URLs that will open
   - Commands that will run
   - Text that will be suggested or copied
   - Other interactive actions

This helps you verify links and commands before clicking them.

#### Chat Notifications
This feature allows you to set up automatic notifications for specific chat messages using string matching or regex patterns.

1. In the config screen, ensure "Enable Chat Notifications" is checked
2. Click "Chat Notifications" to manage your notification rules
3. Click "New Notification" to add a rule
4. Configure each notification:
   - **Pattern**: The text or regex pattern to match in chat messages
   - **Use Regex**: Check if the pattern should be treated as a regular expression
   - **Play Sound**: Check to play a sound when matched, and enter the sound resource location
   - **Set Title**: Check to set the window title when matched, and enter the title text
   - **Send Command**: Check to send a command when matched, and enter the command to send
   - **Enabled**: Check to activate the notification
5. Save and exit the config screen

**Escape Sequences:**
- `\&` - Literal ampersand (&) in patterns and replacement text
- `\$` - Literal dollar sign ($) in replacement text (commands, titles, sounds)
- `&` followed by color codes (0-9,a-f,k-o,r) - Minecraft color formatting in patterns

**Examples:**
- Match "Welcome" and play a notification sound
- Use regex like `Player (.+) joined` to capture player names and set title to `Player $1 joined!`
- Send `/tell $1 Welcome! You earned \$100!` when someone joins (literal $ in message)
- Use regex like "&6([^ ]+) was banned" to capture something like "§6Player123 was banned"

### Regex Tester
For debugging ChatNotifications regex patterns, you can use the [regex tester](https://cursedatom.github.io/CursedAddons/docs/regex-tester/). This tool allows you to:
- Test regex patterns against Minecraft chat messages with § color codes
- See capture groups in the same format used by the mod ($0, $1, etc.)
- Preview how color codes will appear visually
- Convert & color codes to § automatically (like the mod does)

## Building from Source

### Prerequisites
*   Java Development Kit (JDK) `25`
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