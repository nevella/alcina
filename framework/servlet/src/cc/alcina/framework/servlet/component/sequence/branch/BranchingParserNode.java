package cc.alcina.framework.servlet.component.sequence.branch;

import java.util.List;

import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.BranchNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.model.HasClassNames;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * Models a branch
 */
class BranchingParserNode extends Model
		implements HasClassNames, HasStringRepresentation {
	enum MatchType {
		MATCH, DESCENDANT_MATCH
	}

	enum GroupType {
		GROUP, LEAF_OR_MATCH
	}

	BranchNode branchNode;

	public Branch getBranch() {
		return branchNode.branch;
	}

	MatchType matchType;

	GroupType groupType;

	BranchingParserNode(BranchNode branchNode) {
		this.branchNode = branchNode;
	}

	public String getPath() {
		return branchNode.branch.toAncestorString();
	}

	public String getMatch() {
		return Ax.toString(branchNode.match);
	}

	public String getMeasure() {
		Measure lastMeasure = branchNode.branch.getLastMeasure();
		return lastMeasure == null ? null : lastMeasure.text();
	}

	@Override
	public List<String> provideClassNames() {
		return List.of(Ax.cssify(NestedName.get(this)));
	}

	public boolean isTopLvel() {
		return branchNode.branch.parent == null;
	}

	@Override
	public String provideStringRepresentation() {
		return getPath();
	}
}
