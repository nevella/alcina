package cc.alcina.framework.common.client.traversal;

import cc.alcina.framework.common.client.csobjects.IsBindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.HasFilterableString;

public class SelectionView {
	@Registration.NonGenericSubtypes(View.class)
	public static interface View<S extends Selection>
			extends Registration.AllSubtypes {
		default String getDiscriminator(S selection) {
			return "";
		}

		default String getMarkup(S selection) {
			return "";
		}

		default String getPathSegment(S selection) {
			return selection.getPathSegment();
		}

		default String getText(S selection) {
			return HasFilterableString.filterableString(selection.get());
		}

		default Object getExtended(S selection) {
			return null;
		}
	}

	@Registration.NonGenericSubtypes(View.class)
	public static interface RowView<S extends Selection>
			extends Registration.AllSubtypes, IsBindable {
		void putSelection(S selection);

		S getSelection();
	}

	/*
	 * Marker, view should be lazy-loaded. It's not an inner interface of view
	 * to avoid class structure cycles
	 */
	public static interface ViewAsync {
	}
}
