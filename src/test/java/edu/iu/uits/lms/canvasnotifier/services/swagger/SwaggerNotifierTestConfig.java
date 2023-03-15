package edu.iu.uits.lms.canvasnotifier.services.swagger;

import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessageListener;
import edu.iu.uits.lms.iuonly.services.CanvasDataServiceImpl;
import edu.iu.uits.lms.iuonly.services.SisServiceImpl;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class SwaggerNotifierTestConfig {
    @MockBean
    private CanvasDataServiceImpl canvasDataService;

    @MockBean
    private CanvasNotifierMessageListener canvasNotifierMessageListener;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private SisServiceImpl sisService;
}
