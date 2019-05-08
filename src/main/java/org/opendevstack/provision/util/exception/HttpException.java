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
package org.opendevstack.provision.util.exception;

import java.io.IOException;

/**
 * Simple exception to wrap an HTTP non 2xx
 * @author utschig
 */
public class HttpException extends IOException 
{
	private final int httpResponseCode;

	public HttpException(int code, String message) {
		super(message);
		httpResponseCode = code;
	}
	
	public int getResponseCode () {
		return httpResponseCode;
	}
	
	@Override
	public String getMessage () 
	{
		return super.getMessage() + " Errorcode: " + httpResponseCode;
	}
}
