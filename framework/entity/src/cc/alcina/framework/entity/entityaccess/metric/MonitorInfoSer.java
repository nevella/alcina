package cc.alcina.framework.entity.entityaccess.metric;

import java.lang.management.MonitorInfo;

import cc.alcina.framework.entity.projection.GraphProjection;

public class MonitorInfoSer extends LockInfoSer {
	public int stackDepth;

	public StackTraceElement stackFrame;

	public MonitorInfoSer() {
	}

	public MonitorInfoSer(MonitorInfo monitorInfo) {
		super(monitorInfo);
		stackDepth = monitorInfo.getLockedStackDepth();
		stackFrame = monitorInfo.getLockedStackFrame();
	}

	@Override
	public String toString() {
		return GraphProjection.fieldwiseToStringOneLine(this);
	}
}
