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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.util.WriterAccessWriterAppender;

/**
 * 
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = RemoteActionLoggerProvider.class, implementationType = ImplementationType.SINGLETON)
public class RemoteActionLoggerProvider {
	private AtomicInteger counter = new AtomicInteger();

	public synchronized Logger createLogger(Class performerClass) {
		Logger l = makeNewLoggerInstance(performerClass.getName() + "-"
				+ counter.addAndGet(1));
		return l;
	}

	static Layout layout = new PatternLayout("%-5p [%c{1}] %m%n");

	/*
	 * note - this is never registered with log4j.Logger - so no leakys
	 */
	private Logger makeNewLoggerInstance(String name) {
		Logger remoteActionLogger = new RemoteActionLogger(name);
		return remoteActionLogger;
	}
}
