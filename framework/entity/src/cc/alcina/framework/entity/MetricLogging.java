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

import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.PatternLayout;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.util.WriterAccessWriterAppender;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class MetricLogging {
	public static Logger metricLogger = Logger.getLogger(MetricLogging.class);

	public static final String METRIC_MARKER = "jpmtx1:";

	private Logger perThreadLogger;

	private Map<String, Long> metricStart;

	private Map<String, Long> metricStartThreadIds;

	private Map<String, Boolean> sysout;

	private Map<String, Long> ticksSum;

	private Map<String, Long> ticks;

	private Map<String, Long> sum;

	private Map<String, Long> averageCount;

	private Long parentThreadId;

	private Long thisLoggerThreadId = null;

	private Set<String> terminated;

	private Map<String, String> keyToKeyWithParents;

	private WriterAccessWriterAppender wa;

	public static Set<Class> sysoutClasses = new LinkedHashSet<Class>();

	private static Map<Long, WeakReference<MetricLogging>> threadIdLoggingMap = new HashMap<Long, WeakReference<MetricLogging>>();

	public static final String LOG_CONTEXT_THREAD_ID = "threadId";

	public static boolean useLog4j = true;

	private boolean muted = false;

	public static final Layout METRIC_LAYOUT = new PatternLayout("[%d] ["
			+ METRIC_MARKER + "%c{1}:%X{threadId}] %m%n");

	private static ThreadLocal TL = new ThreadLocal() {
		protected synchronized Object initialValue() {
			MetricLogging metricLogging = new MetricLogging();
			metricLogging.reset();
			return metricLogging;
		}
	};

	public static boolean muteLowPriority = true;

	public static MetricLogging get() {
		MetricLogging m = (MetricLogging) TL.get();
		long tid = Thread.currentThread().getId();
		threadIdLoggingMap.put(tid, new WeakReference(m));
		if (m.parentThreadId != null) {
			return threadIdLoggingMap.get(m.parentThreadId).get();
		} else {
			return m;
		}
	}

	public static void resetChildThreadMetricLogger(long parentThreadId) {
		MetricLogging m = (MetricLogging) TL.get();
		m.reset();
		m.parentThreadId = parentThreadId;
		if (useLog4j) {
			MDC.put(LOG_CONTEXT_THREAD_ID, parentThreadId);
		}
	}

	private MetricLogging() {
	}

	public void average(String key) {
		if (averageCount.containsKey(key)) {
			String message = CommonUtils.formatJ("Metric: %s avg %sms", key,
					sum.get(key) / averageCount.get(key));
			System.out.println(message);
		}
	}

	public void end(String key) {
		end(key, "");
	}

	public void endMem(String key) {
		System.gc();
		end(key, "", false);
	}

	public void end(String key, String extraInfo) {
		end(key, extraInfo, true);
	}

	private synchronized void end(String key, String extraInfo, boolean time) {
		key = keyWithParents(key, true);
		if (!metricStart.containsKey(key) && !ticksSum.containsKey(key)) {
			System.out.println("Warning - metric end without start - " + key);
			return;
		}
		long delta = time ? ticksSum.containsKey(key) ? ticksSum.get(key) / 1000000
				: System.currentTimeMillis() - metricStart.get(key)
				: Runtime.getRuntime().freeMemory() - metricStart.get(key);
		ticksSum.remove(key);
		String units = time ? "ms" : "bytes";
		String message = CommonUtils.formatJ("Metric: %s - %s %s%s", key,
				delta, units, CommonUtils.isNullOrEmpty(extraInfo) ? "" : " - "
						+ extraInfo);
		if (useLog4j && metricLogger != null) {
			if (!muted) {
				metricLogger.debug(message);
				perThreadLogger.info(message);
				if (sysout.containsKey(key)) {
					System.out.println(message);
					sysout.remove(key);
				}
			}
		} else {
			System.out.println(message);
		}
		if (!averageCount.containsKey(key)) {
			averageCount.put(key, 0L);
			sum.put(key, 0L);
		}
		averageCount.put(key, averageCount.get(key) + 1);
		sum.put(key, sum.get(key) + delta);
		terminated.add(key);
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

	public String getPerThreadLog() {
		return wa.getWriterAccess().toString();
	}

	public void lowPriorityEnd(String key) {
		if (!muteLowPriority) {
			end(key);
		}
	}

	public void lowPriorityStart(String key) {
		if (!muteLowPriority) {
			start(key);
		}
	}

	public void mute() {
		muted = true;
	}

	public synchronized void reset() {
		muted = false;
		if (useLog4j) {
			MDC.put(LOG_CONTEXT_THREAD_ID, getCurrentThreadId());
			perThreadLogger = Logger.getLogger(getClass().getName() + "-"
					+ getCurrentThreadId());
			perThreadLogger.removeAllAppenders();
			perThreadLogger.setAdditivity(false);
			wa = new WriterAccessWriterAppender();
			wa.setWriter(new StringWriter());
			wa.setLayout(new PatternLayout("%-5p [%c{1}] %m%n"));
			wa.setName(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY);
			perThreadLogger.addAppender(wa);
			parentThreadId = null;
			thisLoggerThreadId = getCurrentThreadId();
		}
		metricStart = new LinkedHashMap<String, Long>();
		metricStartThreadIds = new LinkedHashMap<String, Long>();
		sum = new HashMap<String, Long>();
		averageCount = new HashMap<String, Long>();
		keyToKeyWithParents = new HashMap<String, String>();
		ticks = new HashMap<String, Long>();
		ticksSum = new HashMap<String, Long>();
		sysout = new LinkedHashMap<String, Boolean>();
		terminated = new HashSet<String>();
	}

	public synchronized void start(String key) {
		key = keyWithParents(key, false);
		metricStart.put(key, System.currentTimeMillis());
		metricStartThreadIds.put(key, getCurrentThreadId());
	}

	public synchronized void startMem(String key) {
		key = keyWithParents(key, false);
		metricStart.put(key, Runtime.getRuntime().freeMemory());
		metricStartThreadIds.put(key, getCurrentThreadId());
	}

	public void startTicks(String key) {
		key = keyWithParents(key, false);
		ticks.put(key, System.nanoTime());
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
					&& (tid.equals(thisLoggerThreadId) || tid
							.equals(getCurrentThreadId()))) {
				withParents = parentKey + "/";
			}
		}
		withParents += key;
		keyToKeyWithParents.put(key, withParents);
		return withParents;
	}

	public void start(String key, Class clazz) {
		start(key);
		if (sysoutClasses.contains(clazz)) {
			key = keyWithParents(key, true);
			sysout.put(key, true);
		}
	}

	public void appShutdown() {
		sysoutClasses = null;
	}
}
