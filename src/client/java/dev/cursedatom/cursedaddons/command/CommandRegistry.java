package dev.cursedatom.cursedaddons.command;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.cursedatom.cursedaddons.utils.LoggerUtils;
import dev.cursedatom.cursedaddons.utils.MessageUtils;
import dev.cursedatom.cursedaddons.utils.TextUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
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
                .then(literal("fakechat")
                    .then(argument("json", StringArgumentType.greedyString())
                        .executes(CommandRegistry::executeFakeChat)
                    )
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

    private static int executeFakeChat(CommandContext<FabricClientCommandSource> context) {
        String json = StringArgumentType.getString(context, "json");
        try {
            Gson gson = new Gson();
            JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
            Component component = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement).getOrThrow();
            MessageUtils.sendToNonPublicChat(component);
        } catch (JsonSyntaxException e) {
            MessageUtils.sendToNonPublicChat(TextUtils.trans("text.cursedaddons.error.invalid_json", e.getMessage()));
            LoggerUtils.error("Invalid JSON in fakechat command: " + json + " - " + e.getMessage());
        } catch (Exception e) {
            MessageUtils.sendToNonPublicChat(TextUtils.trans("text.cursedaddons.error.fakechat_failed", e.getMessage()));
            LoggerUtils.error("Failed to execute fakechat command: " + e.getMessage());
        }
        return Command.SINGLE_SUCCESS;
    }
}
