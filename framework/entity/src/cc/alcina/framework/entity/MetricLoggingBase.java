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
package cc.alcina.framework.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 *
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class MetricLoggingBase {
	final static Logger logger = LoggerFactory.getLogger(MetricLogging.class);

	private Map<String, Long> metricStart;

	private Map<String, Long> metricStartThreadIds;

	private Map<String, Long> ticksSum;

	private Map<String, Long> ticks;

	private Map<String, Long> sum;

	private Map<String, Long> averageCount;

	private Long thisLoggerThreadId = null;

	private Set<String> terminated;

	private Map<String, String> keyToKeyWithParents;

	private boolean muted = false;

	protected MetricLoggingBase() {
	}

	public void average(String key) {
		if (averageCount.containsKey(key)) {
			String message = Ax.format("Metric: %s avg %sms", key,
					sum.get(key) / averageCount.get(key));
			System.out.println(message);
		}
	}

	public void end(String key) {
		end(key, "");
	}

	public void end(String key, Logger logger) {
		end(key, null, logger);
	}

	public void end(String key, String extraInfo) {
		end(key, extraInfo, null);
	}

	public void endTicks(String key) {
		long cn = System.nanoTime();
		key = keyWithParents(key, false);
		if (!ticksSum.containsKey(key)) {
			ticksSum.put(key, 0L);
		}
		if (!ticks.containsKey(key)) {
			ticks.put(key, cn);
		}
		ticksSum.put(key, ticksSum.get(key) + (cn - ticks.get(key)));
	}

	public boolean isMuted() {
		return muted;
	}

	public synchronized void reset() {
		if (isMuted()) {
			System.out.println("Unmuting muted metric thread");
			Thread.dumpStack();
		}
		setMuted(false);
		metricStart = new LinkedHashMap<String, Long>();
		metricStartThreadIds = new LinkedHashMap<String, Long>();
		sum = new HashMap<String, Long>();
		averageCount = new HashMap<String, Long>();
		keyToKeyWithParents = new HashMap<String, String>();
		ticks = new HashMap<String, Long>();
		ticksSum = new HashMap<String, Long>();
		terminated = new HashSet<String>();
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	public synchronized void start(String key) {
		key = keyWithParents(key, false);
		metricStart.put(key, System.currentTimeMillis());
		metricStartThreadIds.put(key, getCurrentThreadId());
	}

	public void startTicks(String key) {
		key = keyWithParents(key, false);
		ticks.put(key, System.nanoTime());
	}

	private synchronized void end(String key, String extraInfo,
			Logger overrideLogger) {
		key = keyWithParents(key, true);
		if (!metricStart.containsKey(key) && !ticksSum.containsKey(key)) {
			System.out.println("Warning - metric end without start - " + key);
			return;
		}
		long delta = ticksSum.containsKey(key) ? ticksSum.get(key) / 1000000
				: System.currentTimeMillis() - metricStart.get(key);
		ticksSum.remove(key);
		String units = "ms";
		String message = Ax.format("Metric: %s - %s %s%s", key, delta, units,
				CommonUtils.isNullOrEmpty(extraInfo) ? "" : " - " + extraInfo);
		if (!muted) {
			Logger out = overrideLogger == null ? logger : overrideLogger;
			out.debug(message);
		}
		if (!averageCount.containsKey(key)) {
			averageCount.put(key, 0L);
			sum.put(key, 0L);
		}
		averageCount.put(key, averageCount.get(key) + 1);
		sum.put(key, sum.get(key) + delta);
		terminated.add(key);
	}

	private Long getCurrentThreadId() {
		return Thread.currentThread().getId();
	}

	private synchronized String keyWithParents(String key, boolean end) {
		if (end) {
			return keyToKeyWithParents.get(key);
		}
		String withParents = "";
		for (String parentKey : metricStart.keySet()) {
			Long tid = metricStartThreadIds.get(parentKey);
			if (!terminated.contains(parentKey)
					&& (tid.equals(thisLoggerThreadId)
							|| tid.equals(getCurrentThreadId()))) {
				withParents = parentKey + "/";
			}
		}
		withParents += key;
		keyToKeyWithParents.put(key, withParents);
		return withParents;
	}
}
