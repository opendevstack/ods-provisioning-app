package org.opendevstack.provision.adapter;

import java.io.IOException;
import java.util.Map;

import org.opendevstack.provision.adapter.IServiceAdapter.PERMISSION;

public interface IBugtrackerAdapter extends IServiceAdapter 
{

	public String createBugtrackerProjectForODSProject
		(String projectKey, String projectDescription, 
			Map<PERMISSION, String> permissions, String projectType, 
			String crowdCookieValue)
		throws IOException;
	
}
