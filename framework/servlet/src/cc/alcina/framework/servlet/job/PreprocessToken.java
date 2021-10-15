package cc.alcina.framework.servlet.job;

import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;

public class PreprocessToken {
	public DomainTransformPersistenceEvent event;

	public PreprocessToken(DomainTransformPersistenceEvent event) {
		this.event = event;
	}

	public DomainTransformPersistenceEvent getEvent() {
		return event;
	}

	@Override
	public String toString() {
		return event.toString();
	}
}