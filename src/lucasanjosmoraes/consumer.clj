(ns lucasanjosmoraes.consumer
  (:gen-class)
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io])
  (:import
   (java.time Duration)
   (java.util Properties)
   (org.apache.kafka.clients.consumer KafkaConsumer ConsumerConfig)))

(defn- build-properties
  [config-fname]
  (with-open [config (io/reader (io/resource config-fname))]
    (doto (Properties.)
      (.putAll {ConsumerConfig/GROUP_ID_CONFIG                 "kafka_pubsub_poc"
                ConsumerConfig/KEY_DESERIALIZER_CLASS_CONFIG   "org.apache.kafka.common.serialization.StringDeserializer"
                ConsumerConfig/VALUE_DESERIALIZER_CLASS_CONFIG "org.apache.kafka.common.serialization.StringDeserializer"})
      (.load config))))

(defn- consume-record
  [tc record]
  (let [value  (.value record)
        cnt    (get (json/read-str value) "count")
        new-tc (+ tc cnt)]
    (printf "Consumed record with key %s and value %s, and updated total count to %d\n"
      (.key record)
      value
      new-tc)
    new-tc))

(defn consumer!
  [config-fname topic]
  (with-open [consumer (KafkaConsumer. (build-properties config-fname))]
    (.subscribe consumer [topic])
    (println "Waiting for message in KafkaConsumer.poll")
    (loop [tc      0
           records []]
      (let [new-tc  (reduce consume-record tc records)
            timeout (Duration/ofSeconds 1)]
        (recur new-tc
          (seq (.poll consumer timeout)))))))

(defn -main [& args]
  (apply consumer! args))