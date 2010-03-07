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

import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;


/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class ServerLayerLocator {
	private ServerLayerLocator() {
		super();
	}

	private static ServerLayerLocator theInstance;

	public static ServerLayerLocator get() {
		if (theInstance == null) {
			theInstance = new ServerLayerLocator();
		}
		return theInstance;
	}

	private File dataFolder;

	public void registerDataFolder(File dataFolder) {
		this.dataFolder = dataFolder;
	}

	public File dataFolder() {
		return dataFolder;
	}

	private Logger serverLogger;

	public void registerLogger(Logger serverLogger) {
		this.serverLogger = serverLogger;
	}

	public Logger serverLogger() {
		return serverLogger;
	}

	public void appShutdown() {
		theInstance = null;
	}

	private RemoteActionLoggerProvider remoteActionLoggerProvider;

	public void registerRemoteActionLoggerProvider(
			RemoteActionLoggerProvider remoteActionLoggerProvider) {
		this.remoteActionLoggerProvider = remoteActionLoggerProvider;
	}

	public RemoteActionLoggerProvider remoteActionLoggerProvider() {
		return remoteActionLoggerProvider;
	}

	private CommonPersistenceProvider commonPersistenceProvider;

	public void registerCommonPersistenceProvider(
			CommonPersistenceProvider commonPersistenceProvider) {
		this.commonPersistenceProvider = commonPersistenceProvider;
	}

	public CommonPersistenceProvider commonPersistenceProvider() {
		return commonPersistenceProvider;
	}
	private Logger metricLogger;

	public void registerMetricLogger(Logger metricLogger) {
		this.metricLogger = metricLogger;
	}

	public Logger metricLogger() {
		return metricLogger;
	}
	
	private CommonRemoteServletProvider commonRemoteServletProvider;

	public void registerCommonRemoteServletProvider(CommonRemoteServletProvider commonRemoteServletProvider) {
		this.commonRemoteServletProvider = commonRemoteServletProvider;
	}

	public CommonRemoteServletProvider commonRemoteServletProvider() {
		return commonRemoteServletProvider;
	}
}
