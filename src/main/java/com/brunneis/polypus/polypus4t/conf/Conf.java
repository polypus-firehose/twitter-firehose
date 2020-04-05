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
package com.brunneis.polypus.polypus4t.conf;

import com.brunneis.locker.Locker;
import com.brunneis.sg.exceptions.FileParsingException;
import com.brunneis.sg.io.FileHandler;
import com.brunneis.sg.vo.Document;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author brunneis
 */
public class Conf {

    public final static int KAFKA_AEROSPIKE = 1;

    public final static Locker<String> CONF_FILE = new Locker<>();
    public final static Locker<Level> LOGGER_LEVEL = new Locker<>();
    public final static Locker<DBConf> KAFKA = new Locker<>(); // HBase
    public final static Locker<DBConf> AEROSPIKE = new Locker<>(); // Aerospike
    public final static Locker<Integer> STORE_MODE = new Locker<>();
    public final static Locker<String[]> DICT_FILES = new Locker<>();
    public final static Locker<List<String>> TARGETS = new Locker<>();
    public final static Locker<Integer> THREADS = new Locker<>();
    public final static Locker<Integer> SLEEP = new Locker<>();
    public final static Locker<Integer> MINS = new Locker<>();
    public final static Locker<Integer> BUFFER = new Locker<>();
    public final static Locker<Integer> INCREMENT = new Locker<>();
    public final static Locker<String> SEARCH_FOR = new Locker<>();

    public static void loadConf() throws ConfLoadException {
        if (!CONF_FILE.isLocked()) {
            CONF_FILE.set("crawler.conf");
        }

        if (!LOGGER_LEVEL.isLocked()) {
            LOGGER_LEVEL.set(Level.INFO);
        }

        // Force config file presence
        File file = new File(CONF_FILE.value());
        if (!file.exists()) {
            throw new ConfLoadException();
        }

        Properties properties = new Properties();
        InputStream input;
        try {
            input = new FileInputStream(CONF_FILE.value());
            properties.load(input);

            SEARCH_FOR.set(properties.getProperty("SEARCH_FOR", "tweets"));

            STORE_MODE.set(KAFKA_AEROSPIKE);

            KAFKA.set(new KafkaConf());
            ((KafkaConf) KAFKA.value()).host.set(properties.getProperty("KAFKA_HOST", "kafka"));
            ((KafkaConf) KAFKA.value()).port.set(Integer.parseInt(properties.getProperty("KAFKA_PORT", "9092")));
            ((KafkaConf) KAFKA.value()).topic.set(properties.getProperty("KAFKA_TOPIC", "raw_tweets"));

            AEROSPIKE.set(new AerospikeConf());
            ((AerospikeConf) AEROSPIKE.value()).host.set(properties.getProperty("AEROSPIKE_HOST", "aerospike"));
            ((AerospikeConf) AEROSPIKE.value()).port.set(Integer.parseInt(properties.getProperty("AEROSPIKE_PORT", "3000")));

            if (((AerospikeConf) AEROSPIKE.value()).host.isNull()
                    || ((AerospikeConf) AEROSPIKE.value()).port.isNull()) {
                throw new ConfLoadException();
            }
        } catch (IOException ex) {
            Logger.getLogger(Conf.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (properties.getProperty("FILES") != null) {
            DICT_FILES.set(properties.getProperty("FILES").replaceAll("\\s*,\\s*", ",").split(","));
        } else {
            throw new ConfLoadException("Error loading document.");
        }

        TARGETS.set(new ArrayList<>());

        Document document;
        for (String fileName : DICT_FILES.value()) {
            try {
                document = FileHandler.loadFile(fileName);
                for (String term : document.getGroup("terms").getValues()) {
                    TARGETS.value().add(term);
                }
                Logger.getLogger(Conf.class.getName()).log(Level.INFO, "{0} loaded", fileName);
            } catch (FileParsingException | IOException ex) {
                throw new ConfLoadException("Error loading document.");
            }
        }

        try {
            if (Integer.parseInt(properties.getProperty("THREADS")) == 0) {
                THREADS.set(1);
            } else {
                THREADS.set(Integer.parseInt(properties.getProperty("THREADS", "1")));
            }

            SLEEP.set(Integer.parseInt(properties.getProperty("SLEEP", "5")));
            MINS.set(Integer.parseInt(properties.getProperty("MINS", "0")));
            BUFFER.set(Integer.parseInt(properties.getProperty("BUFFER", "10000")));
            INCREMENT.set(Integer.parseInt(properties.getProperty("STEP", "500")));
        } catch (NumberFormatException ex) {
            throw new ConfLoadException();
        }

    }

}
