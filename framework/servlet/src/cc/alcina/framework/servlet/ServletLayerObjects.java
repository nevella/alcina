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

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * 
 * @author Nick Reddel
 */
public class ServletLayerObjects {
	public static ServletLayerObjects get() {
		ServletLayerObjects singleton = Registry
				.checkSingleton(ServletLayerObjects.class);
		if (singleton == null) {
			singleton = new ServletLayerObjects();
			Registry.registerSingleton(ServletLayerObjects.class, singleton);
		}
		return singleton;
	}

	private File dataFolder;

	private File clusterDataFolder;

	private Logger metricLogger;

	private ServletLayerObjects() {
		super();
	}

	public File getClusterDataFolder() {
		return this.clusterDataFolder;
	}

	public File getDataFolder() {
		return dataFolder;
	}

	public Logger getMetricLogger() {
		return metricLogger;
	}

	public void setClusterDataFolder(File clusterDataFolder) {
		this.clusterDataFolder = clusterDataFolder;
	}

	public void setDataFolder(File dataFolder) {
		this.dataFolder = dataFolder;
	}

	public void setMetricLogger(Logger metricLogger) {
		this.metricLogger = metricLogger;
	}
}
