package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;

public interface DeltaApplicationSerializer {
	DeltaApplicationRecord read(String data);

	List<DeltaApplicationRecord> readMultiple(String data);
}
