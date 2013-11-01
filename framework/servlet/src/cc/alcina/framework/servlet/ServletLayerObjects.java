/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.servlet;

import java.io.File;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

/**
 * 
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ServletLayerObjects.class, implementationType = ImplementationType.SINGLETON)
public class ServletLayerObjects {
	private File dataFolder;

	public void setDataFolder(File dataFolder) {
		this.dataFolder = dataFolder;
	}

	public File getDataFolder() {
		return dataFolder;
	}

	private Logger metricLogger;

	public void setMetricLogger(Logger metricLogger) {
		this.metricLogger = metricLogger;
	}

	public Logger getMetricLogger() {
		return metricLogger;
	}
}
