# kafka-pubsub-poc

POC of a pubsub app using Kafka. It follows the [confluent examples](https://docs.confluent.io/platform/current/tutorials/examples/clients/docs/clojure.html) 
to define a [consumer](https://github.com/confluentinc/examples/blob/7.0.1-post/clients/cloud/clojure/src/io/confluent/examples/clients/clj/consumer.clj) 
and a [producer](https://github.com/confluentinc/examples/blob/7.0.1-post/clients/cloud/clojure/src/io/confluent/examples/clients/clj/producer.clj).

## About the project

- Example of an integration with Kafka using Java Interop;
- Examples of executables with aliases;
- Example of application execution with configurations defined on a `.config` file;
- Example of topic creation when needed;
- [Branch with a namespace that coordinates consumer and producer, including an example of logback configuration file](https://github.com/lucasanjosmoraes/kafka-pubsub-poc/tree/feature/perks);
- [Branch with a great separation of components by responsibility, an example of connection with a Kafka with SSL activated,
examples of: consuming all messages from a topic, consuming messages from the consumer execution and consuming messages
from a specific partition](https://github.com/lucasanjosmoraes/kafka-pubsub-poc/tree/feature/bob2021).

## Usage

### From command line

You can run either components with the name of the config file at the `resources` directory and the name of the topic:
```clj
;; To run the consumer
lein consumer java.config <topic name>

;; To run the producer
lein producer java.config <topic name>
```

The producer will publish 10 messages for test purpose.

## From Intellij

There are scripts in the directory `.run` that allow you to run either components. You just need to choose one of them
from the dropdown menu and press play 🛀🏽.

## License

Copyright © 2022 Lucas dos Anjos Moraes

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
