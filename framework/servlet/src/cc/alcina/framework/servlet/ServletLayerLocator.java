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
import cc.alcina.framework.servlet.servlet.NaiveTransformPersistenceQueue;
import cc.alcina.framework.servlet.servlet.TransformPersistenceQueue;

/**
 * 
 * @author Nick Reddel
 */
public class ServletLayerLocator {
	private ServletLayerLocator() {
		super();
	}

	private static ServletLayerLocator theInstance;

	public static ServletLayerLocator get() {
		if (theInstance == null) {
			theInstance = new ServletLayerLocator();
		}
		return theInstance;
	}

	private TransformPersistenceQueue transformPersistenceQueue = new NaiveTransformPersistenceQueue();

	public void registerTransformPersistenceQueue(
			TransformPersistenceQueue transformPersistenceQueue) {
		this.transformPersistenceQueue = transformPersistenceQueue;
	}

	public TransformPersistenceQueue transformPersistenceQueue() {
		return transformPersistenceQueue;
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

	public void registerCommonRemoteServletProvider(
			CommonRemoteServletProvider commonRemoteServletProvider) {
		this.commonRemoteServletProvider = commonRemoteServletProvider;
	}

	public CommonRemoteServletProvider commonRemoteServletProvider() {
		return commonRemoteServletProvider;
	}
}
