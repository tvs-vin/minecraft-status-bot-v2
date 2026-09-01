package tvs.mcsb.discord;

// Imports

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.entities.Activity;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

import tvs.mcsb.Mcsb;
import tvs.mcsb.Utility;
import tvs.mcsb.ConfigHelper;

public class BotClass implements Runnable {

    // Values to run the bot itself

    private static String token = null;
    private JDA jda;
    public static List<CommandData> commandList = new ArrayList<>();

    private static String playingStringBase = "{} | Minecraft Status Bot V2";
    private static String playingString = playingStringBase.replace("{}", ConfigHelper.nodeName);

    public BotClass(String token) {
        BotClass.token = token;
        if(BotClass.token == null || BotClass.token.isEmpty()) {
            throw new IllegalArgumentException("Discord token is not set");
        }
        
    }

    @Override
    public void run() {
        try {

            // Sets some final values up
            if(Utility.logCheck(2)) {
                Mcsb.LOGGER.info("MCSB | Starting Discord bot");
            }
    
            final String checkedPlayingString = Objects.requireNonNull(playingString, playingStringBase);

            // Sets the bots instance up
            this.jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .setActivity(Activity.playing(checkedPlayingString))
                .addEventListeners(new CommandListener())
                .build();
            
            if(Utility.logCheck(3)) {
                Mcsb.LOGGER.info("MCSB | Discord bot built successfully");
            }

            this.jda.awaitReady();
            if(Utility.logCheck(3)) {
                Mcsb.LOGGER.info("MCSB | Discord bot is connected on background thread");
            }

            // Adds normal commands
            commandList.add(Commands.slash("status", "Checks the status of the Minecraft server"));

            // Enables Debug commands
            if(ConfigHelper.debugEnabled) {
                commandList.add(Commands.slash("ping", "Checks the bot's latency"));
                if(Utility.logCheck(3)) {
                    Mcsb.LOGGER.info("MCSB | Debug commands added");
                }
            }

            //Adds all commands
            jda.updateCommands().addCommands(Objects.requireNonNull(commandList)).queue(
                success -> {
                    if(Utility.logCheck(2)) {
                        Mcsb.LOGGER.info("Successfully synced " + commandList.size() + " commands!");
                    }
                },
                failure -> {
                    if(Utility.logCheck(1)) {
                        Mcsb.LOGGER.error("Failed to sync commands: " + failure.getMessage());
                    }
                }
            );

        } catch (InterruptedException e) {
            Mcsb.LOGGER.error("MCSB | Discord bot interrupted | ", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Mcsb.LOGGER.error("MCSB | Discord bot encountered an error | ", e);
            e.printStackTrace();
        }
    }
    
    public JDA getJda() {
        return this.jda;
    }

}
