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

import com.brunneis.polypus.polypus4t.conf.Conf;
import com.brunneis.polypus.polypus4t.dao.DigitalPostDAO;
import com.brunneis.polypus.polypus4t.dao.DigitalPostSingletonFactoryDAO;
import com.brunneis.polypus.polypus4t.vo.DigitalPost;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.LogFactory;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;

/**
 *
 * @author brunneis
 */
public class ThreadHelper {

    private final DigitalPostDAO dpdao;
    private Logger logger;

    public ThreadHelper() {
        this.dpdao = DigitalPostSingletonFactoryDAO.getDigitalPostDAOinstance();
        logger = Logger.getLogger(ScraperMiner.class.getName());
        logger.setLevel(Conf.LOGGER_LEVEL.value());
    }

    public void dumpBuffer(HashMap<String, DigitalPost> buffer) {
        HashMap<String, DigitalPost> deepCopy = new HashMap<>();

        buffer.keySet().forEach((key) -> {
            deepCopy.put(key, new DigitalPost(buffer.get(key)));
        });
        buffer.clear();

        dpdao.dumpBuffer(deepCopy);
    }

    public boolean inTime(long startTime) {
        // If the MINS parameter is set to 0, then it won't stop
        return (Conf.MINS.value() == 0) ? true : (new Date().getTime()
                - startTime) / 6e4 < Conf.MINS.value();
    }

    public double getRelevance(String nickname) {
        LogFactory.getFactory().setAttribute(
                "org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog"
        );

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit")
                .setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient")
                .setLevel(Level.OFF);

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        // If not enabled, Twitter doesn't show the tweets
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.setCssErrorHandler(new ErrorHandler() {
            @Override
            public void warning(CSSParseException exception)
                    throws CSSException {
            }

            @Override
            public void fatalError(CSSParseException exception)
                    throws CSSException {
            }

            @Override
            public void error(CSSParseException exception)
                    throws CSSException {
            }
        });

        HtmlPage page;

        try {
            String url = "https://twitter.com/" + nickname;
            page = webClient.getPage(url);
        } catch (FailingHttpStatusCodeException | IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            return 1;
        }

        List<?> userFollowersList;
        try {
            userFollowersList
                    = page.getByXPath("//a[@data-nav='followers']/span");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return 1;
        }

        DomElement tweetData = (DomElement) userFollowersList.get(1);
        Long followers = normalizeFollowers(tweetData.getTextContent().trim());
        return getRelevance(followers);
    }

    public double getRelevance(Long followers) {
        return 1 + (0.04 * followers) * 0.5;
    }

    public Long normalizeFollowers(String followers) {
        if (followers == null) {
            return null;
        }

        followers = followers.replace(",", "");
        if (followers.contains("K")) {
            Float f = Float.parseFloat(followers.split("K")[0]);
            return Math.round(f * 10e2);
        } else if (followers.contains("M")) {
            Float f = Float.parseFloat(followers.split("M")[0]);
            return Math.round(f * 10e5);
        } else {
            if (followers.contains(".")) {
                followers = followers.replace(".", "");
            }
            return Long.parseLong(followers);
        }
    }

    public float getMinsLeft(long startTime) {
        return (float) ((Conf.MINS.value() == 0) ? 0 : (Conf.MINS.value()
                - (new Date().getTime() - startTime) / 6e4));
    }

    public void printDigitalPost(DigitalPost tweet) {
        logger.log(Level.INFO,
                "[{0}]@{1}: {2}",
                new Object[]{tweet.getLanguage(),
                    tweet.getAuthorNickname(),
                    tweet.getContent()});
    }

}
