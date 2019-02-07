package org.opendevstack.provision.adapter;

import java.util.Map;

public interface IServiceAdapter 
{
	public static enum PERMISSION 
	{
		PROJECT_ADMIN,
		PROJECT_ADMIN_GROUP,
		PROJECT_USER_GROUP,
		PROJECT_READONLY_GROUP
	}
	
	public Map<String, String> getProjects 
		(String filter, String crowdCookieValue); 
	
	public String getAdapterApiUri ();

}
