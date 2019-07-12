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

package org.opendevstack.provision.storage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.opendevstack.provision.model.AboutChangesData;
import org.opendevstack.provision.model.OpenProjectData;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Store the project in the injected storage path Saves the raw JSON data
     *
     * @param project
     * @return
     * @throws IOException
     */
    @Override
    public String storeProject(OpenProjectData project)
            throws IOException
    {
        if (project == null || project.projectKey == null
                || project.projectKey.trim().length() == 0)
        {
            throw new IOException(
                    "Can't store invalid, null or no key project");
        }
        return writeFile(project, null);
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
    public Map<String, OpenProjectData> listProjectHistory()
    {
        HashMap<String, OpenProjectData> history = new HashMap<>();
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

                        ProjectData temp = null;
                        
                        try 
                        {
                            temp = MAPPER.readValue(project,
                                    ProjectData.class);
                        } catch (Exception MapperEx) {
                            // legacy :)
                        }

                        OpenProjectData data = null;

                        if (temp != null && temp.key != null)
                        {
                            data = ProjectData.toOpenProjectData(
                                    (ProjectData) temp);
                            logger.debug("Project {} is in legacy format, upgrading.",
                                data.projectKey);
                        } else
                        {
                            data = MAPPER.readValue(project,
                                    OpenProjectData.class);
                            logger.debug("Project {} is in std format",
                                    data.projectKey);
                        }
                        data.physicalLocation = file
                                .getAbsolutePath();

                        history.put(dt.format(targetDt), data);
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
    public OpenProjectData getProject(String id)
    {
        if (id == null)
        {
            return null;
        }
        Collection<OpenProjectData> allProjects = listProjectHistory()
                .values();

        for (OpenProjectData project : allProjects)
        {
            if (project.projectKey.equalsIgnoreCase(id))
            {
                logger.debug("found project with id {} - returning",
                        project.projectKey);
                return project;
            }
        }
        logger.debug("Could not find project with id {}", id);
        return null;
    }

    @Override
    public boolean updateStoredProject(OpenProjectData projectNew)
            throws IOException
    {
        if (projectNew == null || projectNew.projectKey == null
                || projectNew.projectKey.trim().length() == 0)
        {
            throw new IOException(
                    "Can't update invalid, null or no key project");
        }

        OpenProjectData data = getProject(projectNew.projectKey);

        if (data == null)
        {
            return false;
        }

        try 
        {
            logger.debug("Updating existing project {} @ {}",
                    data.projectKey, data.physicalLocation);
            writeFile(projectNew, data.physicalLocation);
            return true;
        } catch (IOException ex)
        {
            logger.error("Unable to read/write files", ex);
            return false;
        }
    }

    @Value("${project.storage.local}")
    public void setLocalStoragePath(String localStoragePath)
    {
        this.localStoragePath = localStoragePath +
                File.separator;
    }

    public String getLocalStoragePath()
    {
        return this.localStoragePath;
    }

    String writeFile (OpenProjectData project, String fileName) 
        throws IOException
    {
        if (fileName == null) 
        {
            LocalDateTime dateTime = LocalDateTime.now();
            fileName = String.format(FILE_PATH_PATTERN,
                    localStoragePath,
                    dateTime.format(DateTimeFormatter
                            .ofPattern("yyyyMMddHHmmss")),
                    project.projectKey);
        }
        ObjectWriter ow = new ObjectMapper().writer()
                .withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(project);
        try (FileWriter file = new FileWriter(fileName, false))
        {
            file.write(json);
            file.close();
            logger.debug(
                    "Successfully copied project {} to {}",
                    project.projectKey, fileName);
            logger.debug("JSON Object: {}", json);
            
            fileName = new File(fileName).getAbsolutePath();
            project.physicalLocation = fileName;
            return fileName;
        }
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
