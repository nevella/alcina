package cc.alcina.framework.gwt.client.dirndl.event;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * TODO - is matchesFilter correct API? 'filter?' Two different apis?
 */
public interface Filterable {
	public interface FilterFilterable
			extends Filterable, ModelEvents.Filter.Handler {
		@Override
		default void onFilter(ModelEvents.Filter event) {
			String filterValue = event.provideFilterValue();
			setVisible(matchesFilter(filterValue));
		}

		void setVisible(boolean visible);

		public static abstract class Abstract extends Model.All
				implements FilterFilterable {
			private boolean visible = true;

			@Binding(
				to = "display",
				transform = Binding.DisplayBlankNone.class,
				type = Type.STYLE_ATTRIBUTE)
			public boolean isVisible() {
				return visible;
			}

			public void setVisible(boolean visible) {
				set("visible", this.visible, visible,
						() -> this.visible = visible);
			}
		}
	}

	boolean matchesFilter(String filterString);
}