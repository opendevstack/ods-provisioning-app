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

package org.opendevstack.provision.adapter;

import java.util.Map;

/**
 * Base interface for all service adapters
 * @author utschig
 */
public interface IServiceAdapter
{
    /**
     * Permission definitions
     */
    public static enum PERMISSION
    {
        PROJECT_ADMIN, PROJECT_ADMIN_GROUP, PROJECT_USER_GROUP, PROJECT_READONLY_GROUP
    }

    /**
     * Project template key enum
     */
    public static enum PROJECT_TEMPLATE
    {
        TEMPLATE_KEY, TEMPLATE_TYPE_KEY
    }

    /**
     * Return a list of project per adapter
     * @param filter a filter (e.g. key), can be null
     * @param crowdCookieValue the SSO cookie value
     * @return a map with project key and name, never null, 
     * but potentially empty
     */
    public Map<String, String> getProjects(String filter);

    /**
     * Get the adapter's used rest / api URI
     * @return the URI
     */
    public String getAdapterApiUri();
}