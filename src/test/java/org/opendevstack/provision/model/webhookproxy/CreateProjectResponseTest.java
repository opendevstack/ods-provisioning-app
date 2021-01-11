package org.opendevstack.provision.model.webhookproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
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

    assertEquals("Build", actual.getKind());
    assertEquals("build.openshift.io/v1", actual.getApiVersion());
    Metadata metadata = actual.getMetadata();
    assertEquals("ods-corejob-create-projects-tst04-production-1", metadata.getName());
    assertEquals("prov-cd", metadata.getNamespace());
    assertEquals("913911", metadata.getResourceVersion());
    assertEquals(
        "/apis/build.openshift.io/v1/namespaces/prov-cd/buildconfigs/ods-corejob-create-projects-tst04-production-1/instantiate",
        metadata.getSelfLink());
    assertEquals("b0cb02d7-11a9-11ea-9997-08002750261b", metadata.getUid());

    Annotations annotations = metadata.getAnnotations();
    assertEquals("ods-corejob-create-projects-tst04-production", annotations.getBuildConfigName());
    assertEquals("1", annotations.getBuildNumber());

    Labels labels = metadata.getLabels();
    assertEquals("ods-corejob-create-projects-tst04-production", labels.getBuildconfig());
    assertEquals("ods-corejob-create-projects-tst04-production", labels.getBuildConfigName());

    Status status = actual.getStatus();
    assertEquals("New", status.getPhase());
    Config config = status.getConfig();
    assertEquals("BuildConfig", config.getKind());
    assertEquals("ods-corejob-create-projects-tst04-production", config.getName());
    assertEquals("prov-cd", config.getNamespace());
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
