package edu.iu.uits.lms.canvasnotifier.services.swagger;

import edu.iu.uits.lms.canvasnotifier.config.SecurityConfig;
import edu.iu.uits.lms.canvasnotifier.config.SwaggerConfig;
import edu.iu.uits.lms.canvasnotifier.repository.UserRepository;
import edu.iu.uits.lms.email.config.EmailRestConfiguration;
import edu.iu.uits.lms.iuonly.config.IuCustomRestConfiguration;
import edu.iu.uits.lms.iuonly.services.CanvasDataServiceImpl;
import edu.iu.uits.lms.lti.config.LtiClientTestConfig;
import edu.iu.uits.lms.lti.config.LtiRestConfiguration;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import edu.iu.uits.lms.lti.swagger.SwaggerTestingBean;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;

import static edu.iu.uits.lms.email.EmailConstants.EMAIL_GROUP_CODE_PATH;
import static edu.iu.uits.lms.iuonly.IuCustomConstants.IUCUSTOM_GROUP_CODE_PATH;

@Import({
        SecurityConfig.class,
        SwaggerConfig.class,
        edu.iu.uits.lms.lti.config.SwaggerConfig.class,
        LtiRestConfiguration.class,
        edu.iu.uits.lms.iuonly.config.SwaggerConfig.class,
        IuCustomRestConfiguration.class,
        edu.iu.uits.lms.email.config.SwaggerConfig.class,
        EmailRestConfiguration.class,
        LtiClientTestConfig.class
})
public class NotifierSwaggerConfig {
   @MockBean
   private BufferingApplicationStartup bufferingApplicationStartup;

   @MockBean
   private DefaultInstructorRoleRepository defaultInstructorRoleRepository;

   @MockBean
   private ClientRegistrationRepository clientRegistrationRepository;

   @MockBean
   private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

   @MockBean
   private UserRepository userRepository;


   @Bean
   public SwaggerTestingBean swaggerTestingBean() {
      SwaggerTestingBean stb = new SwaggerTestingBean();

      List<String> expandedList = new ArrayList<>();
      expandedList.add(IUCUSTOM_GROUP_CODE_PATH);
      expandedList.add(EMAIL_GROUP_CODE_PATH);

      stb.setEmbeddedSwaggerToolPaths(expandedList);
      return stb;
   }

}
