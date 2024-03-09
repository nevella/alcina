package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables.ObservableHistory;

class Header extends Model.All {
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
}