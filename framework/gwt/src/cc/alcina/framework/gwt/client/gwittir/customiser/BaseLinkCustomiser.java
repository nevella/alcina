package cc.alcina.framework.gwt.client.gwittir.customiser;

import java.util.function.Function;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundLink;

public abstract class BaseLinkCustomiser<T> implements Customiser, BoundWidgetProvider {
	public static class BaseLink<T> extends BoundLink<T> {
		private BaseLinkCustomiser<T> customiser;
		@Override
		public void setValue(T t) {
			if (t != null) {
				this.setText(customiser.getTextFunction().apply(t));
				setHref("#" + customiser.getTokenFunction().apply(t));
			}
			enableDefault();
		}
	}

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom params) {
		return this;
	}
	protected abstract Function<T,String> getTokenFunction() ;
	protected abstract Function<T,String> getTextFunction() ;

	@Override
	public BoundWidget get() {
		BaseLink baseLink = new BaseLink();
		baseLink.customiser=this;
		return baseLink;
	}
}
