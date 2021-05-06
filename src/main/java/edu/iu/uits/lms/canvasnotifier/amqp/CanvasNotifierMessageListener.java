package edu.iu.uits.lms.canvasnotifier.amqp;

import edu.iu.uits.lms.canvasnotifier.handler.NotificationMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@RabbitListener(queues = "${canvasnotifier.canvasNotifierQueueName}")
@Component
@Profile("!batch")
@Slf4j
public class CanvasNotifierMessageListener {

   @Autowired
   NotificationMessageHandler notificationMessageHandler;

   @RabbitHandler
   public void receive(CanvasNotifierMessage message) {
      log.info("Received <{}>", message);

      // do the message stuff!
      notificationMessageHandler.handleMessage(message);
   }

}
