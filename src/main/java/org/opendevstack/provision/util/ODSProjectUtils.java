package org.opendevstack.provision.util;

import java.util.HashMap;
import java.util.Map;

import org.opendevstack.provision.adapter.IServiceAdapter;
import org.opendevstack.provision.adapter.IServiceAdapter.PERMISSION;
import org.opendevstack.provision.model.ProjectData;

import com.google.common.base.Preconditions;

public class ODSProjectUtils 
{

	public Map<IServiceAdapter.PERMISSION, String> extractProjectPermissions
		(ProjectData project) 
	{
		Preconditions.checkNotNull(project, "Project cannot be null");
		if (!project.createpermissionset)
		{
			return null;
		}
		
		Map<PERMISSION, String> permissions = new HashMap<>();
		permissions.put(PERMISSION.PROJECT_ADMIN, project.admin);
		permissions.put(PERMISSION.PROJECT_ADMIN_GROUP, project.adminGroup);
		permissions.put(PERMISSION.PROJECT_USER_GROUP, project.userGroup);
		permissions.put(PERMISSION.PROJECT_READONLY_GROUP, project.readonlyGroup);

		return permissions;
	}
	
}
