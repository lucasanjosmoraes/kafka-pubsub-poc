(ns lucasanjosmoraes.core
  (:require [lucasanjosmoraes.clients :as clients])
  (:import (org.apache.kafka.clients.producer KafkaProducer)
           (org.apache.kafka.clients.consumer ConsumerRecord))
  (:gen-class))

(def app-id "kafka-pubsub-poc")
(def consumer-topic "user.events")
(def producer-topic "user.events.proxied")

(defn handle-event
  [^ConsumerRecord record]
  (let [key   (.key record)
        value (.value record)]
    (println (str key ":" value))))

(defn handle-event-with-producer
  [^KafkaProducer producer ^String producer-topic ^ConsumerRecord record]
  (let [key   (.key record)
        value (.value record)]
    (clients/produce producer producer-topic key value)))

(defn handle-event-proxied
  [^ConsumerRecord record]
  (let [key   (.key record)
        value (.value record)]
    (println (str "Proxied - " key ":" value))))

(defn -main
  []
  (let [^KafkaProducer producer (clients/get-producer app-id)]
    (clients/consume-all-from-start app-id consumer-topic handle-event)
    (clients/consume-part-from-now app-id consumer-topic #(handle-event-with-producer producer producer-topic %))
    (clients/consume-all-from-now app-id producer-topic handle-event-proxied)))
