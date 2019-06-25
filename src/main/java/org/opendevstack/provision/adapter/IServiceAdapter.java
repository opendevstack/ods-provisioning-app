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

package org.opendevstack.provision.adapter;

import java.util.Map;

public interface IServiceAdapter
{
    public static enum PERMISSION
    {
        PROJECT_ADMIN, PROJECT_ADMIN_GROUP, PROJECT_USER_GROUP, PROJECT_READONLY_GROUP
    }

    public static enum PROJECT_TEMPLATE
    {
        TEMPLATE_KEY, TEMPLATE_TYPE_KEY
    }

    public Map<String, String> getProjects(String filter,
            String crowdCookieValue);

    public String getAdapterApiUri();
}