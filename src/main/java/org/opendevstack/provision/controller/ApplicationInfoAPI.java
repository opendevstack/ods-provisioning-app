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
package org.opendevstack.provision.controller;

/** @author Sebastian Titakis */
public interface ApplicationInfoAPI extends API {

  String ENDPOINT_APPINFO = "appinfo";

  String ENDPOINT_ABOUT = "about";

  String ENDPOINT_HISTORY = "history";

  String APP_INFO_API_V2 = API_ROOT_V2 + "/" + ENDPOINT_APPINFO;

  String ABOUT_APP_INFO_API_V2 = APP_INFO_API_V2 + "/" + ENDPOINT_ABOUT;

  String HISTORY_APP_INFO_API_V2 = APP_INFO_API_V2 + "/" + ENDPOINT_HISTORY;
}
