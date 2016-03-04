package com.stratio.receiver

import java.util
import java.util.Date

import com.rabbitmq.client.BasicProperties
import org.apache.spark.Logging

/**
 * Created by robertsanders on 2/23/16.
 */
class RabbitMQMessage(deliveryTag: Long, exchange: String, routingKey: String, redelivered: Boolean, basicProperties: BasicProperties, body: String) extends Serializable {

  var properties = new RabbitMQMessageProperties(basicProperties)

  def getDeliveryTag: Long = {
    deliveryTag
  }

  def getExchange: String = {
    exchange
  }

  def getRoutingKey: String = {
    routingKey
  }

  def isRedelivered: Boolean = {
    redelivered
  }

  def getProperties: RabbitMQMessageProperties = {
    properties
  }

  def getBody: String = {
    body
  }

  override def toString = s"{ 'deliveryTag': $deliveryTag, 'exchange': '$exchange', 'routingKey': '$routingKey', 'redelivered': $redelivered, 'properties': $properties, 'body': '$body' }"

}

class RabbitMQMessageProperties(basicProperties: BasicProperties) extends Serializable with Logging {

  var contentType: String = null
  var contentEncoding: String = null
  var headers: util.Map[String, Object] = null
  var deliveryMode: Integer = null
  var priority: Integer = null
  var correlationId: String = null
  var replyTo: String = null
  var expiration: String = null
  var messageId: String = null
  var timestamp: Date = null
  var messageType: String = null
  var userId: String = null
  var appId: String = null

  if (basicProperties != null) {
    contentType = basicProperties.getContentType
    contentEncoding = basicProperties.getContentEncoding
    headers = parseHeaders(basicProperties.getHeaders)
    deliveryMode = basicProperties.getDeliveryMode
    priority = basicProperties.getPriority
    correlationId = basicProperties.getCorrelationId
    replyTo = basicProperties.getReplyTo
    expiration = basicProperties.getExpiration
    messageId = basicProperties.getMessageId
    timestamp = basicProperties.getTimestamp
    messageType = basicProperties.getType
    userId = basicProperties.getUserId
    appId = basicProperties.getAppId
  } else {
    log.warn("messageDelivery.basicProperties is null")
  }


  def getContentType: String = {
    contentType
  }

  def getContentEncoding: String = {
    contentEncoding
  }

  def getHeaders: util.Map[String, Object] = {
    headers
  }

  def getDeliveryMode: Integer = {
    deliveryMode
  }

  def getPriority: Integer = {
    priority
  }

  def getCorrelationId: String = {
    correlationId
  }

  def getReplyTo: String = {
    replyTo
  }

  def getExpiration: String = {
    expiration
  }

  def getMessageId: String = {
    messageId
  }

  def getTimestamp: Date = {
    timestamp
  }

  def getMessageType: String = {
    messageType
  }

  def getUserId: String = {
    userId
  }

  def getAppId: String = {
    appId
  }

  def parseHeaders(headersMap: util.Map[String, Object]): util.Map[String, Object] = {
    if (headersMap != null) {
      val newHeaders = new util.HashMap[String, Object]()
      try {
        headersMap.keySet().toArray.foreach(key => {
          val keyStr = key.toString
          val obj = headersMap.get(keyStr)
          var newObj: Object = null
          if (obj.getClass.toString.contains("ByteArrayLongString")) {
            newObj = obj.toString
          } else {
            newObj = obj
          }
          newHeaders.put(keyStr, newObj)
        })
      } catch {
        case e: Exception => log.error(e.toString)
      }
      newHeaders
    } else {
      log.warn("messageDelivery.basicProperties.headers is null")
      return null
    }
  }

  override def toString = s"{ 'contentType': '$contentType', 'contentEncoding': '$contentEncoding', 'headers': $headers, 'deliveryMode': $deliveryMode, 'priority': $priority, correlationId:$correlationId, 'replyTo': '$replyTo', 'expiration': '$expiration', 'messageId': '$messageId', 'timestamp': '$timestamp', 'messageType': '$messageType', 'userId': '$userId', 'appId': '$appId' }"
}
