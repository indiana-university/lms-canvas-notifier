package edu.iu.uits.lms.canvasnotifier.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "canvasnotifier")
@Getter
@Setter
public class ToolConfig {
   private String version;
   private String env;
   private String canvasNotifierQueueName;
   private String[] batchNotificationEmail;
}
