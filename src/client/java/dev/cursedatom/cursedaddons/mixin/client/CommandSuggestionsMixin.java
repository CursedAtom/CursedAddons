package dev.cursedatom.cursedaddons.mixin.client;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import dev.cursedatom.cursedaddons.config.SpecialUnits;
import dev.cursedatom.cursedaddons.features.commandaliases.AliasHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Mixin(CommandSuggestions.class)
public class CommandSuggestionsMixin {

    @Shadow private EditBox input;
    @Shadow @Nullable private ParseResults<ClientSuggestionProvider> currentParse;

    @Unique
    private static final Style UNPARSED_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);
    @Unique
    private static final Style LITERAL_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY);
    @Unique
    private static final List<Style> ARGUMENT_STYLES = (List<Style>) Stream.of(
            ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD
        )
        .map(Style.EMPTY::withColor)
        .collect(ImmutableList.toImmutableList());

    /**
     * Feed the alias-expanded command to Brigadier so it parses the real command,
     * giving the correct usage tooltip and argument coloring parse tree.
     */
    @Redirect(
        method = "updateCommandInfo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/EditBox;getValue()Ljava/lang/String;",
            ordinal = 0
        )
    )
    private String cursedaddons$expandAliasForParse(EditBox editBox) {
        String value = editBox.getValue();
        SpecialUnits.AliasUnit match = AliasHandler.findMatch(value);
        if (match == null) return value;
        return match.replacement + value.substring(match.alias.length());
    }

    // Brigadier parsed the expanded command, but display text is the raw alias. Remap argument
    // color ranges from expanded coords back to alias coords before rendering.
    @Inject(method = "formatChat", at = @At("HEAD"), cancellable = true)
    private void cursedaddons$formatAliasChat(String text, int offset, CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (currentParse == null) return;

        String fullValue = input.getValue();
        SpecialUnits.AliasUnit match = AliasHandler.findMatch(fullValue);
        if (match == null) return;

        int aliasLen = match.alias.length();
        int replacementLen = match.replacement.length();

        cir.setReturnValue(cursedaddons$formatAliasText(currentParse, text, offset, aliasLen, replacementLen));
    }

    @Unique
    private static FormattedCharSequence cursedaddons$formatAliasText(
        ParseResults<ClientSuggestionProvider> parse,
        String text,
        int offset,
        int aliasLen,
        int replacementLen
    ) {
        List<FormattedCharSequence> parts = new ArrayList<>();
        int aliasBoundary = Math.max(aliasLen - offset, 0);

        if (aliasBoundary > 0 && aliasBoundary <= text.length()) {
            parts.add(FormattedCharSequence.forward(text.substring(0, aliasBoundary), LITERAL_STYLE));
        }

        // Brigadier ranges are in expanded-string coords; shift back to display coords:
        // displayPos = expandedPos - (replacementLen - aliasLen) - offset
        int shift = replacementLen - aliasLen;
        int unformattedStart = aliasBoundary;
        int nextColor = -1;
        CommandContextBuilder<ClientSuggestionProvider> context = parse.getContext().getLastChild();

        for (ParsedArgument<ClientSuggestionProvider, ?> argument : context.getArguments().values()) {
            if (++nextColor >= ARGUMENT_STYLES.size()) nextColor = 0;

            int start = Math.max(argument.getRange().getStart() - shift - offset, aliasBoundary);
            if (start >= text.length()) break;

            int end = Math.min(argument.getRange().getEnd() - shift - offset, text.length());
            if (end > unformattedStart) {
                if (start > unformattedStart) {
                    parts.add(FormattedCharSequence.forward(text.substring(unformattedStart, start), LITERAL_STYLE));
                }
                parts.add(FormattedCharSequence.forward(text.substring(start, end), ARGUMENT_STYLES.get(nextColor)));
                unformattedStart = end;
            }
        }

        if (parse.getReader().canRead()) {
            int errorStart = Math.max(parse.getReader().getCursor() - shift - offset, aliasBoundary);
            if (errorStart < text.length()) {
                int end = Math.min(errorStart + parse.getReader().getRemainingLength(), text.length());
                if (errorStart > unformattedStart) {
                    parts.add(FormattedCharSequence.forward(text.substring(unformattedStart, errorStart), LITERAL_STYLE));
                }
                parts.add(FormattedCharSequence.forward(text.substring(errorStart, end), UNPARSED_STYLE));
                unformattedStart = end;
            }
        }

        if (unformattedStart < text.length()) {
            parts.add(FormattedCharSequence.forward(text.substring(unformattedStart), LITERAL_STYLE));
        }

        return FormattedCharSequence.composite(parts);
    }
}
