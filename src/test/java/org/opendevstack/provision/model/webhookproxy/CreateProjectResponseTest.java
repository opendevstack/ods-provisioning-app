package org.opendevstack.provision.model.webhookproxy;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
public class CreateProjectResponseTest {

  @Autowired private JacksonTester jacksonTester;

  @Autowired private ObjectMapper objectMapper;

  private static File testDataDir = new File("src/test/resources/webhookproxy/");

  @Test
  public void parsesCreateProjectResponse() throws Exception {
    String jsonFile = "sample-create-project-response.json";
    CreateProjectResponse actual = readTestData(jsonFile, CreateProjectResponse.class);

    assertThat(actual.getKind()).isEqualTo("Build");
    assertThat(actual.getApiVersion()).isEqualTo("build.openshift.io/v1");
    Metadata metadata = actual.getMetadata();
    assertThat(metadata.getName()).isEqualTo("ods-corejob-create-projects-tst04-production-1");
    assertThat(metadata.getNamespace()).isEqualTo("prov-cd");
    assertThat(metadata.getResourceVersion()).isEqualTo("913911");
    assertThat(metadata.getSelfLink())
        .isEqualTo(
            "/apis/build.openshift.io/v1/namespaces/prov-cd/buildconfigs/ods-corejob-create-projects-tst04-production-1/instantiate");
    assertThat(metadata.getUid()).isEqualTo("b0cb02d7-11a9-11ea-9997-08002750261b");

    Annotations annotations = metadata.getAnnotations();
    assertThat(annotations.getBuildConfigName())
        .isEqualTo("ods-corejob-create-projects-tst04-production");
    assertThat(annotations.getBuildNumber()).isEqualTo("1");

    Labels labels = metadata.getLabels();
    assertThat(labels.getBuildconfig()).isEqualTo("ods-corejob-create-projects-tst04-production");
    assertThat(labels.getBuildConfigName())
        .isEqualTo("ods-corejob-create-projects-tst04-production");

    Status status = actual.getStatus();
    assertThat(status.getPhase()).isEqualTo("New");
    Config config = status.getConfig();
    assertThat(config.getKind()).isEqualTo("BuildConfig");
    assertThat(config.getName()).isEqualTo("ods-corejob-create-projects-tst04-production");
    assertThat(config.getNamespace()).isEqualTo("prov-cd");
  }

  private <T> T readTestData(String name, Class<T> returnType) throws Exception {
    return new ObjectMapper().readValue(findTestFile(name), returnType);
  }

  private File findTestFile(String fileName) throws IOException {
    Preconditions.checkNotNull(fileName, "File cannot be null");
    if (!fileName.endsWith(".json")) {
      fileName = fileName + ".json";
    }
    File dataFile = new File(testDataDir, fileName);
    if (!dataFile.exists()) {
      throw new IOException("Cannot find testfile with name:" + dataFile.getName());
    }
    return dataFile;
  }
}
