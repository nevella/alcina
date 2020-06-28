package cc.alcina.framework.common.client.entity;

import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.HasId;

public abstract class ClientLogRecordPersistent extends ClientLogRecord
		implements HasId {
	private long id;

	@Override
	@Transient
	public long getId() {
		return id;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public abstract void wrap(ClientLogRecord clr);

	public interface ClientLogRecordTupleSerializer {
	}
}
