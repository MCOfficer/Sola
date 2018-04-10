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

    final static String prefix = "sola ";
    Commands commands;
    EventWaiter eventWaiter;
    JDA jda ;

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        eventWaiter = new EventWaiter();
        commands = new Commands(eventWaiter, this);
        try {
            BufferedReader br = new BufferedReader(new FileReader("sola.txt"));
            jda = new JDABuilder(AccountType.BOT)
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
