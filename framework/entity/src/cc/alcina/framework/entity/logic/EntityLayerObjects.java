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
package cc.alcina.framework.entity.logic;

import java.io.File;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * Also available to the server layer - although some methods should not be
 * called from there
 * 
 * @author nick@alcina.cc
 * 
 */
public class EntityLayerObjects {
	private EntityLayerObjects() {
		super();
	}

	public static EntityLayerObjects get() {
		EntityLayerObjects singleton = Registry
				.checkSingleton(EntityLayerObjects.class);
		if (singleton == null) {
			singleton = new EntityLayerObjects();
			Registry.registerSingleton(EntityLayerObjects.class, singleton);
		}
		return singleton;
	}

	private Logger metricLogger;

	public Logger getMetricLogger() {
		return this.metricLogger;
	}

	public void setMetricLogger(Logger metricLogger) {
		this.metricLogger = metricLogger;
	}

	private Logger persistentLogger;

	public void setPersistentLogger(Logger persistentLogger) {
		this.persistentLogger = persistentLogger;
	}

	public Logger getPersistentLogger() {
		return persistentLogger;
	}

	private File dataFolder;

	public void setDataFolder(File dataFolder) {
		this.dataFolder = dataFolder;
	}

	public File getDataFolder() {
		return dataFolder;
	}
}
