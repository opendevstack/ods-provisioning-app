package org.opendevstack.provision.util;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestDataFileReader {

  public static File TEST_DATA_FILE_DIR = new File("src/test/resources/e2e/");

  private File testDataDir;

  public TestDataFileReader(File testDataDir) {
    Preconditions.checkNotNull(testDataDir, "File cannot be null");
    this.testDataDir = testDataDir;
  }

  public String readFileContent(String name) throws IOException {
    File testFile = findTestFile(name);
    return Files.readString(testFile.toPath());
  }

  public File findTestFile(String fileName) throws IOException {
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
