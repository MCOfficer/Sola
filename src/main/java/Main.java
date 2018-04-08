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
import java.util.Arrays;

public class Main extends ListenerAdapter {

    public final static String prefix = "-";
    Commands commands;

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        commands = new Commands();
        try {
            BufferedReader br = new BufferedReader(new FileReader("sola.txt"));
            JDA jda = new JDABuilder(AccountType.BOT)
                    .setToken(br.readLine())
                    .buildBlocking();
            br.close();
            jda.addEventListener(this);
            jda.getPresence().setGame(Game.playing(prefix + "help | Hosted by M*C*O"));
        }
        catch (LoginException | InterruptedException | IOException e) {
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
