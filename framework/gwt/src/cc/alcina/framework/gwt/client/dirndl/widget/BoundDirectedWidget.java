package cc.alcina.framework.gwt.client.dirndl.widget;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;

public class BoundDirectedWidget extends AbstractBoundWidget {

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface BoundDirectedWidgetCustomiserArgs {
		String[] tags();

		String[] cssClasses();
	}

	public static class BoundDirectedWidgetCustomiser
			implements Customiser, BoundWidgetProvider {
		private boolean editable;

		@Override
		public BoundWidget get() {
			if (!editable) {
				return null;
			}
			return null;
		}

		@Override
		public BoundWidgetProvider getProvider(boolean editable,
				Class objectClass, boolean multiple, Custom params) {
			this.editable = editable;
			return null;
		}
	}

	@Override
	public Object getValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setValue(Object value) {
		// TODO Auto-generated method stub
	}
}
