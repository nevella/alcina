package cc.alcina.framework.servlet.sync;

import java.util.List;

import cc.alcina.framework.common.client.util.Multimap;

public class SyncDeltaModel {
	private Multimap<Class, List<SyncPair>> deltas = new Multimap<Class, List<SyncPair>>();

	public Multimap<Class, List<SyncPair>> getDeltas() {
		return this.deltas;
	}

	public void setDeltas(Multimap<Class, List<SyncPair>> deltas) {
		this.deltas = deltas;
	}

	private String generatorLog="";

	public String getGeneratorLog() {
		return this.generatorLog;
	}

	public void setGeneratorLog(String generatorLog) {
		this.generatorLog = generatorLog;
	}
}
