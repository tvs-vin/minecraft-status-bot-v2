package tvs.mcsb.discord;

// Imports

import net.dv8tion.jda.api.JDA;

import tvs.mcsb.ConfigHelper;
import tvs.mcsb.Mcsb;
import tvs.mcsb.Utility;

// Code to handle all the Discord bot interactions and commands

public class DiscordBot {

    // Discord bot values

    private static boolean canRun = true;
    private static String token = ConfigHelper.discordToken;

    private static Thread botThread; 
    private static BotClass botTask;

    private static void startBot() {
        try {
            botTask = new BotClass(token);
        } catch (IllegalArgumentException e) {
            Mcsb.LOGGER.error("MCSB | Failed to make Discord bot instance | ", e);
        }
        botThread = new Thread(botTask, "DiscordBot-Thread");
        if(Utility.logCheck(3)){
            Mcsb.LOGGER.info("MCSB | Made thread for Discord bot");
        }
        botThread.start();

    }

    public static void updateCommands() {
        if(botTask != null) {
            botTask.updateCommands();
        }
    }

    public static void initBot() {
        if(token == null || token.isEmpty()) {
            Mcsb.LOGGER.error("MCSB | Discord token is not set in the config");
            canRun = false;
        }

        // Only runs after all the sanity checks have passed
        if(canRun) {
            // Starts the Discord bot here
            startBot();
        }
    }
}
