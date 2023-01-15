package org.metacubic.enixmeta.config;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.containers.KafkaContainer;
import tech.jhipster.config.JHipsterConstants;

public class TestContainersSpringContextCustomizerFactory implements ContextCustomizerFactory {

    private Logger log = LoggerFactory.getLogger(TestContainersSpringContextCustomizerFactory.class);

    private static KafkaTestContainer kafkaBean;
    private static ElasticsearchTestContainer elasticsearchBean;
    private static SqlTestContainer devTestContainer;
    private static SqlTestContainer prodTestContainer;

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        return (context, mergedConfig) -> {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            TestPropertyValues testValues = TestPropertyValues.empty();
            EmbeddedSQL sqlAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedSQL.class);
            if (null != sqlAnnotation) {
                log.debug("detected the EmbeddedSQL annotation on class {}", testClass.getName());
                log.info("Warming up the sql database");
                if (
                    Arrays
                        .asList(context.getEnvironment().getActiveProfiles())
                        .contains("test" + JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
                ) {
                    if (null == devTestContainer) {
                        try {
                            Class<? extends SqlTestContainer> containerClass = (Class<? extends SqlTestContainer>) Class.forName(
                                this.getClass().getPackageName() + ".PostgreSqlTestContainer"
                            );
                            devTestContainer = beanFactory.createBean(containerClass);
                            beanFactory.registerSingleton(containerClass.getName(), devTestContainer);
                            // ((DefaultListableBeanFactory)beanFactory).registerDisposableBean(containerClass.getName(), devTestContainer);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    testValues =
                        testValues.and(
                            "spring.r2dbc.url=" + devTestContainer.getTestContainer().getJdbcUrl().replace("jdbc", "r2dbc") + ""
                        );
                    testValues = testValues.and("spring.r2dbc.username=" + devTestContainer.getTestContainer().getUsername());
                    testValues = testValues.and("spring.r2dbc.password=" + devTestContainer.getTestContainer().getPassword());
                    testValues = testValues.and("spring.liquibase.url=" + devTestContainer.getTestContainer().getJdbcUrl() + "");
                }
                if (
                    Arrays
                        .asList(context.getEnvironment().getActiveProfiles())
                        .contains("test" + JHipsterConstants.SPRING_PROFILE_PRODUCTION)
                ) {
                    if (null == prodTestContainer) {
                        try {
                            Class<? extends SqlTestContainer> containerClass = (Class<? extends SqlTestContainer>) Class.forName(
                                this.getClass().getPackageName() + ".PostgreSqlTestContainer"
                            );
                            prodTestContainer = beanFactory.createBean(containerClass);
                            beanFactory.registerSingleton(containerClass.getName(), prodTestContainer);
                            // ((DefaultListableBeanFactory)beanFactory).registerDisposableBean(containerClass.getName(), prodTestContainer);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    testValues =
                        testValues.and(
                            "spring.r2dbc.url=" + prodTestContainer.getTestContainer().getJdbcUrl().replace("jdbc", "r2dbc") + ""
                        );
                    testValues = testValues.and("spring.r2dbc.username=" + prodTestContainer.getTestContainer().getUsername());
                    testValues = testValues.and("spring.r2dbc.password=" + prodTestContainer.getTestContainer().getPassword());
                    testValues = testValues.and("spring.liquibase.url=" + prodTestContainer.getTestContainer().getJdbcUrl() + "");
                }
            }
            EmbeddedKafka kafkaAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedKafka.class);
            if (null != kafkaAnnotation) {
                log.debug("detected the EmbeddedKafka annotation on class {}", testClass.getName());
                log.info("Warming up the kafka broker");
                if (null == kafkaBean) {
                    kafkaBean = beanFactory.createBean(KafkaTestContainer.class);
                    beanFactory.registerSingleton(KafkaTestContainer.class.getName(), kafkaBean);
                    // ((DefaultListableBeanFactory)beanFactory).registerDisposableBean(KafkaTestContainer.class.getName(), kafkaBean);
                }
                testValues =
                    testValues.and(
                        "spring.cloud.stream.kafka.binder.brokers=" +
                        kafkaBean.getKafkaContainer().getHost() +
                        ':' +
                        kafkaBean.getKafkaContainer().getMappedPort(KafkaContainer.KAFKA_PORT)
                    );
            }
            EmbeddedElasticsearch elasticsearchAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                testClass,
                EmbeddedElasticsearch.class
            );
            if (null != elasticsearchAnnotation) {
                log.debug("detected the EmbeddedElasticsearch annotation on class {}", testClass.getName());
                log.info("Warming up the elastic database");
                if (null == elasticsearchBean) {
                    elasticsearchBean = beanFactory.createBean(ElasticsearchTestContainer.class);
                    beanFactory.registerSingleton(ElasticsearchTestContainer.class.getName(), elasticsearchBean);
                    // ((DefaultListableBeanFactory)beanFactory).registerDisposableBean(ElasticsearchTestContainer.class.getName(), elasticsearchBean);
                }
                testValues =
                    testValues.and(
                        "spring.elasticsearch.uris=http://" + elasticsearchBean.getElasticsearchContainer().getHttpHostAddress()
                    );
            }
            testValues.applyTo(context);
        };
    }
}
