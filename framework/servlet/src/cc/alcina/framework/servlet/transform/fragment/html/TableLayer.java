package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection.NonIndentContainer;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.TableToken;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;

/**
 * Maps {table,thead,tbody,tr,th,td} tags to tokens (and thus output nodes)
 */
public class TableLayer extends Layer<StyledNode> {
	@Override
	public void process(StyledNode node) throws Exception {
		DomNode domNode = node.get();
		Peer peer = state.traversalContext(Peer.Has.class).getTableLayerPeer();
		if (domNode.tagIs("table")) {
			Measure measure = Measure.fromNode(node.get(),
					TableToken.Table.TYPE);
			select(new TableSelection(node, measure));
		} else if (domNode.tagIs("thead")) {
			Measure measure = Measure.fromNode(node.get(),
					TableToken.TableHead.TYPE);
			select(new TableHeadSelection(node, measure));
		} else if (domNode.tagIs("tbody")) {
			Measure measure = Measure.fromNode(node.get(),
					TableToken.TableBody.TYPE);
			select(new TableBodySelection(node, measure));
		} else if (domNode.tagIs("tr")) {
			Measure measure = Measure.fromNode(node.get(),
					TableToken.TableRow.TYPE);
			select(new TableRowSelection(node, measure));
		} else if (domNode.tagIs("th")) {
			Measure measure = Measure.fromNode(node.get(),
					TableToken.TableHeaderCell.TYPE);
			select(new TableHeaderCellSelection(node, measure));
		} else if (domNode.tagIs("td")) {
			Measure measure = Measure.fromNode(node.get(),
					TableToken.TableCell.TYPE);
			select(new TableCellSelection(node, measure, peer));
		}
	}

	public static class TableSelection extends StyledMeasureSelection
			implements NonIndentContainer {
		TableSelection(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}

	public static class TableHeadSelection extends StyledMeasureSelection
			implements NonIndentContainer {
		TableHeadSelection(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}

	public static class TableBodySelection extends StyledMeasureSelection
			implements NonIndentContainer {
		TableBodySelection(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}

	public static class TableRowSelection extends StyledMeasureSelection
			implements NonIndentContainer {
		TableRowSelection(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}

	public static class TableHeaderCellSelection
			extends StyledMeasureSelection {
		TableHeaderCellSelection(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}

	public static class TableCellSelection extends StyledMeasureSelection
			implements TableToken.TableCell.Data.Has {
		TableToken.TableCell.Data data = new TableToken.TableCell.Data();

		TableCellSelection(StyledNode parent, Measure measure, Peer peer) {
			super(parent, measure);
			DomNode node = measure.startContainingNode();
			data.colspan = node.has("colspan") ? node.attr("colspan") : null;
			data.rowspan = node.has("rowspan") ? node.attr("rowspan") : null;
			data.style = peer.resolveOutputCellStyles(node);
		}

		@Override
		public TableToken.TableCell.Data getTableCellData() {
			return data;
		}
	}

	public static class Peer {
		public interface Has {
			Peer getTableLayerPeer();
		}

		public String resolveOutputCellStyles(DomNode sourceCell) {
			return null;
		}
	}
}