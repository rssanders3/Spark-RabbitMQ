/**
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.receiver

import java.security.InvalidParameterException

import com.rabbitmq.client.QueueingConsumer.Delivery
import org.joda.time.DateTime

import scala.util._

import com.rabbitmq.client._
import org.apache.spark.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.receiver.Receiver

import scala.collection.JavaConverters._

private[receiver]
class RabbitMQInputDStream(@transient ssc_ : StreamingContext,
                            params: Map[String, String]
                            ) extends ReceiverInputDStream[RabbitMQMessage](ssc_) with Logging {

  private val storageLevelParam: String = params.getOrElse("storageLevel", "MEMORY_AND_DISK_SER_2")

  override def getReceiver(): Receiver[RabbitMQMessage] = {
    new RabbitMQReceiver(params, StorageLevel.fromString(storageLevelParam))
  }
}

private[receiver]
class RabbitMQReceiver(params: Map[String, String], storageLevel: StorageLevel)
  extends Receiver[RabbitMQMessage](storageLevel) with Logging {

  private val host: String = params.getOrElse("host", "localhost")
  private val rabbitMQQueueName: Option[String] = params.get("queueName")
  private val queueDurable: Boolean = params.getOrElse("queueDurable", "false").toBoolean //the queue will survive a server restart
  private val queueExclusive: Boolean = params.getOrElse("queueExclusive", "false").toBoolean //restricted to this connection
  private val queueAutoDelete: Boolean = params.getOrElse("queueAutoDelete", "false").toBoolean //server will delete it when no longer in use
  private val enableExchange: Boolean = params.getOrElse("enableExchange", "true").toBoolean
  private val exchangeNames: Array[String] = params.getOrElse("exchangeNames", "rabbitmq-exchange").split(",") //comma separated list of exchange names
  private val exchangeTypes: Array[String] = params.getOrElse("exchangeTypes", "direct").split(",") //comma separated list of exchange types that correspond to the exchange names
  private val routingKeys: Option[String] = params.get("routingKeys")
  private val virtualHost: Option[String] = params.get("virtualHost")
  private val username: Option[String] = params.get("username")
  private val password: Option[String] = params.get("password")
  private val prefetchCount: Int = params.getOrElse("prefetchCount", "0").toInt //default 0 which sets it to unlimited
  private val channelCount: Int = params.getOrElse("channelCount", "1").toInt //number of channels to create
  private val x_max_length: Option[String] = params.get("x-max-length")
  private val x_message_ttl: Option[String] = params.get("x-message-ttl")
  private val x_expires: Option[String] = params.get("x-expires")
  private val x_max_length_bytes: Option[String] = params.get("x-max-length-bytes")
  private val x_dead_letter_exchange: Option[String] = params.get("x-dead-letter-exchange")
  private val x_dead_letter_routing_key: Option[String] = params.get("x-dead-letter-routing-key")
  private val x_max_priority: Option[String] = params.get("x-max-priority")

  val DirectExchangeType: String = "direct"
  val TopicExchangeType: String = "topic"
  val DefaultRabbitMQPort = 5672


  /**
   * Print the user supplied parameters
   */
  def printParams(): Unit = {
    log.info(s"{" +
      s"'host': '$host', " +
      s"'queueName': '$rabbitMQQueueName', " +
      s"'queueDurable': $queueDurable, " +
      s"'queueExclusive': $queueExclusive, " +
      s"'queueAutoDelete': $queueAutoDelete, " +
      s"'enableExchange': $enableExchange, " +
      "'exchangeNames': " + exchangeNames.toString + ", " +
      "'exchangeTypes': " + exchangeTypes.toString + ", " +
      s"'routingKeys': '$routingKeys', " +
      s"'virtualHost': '$virtualHost', " +
      s"'username': '$username', " +
      s"'password': 'PASSWORD_OMITTED', " +
      s"'prefetchCount': $prefetchCount, " +
      s"'channelCount': $channelCount, " +
      s"'x_max_length': '$x_max_length', " +
      s"'x_message_ttl': '$x_message_ttl', " +
      s"'x_expires': '$x_expires', " +
      s"'x_max_length_bytes': '$x_max_length_bytes', " +
      s"'x_dead_letter_exchange': '$x_dead_letter_exchange', " +
      s"'x_dead_letter_routing_key': '$x_dead_letter_routing_key', " +
      s"'x_max_priority': '$x_max_priority' " +
      s"}")
  }

  //TODO: Implement validateParams()
  /**
   * Validate the user supplied parameters
   */
//  def validateParams(): Unit = {
//    throw new InvalidParameterException("")
//  }

  /**
   * Start Receiver
   */
  @Override
  def onStart() {
    printParams()
    implicit val akkaSystem = akka.actor.ActorSystem()
    //TODO: create multiple threads. Each thread creates its own Channel for multithreading.
    getConnection() match {
      case Success(connection: Connection) => log.info("onStart, Establishing Connection...")
        (1 to channelCount).foreach(i => {
          getChannel(connection) match {
            case Success(channel: Channel) => log.info(s"onStart, Creating Channel($i)...")
              new Thread() {
                override def run() {
                  receive(connection, channel)
                }
              }.start()
            case Failure(f) =>
              log.error("Could not create channel")
              restart("Could not create channel", f)
          }
        })
      case Failure(f) =>
        log.error("Could not connect")
        restart("Could not connect", f)
    }
  }

  /**
   * Stop Receiver
   */
  @Override
  def onStop() {
    // There is nothing much to do as the thread calling receive()
    // is designed to stop by itself isStopped() returns false
    log.info("onStop, doing nothing.. relaxing...")
  }

  /**
   * Create a socket connection and receive data until receiver is stopped
   *
   * @param connection
   * @param channel
   */
  @Override
  private def receive(connection: Connection, channel: Channel) {

    try {
      val queueName: String = getQueueName(channel)

      log.info("RabbitMQ Input waiting for messages")
      val consumer: QueueingConsumer = new QueueingConsumer(channel)
      log.info("start consuming data")
      channel.basicQos(prefetchCount)
      channel.basicConsume(queueName, false, consumer)

      while (!isStopped()) {
        //waiting for data
        val delivery: Delivery = consumer.nextDelivery()
        //storing data
        store(new RabbitMQMessage(
          delivery.getEnvelope.getDeliveryTag,
          delivery.getEnvelope.getExchange,
          delivery.getEnvelope.getRoutingKey,
          delivery.getEnvelope.isRedeliver,
          delivery.getProperties,
          new Predef.String(delivery.getBody)
        ))
        channel.basicAck(delivery.getEnvelope.getDeliveryTag, false)
      }

    } catch {
      case unknown : Throwable => log.error("Got this unknown exception: " + unknown, unknown)
    }
    finally {
      log.info("it has been stopped")
      try { channel.close() } catch { case _: Throwable => log.error("error on close channel, ignoring")}
      try { connection.close() } catch { case _: Throwable => log.error("error on close connection, ignoring")}
      restart("Trying to connect again")
    }
  }

  /**
   * Get the queue name
   *
   * @param channel
   * @return queueName
   */
  def getQueueName(channel: Channel): String = {
    // Get the queue name to use (explicit vs auto-generated).
    val queueName = checkQueueName()
    val queueParams = getQueueParams().asJava

    if(enableExchange) {
      val exchangeNamesLength = exchangeNames.length
      val exchangeTypesLength = exchangeTypes.length

      if(exchangeNamesLength != exchangeTypesLength) {
        log.warn("Exchange Names length is not equal to Exchange Types length")
        log.warn(s"Exchange Names value = $exchangeNames, length = $exchangeNamesLength")
        log.warn(s"Exchange Types value = $exchangeTypes, length = $exchangeTypesLength")
      }
      (0 until (exchangeNamesLength min exchangeTypesLength)).foreach(i => {
        val exchangeName = exchangeNames(i)
        val exchangeType = exchangeTypes(i)

        log.info(s"declaring exchange '$exchangeName' of type '$exchangeType'")
        channel.exchangeDeclare(exchangeName, exchangeType, true)

        log.info(s"declaring queue '$queueName'")
        channel.queueDeclare(queueName, queueDurable, queueExclusive, queueAutoDelete, queueParams)

        // Bind the exchange to the queue.
        routingKeys match {
          case Some(routingKey) => {
            // If routing keys were provided, then bind using them.
            for (routingKey: String <- routingKey.split(",")) {
              log.info(s"binding to routing key '$routingKey'")
              channel.queueBind(queueName, exchangeName, routingKey)
            }
          }
          case None => {
            log.warn("Routing key was not provided. Skipping binding exchange to queue.")
          }
        }
      })
    } else {
      log.info("You have opted to not enable exchange")
      log.info(s"declaring queue '$queueName'")
      channel.queueDeclare(queueName, queueDurable, queueExclusive, queueAutoDelete, queueParams)
    }

    queueName
  }

  /**
   * Get Params to be supplied to the queues when being declared
   *
   * @return queueParams
   */
   def getQueueParams() : Map[String, AnyRef] = {
     var params: Map[String, AnyRef] = Map.empty

     if (x_max_length.isDefined) {
       params += ("x-max-length" -> x_max_length.get.toInt.asInstanceOf[AnyRef])
     }
     if (x_message_ttl.isDefined) {
       params += ("x-message-ttl" -> x_message_ttl.get.toInt.asInstanceOf[AnyRef])
     }
     if (x_expires.isDefined) {
       params += ("x-expires" -> x_expires.get.toInt.asInstanceOf[AnyRef])
     }
     if (x_max_length_bytes.isDefined) {
       params += ("x-max-length-bytes" -> x_max_length_bytes.get.toInt.asInstanceOf[AnyRef])
     }
     if (x_dead_letter_exchange.isDefined) {
       params += ("x-dead-letter-exchange" -> x_dead_letter_exchange.get.toString.asInstanceOf[AnyRef])
     }
     if (x_dead_letter_routing_key.isDefined) {
       params += ("x-dead-letter-routing-key" -> x_dead_letter_routing_key.get.toString.asInstanceOf[AnyRef])
     }
     if (x_max_priority.isDefined) {
       params += ("x-max-priority" -> x_max_priority.get.toInt.asInstanceOf[AnyRef])
     }
     params
   }


  /**
   * Get the Queue Name
   *
   * @return queueName
   */
  def checkQueueName(): String = {
    rabbitMQQueueName.getOrElse({
      log.warn("The name of the queue will be a default name")
      s"default-queue-${new DateTime(System.currentTimeMillis())}"
    })
  }

  /**
   * Establishes connection to RabbitMQ
   *
   * @return Try[Connection]
   */
  private def getConnection(): Try[Connection] = {
    log.info("Rabbit host addresses are : " + host)
    for ( address <- Address.parseAddresses(host) ) {
      log.info("Address " + address.toString())
    }

    log.info("creating new connection")
    for {
      connection: Connection <- Try(getConnectionFactory.newConnection(Address.parseAddresses(host)))
    } yield {
      log.info("created new connection")
      connection
    }
  }

  /**
   * Creates a Channel given a connection
   *
   * @param connection - RabbitMQ Connection
   * @return channel
   */
  private def getChannel(connection: Connection): Try[Channel] = {
    log.info("creating new channel")
    for {
      channel: Channel <- Try(connection.createChannel)
    } yield {
      log.info("created new channel")
      channel
    }
  }

  /**
   * Get Connection Factory
   *
   * @return Connection Factory
   */
  private def getConnectionFactory: ConnectionFactory = {
    val factory: ConnectionFactory = new ConnectionFactory

    virtualHost match {
      case Some(v) => {
        factory.setVirtualHost(v)
        log.info(s"Connecting to virtual host ${factory.getVirtualHost}")
      }
      case None =>
        log.info("No virtual host configured")
    }

    username.map(factory.setUsername(_))
    password.map(factory.setPassword(_))
    factory
  }
}
