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
import java.util.Objects;
import java.util.Optional;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.transform.EntityLocatorMap;

/**
 * Also available to the server layer - although some methods should not be
 * called from there
 *
 * @author nick@alcina.cc
 *
 */
public class EntityLayerObjects {
	// pre-registry
	public static EntityLayerObjects get() {
		Optional<EntityLayerObjects> optional = Registry
				.optional(EntityLayerObjects.class);
		if (optional.isPresent()) {
			return optional.get();
		}
		EntityLayerObjects objects = new EntityLayerObjects();
		Registry.register().singleton(EntityLayerObjects.class, objects);
		return objects;
	}

	private Logger metricLogger;

	private Logger persistentLogger;

	private File dataFolder;

	private Registry servletLayerRegistry;

	private ClassLoader servletLayerClassLoader;

	/**
	 * the instance used by the server layer when acting as a client to the ejb
	 * layer. Note - this must be set on webapp startup
	 */
	private ClientInstance serverAsClientInstance;

	private EntityLocatorMap serverAsClientInstanceEntityLocatorMap;

	private EntityLayerObjects() {
		super();
	}

	public File getDataFolder() {
		return dataFolder;
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

	public EntityLocatorMap getServerAsClientInstanceEntityLocatorMap() {
		if (serverAsClientInstanceEntityLocatorMap == null) {
			synchronized (this) {
				if (serverAsClientInstanceEntityLocatorMap == null) {
					this.serverAsClientInstanceEntityLocatorMap = new EntityLocatorMap();
				}
			}
		}
		return this.serverAsClientInstanceEntityLocatorMap;
	}

	public ClassLoader getServletLayerClassLoader() {
		return this.servletLayerClassLoader;
	}

	public Registry getServletLayerRegistry() {
		return this.servletLayerRegistry;
	}

	public boolean isForeignClientInstance(ClientInstance clientInstance) {
		return !Objects.equals(clientInstance, getServerAsClientInstance());
	}

	public void setDataFolder(File dataFolder) {
		this.dataFolder = dataFolder;
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
