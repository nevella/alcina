package cc.alcina.framework.servlet.sync;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.sync.property.PropertyModificationLog;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.servlet.sync.SyncPair.SyncPairAction;

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

	@Override
	public String toString() {
		FormatBuilder fb = new FormatBuilder();
		deltas.entrySet().forEach(e -> {
			fb.line(e.getKey().getClass().getSimpleName());
			Map<SyncPairAction, List<SyncPair>> collect = e.getValue().stream()
					.collect(Collectors.groupingBy(SyncPair::getAction));
			collect.entrySet().forEach(g1 -> {
				fb.line("\t%s: %s", g1.getValue().size(),
						Ax.friendly(g1.getKey()));
			});
		});
		return fb.toString();
	}
}
