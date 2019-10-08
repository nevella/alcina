package cc.alcina.framework.servlet.knowns;

import cc.alcina.framework.common.client.csobjects.KnownNodeMetadata;

public class KnownNodeUnknown extends KnownNode {

	private KnownNodeMetadata metadata;
	public KnownNodeMetadata getMetadata() {
		return this.metadata;
	}
	public void setMetadata(KnownNodeMetadata metadata) {
		this.metadata = metadata;
	}
	public KnownNodeUnknown(KnownsPersistence persistence, KnownNode parent,
			String name) {
		super(persistence, parent, name);
	}
}
