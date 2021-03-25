package org.opendevstack.provision.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import springfox.documentation.spring.web.plugins.Docket;

class SwaggerConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  public void whenDefaultApplicationContext_thenSwaggerUINotAvailable() {
    this.contextRunner
        .withUserConfiguration(SwaggerConfig.class)
        .run(
            context -> {
              try {
                // this raise an exception if the bean does not exists!
                Docket docket = context.getBean(Docket.class);
                Assertions.fail();
              } catch (NoSuchBeanDefinitionException ex) {
                // This is expected
              }
            });
  }

  @Test
  public void whenDocumentationPropertyIsTrue_thenSwaggerUIIsAvailable() {

    this.contextRunner
        .withPropertyValues("springfox.documentation.enabled=true")
        .withUserConfiguration(SwaggerConfig.class)
        .run(
            context -> {
              // this raise an exception if the bean does not exists!
              Docket docket = context.getBean(Docket.class);
              assertThat(docket).isNotNull(); // This is redundant
            });
  }
}
