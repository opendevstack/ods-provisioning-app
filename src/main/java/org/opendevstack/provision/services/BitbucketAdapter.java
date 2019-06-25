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

package org.opendevstack.provision.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.opendevstack.provision.adapter.ISCMAdapter;
import org.opendevstack.provision.authentication.CustomAuthenticationManager;
import org.opendevstack.provision.model.BitbucketData;
import org.opendevstack.provision.model.ProjectData;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsService;

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
    CrowdUserDetailsService crowdUserDetailsService;

    @Autowired
    CustomAuthenticationManager manager;

    private static final String PROJECT_PATTERN = "%s%s/projects";

    private static final String COMPONENT_ID_KEY = "component_id";

    private static final String ID_GROUPS = "groups";
    private static final String ID_USERS = "users";

    public enum PROJECT_PERMISSIONS
    {
        PROJECT_ADMIN, PROJECT_WRITE, PROJECT_READ
    }

    public ProjectData createSCMProjectForODSProject(
            ProjectData project, String crowdCookieValue)
            throws IOException
    {
        BitbucketData data = callCreateProjectApi(project,
                crowdCookieValue);

        project.bitbucketUrl = data.getLinks().get("self").get(0)
                .getHref();
        return project;
    }

    @SuppressWarnings("squid:S3776")
    public ProjectData createComponentRepositoriesForODSProject(
            ProjectData project, String crowdCookieValue)
            throws IOException
    {

        CrowdUserDetails userDetails = (CrowdUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        logger.debug("Creating quickstartProjects");

        Map<String, Map<String, List<Link>>> repoLinks = new HashMap<>();
        List<Map<String, String>> newOptions = new ArrayList<>();
        if (project.quickstart != null)
        {

            logger.debug("new quickstarters: {}",
                    project.quickstart.size());

            for (Map<String, String> option : project.quickstart)
            {
                logger.debug(
                        "create repo for quickstarter: {}  in {}",
                        option.get(COMPONENT_ID_KEY), project.key);

                String repoName = (String.format("%s-%s", project.key,
                        option.get(COMPONENT_ID_KEY))).toLowerCase()
                                .replace('_', '-');
                Repository repo = new Repository();
                repo.setName(repoName);

                if (project.createpermissionset)
                {
                    repo.setAdminGroup(project.adminGroup);
                    repo.setUserGroup(project.userGroup);
                } else
                {
                    repo.setAdminGroup(this.defaultUserGroup);
                    repo.setUserGroup(this.defaultUserGroup);
                }

                try
                {
                    RepositoryData result = callCreateRepoApi(
                            project.key, repo, crowdCookieValue);
                    createWebHooksForRepository(result, project,
                            crowdCookieValue);
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
                                    userDetails.getUsername(),
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
            project.quickstart = newOptions;
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

    public ProjectData createAuxiliaryRepositoriesForODSProject(
            ProjectData project, String crowdCookieValue,
            String[] auxiliaryRepos)
    {
        Map<String, Map<String, List<Link>>> repoLinks = new HashMap<>();
        for (String name : auxiliaryRepos)
        {
            Repository repo = new Repository();
            String repoName = String.format("%s-%s",
                    project.key.toLowerCase(), name);
            repo.setName(repoName);

            if (project.createpermissionset)
            {
                repo.setAdminGroup(project.adminGroup);
                repo.setUserGroup(project.userGroup);
            } else
            {
                repo.setAdminGroup(this.globalKeyuserRoleName);
                repo.setUserGroup(this.defaultUserGroup);
            }

            try
            {
                RepositoryData result = callCreateRepoApi(project.key,
                        repo, crowdCookieValue);
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
            ProjectData project, String crowdCookie)
    {

        // projectOpenshiftJenkinsWebhookProxyNamePattern is e.g.
        // "webhook-proxy-%s-cd%s"
        String webhookProxyHost = String.format(
                projectOpenshiftJenkinsWebhookProxyNamePattern,
                project.key.toLowerCase(),
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
                buildBasePath(), project.key, repo.getSlug());

        try
        {
            client.callHttp(url, webhook, crowdCookie, false,
                    RestClient.HTTP_VERB.POST, Webhook.class);
            logger.info("created hook: {}", webhook.getUrl());
        } catch (IOException ex)
        {
            logger.error("Error in webhook call", ex);
        }
    }

    protected BitbucketData callCreateProjectApi(ProjectData project,
            String crowdCookieValue) throws IOException
    {

        BitbucketProject bbProject = createBitbucketProject(project);

        BitbucketData projectData = client.callHttp(buildBasePath(),
                bbProject, crowdCookieValue, false,
                RestClient.HTTP_VERB.POST, BitbucketData.class);

        if (project.createpermissionset)
        {
            setProjectPermissions(projectData, ID_GROUPS,
                    globalKeyuserRoleName, crowdCookieValue,
                    PROJECT_PERMISSIONS.PROJECT_ADMIN);
            setProjectPermissions(projectData, ID_GROUPS,
                    project.adminGroup, crowdCookieValue,
                    PROJECT_PERMISSIONS.PROJECT_ADMIN);
            setProjectPermissions(projectData, ID_GROUPS,
                    project.userGroup, crowdCookieValue,
                    PROJECT_PERMISSIONS.PROJECT_WRITE);
            setProjectPermissions(projectData, ID_GROUPS,
                    project.readonlyGroup, crowdCookieValue,
                    PROJECT_PERMISSIONS.PROJECT_READ);
        } else
        {
            setProjectPermissions(projectData, ID_GROUPS,
                    defaultUserGroup, crowdCookieValue,
                    PROJECT_PERMISSIONS.PROJECT_WRITE);
        }
        // set the technical user in any case
        setProjectPermissions(projectData, ID_USERS, technicalUser,
                crowdCookieValue, PROJECT_PERMISSIONS.PROJECT_WRITE);

        return projectData;
    }

    protected RepositoryData callCreateRepoApi(String projectKey,
            Repository repo, String crowdCookieValue)
            throws IOException
    {
        String path = String.format("%s/%s/repos", buildBasePath(),
                projectKey);

        RepositoryData data = client.callHttp(path, repo,
                crowdCookieValue, false, RestClient.HTTP_VERB.POST,
                RepositoryData.class);

        setRepositoryPermissions(data, projectKey, ID_GROUPS,
                repo.getUserGroup(), crowdCookieValue);
        setRepositoryPermissions(data, projectKey, ID_USERS,
                technicalUser, crowdCookieValue);

        return data;
    }

    protected void setProjectPermissions(BitbucketData data,
            String pathFragment, String groupOrUser,
            String crowdCookieValue, PROJECT_PERMISSIONS rights)
            throws IOException
    {
        String basePath = buildBasePath();
        String url = String.format("%s/%s/permissions/%s", basePath,
                data.getKey(), pathFragment);
        // http://192.168.56.31:7990/rest/api/1.0/projects/{projectKey}/permissions/groups
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        // utschig - allow group to create new repos (rather than just read / write)
        urlBuilder.addQueryParameter("permission", rights.toString());
        urlBuilder.addQueryParameter("name", groupOrUser);
        client.callHttp(urlBuilder.toString(), null, crowdCookieValue,
                true, RestClient.HTTP_VERB.PUT, String.class);
    }

    protected void setRepositoryPermissions(RepositoryData data,
            String key, String userOrGroup, String groupOrUser,
            String crowdCookieValue) throws IOException
    {
        String basePath = buildBasePath();
        String url = String.format("%s/%s/repos/%s/permissions/%s",
                basePath, key, data.getSlug(), userOrGroup);

        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        // allow people to modify settings (webhooks)
        urlBuilder.addQueryParameter("permission", "REPO_ADMIN");
        urlBuilder.addQueryParameter("name", groupOrUser);

        client.callHttp(urlBuilder.toString(), null, crowdCookieValue, true,
                RestClient.HTTP_VERB.PUT, String.class);
    }

    protected String buildBasePath()
    {
        return String.format(PROJECT_PATTERN, bitbucketUri,
                bitbucketApiPath);
    }

    public static BitbucketProject createBitbucketProject(
            ProjectData jiraProject)
    {
        BitbucketProject project = new BitbucketProject();
        project.setKey(jiraProject.key);
        project.setName(jiraProject.name);
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
        return buildBasePath();
    }

    @Override
    public Map<String, String> getProjects(String filter,
            String crowdCookieValue)
    {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }
}
