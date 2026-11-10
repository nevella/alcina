package cc.alcina.framework.common.client.traversal;

import cc.alcina.framework.common.client.process.TreeProcess;

public abstract class InitialTextSelection extends TextSelection {
	public InitialTextSelection(TreeProcess.Node parentNode, String text) {
		super(parentNode, text, "root");
	}
}