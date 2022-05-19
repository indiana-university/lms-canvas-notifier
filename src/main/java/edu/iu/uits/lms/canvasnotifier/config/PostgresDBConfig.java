package edu.iu.uits.lms.canvasnotifier.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration("canvasnotifierDbConfig")
@EnableJpaRepositories(
        entityManagerFactoryRef = "canvasnotifierEntityMgrFactory",
        transactionManagerRef = "canvasnotifierTransactionMgr",
        basePackages = {"edu.iu.uits.lms.canvasnotifier.repository"})
@EnableTransactionManagement
public class PostgresDBConfig {

    @Bean(name = "canvasnotifierDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "canvasnotifierEntityMgrFactory")
    public LocalContainerEntityManagerFactoryBean canvasnotifierEntityMgrFactory(
            final EntityManagerFactoryBuilder builder,
            @Qualifier("canvasnotifierDataSource") final DataSource dataSource) {
        // dynamically setting up the hibernate properties for each of the datasource.
        final Map<String, String> properties = new HashMap<>();
        return builder
                .dataSource(dataSource)
                .properties(properties)
                .packages("edu.iu.uits.lms.canvasnotifier.model")
                .build();
    }

    @Bean(name = "canvasnotifierTransactionMgr")
    public PlatformTransactionManager canvasnotifierTransactionMgr(
            @Qualifier("canvasnotifierEntityMgrFactory") final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
