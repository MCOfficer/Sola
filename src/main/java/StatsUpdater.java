import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;


public class StatsUpdater implements Runnable {

    private final Main main;
    private final OkHttpClient client;

    StatsUpdater(Main main) {
        this.main = main;
        client = new OkHttpClient();
    }

    @Override
    public void run() {
         long guildCount = main.jda.getGuildCache().size();
         String payload = "{\"server_count\": \"" + guildCount + "\"}";
         RequestBody body = RequestBody.create(MediaType.parse("application/json"), payload);

        Request discordbotsOrg = new Request.Builder()
                .addHeader("Authorization", main.properties.getProperty("discordbots.org"))
                .post(body)
                .url("https://discordbots.org/api/bots/432038554516979712/stats")
                .build();

        Request botsDiscordPw = new Request.Builder()
                .addHeader("Authorization", main.properties.getProperty("bots.discord.pw"))
                .post(body)
                .url("https://bots.discord.pw/api/bots/432038554516979712/stats")
                .build();

        try {
            client.newCall(discordbotsOrg).execute();
            client.newCall(botsDiscordPw).execute();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


}
