package cc.alcina.framework.entity.persistence.metric;

import java.lang.management.LockInfo;

import cc.alcina.framework.entity.projection.GraphProjection;

public class LockInfoSer {
	public String className;

	public int identityHashCode;

	public LockInfoSer() {
	}

	public LockInfoSer(LockInfo lockInfo) {
		this.className = lockInfo.getClassName();
		this.identityHashCode = lockInfo.getIdentityHashCode();
	}

	@Override
	public String toString() {
		return GraphProjection.fieldwiseToStringOneLine(this);
	}
}
