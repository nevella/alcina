package cc.alcina.framework.gwt.client.ide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.gwt.client.gwittir.widget.GridForm;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;

public class ContentViewSections {
	public List<ContentViewSection> sections = new ArrayList<>();

	private List<PaneWrapperWithObjects> beanViews = new ArrayList<>();

	private Handler captionColEqualiser = new Handler() {
		@Override
		public void onAttachOrDetach(AttachEvent event) {
			if (event.isAttached()) {
				int maxLeft = 0;
				for (PaneWrapperWithObjects beanView : beanViews) {
					maxLeft = Math.max(maxLeft, ((GridForm) beanView.getBoundWidget()).getCaptionColumnWidth());
				}
				for (PaneWrapperWithObjects beanView : beanViews) {
					((GridForm) beanView.getBoundWidget()).setCaptionColumnWidth(maxLeft);
					((GridForm) beanView.getBoundWidget()).addStyleName("section-table");
				}
			}
		}
	};

	private boolean editable;

	private boolean autoSave = true;

	public Widget buildWidget(Object bean) {
		FlowPanel fp = new FlowPanel();
		fp.setStyleName("content-view-sections");
		fp.addAttachHandler(captionColEqualiser);
		for (ContentViewSection section : sections) {
			Label sectionLabel = new Label(section.name);
			sectionLabel.setStyleName("section-label");
			fp.add(sectionLabel);
			PaneWrapperWithObjects beanView = new ContentViewFactory().fieldFilter(section).fieldOrder(section)
					.noCaption().createBeanView(bean, editable, null, autoSave, true, null, false);
			beanViews.add(beanView);
			fp.add(beanView);
		}
		return fp;
	}

	public ContentViewSections editable() {
		this.editable = true;
		return this;
	}

	public ContentViewSection section(String name) {
		ContentViewSection section = new ContentViewSection(name);
		sections.add(section);
		return section;
	}

	public class ContentViewSection implements Comparator<Field>, Predicate<Field> {
		public List<String> fieldNames;

		public String name;

		public ContentViewSection(String name) {
			this.name = name;
		}

		@Override
		public int compare(Field o1, Field o2) {
			return fieldNames.indexOf(o1.getPropertyName()) - fieldNames.indexOf(o2.getPropertyName());
		}

		public ContentViewSection fields(String... fieldNames) {
			this.fieldNames = Arrays.asList(fieldNames);
			return this;
		}

		public ContentViewSection fields(List<String> fieldNames) {
			this.fieldNames = fieldNames;
			return this;
		}

		@Override
		public boolean test(Field t) {
			return fieldNames.contains(t.getPropertyName());
		}
	}
}
