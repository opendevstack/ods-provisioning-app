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

package org.opendevstack.provision.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.opendevstack.provision.adapter.IODSAuthnzAdapter;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.model.BitbucketData;
import org.opendevstack.provision.model.OpenProjectData;
import org.opendevstack.provision.model.RepositoryData;
import org.opendevstack.provision.model.bitbucket.BitbucketProject;
import org.opendevstack.provision.model.bitbucket.Link;
import org.opendevstack.provision.model.bitbucket.Repository;
import org.opendevstack.provision.model.bitbucket.Webhook;
import org.opendevstack.provision.util.GitUrlWrangler;
import org.opendevstack.provision.util.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import okhttp3.HttpUrl;

/**
 * Service to interact with Bitbucket and to create projects and repositories
 *
 * @author Brokmeier, Pascal
 */

@Service
public class BitbucketAdapter implements ISCMAdapter
{

    private static final Logger logger = LoggerFactory
            .getLogger(BitbucketAdapter.class);

    @Autowired
    RundeckAdapter rundeckAdapter;

    @Value("${bitbucket.api.path}")
    private String bitbucketApiPath;

    @Value("${bitbucket.uri}")
    private String bitbucketUri;

    @Value("${atlassian.domain}")
    private String confluenceDomain;

    @Value("${openshift.apps.basedomain}")
    private String projectOpenshiftBaseDomain;

    @Value("${openshift.jenkins.webhookproxy.name.pattern}")
    private String projectOpenshiftJenkinsWebhookProxyNamePattern;

    @Value("${openshift.jenkins.trigger.secret}")
    private String projectOpenshiftJenkinsTriggerSecret;

    @Value("${bitbucket.default.user.group}")
    private String defaultUserGroup;

    @Value("${bitbucket.technical.user}")
    private String technicalUser;

    @Value("${global.keyuser.role.name}")
    private String globalKeyuserRoleName;

    @Autowired
    RestClient client;

    @Autowired
    IODSAuthnzAdapter manager;

    private static final String PROJECT_PATTERN = "%s%s/projects";
    private static final String COMPONENT_ID_KEY = "component_id";

    private static final String ID_GROUPS = "groups";
    private static final String ID_USERS = "users";

    public enum PROJECT_PERMISSIONS
    {
        PROJECT_ADMIN, PROJECT_WRITE, PROJECT_READ
    }

    public OpenProjectData createSCMProjectForODSProject(
            OpenProjectData project)
            throws IOException
    {
        BitbucketData data = callCreateProjectApi(project);

        project.scmvcsUrl = data.getLinks().get("self").get(0)
                .getHref();
        return project;
    }

    @SuppressWarnings("squid:S3776")
    @Override
    public OpenProjectData createComponentRepositoriesForODSProject(
            OpenProjectData project)
            throws IOException
    {
        Map<String, Map<String, List<Link>>> repoLinks = new HashMap<>();
        List<Map<String, String>> newOptions = new ArrayList<>();
        if (project.quickstarters != null)
        {

            logger.debug("Project {} - new quickstarters: {}",
                    project.projectKey,
                    project.quickstarters.size());

            for (Map<String, String> option : project.quickstarters)
            {
                logger.debug(
                        "Creating repo for quickstarters: {}  in {}",
                        option.get(COMPONENT_ID_KEY), project.projectKey);

                String repoName = (String.format("%s-%s", project.projectKey,
                        option.get(COMPONENT_ID_KEY))).toLowerCase()
                                .replace('_', '-');
                Repository repo = new Repository();
                repo.setName(repoName);

                if (project.specialPermissionSet)
                {
                    repo.setAdminGroup(project.projectAdminGroup);
                    repo.setUserGroup(project.projectUserGroup);
                } else
                {
                    repo.setAdminGroup(this.defaultUserGroup);
                    repo.setUserGroup(this.defaultUserGroup);
                }

                try
                {
                    RepositoryData result = callCreateRepoApi(
                            project.projectKey, repo);
                    createWebHooksForRepository(result, project);
                    Map<String, List<Link>> links = result.getLinks();
                    if (links != null)
                    {
                        repoLinks.put(result.getName(),
                                result.getLinks());
                        for (Link repoLink : links.get("clone"))
                        {
                            String href = repoLink.getHref();
                            GitUrlWrangler gitUrlWrangler = new GitUrlWrangler();
                            href = gitUrlWrangler.buildGitUrl(
                                    manager.getUserName(),
                                    technicalUser, href);
                            option.put(
                                    String.format("git_url_%s",
                                            repoLink.getName()),
                                    href);
                        }
                        newOptions.add(option);
                    }
                } catch (IOException ex)
                {
                    logger.error(
                            "Error in creating repo: "
                                    + option.get(COMPONENT_ID_KEY),
                            ex);
                    throw new IOException("Error in creating repo: "
                            + option.get(COMPONENT_ID_KEY) + "\n"
                            + "details: " + ex.getMessage());
                }
            }
            project.quickstarters = newOptions;
        }

        if (project.repositories != null)
        {
            project.repositories.putAll(repoLinks);
        } else
        {
            project.repositories = repoLinks;
        }
        return project;
    }

    @Override
    public OpenProjectData createAuxiliaryRepositoriesForODSProject(
            OpenProjectData project,
            String[] auxiliaryRepos)
    {
        Map<String, Map<String, List<Link>>> repoLinks = new HashMap<>();
        for (String name : auxiliaryRepos)
        {
            Repository repo = new Repository();
            String repoName = String.format("%s-%s",
                    project.projectKey.toLowerCase(), name);
            repo.setName(repoName);

            if (project.specialPermissionSet)
            {
                repo.setAdminGroup(project.projectAdminGroup);
                repo.setUserGroup(project.projectUserGroup);
            } else
            {
                repo.setAdminGroup(this.globalKeyuserRoleName);
                repo.setUserGroup(this.defaultUserGroup);
            }

            try
            {
                RepositoryData result = callCreateRepoApi(project.projectKey,
                        repo);
                repoLinks.put(result.getName(), result.getLinks());
            } catch (IOException ex)
            {
                logger.error("Error in creating auxiliary repo", ex);
            }
        }
        if (project.repositories != null)
        {
            project.repositories.putAll(repoLinks);
        } else
        {
            project.repositories = repoLinks;
        }
        return project;
    }

    // Create webhook for CI (using webhook proxy)
    protected void createWebHooksForRepository(RepositoryData repo,
            OpenProjectData project)
    {

        // projectOpenshiftJenkinsWebhookProxyNamePattern is e.g.
        // "webhook-proxy-%s-cd%s"
        String webhookProxyHost = String.format(
                projectOpenshiftJenkinsWebhookProxyNamePattern,
                project.projectKey.toLowerCase(),
                projectOpenshiftBaseDomain);
        String webhookProxyUrl = "https://" + webhookProxyHost
                + "?trigger_secret="
                + projectOpenshiftJenkinsTriggerSecret;
        Webhook webhook = new Webhook();
        webhook.setName("Jenkins");
        webhook.setActive(true);
        webhook.setUrl(webhookProxyUrl);
        List<String> events = new ArrayList<>();
        events.add("repo:refs_changed");
        events.add("pr:merged");
        events.add("pr:declined");
        webhook.setEvents(events);

        // projects/CLE200/repos/cle200-be-node-express/webhooks
        String url = String.format("%s/%s/repos/%s/webhooks",
                getAdapterApiUri(), project.projectKey, repo.getSlug());

        try
        {
            client.callHttp(url, webhook, false,
                    RestClient.HTTP_VERB.POST, Webhook.class);
            logger.info("created hook: {}", webhook.getUrl());
        } catch (IOException ex)
        {
            logger.error("Error in webhook call", ex);
        }
    }

    protected BitbucketData callCreateProjectApi(OpenProjectData project) throws IOException
    {
        BitbucketProject bbProject = createBitbucketProject(project);

        BitbucketData projectData = client.callHttp(getAdapterApiUri(),
                bbProject, false,
                RestClient.HTTP_VERB.POST, BitbucketData.class);

        if (project.specialPermissionSet)
        {
            setProjectPermissions(projectData, ID_GROUPS,
                    globalKeyuserRoleName,
                    PROJECT_PERMISSIONS.PROJECT_ADMIN);
            setProjectPermissions(projectData, ID_GROUPS,
                    project.projectAdminGroup,
                    PROJECT_PERMISSIONS.PROJECT_ADMIN);
            setProjectPermissions(projectData, ID_GROUPS,
                    project.projectUserGroup,
                    PROJECT_PERMISSIONS.PROJECT_WRITE);
            setProjectPermissions(projectData, ID_GROUPS,
                    project.projectReadonlyGroup,
                    PROJECT_PERMISSIONS.PROJECT_READ);
        } else
        {
            setProjectPermissions(projectData, ID_GROUPS,
                    defaultUserGroup,
                    PROJECT_PERMISSIONS.PROJECT_WRITE);
        }
        // set the technical user in any case
        setProjectPermissions(projectData, ID_USERS, technicalUser,
                PROJECT_PERMISSIONS.PROJECT_WRITE);

        return projectData;
    }

    protected RepositoryData callCreateRepoApi(String projectKey,
            Repository repo) throws IOException
    {
        String path = String.format("%s/%s/repos", getAdapterApiUri(),
                projectKey);

        RepositoryData data = client.callHttp(path, repo,
                false, RestClient.HTTP_VERB.POST,
                RepositoryData.class);

        setRepositoryPermissions(data, projectKey, ID_GROUPS,
                repo.getUserGroup());
        setRepositoryPermissions(data, projectKey, ID_USERS,
                technicalUser);

        return data;
    }

    protected void setProjectPermissions(BitbucketData data,
            String pathFragment, String groupOrUser,
            PROJECT_PERMISSIONS rights)
            throws IOException
    {
        String basePath = getAdapterApiUri();
        String url = String.format("%s/%s/permissions/%s", basePath,
                data.getKey(), pathFragment);
        // http://192.168.56.31:7990/rest/api/1.0/projects/{projectKey}/permissions/groups
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        // utschig - allow group to create new repos (rather than just read / write)
        urlBuilder.addQueryParameter("permission", rights.toString());
        urlBuilder.addQueryParameter("name", groupOrUser);
        client.callHttp(urlBuilder.toString(), null, 
                true, RestClient.HTTP_VERB.PUT, String.class);
    }

    protected void setRepositoryPermissions(RepositoryData data,
            String key, String userOrGroup, String groupOrUser) 
                    throws IOException
    {
        String basePath = getAdapterApiUri();
        String url = String.format("%s/%s/repos/%s/permissions/%s",
                basePath, key, data.getSlug(), userOrGroup);

        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        // allow people to modify settings (webhooks)
        urlBuilder.addQueryParameter("permission", "REPO_ADMIN");
        urlBuilder.addQueryParameter("name", groupOrUser);

        client.callHttp(urlBuilder.toString(), null, true,
                RestClient.HTTP_VERB.PUT, String.class);
    }

    static BitbucketProject createBitbucketProject(
            OpenProjectData jiraProject)
    {
        BitbucketProject project = new BitbucketProject();
        project.setKey(jiraProject.projectKey);
        project.setName(jiraProject.projectName);
        project.setDescription((jiraProject.description != null)
                ? jiraProject.description
                : "");
        return project;
    }

    /**
     * Get the bitbucket http endpoint
     * 
     * @return the endpoint - cant be null
     */
    @Override
    public String getAdapterApiUri()
    {
        return String.format(PROJECT_PATTERN, bitbucketUri,
                bitbucketApiPath);
    }

    @Override
    public Map<String, String> getProjects(String filter)
    {
        throw new NotImplementedException();
    }
}
