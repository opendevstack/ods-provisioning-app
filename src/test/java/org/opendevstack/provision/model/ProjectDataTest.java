/* Copyright 2018 the original author or authors.
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

package org.opendevstack.provision.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Test Project data equals / hashcode
 * @author utschig
 *
 */
public class ProjectDataTest {

	@Test
	public void testEquals() {
		ProjectData data = new ProjectData();
		data.key = "key";
		data.name = "name";
		
		ProjectData dataCheck = new ProjectData();
		dataCheck.key = "key";
		dataCheck.name = "name";

		assertNotEquals(null, dataCheck);

		assertEquals(dataCheck, data);
		
		data.name = null;
		assertNotEquals(dataCheck, data);
		
		dataCheck.name = null;
		assertEquals(dataCheck, data);
	}

	
	@Test
	public void testHashcode() {
		List<ProjectData> dataL = new ArrayList<>();
		
		ProjectData data = new ProjectData();
		data.key = "key";
		data.name = "name";
		
		dataL.add(data);
		dataL.add(data);
		
		assertTrue(dataL.contains(data));
		
		ProjectData dataCheck = new ProjectData();
		dataCheck.key = "key";
		dataCheck.name = "name";
		
		dataL.add(dataCheck);

		assertEquals(3, dataL.size());
		
		data.name = null;
		dataL.add(data);

		assertEquals(4, dataL.size());
		assertTrue(dataL.contains(data));
	}
	
}
