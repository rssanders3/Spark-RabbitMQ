package com.stratio.receiver

import com.rabbitmq.client.BasicProperties

/**
 * Created by robertsanders on 2/23/16.
 */
class RabbitMQMessage(propertiesInput: BasicProperties, bodyInput: String) extends Serializable {
  var properties = new RabbitMQMessageProperties(propertiesInput)
  var body = bodyInput

  override def toString: String = {
    "{properties:" + properties.toString + ", body:" + body + "}"
  }
}

class RabbitMQMessageProperties(basicProperties: BasicProperties) extends Serializable {
  var contentType = basicProperties.getContentType
  var contentEncoding = basicProperties.getContentEncoding
  var headers = basicProperties.getHeaders
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

  override def toString = s"{contentType:$contentType, contentEncoding:$contentEncoding, headers:$headers, deliveryMode:$deliveryMode, priority:$priority, correlationId:$correlationId, replyTo:$replyTo, expiration:$expiration, messageId:$messageId, timestamp:$timestamp, messageType:$messageType, userId:$userId, appId:$appId}"
}
