package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalHistories.TraversalHistory;

@Directed
class Page extends Model.All {
	Header header = new Header();

	SelectionLayers layers = new SelectionLayers();

	Properties properties = new Properties();

	RenderedSelections input = new RenderedSelections();

	@Directed.Exclude
	TraversalHistory history;

	void setHistory(TraversalHistory history) {
		set("history", this.history, history, () -> this.history = history);
	}

	class Header extends Model.All {
		String name = TraversalProcessView.Ui.get().getMainCaption();

		Header() {
			bindings().build().from(Page.this).on("history")
					.typed(TraversalHistory.class).map(this::computeName)
					.accept(this::setName);
		}

		String computeName(TraversalHistory history) {
			FormatBuilder format = new FormatBuilder().separator(" - ");
			format.append(TraversalProcessView.Ui.get().getMainCaption());
			format.appendIfNonNull(history, TraversalHistory::displayName);
			return format.toString();
		}

		public void setName(String name) {
			set("name", this.name, name, () -> this.name = name);
		}
	}

	Page() {
		bindings().addListener(() -> TraversalHistories.get().subscribe(null,
				this::setHistory));
	}
}
