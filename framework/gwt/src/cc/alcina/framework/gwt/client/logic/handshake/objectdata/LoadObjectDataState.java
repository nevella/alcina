package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;

public class LoadObjectDataState extends ExtensibleEnum {
	public static final LoadObjectDataState OBJECT_DATA_LOADED = new LoadObjectDataState(
			"OBJECT_DATA_LOADED");

	public static final LoadObjectDataState OBJECT_DATA_LOAD_FAILED = new LoadObjectDataState(
			"OBJECT_DATA_LOAD_FAILED");

	public static final LoadObjectDataState DELTA_STORE_MERGED_IF_NECESSARY = new LoadObjectDataState(
			"DELTA_STORE_MERGED_IF_NECESSARY");

	public static final LoadObjectDataState LOADED_DELTA_APPLICATIONS_FROM_LOCAL_STORAGE = new LoadObjectDataState(
			"LOADED_TRANSFORMS_FROM_LOCAL_STORAGE");

	public static final LoadObjectDataState HELLO_OK_REQUIRES_OBJECT_DATA_UPDATE = new LoadObjectDataState(
			"HELLO_OK_REQUIRES_OBJECT_DATA_UPDATE");

	public static final LoadObjectDataState HELLO_OFFLINE = new LoadObjectDataState(
			"HELLO_OFFLINE");

	public static final LoadObjectDataState SOLE_OPEN_TAB_CHECKED = new LoadObjectDataState(
			"SOLE_OPEN_TAB_CHECKED");

	public LoadObjectDataState(String key) {
		super(key);
	}
}
