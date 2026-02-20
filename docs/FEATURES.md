# Feature Guide

CursedAddons can be configured through its configuration screen, accessible in two ways:
- **Via ModMenu**: If you have ModMenu installed, go to Mods → CursedAddons → Config
- **Via Command**: Type `/cursedaddons` in chat to open the configuration screen

---

<details>
<summary><strong>Command Keybindings</strong></summary>

Create macros that send chat messages or commands when you press specific key combinations.

1. In the config screen, ensure **Enable Command Keybindings** is checked
2. Click **Command Keybindings** to manage your macros
3. Click **Add** to create a new macro and configure it:
   - **Key**: The main key (e.g., F1, F2, Numpad 0, X)
   - **Combination Key**: A modifier (Shift, Ctrl, Alt) or "None" for a single key press
   - **Command**: The chat message or command to send (e.g., `/say Hello World!`)
   - **Enabled**: Check to activate the macro

Example: Set Shift+F1 to send `/say Hello everyone!`

</details>

---

<details>
<summary><strong>Command Aliases</strong></summary>

Create shortcuts for commands or messages that expand when typed in chat.

1. In the config screen, ensure **Enable Command Aliases** is checked
2. Click **Command Aliases** to manage your aliases
3. Click **Add** to create a new alias and configure it:
   - **Alias**: The shortcut you type (e.g., `/s`)
   - **Command**: What it expands to (e.g., `/say`)
   - **Enabled**: Check to activate the alias

Examples:
- `/s hello` → `/say hello` (command expansion)
- `/about` → `this is a message about something` (text message)

</details>

---

<details>
<summary><strong>Click Events Preview</strong></summary>

Shows a preview of what will happen when you click on interactive text in chat.

1. In the config screen, check **Enable Click Events Preview**
2. When hovering over clickable text in chat, a tooltip will show:
   - URLs that will open
   - Commands that will run
   - Text that will be suggested or copied
   - Other interactive actions

This helps you verify links and commands before clicking them.

</details>

---

<details>
<summary><strong>Image Hover Preview</strong></summary>

Displays image previews when hovering over image URLs in chat.

1. In the config screen, check **Enable Image Hover Preview**
2. Click **Open Domain Whitelist** to manage allowed domains
3. Add trusted domains (e.g., `imgur.com`, `i.redd.it`)
4. Hover over any image URL from a whitelisted domain to see a preview
5. Supports PNG, JPG, JPEG, GIF (animated), and WebP formats
6. Hold **Shift** while hovering to view the image at full size

Links that redirect to images (e.g., Imgur, Tenor) are resolved automatically if the domain has **Resolve Embed** enabled in the whitelist.

> **Security Note**: Only images from whitelisted domains will be previewed, helping prevent IP leaks from malicious image links.

</details>

---

<details>
<summary><strong>Chat Notifications</strong></summary>

Set up automatic notifications for specific chat messages using string matching or regex patterns.

1. In the config screen, ensure **Enable Chat Notifications** is checked
2. Click **Chat Notifications** to manage your rules
3. Click **Add** to create a new rule and configure it:
   - **Pattern**: The text or regex pattern to match
   - **Use Regex**: Check if the pattern is a regular expression
   - **Play Sound**: Check to play a sound on match, and enter the sound resource location
   - **Set Title**: Check to set the window title on match, and enter the title text
   - **Send Command**: Check to send a command on match, and enter the command
   - **Enabled**: Check to activate the rule

**Escape Sequences:**
| Sequence | Meaning |
|---|---|
| `\&` | Literal `&` in patterns and replacement text |
| `\$` | Literal `$` in replacement text (commands, titles, sounds) |
| `&0`–`&f`, `&k`–`&r` | Minecraft color/format codes in patterns |

**Examples:**
- Match `Welcome` and play a notification sound
- Regex `Player (.+) joined` → set title to `Player $1 joined!`
- Regex `Player (.+) joined` → send `/tell $1 Welcome! You earned \$100!` (literal `$` in message)
- Regex `&6([^ ]+) was banned` → matches `§6Player123 was banned`

For debugging regex patterns, use the [online regex tester](https://cursedatom.github.io/CursedAddons/docs/regex-tester/).

</details>

---

<details>
<summary><strong>Fake Chat Messages</strong></summary>

Send custom formatted messages to chat using Minecraft's tellraw JSON syntax.

**Usage:** `/cursedaddons fakechat <json>`

**Examples:**
```
/cursedaddons fakechat {"text":"Hello World","color":"red"}
```
```
/cursedaddons fakechat ["",{"text":"[Click Here]","click_event":{"action":"open_url","url":"https://example.com"}}," ",{"text":"[Hover Here]","hover_event":{"action":"show_text","value":"Hover text here"}}]
```

For full tellraw syntax reference, see the [Minecraft Wiki](https://minecraft.wiki/w/Raw_JSON_text_format).

</details>

---

<details>
<summary><strong>MC-122477 Fix (Double Chat/Command Input)</strong></summary>

Fixes a bug on Unix systems where the key to open chat or the command input is sometimes registered twice when pressed, causing an unintended character to appear.

This fix is applied automatically - no configuration needed.

</details>
