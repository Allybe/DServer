package tech.allydoes.discord.listener;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import tech.allydoes.Main;
import tech.allydoes.discord.Command;
import tech.allydoes.discord.DiscordManager;

public class SlashCommandInteractionListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        DiscordManager discordManager = Main.getDiscordManager();
        Command command = discordManager.getCommand(event.getName());
        if (command == null) return;
        command.processSlashCommandInteractionEvent(event);
    }
}
