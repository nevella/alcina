package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;

public interface DeltaApplicationSerializer {

	List<DeltaApplicationRecord> readMultiple(String data);

	DeltaApplicationRecord read(String data);
}
