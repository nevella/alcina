package cc.alcina.framework.common.client.traversal.layer.block;

import cc.alcina.framework.common.client.dom.DomEnvironment.StyleResolver;
import cc.alcina.framework.common.client.dom.DomEnvironment.StyleResolverHtml;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.LayerParser;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.util.TextUtils;

public abstract class AbstractHtmlState extends LayerParser.ExtendedState {
	protected Selection<?> selection;

	public boolean forwards;

	StyleResolver styleResolver;

	protected AbstractHtmlState(boolean forwards, Selection selection) {
		this.forwards = forwards;
		this.selection = (Selection) selection;
		styleResolver = new StyleResolverHtml();
	}

	public boolean isBlock() {
		return styleResolver.isBlock(parserState.node());
	}

	public Text text() {
		return new Text();
	}

	public boolean textEquals(String string) {
		return parserState.node().textIs(string);
	}

	public class Text {
		public boolean isDate() {
			return TextUtils.isDateEn(parserState.node().ntc());
		}
	}

	public static class Forwards extends AbstractHtmlState {
		public Forwards(Selection<?> selection) {
			super(true, selection);
		}
	}

	public interface Token<T extends AbstractHtmlState> extends BranchToken {
		@Override
		default Measure match(ParserState state) {
			T extendedState = (T) state.extendedState();
			// entering a filtered area
			// if (extendedState.styledNode() == null) {
			// return null;
			// }
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

	public boolean isBlockOrBlockSeparator() {
		DomNode node = parserState.node();
		return styleResolver.isBlock(node)
				|| node == node.document.getDocumentElementNode();
	}
}