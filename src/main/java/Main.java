import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends ListenerAdapter {

    final static String prefix = "sola ";
    Commands commands;
    EventWaiter eventWaiter;
    JDA jda;
    Properties properties = new Properties();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        eventWaiter = new EventWaiter();
        commands = new Commands(eventWaiter, this);
        try {
            properties.load(new FileReader("sola.txt"));
            jda = new JDABuilder(AccountType.BOT)
                    .setToken(properties.getProperty("token"))
                    .addEventListener(eventWaiter)
                    .buildBlocking();
            jda.addEventListener(this);
            jda.getPresence().setGame(Game.playing(prefix + "help"));

            scheduler.scheduleAtFixedRate(new StatsUpdater(this), 0, 6, TimeUnit.HOURS);

            Path file = Paths.get(".solarestart");
            String channelId = Files.readAllLines(file).get(0);
            Files.delete(file);
            jda.getTextChannelById(channelId).sendMessage("...Done").queue();
        }
        catch (LoginException | InterruptedException | IOException e) {
            if(!e.getMessage().equals(".solarestart"))
                e.printStackTrace();
            }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromType(ChannelType.TEXT) && event.getAuthor().isBot()) return;
        Guild guild = event.getGuild();
        TextChannel channel = event.getTextChannel();
        User author = event.getAuthor();
        Member member = event.getMember();
        Message message = event.getMessage();
        String content = message.getContentRaw();
        String[] args = content.split(" ");
        if (!args[0].equalsIgnoreCase("sola")) return;
        String[] argsStripped = Arrays.copyOfRange(args, 2, args.length);

        try {
            if (args[1].equalsIgnoreCase("play"))
                commands.onPlayCommand(guild, channel, member, argsStripped);
            else if (args[1].equalsIgnoreCase("help"))
                commands.onHelpCommand(channel);
            else if (args[1].equalsIgnoreCase("stop"))
                commands.onStopCommand(guild, channel);
            else if (args[1].equalsIgnoreCase("ping"))
                commands.onPingCommand(channel);
            else if (args[1].equalsIgnoreCase("update"))
                commands.onUpdateCommand(channel, author);
            else if (args[1].equalsIgnoreCase("current"))
                commands.onCurrentCommand(guild, channel);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
