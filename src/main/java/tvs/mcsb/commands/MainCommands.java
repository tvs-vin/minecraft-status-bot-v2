package tvs.mcsb.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import tvs.mcsb.ConfigHelper;
import tvs.mcsb.features.discord.DiscordBot;

public class MainCommands {

    private static void serverOnlyCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            Commands.literal("mcsb")
                .then(Commands.literal("basic-status")
                    .executes(context -> {
                        context.getSource().sendSuccess(
                            () -> Component.literal("MCSB: web UI " + (ConfigHelper.webUI ? "enabled" : "disabled")
                                + ", Discord " + (ConfigHelper.discordIntegration ? "enabled" : "disabled")),
                            false
                        );
                        return 1;
                    })
                ).then(Commands.literal("admin").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .executes(context -> {context.getSource().sendSuccess(() -> Component.literal("Admin Commands"), false); return 1;})
                    .then(Commands.literal("reload-config")
                        .executes(context -> admin_reloadConfig(context))
                    ).then(Commands.literal("discord")
                        .executes(context -> admin_discord(context))
                        .then(Commands.literal("reload-commands")
                            .executes(context -> admin_discord_reloadCommands(context))
                        )
                    )
                )
        ));
    }

    // The actual commands - Made this way for readability and to avoid cluttering the main class with too much code.

    /*
    The command structure is as follows:
    
    /mcsb                   | Main command, states version
    ├── basic-status        | Anyone can run, states running or no
    └── admin               | Only opped users and see
        ├── reload-config   | Reloads configs from the config file
        ├── discord         | Any discord related commands
        │   └── Any conf    |
        ├── webui           |
        │   └── Any conf    |
        └── TBD             |
    
    */

    // Admin commands

    private static int admin_reloadConfig(CommandContext<CommandSourceStack> context){
        ConfigHelper.init();
        context.getSource().sendSuccess(
            () -> Component.literal("MCSB: Config reloaded"),
            false
        );
        return 1;
    }

    private static int admin_discord(CommandContext<CommandSourceStack> context){
        context.getSource().sendSuccess(
            () -> Component.literal("MCSB: Discord status: " + (ConfigHelper.discordIntegration ? "enabled" : "disabled")),
            false
        );
        return 1;
    }

    private static int admin_discord_reloadCommands(CommandContext<CommandSourceStack> context){
        if(!ConfigHelper.discordIntegration){
            context.getSource().sendFailure(Component.literal("MCSB | Discord integration is disabled!"));
            return 0;
        } else {
            DiscordBot.updateCommands();
            context.getSource().sendSuccess(
                () -> Component.literal("MCSB: Discord commands reloaded"),
                false
            );
            return 1;

        }
    }

    // Public method

    public static void register() {
        
        // Starts with server only stuff
        serverOnlyCommands();

    }
}
