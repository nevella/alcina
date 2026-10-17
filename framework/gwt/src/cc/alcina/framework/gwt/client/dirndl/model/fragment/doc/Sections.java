package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.dom.client.HtmlParser;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block.SectionHeader;

/**
 * Models the sections of a FragmentDocument
 */
public class Sections {
	FragmentDocument<?> doc;

	public Sections(FragmentDocument<?> doc) {
		this.doc = doc;
		parseSections();
	}

	public List<Section> sections = new ArrayList<>();

	Block lastBlock;

	Block.SectionHeader lastHeader;

	public boolean withSelfContainedFootnotes;

	public boolean withMultiversionFootnotes;

	void parseSections() {
		Stream.concat(doc.byType(Block.SectionHeader.class),
				doc.byType(Block.VolumeStart.class))
				.sorted(FragmentNode.DocumentOrderComparator.INSTANCE)
				.forEach(this::visitStructuralBlock);
		visitStructuralBlock(null);
	}

	/*
	 * structural blocks can either be a section header (most common) or a
	 * volume start
	 */
	void visitStructuralBlock(Block block) {
		if (lastHeader != null) {
			Location end = block == null ? doc.root.domNode().asRange().end
					: block.domNode().asLocation();
			end = end.relativeLocation(RelativeDirection.PREVIOUS_LOCATION);
			sections.add(new Section(lastHeader, end));
		}
		if (block instanceof SectionHeader) {
			Block.SectionHeader header = (SectionHeader) block;
			lastHeader = header;
		}
	}

	static Logger logger = LoggerFactory.getLogger(Sections.class);

	public class Section {
		public SectionHeader header;

		Location end;

		public Section(SectionHeader header, Location end) {
			this.header = header;
			this.end = end;
		}

		FragmentDocument<?> sectionDoc;

		public FragmentDocument getDocument() {
			if (sectionDoc == null) {
				Range range = new Location.Range(header.domNode().asLocation(),
						end);
				String markup = range.markup();
				/*
				 * markup may be invalid
				 */
				HtmlParser.CONTEXT_DO_NOT_CLOSE_TO_VALIDATE.runWithTrue(
						() -> sectionDoc = FragmentDocument.from(markup));
				if (withSelfContainedFootnotes) {
					sectionDoc.footnotes().renderSelfContainedFootnotes(doc,
							false);
				}
			}
			return sectionDoc;
		}

		@Override
		public String toString() {
			return header.toString();
		}

		public DomNode getFirstNode(Predicate<DomNode> predicate) {
			return getDocument().root.domNode().stream().filter(predicate)
					.findFirst().orElse(null);
		}
	}
}
