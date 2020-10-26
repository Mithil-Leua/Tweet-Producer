package tweet;

import com.gojek.ApplicationConfiguration;
import com.gojek.Figaro;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TweetProducer {
    static KafkaProducer<String, String> producer;

    public void getTweetStream(ApplicationConfiguration appConfig) throws InterruptedException {
        String topic = appConfig.getValueAsString("TWEETER_TOPIC");
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(1000);
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        endpoint.trackTerms(Collections.singletonList(topic));
        Authentication authentication = new OAuth1(appConfig.getValueAsString("CONSUMER_KEY"),
                appConfig.getValueAsString("CONSUMER_SECRET"),
                appConfig.getValueAsString("TOKEN"),
                appConfig.getValueAsString("SECRET"));
        Client client = new ClientBuilder().hosts(Constants.STREAM_HOST)
                .endpoint(endpoint)
                .authentication(authentication)
                .processor(new StringDelimitedProcessor(queue))
                .build();
        client.connect();
        getProducer(appConfig);
        int count = 1;
        while (true) {
            String tweet = queue.take();
            JsonObject jsonObject = new Gson().fromJson(tweet, JsonObject.class);
            try {
                String tweetText = jsonObject.get("text").toString();
                JsonObject objectToSend = new JsonObject();
                objectToSend.addProperty("topic", topic);
                objectToSend.addProperty("text", tweetText);
                System.out.println(objectToSend.toString());
                ProducerRecord<String, String> record = new ProducerRecord<String, String>(appConfig.getValueAsString("KAFKA_TOPIC"),
                        objectToSend.toString());
                producer.send(record);
                System.out.println(count + " records added");
                count++;
            } catch (NullPointerException e) {
                System.out.println("Some error occurred");
            }
        }
    }

    private void getProducer(ApplicationConfiguration appConfig) {
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getValueAsString("KAFKA_BROKER"));
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "1");
        producerProperties.put(ProducerConfig.LINGER_MS_CONFIG, 500);
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, 0);
        producer = new KafkaProducer<>(producerProperties);
    }

    public static void main(String[] args) throws InterruptedException {
        ApplicationConfiguration appConfig = Figaro.configure(null);
        TweetProducer tweetProducer = new TweetProducer();
        tweetProducer.getTweetStream(appConfig);
    }
}
