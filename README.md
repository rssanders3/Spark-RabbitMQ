# RabbitMQ-Receiver

RabbitMQ-Receiver is a library that allows the user to read data with [Apache Spark](https://spark.apache.org/)
from [RabbitMQ](https://www.rabbitmq.com/).

## Requirements

This library requires Spark 1.4+, Scala 2.10+, RabbitMQ 3.5+

## Using the library

There are two ways of using RabbitMQ-Receiver library:

The first one is to add the next dependency in your pom.xml:

```
<dependency>
  <groupId>com.stratio.receiver</groupId>
  <artifactId>rabbitmq</artifactId>
  <version>LATEST</version>
</dependency>
```

The other one is to clone the full repository and build the project:

```
git clone https://github.com/rsanders3/RabbitMQ-Receiver.git
mvn clean install
```

### Build

There are two modules to package with different versions of Spark

- To package it with Spark-1.4:

`mvn clean package -pl com.stratio.receiver:spark-rabbitmq_1.4`

- To package it with Spark-1.5 (default):

`mvn clean package -pl com.stratio.receiver:spark-rabbitmq_1.5`

### Scala API

```
val receiverStream = RabbitMQUtils.createStream(sparkStreamingContext, params)
```

### Java API

```
JavaReceiverInputDStream receiverStream = RabbitMQUtils.createJavaStream(javaSparkStreamingContext, params);


```

| Parameter                 | Description                                        | Optional                             |
|---------------------------|----------------------------------------------------|--------------------------------------|
| host                      | RabbitMQ host                                      | Yes (default: localhost)             |
| queueName                 | Queue name                                         | Yes                                  |
| queueDurable              | The queue will survive a server restart            | Yes (default: false)                 |
| queueExclusive            | Restricted to this connection                      | Yes (default: false)                 |
| queueAutoDelete           | Server will delete the queue when no longer in use | Yes (default: false)                 |
| enableExchange            | Enable Exchange                                    | Yes (default: true)                  |
| exchangeNames             | Comma separated list of Exchange names             | Yes (default: rabbitmq-exchange)     |
| exchangeTypes             | Comma separated list of Exchange types             | Yes (default: direct)                |
| routingKeys               | Routing keys comma separated                       | Yes                                  |
| virtualHost               | RabbitMQ Virtual Host                              | Yes                                  |
| username                  | RabbitMQ username                                  | Yes                                  |
| password                  | RabbitMQ password                                  | Yes                                  |
| prefetchCount             | Prefetch Count                                     | Yes (default: 0)                     |
| channelCount              | Number of channel threads to spawn                 | Yes (default: 1)                     |
| storageLevel              | Apache Spark storage level                         | Yes (default: MEMORY_AND_DISK_SER_2) |
| x-max-length              | RabbitMQ queue property                            | Yes                                  |
| x-message-ttl             | RabbitMQ queue property                            | Yes                                  |
| x-expires                 | RabbitMQ queue property                            | Yes                                  |
| x-max-length-bytes        | RabbitMQ queue property                            | Yes                                  |
| x-dead-letter-exchange    | RabbitMQ queue property                            | Yes                                  |
| x-dead-letter-routing-key | RabbitMQ queue property                            | Yes                                  |
| x-max-priority            | RabbitMQ queue property                            | Yes                                  |

# Updates from Previous Project #

This project was forked from https://github.com/Stratio/Spark-RabbitMQ and includes a number of improvements:

 * Returns more Message Data
    * Message Delivery Tag
    * Message Exchange
    * Message Routing Key
    * Message Redelivered
    * Message Properties
    * Message payload
 * More control over how you connect to the RabbitMQ Queue
    * Exchanges
        * Are now optional
        * Can define multiple
    * Queues
        * Can define durable
        * Can define exclusive connection
        * Can define auto delete
 * Adding performance enhancements 
    * Prefetch
    * Multi-threading
    

# License #

Licensed to STRATIO (C) under one or more contributor license agreements.
See the NOTICE file distributed with this work for additional information
regarding copyright ownership.  The STRATIO (C) licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
