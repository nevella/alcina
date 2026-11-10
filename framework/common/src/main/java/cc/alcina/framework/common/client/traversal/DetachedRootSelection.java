package cc.alcina.framework.common.client.traversal;

import cc.alcina.framework.common.client.process.TreeProcess;

/*
 * Used for ad-hoc selection parenting
 */
public class DetachedRootSelection extends AbstractSelection<String> {
	public DetachedRootSelection() {
		super(TreeProcess.detachedSelectionProcess().root(), "detached",
				"root");
	}
}