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
package com.brunneis.polypus.polypus4t.dao;

import com.brunneis.polypus.polypus4t.vo.DigitalPost;
import java.util.ArrayList;
import java.util.HashMap;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.brunneis.polypus.polypus4t.conf.AerospikeConf;
import com.brunneis.polypus.polypus4t.conf.Conf;
import com.brunneis.polypus.polypus4t.conf.KafkaConf;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.hbase.client.Put;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 *
 * @author brunneis
 */
public final class DigitalPostSingletonKafkaAerospikeDAO implements DigitalPostDAO {

    private static final DigitalPostSingletonKafkaAerospikeDAO INSTANCE
            = new DigitalPostSingletonKafkaAerospikeDAO();

    private Logger logger;

    // KAFKA
    private String KAFKA_TOPIC = ((KafkaConf) Conf.KAFKA.value()).topic.value();
    private Properties KAFKA_PROPERTIES;
    private Producer kafkaProducer;

    // AEROSPIKE
    private AerospikeClient aerospikeClient;

    private DigitalPostSingletonKafkaAerospikeDAO() {
        logger = Logger.getLogger(DigitalPostSingletonKafkaAerospikeDAO.class.getName());
        logger.setLevel(Conf.LOGGER_LEVEL.value());

        // Configure the Producer
        KAFKA_PROPERTIES = new Properties();

        String kafka_host = ((KafkaConf) Conf.KAFKA.value()).host.value();
        Integer kafka_port = ((KafkaConf) Conf.KAFKA.value()).port.value();

        KAFKA_PROPERTIES.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka_host + ":" + kafka_port);
        KAFKA_PROPERTIES.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        KAFKA_PROPERTIES.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        // Start clients
        this.connect();
    }

    public static DigitalPostDAO getInstance() {
        return INSTANCE;
    }

    private void connectAerospike() {
        String host = ((AerospikeConf) Conf.AEROSPIKE.value()).host.value();
        Integer port = ((AerospikeConf) Conf.AEROSPIKE.value()).port.value();
        // Single Seed Node
        this.aerospikeClient = new AerospikeClient(host, port);
    }

    private void disconnectAerospike() {
        this.aerospikeClient.close();
    }

    private void reconnectAerospike() {
        this.disconnectAerospike();
        this.connectAerospike();
    }

    private void connectKafka() {
        kafkaProducer = new KafkaProducer<>(KAFKA_PROPERTIES);
    }

    private void disconnectKafka() {
        kafkaProducer.close();
    }

    private void reconnectKafka() {
        this.disconnectKafka();
        this.connectKafka();
    }

    @Override
    public synchronized void connect() {
        this.connectAerospike();
        this.connectKafka();
    }

    @Override
    public synchronized void disconnect() {
        this.disconnectAerospike();
        this.disconnectKafka();
    }

    @Override
    public synchronized void dumpBuffer(HashMap<String, DigitalPost> buffer) {
        // HBase puts to be written
        List<Put> puts = new ArrayList<>();

        WritePolicy wpolicyOutputBuffer = new WritePolicy();
        // The record never expires
        wpolicyOutputBuffer.expiration = -1;
        // Write only if record does not exist
        wpolicyOutputBuffer.recordExistsAction = RecordExistsAction.CREATE_ONLY;
        wpolicyOutputBuffer.timeout = 1000;
        wpolicyOutputBuffer.retryOnTimeout = true;
        wpolicyOutputBuffer.maxRetries = 5;
        wpolicyOutputBuffer.sleepBetweenRetries = 50;

        WritePolicy wpolicyProcessedPosts = new WritePolicy();
        // The record is removed after 2 days (seconds)
        wpolicyProcessedPosts.expiration = 172800;
        // Write if record does not exist, otherwise replace it
        wpolicyProcessedPosts.recordExistsAction = RecordExistsAction.REPLACE;
        wpolicyProcessedPosts.timeout = 1000;
        wpolicyProcessedPosts.retryOnTimeout = true;
        wpolicyProcessedPosts.maxRetries = 5;
        wpolicyProcessedPosts.sleepBetweenRetries = 50;

        // Buffer filter
        buffer.keySet().forEach((key) -> {
            Key keyToHash = new Key(
                    // Source as namespace (twttr)
                    "polypus_" + buffer.get(key).getSource(),
                    "ids", // set
                    key
            );
            boolean postExists = false;
            try {
                postExists = aerospikeClient.exists(null, keyToHash);
            } catch (AerospikeException ex) {
                logger.log(Level.SEVERE, null, ex);
                this.reconnectAerospike();
            }

            // Check new posts
            if (!postExists) {
                try {
                    this.aerospikeClient.put(
                            wpolicyProcessedPosts,
                            keyToHash,
                            new Bin("processed", true)
                    );
                } catch (AerospikeException ex) {
                    logger.log(Level.SEVERE, "ids put", ex);

                    this.reconnectAerospike();
                }

                String postId = buffer.get(key).getPostId();
                Long timestamp = buffer.get(key).getPublicationTimestamp();
                String userId = buffer.get(key).getAuthorNickname();

                String content = buffer.get(key).getContent();
                content = content.replaceAll("\\r", "");
                content = content.replaceAll("\\n", " ");

                String language = buffer.get(key).getLanguage();
                String tuple = userId + ';' + postId + ";" + timestamp + ";" + language + ";" + content;

                ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_TOPIC, tuple);
                kafkaProducer.send(record);

            }
        });
    }
}
