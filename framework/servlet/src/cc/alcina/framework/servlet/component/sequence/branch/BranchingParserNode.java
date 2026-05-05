package cc.alcina.framework.servlet.component.sequence.branch;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.BranchNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
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
		GROUP, LEAF_OR_MATCH, NAMED
	}

	BranchNode branchNode;

	public Branch getBranch() {
		return branchNode.branch;
	}

	MatchType matchType;

	GroupType groupType;

	int nameDepth = -1;

	int containingBranchMaxLength;

	int getContainingBranchMaxLength() {
		return containingBranchMaxLength;
	}

	public int getNameDepth() {
		if (nameDepth == -1) {
			nameDepth = 0;
			Branch cursor = branchNode.branch;
			while (cursor != null) {
				if (cursor.group.isNamed()) {
					nameDepth++;
				}
				cursor = cursor.parent;
			}
		}
		return nameDepth;
	}

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
		return branchNode.branch.match == null ? null
				: branchNode.branch.match.text();
	}

	public String getMeasureRange() {
		return branchNode.branch.match == null ? null
				: branchNode.branch.match.toIntPair().toDashStringOrPoint();
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

	public String getMatchAgainst() {
		if (matchAgainst == null) {
			matchAgainst = new MatchAgainst();
		}
		return matchAgainst.markup;
	}

	MatchAgainst matchAgainst;

	class MatchAgainst {
		String markup;

		MatchAgainst() {
			/*
			 * limit to parser input range
			 * 
			 * show first 5 chars.
			 * 
			 * elide if necc to show underlined match
			 * 
			 * 
			 */
			int maxChars = 40;
			FormatBuilder format = new FormatBuilder();
			Location preMatchStart = branchNode.branch.location;
			int docEndIndex = preMatchStart.getContainingNode().document
					.getDocumentElementNode().asRange().end.getIndex();
			Measure match = branchNode.branch.match;
			int startIndex = preMatchStart.getIndex();
			if (match == null) {
				int end = Math.min(startIndex + maxChars, docEndIndex);
				Range range = new Location.Range(preMatchStart, preMatchStart
						.textRelativeLocation(end - startIndex, false));
				format.append(StringEscapeUtils.escapeHtml(range.text()));
			} else {
				int matchStart = match.start.getIndex();
				{
					/*
					 * pre
					 */
					boolean ellipses = false;
					int end = matchStart;
					if (matchStart > startIndex + 8) {
						end = matchStart + 5;
						ellipses = true;
					}
					Range range = new Location.Range(preMatchStart,
							preMatchStart.textRelativeLocation(end - startIndex,
									false));
					format.append(StringEscapeUtils.escapeHtml(range.text()));
					if (ellipses) {
						format.append("...");
					}
				}
				{
					/*
					 * match
					 */
					format.append("<match>");
					format.append(match.markup());
					format.append("</match>");
				}
				{
					int postMatchStart = match.end.getIndex();
					/*
					 * post
					 */
					boolean ellipses = false;
					int end = Math.min(postMatchStart + 30, docEndIndex);
					Range range = new Location.Range(match.end, match.end
							.textRelativeLocation(end - postMatchStart, false));
					format.append(StringEscapeUtils.escapeHtml(range.text()));
					if (ellipses) {
						format.append("...");
					}
				}
			}
			markup = format.toString().replace(" ", "\u00B7");
		}
	}
}
