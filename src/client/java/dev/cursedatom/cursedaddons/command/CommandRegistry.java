package dev.cursedatom.cursedaddons.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import dev.cursedatom.cursedaddons.utils.LoggerUtils;
import dev.cursedatom.cursedaddons.utils.MessageUtils;
import dev.cursedatom.cursedaddons.utils.TextUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandRegistry {
    public static boolean shouldOpenConfigScreen = false;

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("cursedaddons")
                .executes(context -> {
                    // Default action: open gui
                     openGui(context);
                     return Command.SINGLE_SUCCESS;
                })
                .then(literal("config")
                    .executes(context -> {
                        openGui(context);
                        return Command.SINGLE_SUCCESS;
                    })
                )
            );
        });
    }

    private static void openGui(CommandContext<FabricClientCommandSource> context) {
         Minecraft.getInstance().execute(() -> {
            try {
                shouldOpenConfigScreen = true;
            } catch (Exception e) {
                LoggerUtils.error("Failed to open CursedAddons config GUI: " + e.getMessage());
                MessageUtils.sendToNonPublicChat(TextUtils.trans("text.cursedaddons.error.gui_open_failed", e.getMessage()));
            }
         });
    }
}
