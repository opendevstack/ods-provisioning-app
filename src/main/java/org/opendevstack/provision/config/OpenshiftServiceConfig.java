/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendevstack.provision.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.opendevstack.provision.services.openshift.OpenshiftClient;
import org.opendevstack.provision.services.openshift.OpenshiftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/** @author Sebastian Titakis */
@Configuration
public class OpenshiftServiceConfig {

  private static final Logger logger = LoggerFactory.getLogger(OpenshiftServiceConfig.class);

  public static final String DEFAULT_KUBERNETES_IO_SERVICEACCOUNT_TOKEN_FILE =
      "/var/run/secrets/kubernetes.io/serviceaccount/token";

  @Value(
      "${openshift.provisioning-app.service-account.file:"
          + DEFAULT_KUBERNETES_IO_SERVICEACCOUNT_TOKEN_FILE
          + "}")
  private String ocTokenResourceFile;

  @Value("${openshift.provisioning-app.service-account.token:}")
  private String ocToken;

  @Value("${openshift.api.uri}")
  private String openshiftApiUri;

  @Autowired private ResourceLoader resourceLoader;

  @Bean
  @ConditionalOnProperty(
      name = "services.openshift.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public OpenshiftService openshiftService(OpenshiftClient openshiftClient) {
    return new OpenshiftService(openshiftClient);
  }

  @Bean
  @ConditionalOnProperty(
      name = "services.openshift.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public OpenshiftClient openshiftClient() {

    if (null != ocToken && !ocToken.isEmpty()) {
      logger.info(
          "Found oc token configured in property 'openshift.provisioning-app.service-account.token'. Using it to log into to openshift! [openshift.api.uri={}]",
          openshiftApiUri);

      return create(openshiftApiUri, ocToken);

    } else if (null != ocTokenResourceFile) {

      if (!ocTokenResourceFile.equals(DEFAULT_KUBERNETES_IO_SERVICEACCOUNT_TOKEN_FILE)) {
        logger.info(
            "Found oc service account file configured in property 'openshift.provisioning-app.service-account.file'. Using it to log into to openshift! [openshift.api.uri={}, serviceAccountTokenFile={}]",
            openshiftApiUri,
            ocTokenResourceFile);
      } else {
        logger.info(
            "Using default service account file to connect to openshift! [openshift.api.uri={}, serviceAccountTokenFile={}]",
            openshiftApiUri,
            DEFAULT_KUBERNETES_IO_SERVICEACCOUNT_TOKEN_FILE);
      }

      Resource resource = resourceLoader.getResource(ocTokenResourceFile);

      if (!resource.exists()) {
        throw new RuntimeException(
            "Cannot load oc token from file because file does not exists! [file="
                + ocTokenResourceFile
                + "]");
      }

      return create(openshiftApiUri, resource);

    } else {
      throw new RuntimeException("This should never happens! Ask developers to take a look!");
    }
  }

  private OpenshiftClient create(String openshiftApiUri, Resource token) {
    Assert.notNull(token, "Parameter 'token' is null!");

    try {
      if (!token.exists()) {
        throw new RuntimeException(
            String.format(
                "File with oc token does not exists! [file=%s]!", token.getURI().toString()));
      }

      String ocToken = new String(Files.readAllBytes(Path.of(token.getURI())));

      return create(openshiftApiUri, ocToken);

    } catch (IOException ex) {
      logger.error("Failed to create openshift service!", ex);
      throw new RuntimeException(ex);
    }
  }

  private OpenshiftClient create(String openshiftApiUri, String token) {
    Assert.notNull(openshiftApiUri, "Parameter 'openshiftApiUri' is null!");
    Assert.notNull(token, "Parameter 'token' is null!");

    return new OpenshiftClient(openshiftApiUri, token);
  }
}
