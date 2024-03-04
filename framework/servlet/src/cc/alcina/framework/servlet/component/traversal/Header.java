package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables.ObservableHistory;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;

class Header extends Model.All
		implements DomEvents.Click.Handler, ModelEvents.Change.Handler {
	class Left extends Model.All {
		String name;

		public void setName(String name) {
			set("name", this.name, name, () -> this.name = name);
		}

		Left() {
			bindings().from(Header.this.page).on(Page.Property.history)
					.typed(ObservableHistory.class).map(this::computeName)
					.accept(this::setName);
		}

		String computeName(ObservableHistory history) {
			FormatBuilder format = new FormatBuilder().separator(" - ");
			format.append(TraversalProcessView.Ui.get().getMainCaption());
			format.appendIfNonNull(history, ObservableHistory::displayName);
			return format.toString();
		}
	}

	class Mid extends Model.All {
		AppSuggestorTraversal suggestor;

		Mid() {
			suggestor = new AppSuggestorTraversal();
		}
	}

	class Right extends Model.All {
	}

	Left left;

	Mid mid;

	Right right;

	@Property.Not
	Page page;

	Header(Page page) {
		this.page = page;
		left = new Left();
		mid = new Mid();
		right = new Right();
	}

	@Override
	public void onChange(Change event) {
		new TraversalPlace().withTextFilter((String) event.getModel()).go();
	}

	@Override
	public void onClick(Click event) {
		new TraversalPlace().go();
	}
}