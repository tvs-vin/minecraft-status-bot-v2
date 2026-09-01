package tvs.mcsb.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import tvs.mcsb.ConfigHelper;

public class MainCommands {

    public static void register() {
	CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
		Commands.literal("mcsb")
			.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.then(Commands.literal("status")
				.executes(context -> {
				    context.getSource().sendSuccess(
					    () -> Component.literal("MCSB: web UI " + (ConfigHelper.webUI ? "enabled" : "disabled")
						    + ", Discord " + (ConfigHelper.discordIntegration ? "enabled" : "disabled")),
					    false
				    );
				    return 1;
				})
			)
	));
    }
}
