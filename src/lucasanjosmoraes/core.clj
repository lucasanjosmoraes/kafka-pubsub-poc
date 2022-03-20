(ns lucasanjosmoraes.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [environ.core :refer [env]])
  (:import
   (org.apache.kafka.clients.consumer KafkaConsumer)
   (org.apache.kafka.common.serialization StringSerializer StringDeserializer)
   (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)
   (org.apache.kafka.clients.admin AdminClientConfig AdminClient NewTopic)
   (java.time Duration)))

(defn create-topics!
  "Create the topic"
  [bootstrap-server topics ^Integer partitions ^Short replication]
  (let [config      {AdminClientConfig/BOOTSTRAP_SERVERS_CONFIG bootstrap-server}
        adminClient (AdminClient/create config)
        new-topics  (map (fn [^String topic-name] (NewTopic. topic-name partitions replication)) topics)]
    (.createTopics adminClient new-topics)))

(defn- build-consumer
  "Create the consumer instance to consume from the provided kafka topic name"
  [bootstrap-server]
  (let [consumer-props {"bootstrap.servers",  bootstrap-server
                        "group.id",           "kafka_pubsub_poc"
                        "key.deserializer",   StringDeserializer
                        "value.deserializer", StringDeserializer
                        "auto.offset.reset",  "earliest"
                        "enable.auto.commit", "true"}]
    (KafkaConsumer. consumer-props)))

(defn consumer-subscribe
  [consumer topic]
  (.subscribe consumer [topic]))

(defn- build-producer
  "Create the kafka producer to send on messages received"
  [bootstrap-server]
  (let [producer-props {"value.serializer"  StringSerializer
                        "key.serializer"    StringSerializer
                        "bootstrap.servers" bootstrap-server}]
    (KafkaProducer. producer-props)))

(defn run-application
  "Create the simple read and write topology with Kafka"
  [bootstrap-server]
  (let [consumer-topic   "example-consumer-topic"
        producer-topic   "example-produced-topic"
        bootstrap-server (env :bootstrap-server bootstrap-server)
        consumer         (build-consumer bootstrap-server)
        producer         (build-producer bootstrap-server)]
    (log/infof "Creating the topics %s" [producer-topic consumer-topic])
    (create-topics! bootstrap-server [producer-topic consumer-topic] 1 1)
    (log/infof "Starting the kafka example app. With topic consuming topic %s and producing to %s"
      consumer-topic producer-topic)
    (consumer-subscribe consumer consumer-topic)
    (while true
      (let [records (.poll consumer (Duration/ofMillis 100))]
        (doseq [record records]
          (log/info "Sending on value" (str "Processed Value: " (.value record)))
          (.send producer (ProducerRecord. producer-topic "a" (str "Processed Value: " (.value record))))))
      (.commitAsync consumer))))

(defn -main
  [& _]
  (.addShutdownHook (Runtime/getRuntime) (Thread. #(log/info "Shutting down")))
  (run-application "localhost:9092"))
