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

  override def toString = s"{ 'deliveryTag': $deliveryTag, 'exchange': '$exchange', 'routingKey': '$routingKey', 'redelivered': $redelivered, 'properties': ${properties.toString}, 'body': '$body' }"

}

class RabbitMQMessageProperties(basicProperties: BasicProperties) extends Serializable with Logging {

  var contentType = basicProperties.getContentType
  var contentEncoding = basicProperties.getContentEncoding
  var headers: util.Map[String, Object] = parseHeaders(basicProperties.getHeaders)
  var deliveryMode = basicProperties.getDeliveryMode
  var priority = basicProperties.getPriority
  var correlationId = basicProperties.getCorrelationId
  var replyTo = basicProperties.getReplyTo
  var expiration = basicProperties.getExpiration
  var messageId = basicProperties.getMessageId
  var timestamp = basicProperties.getTimestamp
  var messageType = basicProperties.getType
  var userId = basicProperties.getUserId
  var appId = basicProperties.getAppId

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

  def parseHeaders(map: util.Map[String, Object]): util.Map[String, Object] = {
    val newMap = new util.HashMap[String, Object]()
    try {
      map.keySet().toArray.foreach(key => {
        val keyStr = key.toString
        val obj = map.get(keyStr)
        val objClass = obj.getClass
        var newObj: Object = null
        if (objClass.toString.contains("ByteArrayLongString")) {
          newObj = obj.toString
        } else {
          newObj = obj
        }
        newMap.put(keyStr, newObj)
      })
    } catch {
      case e: Exception => log.error(e.toString)
    }
    newMap
  }

  override def toString = s"{ 'contentType': '$contentType', 'contentEncoding': '$contentEncoding', 'headers': $headers, 'deliveryMode': $deliveryMode, 'priority': $priority, correlationId:$correlationId, 'replyTo': '$replyTo', 'expiration': '$expiration', 'messageId': '$messageId', 'timestamp': '$timestamp', 'messageType': '$messageType', 'userId': '$userId', 'appId': '$appId' }"
}
