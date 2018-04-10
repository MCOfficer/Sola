import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main extends ListenerAdapter {

    public final static String prefix = "-";
    Commands commands;
    EventWaiter eventWaiter;

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        eventWaiter = new EventWaiter();
        commands = new Commands(eventWaiter);
        try {
            BufferedReader br = new BufferedReader(new FileReader("sola.txt"));
            JDA jda = new JDABuilder(AccountType.BOT)
                    .setToken(br.readLine())
                    .addEventListener(eventWaiter)
                    .buildBlocking();
            br.close();
            jda.addEventListener(this);
            jda.getPresence().setGame(Game.playing(prefix + "help"));
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
        String[] argsStripped = Arrays.copyOfRange(args, 1, args.length);

        try {
            if (args[0].equalsIgnoreCase(prefix + "play"))
                commands.onPlayCommand(guild, channel, member, argsStripped);
            else if (args[0].equalsIgnoreCase(prefix + "help"))
                commands.onHelpCommand(channel);
            else if (args[0].equalsIgnoreCase(prefix + "stop"))
                commands.onStopCommand(guild, channel);
            else if (args[0].equalsIgnoreCase(prefix + "ping"))
                commands.onPingCommand(channel);
            else if (args[0].equalsIgnoreCase(prefix + "update"))
                commands.onUpdateCommand(channel);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
