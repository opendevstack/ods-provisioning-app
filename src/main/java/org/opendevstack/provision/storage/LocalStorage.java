/*
 * Copyright 2018 the original author or authors.
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

package org.opendevstack.provision.storage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.opendevstack.provision.model.AboutChangesData;
import org.opendevstack.provision.model.ProjectData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Class handling local storage of JSON files for a simple historization
 *
 * @author Torsten Jaeschke
 */

@Component
public class LocalStorage implements IStorage
{

    private static final Logger logger = LoggerFactory
            .getLogger(LocalStorage.class);

    private String localStoragePath;

    private static String FILE_PATH_PATTERN = "%s%s-%s.txt";

    private static String ABOUT_CHANGES_LOGFILENAME = "about_change_log";

    /**
     * Store the project in the injected storage path Saves the raw JSON data
     *
     * @param project
     * @return
     * @throws IOException
     */
    @Override
    public String storeProject(ProjectData project) throws IOException
    {
        if (project == null || project.key == null
                || project.key.trim().length() == 0)
        {
            throw new IOException(
                    "Can't store invalid, null or no key project");
        }
        LocalDateTime dateTime = LocalDateTime.now();
        ObjectWriter ow = new ObjectMapper().writer()
                .withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(project);
        String filePath = String
                .format(FILE_PATH_PATTERN, localStoragePath,
                        dateTime.format(DateTimeFormatter
                                .ofPattern("yyyyMMddHHmmss")),
                        project.key);
        try (FileWriter file = new FileWriter(filePath, false))
        {
            file.write(json);
            logger.debug(
                    "Successfully Copied JSON Object to File...");
            logger.debug("JSON Object: {}", json);
            return filePath;
        }

    }

    /**
     * Load all files from the defined storage path and map them to the
     * corresponding object
     *
     * TODO Fix comparator, because it does not sort in the correct order
     *
     * @return a desc date sorted list of projects
     */
    @Override
    public Map<String, ProjectData> listProjectHistory()
    {
        HashMap<String, ProjectData> history = new HashMap<>();
        try
        {
            File folder = new File(localStoragePath);
            if (folder.isDirectory())
            {
                File[] fileList = folder.listFiles();
                Arrays.sort(fileList,
                        LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
                for (File file : fileList)
                {
                    if (!file.isDirectory() && !file.isHidden()
                            && file.getName().endsWith(".txt"))
                    {
                        logger.debug("File {}:", file);
                        DateTimeFormatter formatter = DateTimeFormatter
                                .ofPattern("yyyyMMddHHmmss");
                        DateTimeFormatter targetDt = DateTimeFormatter
                                .ofPattern("dd/MM/yyyy HH:mm:ss");
                        String project = FileUtils
                                .readFileToString(file);
                        LocalDateTime dt = LocalDateTime.parse(
                                file.getName().substring(0, 14),
                                formatter);
                        history.put(dt.format(targetDt),
                                new ObjectMapper().readValue(project,
                                        ProjectData.class));
                    }
                }
            }
        } catch (IOException ex)
        {
            logger.error("Unable to read history", ex);
        }

        return history;
    }

    @Override
    public ProjectData getProject(String id)
    {
        ProjectData project = null;
        if (id == null)
        {
            return project;
        }
        try
        {
            File folder = new File(localStoragePath);
            if (folder.isDirectory())
            {
                File[] fileList = folder.listFiles();
                Arrays.sort(fileList,
                        LastModifiedFileComparator.LASTMODIFIED_REVERSE);
                for (File file : fileList)
                {
                    if (!file.isDirectory() && !file.isHidden()
                            && file.getName().endsWith(".txt"))
                    {
                        project = new ObjectMapper().readValue(
                                FileUtils.readFileToString(file),
                                ProjectData.class);
                        if (project.key.equalsIgnoreCase(id))
                        {
                            return project;
                        }
                    }
                }
            }
        } catch (IOException ex)
        {
            logger.error("Unable to read history", ex);
        }
        return project;
    }

    @Override
    public boolean updateStoredProject(ProjectData projectNew)
            throws IOException
    {
        if (projectNew == null || projectNew.key == null
                || projectNew.key.trim().length() == 0)
        {
            throw new IOException(
                    "Can't update invalid, null or no key project");
        }

        try
        {
            String filePath = "";
            File folder = new File(localStoragePath);
            if (folder.isDirectory())
            {
                File[] fileList = folder.listFiles();
                for (File file : fileList)
                {
                    if (!file.isDirectory() && !file.isHidden()
                            && file.getName().endsWith(".txt"))
                    {
                        ProjectData project = new ObjectMapper()
                                .readValue(
                                        FileUtils.readFileToString(
                                                file),
                                        ProjectData.class);
                        if (projectNew.key
                                .equalsIgnoreCase(project.key))
                        {
                            filePath = file.getPath();
                        }
                    }
                }
                ObjectWriter ow = new ObjectMapper().writer()
                        .withDefaultPrettyPrinter();
                String json = ow.writeValueAsString(projectNew);
                try (FileWriter file = new FileWriter(filePath,
                        false))
                {
                    file.write(json);
                    logger.debug(
                            "Successfully updated JSON Object in file...");
                    logger.debug("JSON Object: {}", json);
                    return true;
                }
            }
        } catch (IOException ex)
        {
            logger.error("Unable to read files", ex);
        }
        return false;
    }

    @Value("${project.storage.local}")
    public void setLocalStoragePath(String localStoragePath)
    {
        this.localStoragePath = localStoragePath;
    }

    /**
     * Test Impl only
     */
    @Override
    public String storeAboutChangesData(AboutChangesData aboutData)
            throws IOException
    {
        ObjectWriter ow = new ObjectMapper().writer()
                .withDefaultPrettyPrinter();
        StringWriter writer = new StringWriter();
        ow.writeValue(writer, aboutData);
        return writer.getBuffer().toString();
    }

    @Override
    public AboutChangesData listAboutChangesData()
    {
        InputStream aboutChangesStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(ABOUT_CHANGES_LOGFILENAME);

        try
        {
            return (AboutChangesData) new ObjectMapper().readValue(
                    aboutChangesStream, AboutChangesData.class);
        } catch (Exception e)
        {
            logger.error("Could not deserialize content: "
                    + e.getMessage());
            return null;
        } finally
        {
            try
            {
                aboutChangesStream.close();
            } catch (IOException ioE)
            {
                logger.error(ioE.toString());
            }
        }
    }
}
