package edu.iu.uits.lms.canvasnotifier.services.swagger;

import edu.iu.uits.lms.lti.swagger.AbstractSwaggerCustomTest;
import edu.iu.uits.lms.lti.swagger.AbstractSwaggerDisabledTest;
import edu.iu.uits.lms.lti.swagger.AbstractSwaggerEmbeddedToolTest;
import edu.iu.uits.lms.lti.swagger.AbstractSwaggerUiCustomTest;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;

import static edu.iu.uits.lms.email.EmailConstants.EMAILREST_PROFILE;
import static edu.iu.uits.lms.iuonly.IuCustomConstants.IUCUSTOMREST_PROFILE;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;


@NestedTestConfiguration(INHERIT)
public class SwaggerSuiteTest {
   @Nested
    @SpringBootTest(classes = {NotifierSwaggerConfig.class})
    public class SwaggerCustomTest extends AbstractSwaggerCustomTest {

    }

    @Nested
    @SpringBootTest(classes = {NotifierSwaggerConfig.class})
    public class SwaggerDisabledTest extends AbstractSwaggerDisabledTest {

    }

    @Nested
    @SpringBootTest(classes = {NotifierSwaggerConfig.class})
    @ActiveProfiles({IUCUSTOMREST_PROFILE, EMAILREST_PROFILE})
    public class SwaggerEmbeddedToolTest extends AbstractSwaggerEmbeddedToolTest {

    }

    @Nested
    @SpringBootTest(classes = {NotifierSwaggerConfig.class})
    public class SwaggerUiCustomTest extends AbstractSwaggerUiCustomTest {

    }
}