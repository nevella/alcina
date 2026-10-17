package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens;

import cc.alcina.framework.common.client.dom.Measure.Token;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.FragmentDocumentOverlayToken.CanContainLeaf;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.TextChildrenNotVisible;

/*
 * Naming container for html table tokens
 */
public interface TableToken {
	public static class Table extends FragmentDocumentOverlayToken.AbstractToken
			implements TableToken, TextChildrenNotVisible,
			Token.RestrictedChildContainment {
		public static final Table TYPE = new Table();

		private Table() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<Table, Block.TableNode.Table> {
		}

		@Override
		public boolean allowChildContainment(Token otherToken) {
			return otherToken == TableHead.TYPE || otherToken == TableBody.TYPE
					|| otherToken == LayoutTokens.Text.TYPE;
		}
	}

	public static class TableHead extends
			FragmentDocumentOverlayToken.AbstractToken implements TableToken,
			TextChildrenNotVisible, Token.RestrictedChildContainment {
		public static final TableHead TYPE = new TableHead();

		private TableHead() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<TableHead, Block.TableNode.TableHead> {
		}

		@Override
		public boolean allowChildContainment(Token otherToken) {
			return otherToken == TableRow.TYPE
					|| otherToken == LayoutTokens.Text.TYPE;
		}
	}

	public static class TableBody extends
			FragmentDocumentOverlayToken.AbstractToken implements TableToken,
			TextChildrenNotVisible, Token.RestrictedChildContainment {
		public static final TableBody TYPE = new TableBody();

		private TableBody() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<TableBody, Block.TableNode.TableBody> {
		}

		@Override
		public boolean allowChildContainment(Token otherToken) {
			return otherToken == TableRow.TYPE
					|| otherToken == LayoutTokens.Text.TYPE;
		}
	}

	public static class TableRow extends
			FragmentDocumentOverlayToken.AbstractToken implements TableToken,
			TextChildrenNotVisible, Token.RestrictedChildContainment {
		public static final TableRow TYPE = new TableRow();

		private TableRow() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<TableRow, Block.TableNode.TableRow> {
		}

		@Override
		public boolean allowChildContainment(Token otherToken) {
			return otherToken == TableHeaderCell.TYPE
					|| otherToken == TableCell.TYPE
					|| otherToken == LayoutTokens.Text.TYPE;
		}
	}

	public static class TableHeaderCell extends
			FragmentDocumentOverlayToken.AbstractToken implements TableToken {
		public static final TableHeaderCell TYPE = new TableHeaderCell();

		private TableHeaderCell() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<TableHeaderCell, Block.TableNode.TableHeaderCell> {
		}
	}

	public static class TableCell
			extends FragmentDocumentOverlayToken.AbstractToken
			implements TableToken, CanContainLeaf {
		public static final TableCell TYPE = new TableCell();

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<TableCell, Block.TableNode.TableCell> {
			@Override
			public Block.TableNode.TableCell apply(MeasureSelection t) {
				Data.Has has = (Data.Has) t;
				return new Block.TableNode.TableCell(has);
			}
		}

		public static class Data {
			public String style;

			public String colspan;

			public String rowspan;

			public interface Has {
				Data getTableCellData();
			}
		}
	}
}