// CursedAddons Regex Tester
// Tests regex patterns against Minecraft chat messages with § color codes

document.addEventListener('DOMContentLoaded', function() {
    const regexInput = document.getElementById('regex-input');
    const textInput = document.getElementById('text-input');
    const testBtn = document.getElementById('test-btn');
    const matchResult = document.getElementById('match-result');
    const captureGroups = document.getElementById('capture-groups');
    const visualPreview = document.getElementById('visual-preview');

    testBtn.addEventListener('click', testRegex);
    regexInput.addEventListener('input', updateVisualPreview);
    textInput.addEventListener('input', updateVisualPreview);

    // Initial visual preview
    updateVisualPreview();

    function testRegex() {
        const pattern = regexInput.value.trim();
        const text = textInput.value;

        if (!pattern) {
            showError('Please enter a regex pattern');
            return;
        }

        try {
            // Check if pattern contains unescaped & characters (like the mod does)
            const hasUnescapedAmpersands = pattern.match(/(?<!\\)&/);
            const textToMatch = hasUnescapedAmpersands ? text : text.replace(/§./g, ''); // Use legacy or plain text

            // Convert & to § in regex pattern (like the mod does)
            let convertedPattern = pattern.replace(/&r/g, '').replace(/\\&/g, '{{LITERAL_AMP}}').replace(/&/g, '§');
            convertedPattern = convertedPattern.replace(/{{LITERAL_AMP}}/g, '&');

            const regex = new RegExp(convertedPattern, 'i'); // Case insensitive like the mod

            const match = regex.exec(textToMatch);

            if (match) {
                showSuccess('Pattern matches!');

                // Display capture groups with $ replacement simulation
                let groupsText = '';
                for (let i = 0; i < match.length; i++) {
                    groupsText += `$${i}: "${match[i]}"\n`;
                }

                captureGroups.textContent = groupsText.trim();
                captureGroups.className = 'result-box success';
            } else {
                showNoMatch('Pattern does not match');
                captureGroups.textContent = 'No capture groups (no match found)';
                captureGroups.className = 'result-box';
            }

        } catch (error) {
            showError('Invalid regex pattern: ' + error.message);
            captureGroups.textContent = 'Cannot extract capture groups due to regex error';
            captureGroups.className = 'result-box error';
        }
    }

    function showSuccess(message) {
        matchResult.textContent = message;
        matchResult.className = 'result-box success';
    }

    function showError(message) {
        matchResult.textContent = message;
        matchResult.className = 'result-box error';
    }

    function showNoMatch(message) {
        matchResult.textContent = message;
        matchResult.className = 'result-box warning';
    }

    function updateVisualPreview() {
        const text = textInput.value;
        if (!text) {
            visualPreview.innerHTML = '<span style="color: var(--text-secondary);">Enter text to see color preview</span>';
            return;
        }

        visualPreview.innerHTML = parseMinecraftColors(text);
    }

    function parseMinecraftColors(text) {
        const colorCodes = {
            '0': 'color-black',
            '1': 'color-dark-blue',
            '2': 'color-dark-green',
            '3': 'color-dark-aqua',
            '4': 'color-dark-red',
            '5': 'color-dark-purple',
            '6': 'color-gold',
            '7': 'color-gray',
            '8': 'color-dark-gray',
            '9': 'color-blue',
            'a': 'color-green',
            'b': 'color-aqua',
            'c': 'color-red',
            'd': 'color-light-purple',
            'e': 'color-yellow',
            'f': 'color-white'
        };

        const formattingCodes = {
            'k': 'color-obfuscated',
            'l': 'color-bold',
            'm': 'color-strikethrough',
            'n': 'color-underline',
            'o': 'color-italic'
        };

        let result = '';
        let currentClasses = [];
        let i = 0;

        while (i < text.length) {
            if (text[i] === '§' && i + 1 < text.length) {
                const code = text[i + 1].toLowerCase();

                if (code === 'r') {
                    // Reset
                    currentClasses = [];
                } else if (colorCodes[code]) {
                    // Color code
                    currentClasses = currentClasses.filter(cls => !cls.startsWith('color-'));
                    currentClasses.push(colorCodes[code]);
                } else if (formattingCodes[code]) {
                    // Formatting code
                    if (!currentClasses.includes(formattingCodes[code])) {
                        currentClasses.push(formattingCodes[code]);
                    }
                }

                i += 2; // Skip the § and code
            } else {
                // Regular character
                const char = text[i];
                if (currentClasses.length > 0) {
                    result += `<span class="${currentClasses.join(' ')}">${escapeHtml(char)}</span>`;
                } else {
                    result += escapeHtml(char);
                }
                i++;
            }
        }

        return `<div class="color-preview">${result || '<span style="color: var(--text-secondary);">No visible text</span>'}</div>`;
    }

    function escapeHtml(text) {
        const map = {
            '&': '&',
            '<': '<',
            '>': '>',
            '"': '"',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, m => map[m]);
    }
});
