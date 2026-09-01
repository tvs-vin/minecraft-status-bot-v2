package tvs.mcsb.discord;

// Imports

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import tvs.mcsb.ConfigHelper;
import tvs.mcsb.Mcsb;

public class CommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Slash command handler
        if (ConfigHelper.discordBotCommandOutput) {
                Mcsb.LOGGER.info("MCSB | Received slash command");
            }
        if (event.getName().equals("ping")) {
            if (ConfigHelper.discordBotCommandOutput) {
                Mcsb.LOGGER.info("MCSB | Running ping command");
            }
            long time = System.currentTimeMillis();
            event.reply("Pong! " + time + "ms")
            .setEphemeral(true)
            .flatMap(v -> event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time))
            .queue();
        }
        if (event.getName().equals("status")) {
            // Handle the status command here
            event.reply("TBD")
            .setEphemeral(true)
            .queue();
            if (ConfigHelper.discordBotCommandOutput) {
                Mcsb.LOGGER.info("MCSB | Running status command");
            }
        }
    }

}
