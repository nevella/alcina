package cc.alcina.framework.common.client.traversal.layer;

import java.util.Objects;

import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * An API to extract ('select') a section of markup from a larger document
 * 
 * <p>
 * To render correctly, this can involve replicating the ancestor tag structure
 * of the selected range
 * 
 */
public abstract class SelectionMarkup {
	public interface Has {
		SelectionMarkup getSelectionMarkup();
	}

	public class Query {
		public interface ElementToSelection {
			Selection getSelection(Query query, Element container,
					Element source);
		}

		public Selection<?> selection;

		public boolean input;

		public String styleScope;

		public ElementToSelection elementToSelection;

		@Override
		public boolean equals(Object o) {
			if (o instanceof Query) {
				Query typed = (Query) o;
				return CommonUtils.equals(selection, typed.selection, input,
						typed.input, styleScope, typed.styleScope);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(selection, input, styleScope);
		}

		Query(Selection<?> selection, String styleScope, boolean input) {
			this.selection = selection;
			this.styleScope = styleScope;
			this.input = input;
		}

		public Model getModel() {
			return SelectionMarkup.this.getModel(this);
		}

		public String getCss() {
			return SelectionMarkup.this.getCss(this);
		}
	}

	public Query query(Selection<?> selection, String styleScope,
			boolean input) {
		return new Query(selection, styleScope, input);
	}

	/*
	 * Use another query as a parameter source
	 */
	public Query query(Query query) {
		return new Query(query.selection, query.styleScope, query.input);
	}

	protected abstract String getCss(Query query);

	protected abstract Model getModel(Query query);
}
