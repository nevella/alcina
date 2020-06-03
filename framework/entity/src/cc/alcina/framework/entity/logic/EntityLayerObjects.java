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

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * Also available to the server layer - although some methods should not be
 * called from there
 * 
 * @author nick@alcina.cc
 * 
 */
public class EntityLayerObjects {
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

	private Logger persistentLogger;

	private File dataFolder;

	private Registry servletLayerRegistry;

	private ClassLoader servletLayerClassLoader;

	private ClassLoader entityLayerClassLoader;

	/**
	 * the instance used by the server layer when acting as a client to the ejb
	 * layer. Note - this must be set on webapp startup
	 */
	private ClientInstance serverAsClientInstance;

	private EntityLayerObjects() {
		super();
	}

	public File getDataFolder() {
		return dataFolder;
	}

	public ClassLoader getEntityLayerClassLoader() {
		return this.entityLayerClassLoader;
	}

	public Logger getMetricLogger() {
		return this.metricLogger;
	}

	public Logger getPersistentLogger() {
		return persistentLogger;
	}

	public ClientInstance getServerAsClientInstance() {
		return this.serverAsClientInstance;
	}

	public ClassLoader getServletLayerClassLoader() {
		return this.servletLayerClassLoader;
	}

	public Registry getServletLayerRegistry() {
		return this.servletLayerRegistry;
	}

	public void setDataFolder(File dataFolder) {
		this.dataFolder = dataFolder;
	}

	public void setEntityLayerClassLoader(ClassLoader entityLayerClassLoader) {
		this.entityLayerClassLoader = entityLayerClassLoader;
	}

	public void setMetricLogger(Logger metricLogger) {
		this.metricLogger = metricLogger;
	}

	public void setPersistentLogger(Logger persistentLogger) {
		this.persistentLogger = persistentLogger;
	}

	public void
			setServerAsClientInstance(ClientInstance serverAsClientInstance) {
		this.serverAsClientInstance = serverAsClientInstance;
	}

	public void
			setServletLayerClassLoader(ClassLoader servletLayerClassLoader) {
		this.servletLayerClassLoader = servletLayerClassLoader;
	}

	public void setServletLayerRegistry(Registry servletLayerRegistry) {
		this.servletLayerRegistry = servletLayerRegistry;
	}
}
