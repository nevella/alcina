package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;

public class AsyncConfigConsortState extends ExtensibleEnum {
	public static final AsyncConfigConsortState TRANSFORM_DB_INITIALISED = new AsyncConfigConsortState(
			"TRANSFORM_DB_INITIALISED");

	public static final AsyncConfigConsortState LOG_DB_INITIALISED = new AsyncConfigConsortState(
			"LOG_DB_INITIALISED");

	public AsyncConfigConsortState(String key) {
		super(key);
	}
}
