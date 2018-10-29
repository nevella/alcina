package cc.alcina.framework.servlet.knowns;

import java.util.Objects;

import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;
import cc.alcina.framework.common.client.csobjects.KnownsDelta;
import cc.alcina.framework.common.client.util.TimeConstants;

public class KnownsDeltaRequestHandler {
	KnownRenderableNode lastRoot = null;

	public KnownsDelta getDelta(long since, String clientId) {
		boolean modified = true;
		if (since != 0 && since == Knowns.lastModified) {
			modified = false;
			long lastModified = 0;
			synchronized (Knowns.reachableKnownsModificationNotifier) {
				try {
					lastModified = Knowns.lastModified;
					Knowns.reachableKnownsModificationNotifier
							.wait(30 * TimeConstants.ONE_SECOND_MS);
				} catch (InterruptedException e) {
				}
				modified = lastModified != Knowns.lastModified;
			}
		}
		KnownsDelta delta = new KnownsDelta();
		// build tree
		// TODO - freeze mods?
		delta.timeStamp = Knowns.lastModified;
		if (modified) {
			KnownRenderableNode root = new KnownRenderableNode();
			root.name = "root";
			KnownRenderableNode appRoot = Knowns.renderableRoot();
			root.children.add(appRoot);
			KnownsCluster.get().systemDeltas.getMap().values().stream()
					.map(kcse -> kcse.lastDelta).filter(Objects::nonNull)
					.forEach(clusterDelta -> root.merge(clusterDelta));
			root.children.forEach(c -> c.name = c.name.toLowerCase());
			delta.added.add(root);
		}
		// build delta from last tree
		return delta;
	}
}
