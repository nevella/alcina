package cc.alcina.framework.servlet.component.sequence.branch;

import cc.alcina.framework.common.client.traversal.layer.BranchingParser.BranchNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * Models a branch
 */
class BranchingParserNode extends Model {
	BranchNode branchNode;

	BranchingParserNode(BranchNode branchNode) {
		this.branchNode = branchNode;
	}

	public String getPath() {
		return branchNode.branch.toAncestorString();
	}

	public String getMatch() {
		return Ax.toString(branchNode.match);
	}
}
