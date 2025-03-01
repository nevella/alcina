package cc.alcina.framework.gwt.client.logic.process;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.rpc.OutOfBandMessage;

public class ProcessMetric extends Model implements ProcessObservable {
	private static final String CONTEXT_METRIC_NAME = ProcessMetric.class
			.getName() + ".CONTEXT_METRIC_NAME";

	private static Logger logger = LoggerFactory.getLogger(ProcessMetric.class);

	public static void end(Type type) {
		end(type, 0);
	}

	public static void end(Type type, int size) {
		ProcessObservers.publish(ProcessMetric.class,
				() -> new ProcessMetric(System.currentTimeMillis(), type, size,
						true));
	}

	public static String getContextName() {
		return LooseContext.getString(CONTEXT_METRIC_NAME);
	}

	public static void publish(long time, Type type, int bytes) {
		ProcessObservers.publish(ProcessMetric.class,
				() -> new ProcessMetric(time, type, bytes));
	}

	public static void setContextName(String name) {
		LooseContext.set(CONTEXT_METRIC_NAME, name);
	}

	private boolean end;

	private long time;

	private int size;

	private int objectCount;

	private Type type;

	private String name;

	public ProcessMetric() {
	}

	private ProcessMetric(long time, Type type, int bytes) {
		this.time = time;
		this.type = type;
		this.name = LooseContext.getString(CONTEXT_METRIC_NAME);
		this.size = bytes;
	}

	private ProcessMetric(long time, Type type, int size, boolean end) {
		this.time = time;
		this.type = type;
		this.name = LooseContext.getString(CONTEXT_METRIC_NAME);
		this.size = size;
		this.end = end;
	}

	public String getName() {
		return this.name;
	}

	public int getObjectCount() {
		return this.objectCount;
	}

	public int getSize() {
		return this.size;
	}

	public long getTime() {
		return this.time;
	}

	public Type getType() {
		return this.type;
	}

	public boolean isEnd() {
		return this.end;
	}

	public void setEnd(boolean end) {
		this.end = end;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setObjectCount(int objectCount) {
		this.objectCount = objectCount;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		FormatBuilder fb = new FormatBuilder().separator(" ");
		fb.format("Metric: %s",
				CommonUtils.padStringRight(type.toString(), 16, ' '));
		fb.format("%s   ", DateStyle.TIMESTAMP_NO_DAY.format(new Date(time)));
		fb.appendPadRight(16, name == null ? "" : name);
		fb.conditionalFormat(size != 0, "size: %s ", size);
		fb.conditionalFormat(!end, "[start] ");
		fb.conditionalFormat(objectCount != 0, "objectCount: %s ", objectCount);
		return fb.toString();
	}

	@Reflected
	public enum ClientType implements Type {
		startup, load_js, eval, rpc
	}

	public static class Observer extends Model
			implements ProcessObserver<ProcessMetric>, OutOfBandMessage {
		private List<ProcessMetric> metrics = new ArrayList<>();

		private String sequenceId;

		public List<ProcessMetric> getMetrics() {
			return this.metrics;
		}

		public String getSequenceId() {
			return this.sequenceId;
		}

		public void setMetrics(List<ProcessMetric> metrics) {
			this.metrics = metrics;
		}

		public void setSequenceId(String sequenceId) {
			this.sequenceId = sequenceId;
		}

		@Override
		public void topicPublished(ProcessMetric message) {
			logger.info(message.toString());
			metrics.add(message);
		}
	}

	@Reflected
	public enum ServerType implements Type {
		rpc, rpc_prepare, rpc_process, authenicate, project, serialize
	}

	@ReflectiveSerializer.Checks(hasReflectedSubtypes = true)
	public interface Type {
	}
}
