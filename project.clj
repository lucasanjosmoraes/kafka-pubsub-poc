(defproject lucasanjosmoraes/kafka-pubsub-poc "0.1.0-SNAPSHOT"
  :description "POC of a pubsub app using Kafka"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[environ "1.2.0"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/data.json "2.4.0"]
                 [org.apache.kafka/kafka-clients "3.1.0"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.slf4j/slf4j-log4j12 "1.7.36"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]]
  :main ^:skip-aot lucasanjosmoraes.core
  :plugins [[lein-ancient "1.0.0-RC3"]]
  :aliases {"consumer" ["run" "-m" "lucasanjosmoraes.consumer"]
            "producer" ["run" "-m" "lucasanjosmoraes.producer"]})
