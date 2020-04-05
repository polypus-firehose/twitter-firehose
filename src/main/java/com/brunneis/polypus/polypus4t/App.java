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
package com.brunneis.polypus.polypus4t;

import com.brunneis.polypus.polypus4t.conf.Conf;
import com.brunneis.polypus.polypus4t.conf.ConfLoadException;
import com.brunneis.polypus.polypus4t.threads.ScraperMiner;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.brunneis.polypus.polypus4t.dao.DigitalPostSingletonFactoryDAO;

public class App {

    public static void main(String[] args) throws ConfLoadException {

        // BasicConfigurator.configure();
        Logger logger = Logger.getLogger(App.class.getName());

        // Read in-line parameters
        readParams(args, logger);

        // Load config file
        Conf.loadConf();

        logger.setLevel(Conf.LOGGER_LEVEL.value());

        // Showing loaded parameters
        logger.log(Level.INFO, "SEARCH_FOR = {0}", Conf.SEARCH_FOR.value());
        logger.log(Level.INFO, "CONF_FILE = {0}", Conf.CONF_FILE.value());
        logger.log(Level.INFO, "THREADS = {0}", Conf.THREADS.value());
        logger.log(Level.INFO, "SLEEP = {0}", Conf.SLEEP.value());
        logger.log(Level.INFO, "BUFFER = {0}", Conf.BUFFER.value());
        logger.log(Level.INFO, "INCREMENT = {0}", Conf.INCREMENT.value());
        logger.log(Level.INFO, "MINS = {0}", Conf.MINS.value());

        ArrayList<ScraperMiner> miners = new ArrayList<>();

        if (Conf.THREADS.value() > 0) {
            ArrayList<ArrayList<String>> targetGroups
                    = new ArrayList<>();

            // A group for every thread is filled up with an empty HashMap 
            // with the selected languages
            for (Integer i = 0; i < Conf.THREADS.value(); i++) {
                targetGroups.add(new ArrayList<>());
            }

            // Assignment of targets for each scraper-thread (round-robin)
            int counter = 0;
            for (String term : Conf.TARGETS.value()) {
                targetGroups.get(counter).add(term);
                counter = (counter + 1) % Conf.THREADS.value();
            }

            // It is created an instance for each scraper-thread with their
            // targets, target language and ID
            counter = 1;
            for (ArrayList<String> group : targetGroups) {
                miners.add(new ScraperMiner(counter + "_SCRPR", group));
                counter++;
            }

            // Each scraper-thread begins its work
            miners.forEach((miner) -> {
                miner.start();
            });
        }

        if (Conf.THREADS.value() > 0) {
            // The main thread waits for every thread to finish
            miners.forEach((miner) -> {
                try {
                    miner.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }

        // Close connections
        DigitalPostSingletonFactoryDAO.getDigitalPostDAOinstance().disconnect();
    }

    private static void readParams(String[] args, Logger logger) throws ConfLoadException {
        if (args.length > 0) {
            switch (args[0]) {
                case "-h":
                case "--help":
                    logger.log(Level.INFO,
                            "Polypus twitter-crawler\nOptions:"
                            + "\n\t-c, --conf: configuration file to be loaded."
                            + "\n\t-h, --help: to show this help.");
                    System.exit(0);
            }

            for (int i = 0; i < args.length - 1; i++) {
                try {
                    switch (args[i]) {
                        case "-c":
                        case "--conf":
                            Conf.CONF_FILE.set(args[i + 1]);
                            break;
                        case "-l":
                        case "--logger-level":
                            Conf.LOGGER_LEVEL.set(Level.parse(args[i + 1]));
                            break;
                    }
                } catch (NumberFormatException e) {
                    throw new ConfLoadException("Integer parse error.");
                }
            }
        }
    }
}
