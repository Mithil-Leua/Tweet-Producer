# Tweet Producer

## Description

Given a term, it fetches continuous stream of tweets related to the term using the twitter API. Each tweet is then pushed to given Kafka topic as a message for Ziggurat to read from.

## Dev Environment Setup

1. Install Java8

## Build Instructions

```
./gradlew build
```

## Test Instructions

```
./gradlew test
```

## Run Instructions

application.yml under `src/resources` should be configured first.
- CONSUMER_KEY, CONSUMER_SECRET, TOKEN, SECRET are authentication parameters required by the Twitter API.
- KAFKA_BROKER is set to be `localhost:9092` but can be changed depending on where your Kafka broker is running.
- TWEETER_TOPIC is the term you want to search for on the twitter. 

``` yml
CONSUMER_KEY: 'your consumer key'
CONSUMER_SECRET: 'your consumer secret'
TOKEN: 'your token'
SECRET: 'your secret'
KAFKA_BROKER: "localhost:9092"
KAFKA_TOPIC: "topic"
TWEETER_TOPIC: "elon musk"
```

After configuring the yml, run the program using
```
./gradlew run
```