package tech.allydoes.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.allydoes.Constants;
import tech.allydoes.discord.listener.SlashCommandInteractionListener;

import java.util.HashMap;

public class DiscordManager {
    public final Logger LOGGER;
    private final JDA jda;
    private final HashMap<String, Command> commands;

    public DiscordManager() throws InterruptedException {
        LOGGER = LogManager.getLogger(DiscordManager.class);
        commands = new HashMap<>();
        //put commands here

        String botToken = Constants.PRODUCTION ? Constants.BOT_TOKEN : Constants.TESTING_TOKEN;
        jda = JDABuilder
                .create(botToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new SlashCommandInteractionListener())
                .build()
                .awaitReady();
        registerSlashCommands();
        LOGGER.info("Logged in as {}", jda.getSelfUser().getName());
    }

    private void registerSlashCommands() {
        if (Constants.PRODUCTION) {
            registerSlashCommandsGlobal();
            registerPrivateSlashCommands();
        } else {
            registerSlashCommandsGuild(Constants.TEST_GUILD_ID);
        }
    }

    private void registerPrivateSlashCommands() {
        Guild guild = jda.getGuildById(Constants.PRIVATE_GUILD_ID);
        if (guild == null) return;

        CommandListUpdateAction updateAction = guild.updateCommands();
        for (Command command : commands.values()) {
            if (!command.isPrivate()) continue;
            updateAction = updateAction.addCommands(command.getCommandData());
            LOGGER.debug("Registered private guild command: {}", command.getCommandData().getName());
        }
        updateAction.queue();
    }

    private void registerSlashCommandsGlobal() {
        CommandListUpdateAction commandListUpdateAction = jda.updateCommands();
        for (Command command : commands.values()) {
            if (command.isPrivate()) continue;
            commandListUpdateAction = commandListUpdateAction.addCommands(command.getCommandData());
            LOGGER.debug("Registered global command: {}", command.getCommandData().getName());
        }
        commandListUpdateAction.queue();
    }

    private void registerSlashCommandsGuild(String guildID) {
        Guild guild = jda.getGuildById(guildID);
        if (guild == null) return;

        CommandListUpdateAction updateAction = guild.updateCommands();
        for (Command command : commands.values()) {
            updateAction = updateAction.addCommands(command.getCommandData());
            LOGGER.debug("Registered guild command: {}", command.getCommandData().getName());
        }
        updateAction.queue();
    }

    public Command getCommand(String command) {
        return commands.get(command);
    }

    public JDA getJDA() {
        return jda;
    }
}
