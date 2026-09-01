package tvs.mcsb;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;
import tvs.mcsb.commands.MainCommands;
import tvs.mcsb.discord.DiscordBot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mcsb implements ModInitializer {
	public static final String MOD_ID = "mcsb";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("MCSB | Initializing Minecraft Status Bot V2");
		ConfigHelper.init();
		if (ConfigHelper.webUI) {
			WebServer.start();
		}
		if(ConfigHelper.discordIntegration){
			if(Utility.logCheck(3)){
				LOGGER.info("MCSB | Discord bot Enabled");
			}
			DiscordBot.initBot();
		}

		MainCommands.register();

		if(Utility.logCheck(2)){
			LOGGER.debug("MCSB | Finished Init");
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

