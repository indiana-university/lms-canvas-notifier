package edu.iu.uits.lms.canvasnotifier.amqp;

import com.rabbitmq.client.Channel;
import edu.iu.uits.lms.canvasnotifier.handler.NotificationMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RabbitListener(queues = "${canvasnotifier.canvasNotifierQueueName}")
@Component
@Profile("!batch")
@Slf4j
public class CanvasNotifierMessageListener {

   @Autowired
   NotificationMessageHandler notificationMessageHandler;

   @RabbitHandler
   public void receive(CanvasNotifierMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
      log.info("Received <{}>", message);

      try {
         // ack the message
         channel.basicAck(deliveryTag, false);

         // do the message stuff!
         notificationMessageHandler.handleMessage(message);
      } catch (IOException e) {
         log.error("unable to ack the message from the queue", e);
      }
   }

}
