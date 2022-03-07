(ns lucasanjosmoraes.producer
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:import (java.util Properties)
           (org.apache.kafka.clients.producer ProducerConfig ProducerRecord KafkaProducer Callback)
           (org.apache.kafka.clients.admin AdminClient NewTopic)
           (org.apache.kafka.common.errors TopicExistsException)))

(defn- build-properties
  [config-fname]
  (with-open [config (io/reader (io/resource config-fname))]
    (doto (Properties.)
      (.putAll {ProducerConfig/KEY_SERIALIZER_CLASS_CONFIG   "org.apache.kafka.common.serialization.StringSerializer"
                ProducerConfig/VALUE_SERIALIZER_CLASS_CONFIG "org.apache.kafka.common.serialization.StringSerializer"})
      (.load config))))

(defn- create-topic!
  [topic partitions replication config]
  (let [ac (AdminClient/create config)]
    (try
      (.createTopics ac [(NewTopic. ^String topic (int partitions) (short replication))])
      (catch TopicExistsException _ nil)
      (finally
        (.close ac)))))

(def print-ex
  (comp println (partial str "Failed to deliver message: ")))

(defn- print-metadata
  [meta-data]
  (printf "Produced record to topic %s partition [%d] @ offest %d\n"
    (.topic meta-data)
    (.partition meta-data)
    (.offset meta-data)))

(defn- create-msg
  [count topic]
  (let [k "alice"
        v (json/write-str {:count count})]
    (printf "Producing record: %s\t%s\n" k v)
    (ProducerRecord. topic k v)))

(defn producer!
  [config-fname topic]
  (let [props (build-properties config-fname)]
    (with-open [producer (KafkaProducer. props)]
      (create-topic! topic 1 3 props)
      ;; We can use callbacks to handle the result of a send, like this:
      (let [callback (reify Callback
                       (onCompletion [_ metadata exception]
                         (if exception
                           (print-ex exception)
                           (print-metadata metadata))))]
        (doseq [i (range 5)]
          (.send producer (create-msg i topic) callback))
        (.flush producer)
        ;; Or we could wait for the returned futures to resolve, like this:
        (let [futures (doall (map #(.send producer (create-msg % topic)) (range 5 10)))]
          (.flush producer)
          (while (not-every? future-done? futures)
            (Thread/sleep 50))
          (doseq [fut futures]
            (try
              (let [metadata (deref fut)]
                (print-metadata metadata))
              (catch Exception e
                (print-ex e))))))
      (printf "10 messages were produced to topic %s!\n" topic))))

(defn -main [& args]
  (apply producer! args))