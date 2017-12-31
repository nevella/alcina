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
package cc.alcina.framework.entity.entityaccess;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import cc.alcina.framework.entity.logic.EntityLayerUtils;

/**
 * 
 * @author Nick Reddel
 */
public class DbAppender extends AppenderSkeleton {
	public DbAppender(Layout l) {
		setLayout(l);
	}

	public void close() {
	}

	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {
		String renderedMessage = event.getRenderedMessage();
		String[] split = renderedMessage.split(" - ", 2);
		ThrowableInformation ti = event.getThrowableInformation();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		if (ti != null && ti.getThrowable() != null) {
			ti.getThrowable().printStackTrace(pw);
		}
		if (split.length == 2) {
			EntityLayerUtils.persistentLog(
					String.format("%s\n%s", split[1], sw.toString()), split[0]);
		} else {
			EntityLayerUtils.persistentLog(
					String.format("%s\n%s", renderedMessage, sw.toString()),
					"Unknown exception type");
		}
	}
}