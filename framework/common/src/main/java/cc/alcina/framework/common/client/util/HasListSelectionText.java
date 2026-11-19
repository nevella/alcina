package cc.alcina.framework.common.client.util;

import java.util.Optional;

import cc.alcina.framework.common.client.actions.PermissibleActionHandler;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * For displaying an object in, say, a choice dropdown
 */
public interface HasListSelectionText {
	Object getListSelectionText();

	public static String toListSelectionText(Object obj) {
		Object cursor = obj;
		for (;;) {
			if (cursor == null) {
				return null;
			} else if (cursor instanceof HasListSelectionText) {
				cursor = ((HasListSelectionText) cursor).getListSelectionText();
			} else {
				Optional<To> transformer = Registry.query(To.class)
						.addKeys(cursor.getClass()).optional();
				if (transformer.isPresent()) {
					cursor = transformer.get().apply(cursor);
				} else {
					return cursor.toString();
				}
			}
		}
	}

	@Registration.NonGenericSubtypes(To.class)
	public interface To<T> {
		Object apply(T t);
	}
}
