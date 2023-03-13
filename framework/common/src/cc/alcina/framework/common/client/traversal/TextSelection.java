package cc.alcina.framework.common.client.traversal;

import cc.alcina.framework.common.client.process.TreeProcess;

public abstract class TextSelection extends AbstractSelection<String> {
	public TextSelection(Selection parentSelection, String value,
			String pathSegment) {
		super(parentSelection, value, pathSegment);
	}

	public TextSelection(TreeProcess.Node parentNode, String value,
			String pathSegment) {
		super(parentNode, value, pathSegment);
	}
}