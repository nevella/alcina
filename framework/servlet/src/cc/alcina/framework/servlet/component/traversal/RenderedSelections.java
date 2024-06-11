package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup.Query;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.RestrictedHtmlTag;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

@Directed(tag = "selections")
class RenderedSelections extends Model.Fields {
	class SelectionMarkupArea extends Model.All {
		SelectionMarkupArea(Model model) {
			this.model = model;
		}

		Model model;
	}

	Page page;

	@Directed
	Model style;

	public void setStyle(Model style) {
		set("style", this.style, style, () -> this.style = style);
	}

	@Directed
	Heading heading;

	@Directed
	SelectionMarkupArea selectionMarkup;

	Selection<?> selection;

	boolean input;

	RenderedSelections(Page page, boolean input) {
		this.page = page;
		this.input = input;
		this.heading = new Heading(input ? "Input" : "Output");
		bindings().from(page).on(Page.Property.place)
				.typed(TraversalPlace.class)
				.map(p -> p.provideSelection(SelectionType.VIEW))
				.accept(this::setSelection);
		bindings().from(this).on("selection").signal(this::onSelectionChange);
	}

	public void setSelectionMarkup(SelectionMarkupArea selectionMarkup) {
		set("selectionMarkup", this.selectionMarkup, selectionMarkup,
				() -> this.selectionMarkup = selectionMarkup);
	}

	public void setSelection(Selection selection) {
		set("selection", this.selection, selection,
				() -> this.selection = selection);
	}

	void onSelectionChange() {
		setSelectionMarkup(null);
		if (selection == null) {
			return;
		}
		// workaround for vs.code (or eclipse) compilation issue - the local
		// traversal variable is a required intermediate
		SelectionTraversal traversal = page.history.getObservable();
		SelectionMarkup.Has markupProvider = traversal
				.context(SelectionMarkup.Has.class);
		if (markupProvider == null) {
			return;
		}
		SelectionMarkup markup = markupProvider.getSelectionMarkup();
		String styleScope = Ax.format(
				"selections.%s > selection-markup-area > div",
				input ? "input" : "output");
		Query query = markup.query(selection, styleScope, input);
		Model model = query.getModel();
		SelectionMarkupArea markupArea = new SelectionMarkupArea(model);
		setSelectionMarkup(markupArea);
		if (style == null) {
			Model style = new Style(query.getCss());
			setStyle(style);
		}
	}

	class Style extends Model.Fields implements RestrictedHtmlTag {
		Style(String style) {
			this.style = style;
		}

		@Binding(type = Type.INNER_TEXT)
		String style;
	}
}
