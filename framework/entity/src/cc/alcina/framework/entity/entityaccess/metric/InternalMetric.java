package cc.alcina.framework.entity.entityaccess.metric;

import java.util.Date;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.csobjects.AbstractDomainBase;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.entity.projection.GraphProjection;

@MappedSuperclass
@DomainTransformPersistable
public abstract class InternalMetric<U extends InternalMetric>
		extends AbstractDomainBase<U> {
	protected long id;

	private String threadName;

	private String callName;

	private Date startTime;

	private Date endTime;

	private String obfuscatedArgs;

	private int sliceCount;

	private String sliceJson;

	private String hostName;

	private String lockType;

	public InternalMetric() {
	}

	public String getCallName() {
		return this.callName;
	}

	public Date getEndTime() {
		return this.endTime;
	}

	public String getHostName() {
		return this.hostName;
	}

	public String getLockType() {
		return this.lockType;
	}

	@Lob
	@Transient
	public String getObfuscatedArgs() {
		return this.obfuscatedArgs;
	}

	public int getSliceCount() {
		return this.sliceCount;
	}

	@Lob
	@Transient
	public String getSliceJson() {
		return this.sliceJson;
	}

	public Date getStartTime() {
		return this.startTime;
	}

	public String getThreadName() {
		return this.threadName;
	}

	public void setCallName(String callName) {
		this.callName = callName;
	}

	public void setEndTime(Date end) {
		this.endTime = end;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setLockType(String lockType) {
		this.lockType = lockType;
	}

	public void setObfuscatedArgs(String obfuscatedArgs) {
		this.obfuscatedArgs = obfuscatedArgs;
	}

	public void setSliceCount(int sliceCount) {
		this.sliceCount = sliceCount;
	}

	public void setSliceJson(String sliceJson) {
		this.sliceJson = sliceJson;
	}

	public void setStartTime(Date start) {
		this.startTime = start;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	@Override
	public String toString() {
		return GraphProjection.fieldwiseToStringOneLine(this);
	}
}