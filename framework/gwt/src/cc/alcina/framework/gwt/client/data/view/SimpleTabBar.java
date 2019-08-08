package cc.alcina.framework.gwt.client.data.view;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.widget.Link;

public abstract class SimpleTabBar<T extends HasDisplayName> extends Composite {
	private FlowPanel fp;

	protected T selected;

	public SimpleTabBar(T selected) {
		this.selected = selected;
		this.fp = new FlowPanel();
		initWidget(fp);
		setStyleName("simple-tab");
		addAttachHandler(evt -> refresh());
	}

	public void refresh() {
		fp.clear();
		T current = getCurrent();
		for (T value : values()) {
			Link link = new Link(value.displayName());
			link.setStyleName("tab-caption-2");
			link.setStyleName("unselected", value != current);
			link.setHref(getHref(value));
			fp.add(link);
		}
	}

	protected T getCurrent() {
		return selected;
	}

	protected abstract String getHref(T value);

	protected abstract List<T> values();

	public static class SingleTabBar extends SimpleTabBar<HasDisplayName> {
		public SingleTabBar(String name) {
			super(new HasDisplayName() {
				@Override
				public String displayName() {
					return name;
				}
			});
		}

		@Override
		protected String getHref(HasDisplayName value) {
			return Window.Location.getHref();
		}

		@Override
		protected List<HasDisplayName> values() {
			return Arrays.asList(selected);
		}
	}
}
