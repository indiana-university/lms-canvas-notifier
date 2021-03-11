package edu.iu.uits.lms.canvasnotifier.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

   @Autowired
   private ToolConfig toolConfig = null;

   @Bean(name = "canvasNotifierQueue")
   Queue canvasNotifierQueue() {
      return new Queue(toolConfig.getCanvasNotifierQueueName());
   }
}
