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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import cc.alcina.framework.entity.util.WriterAccessWriterAppender;


/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class RemoteActionLoggerProvider {
	class ClassAndThreadToken {
		private final Class clazz;

		private Thread thread;

		public ClassAndThreadToken(Class clazz) {
			this.clazz = clazz;
			this.thread = Thread.currentThread();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ClassAndThreadToken) {
				ClassAndThreadToken token = (ClassAndThreadToken) obj;
				return token.thread.equals(thread) && token.clazz == clazz;
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return thread.hashCode() ^ clazz.hashCode();
		}
	}

	int logCounter;

	Map<ClassAndThreadToken, Logger> runningLoggers = new HashMap<ClassAndThreadToken, Logger>();

	Map<Class, List<Logger>> finishedLoggers = new HashMap<Class, List<Logger>>();

	public Logger getLogger(Class clazz) {
		ClassAndThreadToken token = new ClassAndThreadToken(clazz);
		if (runningLoggers.containsKey(token)) {
			return runningLoggers.get(token);
		}
		if (finishedLoggers.containsKey(clazz)
				&& finishedLoggers.get(clazz).size() > 0) {
			Logger logger = finishedLoggers.get(clazz).remove(0);
			resetAppenders(logger);
		}
		 Logger l = makeNewLoggerInstance(clazz.getName() + "-" + logCounter++);
		 runningLoggers.put(token,l);
		 return l;
	}

	public static void resetAppenders(Logger logger) {
		logger.removeAllAppenders();
		WriterAppender wa = new WriterAccessWriterAppender();
		wa.setWriter(new StringWriter());
		wa.setLayout(l);
		wa.setName(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY);
		logger.addAppender(wa);
	}

	public String closeLogger(Class clazz) {
		ClassAndThreadToken token = new ClassAndThreadToken(clazz);
		if (runningLoggers.containsKey(token)) {
			Logger logger = runningLoggers.remove(token);
			if (!finishedLoggers.containsKey(clazz)) {
				finishedLoggers.put(clazz, new ArrayList<Logger>());
			}
			finishedLoggers.get(clazz).add(logger);
			StringWriter writerAccess = (StringWriter) ((WriterAccessWriterAppender) logger
					.getAppender(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY)).getWriterAccess();
			return writerAccess.toString();
		} else {
			return null;
		}
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
