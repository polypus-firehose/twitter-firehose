/*
    Polypus: a Big Data Self-Deployable Architecture for Microblogging 
    Text Extraction and Real-Time Sentiment Analysis

    Copyright (C) 2017 Rodrigo Martínez (brunneis) <dev@brunneis.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.brunneis.polypus.polypus4t.threads;

import com.brunneis.polypus.polypus4t.vo.DigitalPost;
import com.brunneis.polypus.polypus4t.conf.Conf;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HTMLParser;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class ScraperMiner extends Thread {

    private final HashMap<String, DigitalPost> buffer;
    private final ArrayList<String> targets;
    private final long startTime;
    private final ThreadHelper th;
    private final int bufferSize;

    private Logger logger;

    public ScraperMiner(String id, ArrayList<String> targets) {
        logger = Logger.getLogger(ScraperMiner.class.getName());
        logger.setLevel(Conf.LOGGER_LEVEL.value());

        super.setName(id);
        this.buffer = new HashMap<>();
        this.targets = targets;
        this.th = new ThreadHelper();

        int thread_int = Integer.parseInt(id.split("_")[0]);

        this.bufferSize = Conf.BUFFER.value()
                + Conf.INCREMENT.value() * thread_int;

        // Initial date in ms
        this.startTime = new Date().getTime();
    }

    public HtmlPage getPage(String url) throws IOException {
        try (WebClient webClient = new WebClient(BrowserVersion.BEST_SUPPORTED,
                "polypus-twitter-proxy-balancer", 9999)) {
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setTimeout(5000);
            webClient.getOptions().setRedirectEnabled(true);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setUseInsecureSSL(true);
            webClient.getOptions().setAppletEnabled(false);

            Page jsonPage = webClient.getPage(url);
            WebResponse response = jsonPage.getWebResponse();
            String json = response.getContentAsString();

            Map<String, String> map = new Gson().fromJson(
                    json,
                    new TypeToken<Map<String, String>>() {
                    }.getType()
            );

            StringWebResponse newResponse = new StringWebResponse(
                    map.get("items_html"),
                    new URL("http://localhost")
            );

            return HTMLParser.parseHtml(
                    newResponse,
                    webClient.getCurrentWindow()
            );
        }
    }

    @Override
    public void run() {
        logger.log(Level.INFO, "thread {0} | starting job...", getName());

        if (this.targets.isEmpty()) {
            logger.log(Level.INFO,
                    "thread {0} | No target terms, job finished.", getName());
            return;
        }

        // While there is time left, another lap is started
        while (this.th.inTime(this.startTime)) {
            // Exit if no more time left
            if (!this.th.inTime(this.startTime)) {
                break;
            }

            // For every term associated to each language
            for (String query : this.targets) {
                logger.log(
                        Level.FINE,
                        "thread {0} | counter: {1}",
                        new Object[]{getName(), this.buffer.size()}
                );

                // Exit if not more time left
                if (!this.th.inTime(this.startTime)) {
                    break;
                }

                switch (query.charAt(0)) {
                    case '@':
                        query = "%40" + query.substring(1);
                        break;
                    case '#':
                        query = "%23" + query.substring(1);
                        break;
                }

                String f = "";
                switch (Conf.SEARCH_FOR.value()) {
                    default:
                    case "tweets":
                        f = "tweets";
                        break;
                    case "news":
                        f = "news";
                        break;
                    case "images":
                        f = "images";
                        break;
                    case "videos":
                        f = "videos";
                        break;
                    case "broadcasts":
                        f = "broadcasts";
                }

                // src indica el origen de la consulta (desde dónde se hace)
                // pero no altera el resultado
                String url = "https://twitter.com/i/search/timeline";
                if (!f.isEmpty()) {
                    url += "?f=" + f;
                }
                url += "&vertical=default&q=" + query + "&src=typd";

                String demoUrl = "https://twitter.com/search?f=" + f
                        + "&vertical=default&q=" + query;

                HtmlPage page;
                try {
                    page = getPage(url);
                    logger.log(Level.INFO, "Retrieved URL: {0}", url);
                    logger.log(Level.INFO, "Test URL: {0}", demoUrl);
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                            "thread {0} | mins_left: {1} | couldn't retrieve the page",
                            new Object[]{getName(),
                                this.th.getMinsLeft(startTime)}
                    );
                    sleep_under_exception();
                    break;
                }

                List<?> tweetDataList;
                List<?> tweetTextList;
                List<?> tweetTimeList;
                try {
                    tweetDataList = page.getByXPath("//div[@data-tweet-id]");
                    tweetTextList = page.getByXPath("//p[@lang]");
                    tweetTimeList = page.getByXPath("//span[@data-time-ms]");
                    page.cleanUp();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                    continue;
                }

                Integer streamSize = tweetDataList.size();

                for (int i = 0; i < streamSize; i++) {
                    DomElement tweetData = (DomElement) tweetDataList.get(i);
                    DomElement tweetText = (DomElement) tweetTextList.get(i);
                    DomElement tweetTime = (DomElement) tweetTimeList.get(i);

                    // Extracting information of the tweet
                    String language = tweetText
                            .getAttribute("lang").trim();
                    // String content = tweetText
                    //         .getTextContent().trim();
                    // Since emojis are encoded as images, further processing
                    // is needed
                    String content = tweetText
                            .asXml().trim();
                    String authorName = tweetData
                            .getAttribute("data-name").trim();
                    String authorNickname = tweetData
                            .getAttribute("data-screen-name").trim();
                    String authorId = tweetData
                            .getAttribute("data-user-id").trim();
                    String postId = tweetData
                            .getAttribute("data-tweet-id").trim();
                    String publicationTimestamp = tweetTime
                            .getAttribute("data-time-ms").trim();

                    // Get user followers
                    // String url = "https://twitter.com/" + authorNickname;
                    // try {
                    //     page = webClient.getPage(url);
                    // } catch (IOException | FailingHttpStatusCodeException ex) {
                    //     continue;
                    // }
                    // List<?> userFollowersList = page.getByXPath("//a[@data-nav='followers']/span");
                    // tweetData = (DomElement) userFollowersList.get(1);
                    // Long followers = normalizeFollowers(tweetData.getTextContent().trim());
                    // if (followers == null) {
                    //     continue;
                    // }

                    // Every tweet is saved in the HashMap buffer
                    DigitalPost tweet = new DigitalPost(
                            "twttr",
                            language,
                            content,
                            authorName,
                            authorNickname,
                            authorId,
                            postId,
                            publicationTimestamp,
                            null // getRelevance(followers)
                    );

                    // The tweet is added to the buffer if it is 
                    // not already there
                    if (!this.buffer
                            .containsKey(tweet.getPostId())) {
                        this.buffer.put(tweet.getPostId(), tweet);
                    }

                    if (this.buffer.size() >= this.bufferSize) {
                        logger.log(Level.INFO,
                                "thread {0} | mins_left: {1} | dumping...",
                                new Object[]{getName(),
                                    this.th.getMinsLeft(startTime)}
                        );
                        this.th.dumpBuffer(buffer);
                    }

                }

                sleep();
            }
        }

        this.th.dumpBuffer(buffer);
        logger.log(Level.WARNING,
                "thread {0} | job finished.", getName());
    }

    private void sleep_under_exception() {
        try {
            Thread.sleep(Conf.SLEEP.value() + 1000);
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(Conf.SLEEP.value());
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

}
