(ns lucasanjosmoraes.clients
  (:import (org.apache.kafka.clients CommonClientConfigs)
           (org.apache.kafka.clients.consumer ConsumerConfig KafkaConsumer ConsumerRecords ConsumerRecord)
           (org.apache.kafka.clients.producer KafkaProducer ProducerConfig ProducerRecord)
           (org.apache.kafka.common.config SslConfigs)
           (org.apache.kafka.common.security.auth SecurityProtocol)
           (org.apache.kafka.common.serialization StringSerializer StringDeserializer)
           (java.time Duration)
           (java.util Properties UUID)))

(def brokers (or (System/getenv "KAFKA_BROKERS") "localhost:9092"))
(def keystore-location (System/getenv "SSL_KEYSTORE_LOCATION"))
(def truststore-location (System/getenv "SSL_TRUSTSTORE_LOCATION"))
(def ssl-password (System/getenv "SSL_PASSWORD"))

(defn produce
  [^KafkaProducer producer ^String topic-name ^String key ^String value]
  (if-let [pr (ProducerRecord. topic-name key value)]
    (.send producer pr)))

(defn produce-without-key
  [^KafkaProducer producer ^String topic-name ^String value]
  (if-let [pr (ProducerRecord. topic-name value)]
    (.send producer pr)))

(defn optionally-add-ssl
  [properties]
  (when (and keystore-location truststore-location ssl-password)
    (doto properties
      (.put CommonClientConfigs/SECURITY_PROTOCOL_CONFIG (.name SecurityProtocol/SSL))
      (.put SslConfigs/SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG "")
      (.put SslConfigs/SSL_TRUSTSTORE_LOCATION_CONFIG (str "/etc/kafka/secrets/" truststore-location))
      (.put SslConfigs/SSL_TRUSTSTORE_PASSWORD_CONFIG ssl-password)
      (.put SslConfigs/SSL_KEYSTORE_LOCATION_CONFIG (str "/etc/kafka/secrets/" keystore-location))
      (.put SslConfigs/SSL_KEYSTORE_PASSWORD_CONFIG ssl-password)
      (.put SslConfigs/SSL_KEY_PASSWORD_CONFIG ssl-password))))

(defn get-producer
  [client-id & {:keys [config]}]
  (let [properties (Properties.)]
    (doto properties
      (.put ProducerConfig/BOOTSTRAP_SERVERS_CONFIG brokers)
      (.put ProducerConfig/CLIENT_ID_CONFIG client-id)
      (.put ProducerConfig/KEY_SERIALIZER_CLASS_CONFIG (.getName StringSerializer))
      (.put ProducerConfig/VALUE_SERIALIZER_CLASS_CONFIG (.getName StringSerializer))
      (.put ProducerConfig/LINGER_MS_CONFIG (.intValue 100))
      (.put ProducerConfig/ACKS_CONFIG "all")
      (optionally-add-ssl)
      #(doseq [[prop-name prop-val] config] (.put % prop-name prop-val)))
    (KafkaProducer. properties)))

(defn get-consumer
  [client-id group-id from-start & {:keys [config]}]
  (let [properties (Properties.)
        reset-config (if from-start "earliest" "latest")]
    (doto properties
      (.put ConsumerConfig/BOOTSTRAP_SERVERS_CONFIG brokers)
      (.put ConsumerConfig/CLIENT_ID_CONFIG client-id)
      (.put ConsumerConfig/GROUP_ID_CONFIG group-id)
      (.put ConsumerConfig/AUTO_OFFSET_RESET_CONFIG reset-config)
      (.put ConsumerConfig/MAX_POLL_RECORDS_CONFIG (int 100))
      (.put ConsumerConfig/KEY_DESERIALIZER_CLASS_CONFIG (.getName StringDeserializer))
      (.put ConsumerConfig/VALUE_DESERIALIZER_CLASS_CONFIG (.getName StringDeserializer))
      (.put ConsumerConfig/ENABLE_AUTO_COMMIT_CONFIG false)
      (optionally-add-ssl)
      #(doseq [[prop-name prop-val] config] (.put % prop-name prop-val)))
    (KafkaConsumer. properties)))

(defn poll-execute
  [^KafkaConsumer consumer function]
  (let [^ConsumerRecords records (.poll consumer (Duration/ofMillis 100))]
    (doseq [^ConsumerRecord record records] (function record))))

(defn consumer-loop [keep-running ^KafkaConsumer consumer function]
  (if @keep-running
    (do
      (poll-execute consumer function)
      (recur keep-running consumer function))
    (.close consumer)))

(defn consume
  [client-id group-id topic from-start function & {:keys [config]}]
  (let [keep-running (atom true)
        consumer     (if config
                       (get-consumer client-id group-id from-start config)
                       (get-consumer client-id group-id from-start))
        topics       (if (vector? topic)
                       topic
                       [topic])]
    (.subscribe consumer topics)
    (println "Waiting for message in KafkaConsumer.poll")
    (future (consumer-loop keep-running consumer function))
    #(reset! keep-running false)))

(defn consume-all-from-start
  "Consume all messages already sent to the given topic and the new ones."
  [app-id topic function]
  (consume (str app-id "-" topic) (str app-id "-" (UUID/randomUUID)) topic true function))

(defn consume-all-from-now
  "Consume the new messages using a unique group-id with the given app-id as prefix."
  [app-id topic function]
  (consume (str app-id "-" topic) (str app-id "-" (UUID/randomUUID)) topic false function))

(defn consume-part-from-now
  "Consume the new messages using the given app-id as the group-id."
  [app-id topic function]
  (consume (str app-id "-" topic) app-id topic false function))