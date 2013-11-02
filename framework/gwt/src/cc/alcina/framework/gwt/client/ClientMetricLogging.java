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
package cc.alcina.framework.gwt.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public class ClientMetricLogging {
	private Map<String, Long> metricStartTimes;

	private static ClientMetricLogging INSTANCE = new ClientMetricLogging();

	public static ClientMetricLogging get() {
		return INSTANCE;
	}

	private boolean muted;

	public boolean isMuted() {
		return this.muted;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	private ClientMetricLogging() {
		reset();
	}

	private Map<String, Long> sum;

	private Map<String, Long> averageCount;

	private HashSet<String> terminated;

	public void end(String key) {
		end(key, "");
	}

	public void end(String key, String extraInfo) {
		end(key, extraInfo, false);
	}

	public void end(String key, String extraInfo, boolean ignoreNotRunning) {
		key = keyWithParents(key, true);
		if (terminated.contains(key) && ignoreNotRunning) {
			return;
		}
		terminated.add(key);
		if (!metricStartTimes.containsKey(key)) {
			System.out.println("Warning - metric end without start - " + key);
			return;
		}
		long elapsed = System.currentTimeMillis() - metricStartTimes.get(key);
		String message = CommonUtils.formatJ("[Metric] %s - %s ms%s", key,
				elapsed, CommonUtils.isNullOrEmpty(extraInfo) ? "" : " - "
						+ extraInfo);
		if (!isMuted()) {
			Registry.impl(ClientNotifications.class).log(message);
		}
		if (!averageCount.containsKey(key)) {
			averageCount.put(key, 0L);
			sum.put(key, 0L);
		}
		averageCount.put(key, averageCount.get(key) + 1);
		sum.put(key, sum.get(key) + elapsed);
	}

	public void reset() {
		terminated = new HashSet<String>();
		metricStartTimes = new LinkedHashMap<String, Long>();
		sum = new HashMap<String, Long>();
		averageCount = new HashMap<String, Long>();
		keyToKeyWithParents = new HashMap<String, String>();
	}

	public void start(String key) {
		key = keyWithParents(key, false);
		metricStartTimes.put(key, System.currentTimeMillis());
		terminated.remove(key);
	}

	private Map<String, String> keyToKeyWithParents;

	private String keyWithParents(String key, boolean end) {
		if (end) {
			return keyToKeyWithParents.get(key);
		}
		String withParents = "";
		for (String parentKey : metricStartTimes.keySet()) {
			if (!terminated.contains(parentKey)) {
				withParents = parentKey + "/";
			}
		}
		withParents += key;
		keyToKeyWithParents.put(key, withParents);
		return withParents;
	}

	public void endIfRunning(String key) {
		end(key, "", true);
	}

	public void endAndStart(String keyToEnd, String keyToStart) {
		end(keyToEnd);
		start(keyToStart);
	}
}
