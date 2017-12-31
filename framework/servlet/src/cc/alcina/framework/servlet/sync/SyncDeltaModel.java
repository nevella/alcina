package cc.alcina.framework.servlet.sync;

import java.util.List;

import cc.alcina.framework.common.client.sync.property.PropertyModificationLog;
import cc.alcina.framework.common.client.util.Multimap;

public class SyncDeltaModel {
	private Multimap<Class, List<SyncPair>> deltas = new Multimap<Class, List<SyncPair>>();

	private PropertyModificationLog propertyModificationLog = new PropertyModificationLog();

	private String generatorLog = "";

	public Multimap<Class, List<SyncPair>> getDeltas() {
		return this.deltas;
	}

	public String getGeneratorLog() {
		return this.generatorLog;
	}

	public PropertyModificationLog getPropertyModificationLog() {
		return this.propertyModificationLog;
	}

	public void setDeltas(Multimap<Class, List<SyncPair>> deltas) {
		this.deltas = deltas;
	}

	public void setGeneratorLog(String generatorLog) {
		this.generatorLog = generatorLog;
	}

	public void setPropertyModificationLog(
			PropertyModificationLog propertyModificationLog) {
		this.propertyModificationLog = propertyModificationLog;
	}
}
