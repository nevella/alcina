package cc.alcina.framework.servlet.transform.fragment.structure;

import java.util.stream.Collectors;

import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;

/**
 * Marks a measure as a Toc. FIXME - there should only be one elt - w:sdt - but
 * because word html/"x" is so flaky, the structure is wonky
 * 
 * Real fix is to fix the word html by normalising attrs, metas etc. Not there
 * yet, so just mark all toc elements as toc
 */
public class TocLayer extends Layer<StyledNode> {
	@Override
	public void process(StyledNode selection) throws Exception {
		// oracle me
		DomNode node = selection.get();
		// FIXME - to Styles
		if (node.attrIs("docparttype", "Table of Contents")) {
			Measure measure = Measure.fromNode(node,
					LayoutTokens.TableOfContents.TYPE);
			select(new TableOfContents(selection, measure));
		}
		if (node.tagIs("p") && node.attrMatches("class", "MsoToc(\\d+).*")) {
			Measure measure = Measure.fromNode(node,
					LayoutTokens.TocEntry.TYPE);
			TocEntry tocSelection = new TocEntry(selection, measure);
			if (tocSelection.isValid()) {
				select(tocSelection);
			}
		}
	}

	public static class TableOfContents extends StyledMeasureSelection
			implements MeasureSelection.IgnoreOverlaps {
		TableOfContents(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}

	public static class TocEntry extends StyledMeasureSelection
			implements MeasureSelection.IgnoreOverlaps {
		String text;

		int level;

		String id;

		String getId() {
			return id;
		}

		public boolean isValid() {
			return Ax.notBlank(id);
		}

		TocEntry(Selection parent, Measure measure) {
			super(parent, measure);
			DomNode node = measure.startContainingNode();
			level = Integer.parseInt(
					node.getClassName().replaceFirst("MsoToc(\\d+).*", "$1"));
			DomNode firstElement = node.children.firstElement();
			text = firstElement == null ? node.ntc()
					: Ax.ntrim(firstElement.stream().filter(DomNode::isText)
							.map(t -> t.textMatches("\\.+") ? " "
									: t.textContent())
							.collect(Collectors.joining(" ")));
			String markup = node.toMarkup();
			// the markup ref -- complex, might be to filter images?
			if (markup.length() < 30000) {
				if (markup.contains("PAGEREF")) {
					id = Ax.ntrim(markup)
							.replaceFirst("(?s).+ PAGEREF (\\S+) .+", "$1");
				}
			} else {
				//
			}
		}
	}
}