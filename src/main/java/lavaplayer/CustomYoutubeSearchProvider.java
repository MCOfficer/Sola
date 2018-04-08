package lavaplayer;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CustomYoutubeSearchProvider extends com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(CustomYoutubeAudioSourceManager.class);

    private final YoutubeAudioSourceManager sourceManager;
    private final HttpInterfaceManager httpInterfaceManager;

    public CustomYoutubeSearchProvider(YoutubeAudioSourceManager sourceManager) {
        super(sourceManager);
        this.sourceManager = sourceManager;
        this.httpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager();
    }

    public AudioItem loadSearchResult(String query) {
        List<AudioTrack> tracks = new ArrayList<>();
        String nextPage = "";

        log.debug("Performing a search with query {}", query);

        for(int i = 1; i <= 5; i++) {

            try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
                URI url;
                if (i == 1)
                    url = new URIBuilder("https://www.youtube.com/results").addParameter("search_query", query).build();
                else
                    url = new URI("https://www.youtube.com" + nextPage);
                try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != 200) {
                        throw new IOException("Invalid status code for search response: " + statusCode);
                    }

                    Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
                    extractSearchResults(tracks, document, query);

                    if(tracks.size() >= 10) break;
                    log.debug("Not enough results, continuing on page " + (i + 1));
                    for(Element button : document.select("#page > #content .yt-uix-button-default > span")) {
                        if (button.text().equals(String.valueOf(i + 1))) {
                            nextPage = button.parent().attr("href");
                            break;
                        }
                    }

                }
            } catch (Exception e) {
                throw ExceptionTools.wrapUnfriendlyExceptions(e);
            }
        }
        if(tracks.isEmpty())
            return null; //If we didn't find anything, return null to trigger noMatches()
        return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
    }

    private void extractSearchResults(List<AudioTrack> tracks, Document document, String query) {

        for (Element results : document.select("#page > #content #results")) {
            for (Element result : results.select(".yt-lockup-video")) {
                if (!result.hasAttr("data-ad-impressions") && result.select(".standalone-ypc-badge-renderer-label").isEmpty()) {
                    extractTrackFromResultEntry(tracks, result);
                }
            }
        }
    }

    private void extractTrackFromResultEntry(List<AudioTrack> tracks, Element element) {
        Element durationElement = element.select("[class^=video-time]").first();
        Element contentElement = element.select(".yt-lockup-content").first();
        String videoId = element.attr("data-context-item-id");

        if (durationElement == null) {
            long duration = Long.MAX_VALUE;

            String title = contentElement.select(".yt-lockup-title > a").text();
            String author = contentElement.select(".yt-lockup-byline > a").text();

            tracks.add(sourceManager.buildTrackObject(videoId, title, author, true, duration));
        }
    }
}
