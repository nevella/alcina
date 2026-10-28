package cc.alcina.framework.servlet.transform.fragment;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.LayerParser;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.servlet.transform.fragment.StyledDocument.AccessorSelection;

public abstract class AbstractHtmlState extends LayerParser.ExtendedState {
	// since AccessorSelection and Selection are different hierarchies, expose
	// the same object as both
	protected AccessorSelection accessorSelection;

	protected Selection<?> selection;

	public boolean forwards;

	StyledDocument ancestorDocument;

	protected AbstractHtmlState(boolean forwards,
			AccessorSelection transformerSelection) {
		this.forwards = forwards;
		this.accessorSelection = transformerSelection;
		this.selection = (Selection) transformerSelection;
		ancestorDocument = StyledDocument
				.ancestorDocument((Selection<?>) accessorSelection);
	}

	public boolean hasStyles() {
		return styledNode() != null;
	}

	public boolean isBlock() {
		return hasStyles() && styledNode().styles().isBlock();
	}

	public Text text() {
		return new Text();
	}

	public boolean textEquals(String string) {
		return parserState.node().textIs(string);
	}

	public StyledNode styledNode() {
		return ancestorDocument.styledNode(parserState.node());
	}

	public StyledNode styledNode(Location location) {
		return ancestorDocument.styledNode(location.getContainingNode());
	}

	public class Text {
		public boolean isDate() {
			return TextUtils.isDateEn(parserState.node().ntc());
		}
	}

	public static class Forwards extends AbstractHtmlState {
		public Forwards(Selection<?> selection) {
			super(true, selection.ancestor(DocumentTransformerSelection.class));
		}
	}

	public interface Token<T extends AbstractHtmlState> extends BranchToken {
		@Override
		default Measure match(ParserState state) {
			T extendedState = (T) state.extendedState();
			// entering a filtered area
			if (extendedState.styledNode() == null) {
				return null;
			}
			return match0(extendedState);
		}

		default Measure match0(T state) {
			throw new UnsupportedOperationException(
					"See BranchToken constraints");
		}
	}

	public enum Tokens implements Token {
		NOOP {
			@Override
			public Measure match0(AbstractHtmlState state) {
				// placeholder
				return null;
			}
		};
	}
}