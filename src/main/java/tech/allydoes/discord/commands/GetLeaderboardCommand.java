package tech.allydoes.discord.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import tech.allydoes.discord.Command;

public class GetLeaderboardCommand implements Command {
    @Override
    public SlashCommandData getCommandData() {
        return null;
    }

    @Override
    public void processSlashCommandInteractionEvent(SlashCommandInteractionEvent event) {

    }

    @Override
    public void processButtonInteractionEvent(ButtonInteractionEvent event) {

    }

    @Override
    public boolean isPrivate() {
        return false;
    }
}
