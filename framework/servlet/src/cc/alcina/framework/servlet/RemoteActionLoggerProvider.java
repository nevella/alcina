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

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.util.WriterAccessWriterAppender;

/**
 * 
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = RemoteActionLoggerProvider.class, implementationType = ImplementationType.SINGLETON)
public class RemoteActionLoggerProvider {
	class ClassAndThreadToken {
		private final Class clazz;

		private long threadId;

		public ClassAndThreadToken(Class clazz) {
			this.clazz = clazz;
			this.threadId = Thread.currentThread().getId();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ClassAndThreadToken) {
				ClassAndThreadToken token = (ClassAndThreadToken) obj;
				return token.threadId == threadId && token.clazz == clazz;
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return Long.valueOf(threadId).hashCode() ^ clazz.hashCode();
		}
	}

	int logCounter;

	Map<ClassAndThreadToken, Logger> runningLoggers = new HashMap<ClassAndThreadToken, Logger>();

	Map<ClassAndThreadToken, Logger> finishedLoggers = new HashMap<ClassAndThreadToken, Logger>();

	public synchronized Logger getLogger(Class clazz) {
		ClassAndThreadToken token = new ClassAndThreadToken(clazz);
		if (finishedLoggers.containsKey(token)) {
			Logger logger = finishedLoggers.get(token);
			resetAppenders(logger);
			finishedLoggers.remove(token);
		}
		if (runningLoggers.containsKey(token)) {
			return runningLoggers.get(token);
		}
		Logger l = makeNewLoggerInstance(clazz.getName() + "-" + logCounter++);
		runningLoggers.put(token, l);
		return l;
	}

	public synchronized void clearAllThreadLoggers() {
		long id = Thread.currentThread().getId();
		Iterator<Entry<ClassAndThreadToken, Logger>> itr = runningLoggers
				.entrySet().iterator();
		for (; itr.hasNext();) {
			Entry<ClassAndThreadToken, Logger> entry = itr.next();
			if (entry.getKey().threadId == id) {
				resetAppenders(entry.getValue());
				itr.remove();
			}
		}
		itr = finishedLoggers.entrySet().iterator();
		for (; itr.hasNext();) {
			Entry<ClassAndThreadToken, Logger> entry = itr.next();
			if (entry.getKey().threadId == id) {
				resetAppenders(entry.getValue());
				itr.remove();
			}
		}
	}

	public static void resetAppenders(Logger logger) {
		logger.removeAllAppenders();
		WriterAppender wa = new WriterAccessWriterAppender();
		wa.setWriter(new StringWriter());
		wa.setLayout(l);
		wa.setName(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY);
		logger.addAppender(wa);
	}

	public synchronized String resetLogBuffer(Class clazz) throws Exception {
		ClassAndThreadToken token = new ClassAndThreadToken(clazz);
		if (runningLoggers.containsKey(token)) {
			Logger logger = runningLoggers.remove(token);
			StringWriter writerAccess = (StringWriter) ((WriterAccessWriterAppender) logger
					.getAppender(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY))
					.getWriterAccess();
			((WriterAccessWriterAppender) logger
					.getAppender(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY))
					.resetWriter();
			return writerAccess.toString();
		}
		return null;
	}

	public synchronized String closeLogger(Class clazz) {
		ClassAndThreadToken token = new ClassAndThreadToken(clazz);
		if (runningLoggers.containsKey(token)) {
			Logger logger = runningLoggers.remove(token);
			finishedLoggers.put(token, logger);
			StringWriter writerAccess = (StringWriter) ((WriterAccessWriterAppender) logger
					.getAppender(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY))
					.getWriterAccess();
			return writerAccess.toString();
		} else {
			return null;
		}
	}

	public synchronized int getLoggerBufferLength(Class clazz) {
		ClassAndThreadToken token = new ClassAndThreadToken(clazz);
		if (runningLoggers.containsKey(token)) {
			Logger logger = runningLoggers.get(token);
			return getLoggerBufferLength(logger);
		}
		return -1;
	}

	public static int getLoggerBufferLength(Logger logger) {
		StringWriter writerAccess = (StringWriter) ((WriterAccessWriterAppender) logger
				.getAppender(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY))
				.getWriterAccess();
		return writerAccess.getBuffer().length();
	}

	public synchronized String getLoggerBufferSubstring(Class clazz, int from,
			int to) {
		ClassAndThreadToken token = new ClassAndThreadToken(clazz);
		if (runningLoggers.containsKey(token)) {
			Logger logger = runningLoggers.get(token);
			return getLoggerBufferSubstring(logger, from, to);
		}
		return null;
	}

	public static String getLoggerBufferSubstring(Logger logger, int from,
			int to) {
		StringWriter writerAccess = (StringWriter) ((WriterAccessWriterAppender) logger
				.getAppender(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY))
				.getWriterAccess();
		return writerAccess.getBuffer().substring(from, to);
	}

	static Layout l = new PatternLayout("%-5p [%c{1}] %m%n");

	public Logger makeNewLoggerInstance(String name) {
		Logger remoteActionLogger = Logger.getLogger(name);
		resetAppenders(remoteActionLogger);
		return remoteActionLogger;
	}

	public Logger getInfoLevelLogger(Class clazz) {
		Logger logger = getLogger(clazz);
		logger.setLevel(Level.INFO);
		logger.setAdditivity(false);
		logger.addAppender(new ConsoleAppender(l));
		return logger;
	}
}
