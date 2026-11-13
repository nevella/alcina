package cc.alcina.framework.gwtpeer.client.dom;

import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwtpeer.client.dom.Location.IndexTuple;

public class TrackingLocationMutationObservable implements ContextObservable {
	public Location location;

	public boolean pre;

	public TrackingLocationMutationObservable(Location location, boolean pre) {
		this.location = location;
		this.pre = pre;
	}

	public boolean tagIs(String tag) {
		return location.asIndexTuple().containingNode.nameIs(tag);
	}

	public String indexTupleRepr() {
		return location.asIndexTuple().toString();
	}

	public void debug() {
		IndexTuple indexTuple = location.asIndexTuple();
		if (pre) {
			Ax.out("%s -> %s :: %s", location.documentMutationPosition,
					location.locationContext.getDocumentMutationPosition(),
					indexTuple);
		} else {
			Ax.out(" ==> %s", location.asIndexTuple());
		}
	}
}
