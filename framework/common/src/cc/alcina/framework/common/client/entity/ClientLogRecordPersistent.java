package cc.alcina.framework.common.client.entity;

import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.gwt.persistence.client.ClientLogRecord;

public abstract class ClientLogRecordPersistent extends ClientLogRecord implements HasId{
	private long id;

	public void setId(long id) {
		this.id = id;
	}
	public abstract void wrap(ClientLogRecord clr);
	@Transient
	public long getId() {
		return id;
	}
	public interface ClientLogRecordTupleSerializer{
		
	}
}
