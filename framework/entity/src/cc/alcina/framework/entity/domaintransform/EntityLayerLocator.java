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

package cc.alcina.framework.entity.domaintransform;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;


/**
 * Also available to the server layer - although some methods should not be
 * called from there
 * 
 * @author nick@alcina.cc
 * 
 */
public class EntityLayerLocator {
	private EntityLayerLocator() {
		super();
	}

	private static EntityLayerLocator theInstance;

	public static EntityLayerLocator get() {
		if (theInstance == null) {
			theInstance = new EntityLayerLocator();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	private WrappedObjectProvider wrappedObjectProvider;

	public void registerWrappedObjectProvider(
			WrappedObjectProvider wrappedObjectProvider) {
		this.wrappedObjectProvider = wrappedObjectProvider;
	}

	public WrappedObjectProvider wrappedObjectProvider() {
		return wrappedObjectProvider;
	}

	private Logger metricLogger;

	public Logger getMetricLogger() {
		return this.metricLogger;
	}

	public void setMetricLogger(Logger metricLogger) {
		this.metricLogger = metricLogger;
	}

	private CommonPersistenceProvider commonPersistenceProvider;

	public void registerCommonPersistenceProvider(
			CommonPersistenceProvider commonPersistenceProvider) {
		this.commonPersistenceProvider = commonPersistenceProvider;
	}

	public CommonPersistenceProvider commonPersistenceProvider() {
		return commonPersistenceProvider;
	}

	private Logger persistentLogger;

	public void log(LogMessageType componentKey, String message) {
		this.persistentLogger.info(componentKey + " - " + message);
	}

	public void log(LogMessageType componentKey, String message,
			Throwable throwable) {
		this.persistentLogger.warn(componentKey + " - " + message + "\n"
				+ throwable.toString(), throwable);
	}

	// convenience
	public void persistentLog(String message, String componentKey) {
		commonPersistenceProvider().getCommonPersistence().log(message,
				componentKey);
	}

	public void setPersistentLogger(Logger persistentLogger) {
		this.persistentLogger = persistentLogger;
	}

	public Logger getPersistentLogger() {
		return persistentLogger;
	}
	private JPAImplementation jpaImplementation;

	public void registerJPAImplementation(JPAImplementation jpaImplementation) {
		this.jpaImplementation = jpaImplementation;
	}

	public JPAImplementation jpaImplementation() {
		return jpaImplementation;
	}
}
