package cc.alcina.framework.servlet.transform.fragment.structure;

import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.servlet.transform.fragment.api.DocumentTransformerHostApi;

public abstract class SectionHeadingLayer extends Layer<StyledNode> {
	public static class GenericHeading extends StyledMeasureSelection
			implements LayoutTokens.HasLevel {
		private int level;

		GenericHeading(Selection parent, Measure measure, int level) {
			super(parent, measure);
			this.level = level;
		}

		public int getLevel() {
			return level;
		}
	}

	public static class Peer {
		public interface Has {
			Peer getSectionHeadingLayerPeer();
		}

		public boolean isSimpleHeadingSections() {
			return false;
		}

		public int adjustHeadingLevel(int headingLevel) {
			return headingLevel;
		}

		public boolean isSectionHeading(StyledNode selection) {
			return selection.styles().isHeading();
		}

		public boolean determineHeadingLevel() {
			return true;
		}
	}

	static class SectionHeading extends StyledMeasureSelection
			implements LayoutTokens.HasSectionData {
		String sectionNumber;

		String sectionName;

		int sectionDepth;

		String sectionType;

		SectionHeading(Selection parent, Measure measure, String sectionType,
				String sectionNumber, String sectionName, int depth) {
			super(parent, measure);
			this.sectionType = sectionType;
			this.sectionNumber = sectionNumber;
			this.sectionName = sectionName;
			this.sectionDepth = depth;
		}

		public void setSectionType(String sectionType) {
			this.sectionType = sectionType;
		}

		public String getSectionType() {
			return sectionType;
		}

		public String getSectionNumber() {
			return sectionNumber;
		}

		public String getSectionName() {
			return sectionName;
		}

		public int getSectionDepth() {
			return sectionDepth;
		}

		@Override
		public String provideSectionTypeFallbackOnNumber() {
			if (Ax.notBlank(sectionType)) {
				return sectionType;
			}
			if (Ax.isBlank(sectionNumber)) {
				return null;
			}
			String test = sectionNumber.replaceFirst("(\\S+) .+", "$1");
			String normalisedSectionTypeText = DocumentTransformerHostApi.get()
					.getNormalisedSectionTypeText(test);
			return normalisedSectionTypeText;
		}
	}
}
