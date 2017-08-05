package cc.alcina.framework.gwt.client.ide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.util.ClientUtils;

public class ContentViewSections {
	public List<ContentViewSection> sections = new ArrayList<>();

	private List<PaneWrapperWithObjects> beanViews = new ArrayList<>();

	private Handler captionColEqualiser = new Handler() {
		@Override
		public void onAttachOrDetach(AttachEvent event) {
			if (event.isAttached()) {
				int maxLeft = 0;
				// for (PaneWrapperWithObjects beanView : beanViews) {
				// System.out.println("get max cc w");
				// maxLeft = Math.max(maxLeft,
				// ((GridForm) beanView.getBoundWidget())
				// .getCaptionColumnWidth());
				// }
				// for (PaneWrapperWithObjects beanView : beanViews) {
				// ((GridForm) beanView.getBoundWidget())
				// .setCaptionColumnWidth(maxLeft);
				// ((GridForm) beanView.getBoundWidget())
				// .addStyleName("section-table");
				// }
			}
		}
	};

	private boolean editable;

	private boolean autoSave = true;

	private PermissibleActionListener createListener;

	private PermissibleActionListener actionListener;

	public boolean isAutoSave() {
		return this.autoSave;
	}

	public void setAutoSave(boolean autoSave) {
		this.autoSave = autoSave;
	}

	public Widget buildWidget(Object bean) {
		FlowPanel fp = new FlowPanel();
		fp.setStyleName("content-view-sections");
		fp.addAttachHandler(captionColEqualiser);
		for (int idx = 0; idx < sections.size(); idx++) {
			ContentViewSection section = sections.get(idx);
			InlineLabel sectionLabel = new InlineLabel(section.name);
			SimplePanel holder = new SimplePanel(sectionLabel);
			holder.setStyleName("section-label");
			fp.add(holder);
			ContentViewFactory contentViewFactory = new ContentViewFactory();
			if (idx < sections.size() - 1) {
				contentViewFactory.setNoButtons(true);
			}
			if (!autoSave) {
				contentViewFactory.okButtonName("Create");
			}
			PaneWrapperWithObjects beanView = contentViewFactory
					.fieldFilter(section).fieldOrder(section)
					.editableFieldFilter(section.editableFieldFilter())
					.fieldPostReflectiveSetupModifier(
							section.fieldPostReflectiveSetupModifier)
					.noCaption().createBeanView(bean, editable, createListener,
							autoSave, true, null, false);
			beanViews.add(beanView);
			fp.add(beanView);
		}
		return fp;
	}

	public ContentViewSections editable() {
		this.editable = true;
		return this;
	}

	public boolean isEditable() {
		return this.editable;
	}

	public ContentViewSection section(String name) {
		ContentViewSection section = new ContentViewSection(name);
		sections.add(section);
		return section;
	}

	public class ContentViewSection
			implements Comparator<Field>, Predicate<Field> {
		public List<String> fieldNames;

		public String name;

		private List<String> editableFieldNames;

		private Consumer<Field> fieldPostReflectiveSetupModifier;

		public ContentViewSection(String name) {
			this.name = name;
		}

		public Predicate<String> editableFieldFilter() {
			return s -> editableFieldNames == null
					|| editableFieldNames.contains(s);
		}

		public ContentViewSection fieldPostReflectiveSetupModifier(
				Consumer<Field> fieldPostReflectiveSetupModifier) {
			this.fieldPostReflectiveSetupModifier = fieldPostReflectiveSetupModifier;
			return this;
		}

		@Override
		public int compare(Field o1, Field o2) {
			return fieldNames.indexOf(o1.getPropertyName())
					- fieldNames.indexOf(o2.getPropertyName());
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

		public ContentViewSection editableFields(String... editableFieldNames) {
			this.editableFieldNames = Arrays.asList(editableFieldNames);
			return this;
		}
	}

	public void addCreateListener(PermissibleActionListener createListener) {
		this.createListener = createListener;
	}

	public ContentViewSections editable(boolean editable) {
		this.editable = editable;
		return this;
	}

	public ContentViewSections allFields(Object bean) {
		BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
		Field[] fields = GwittirBridge.get()
				.fieldsForReflectedObjectAndSetupWidgetFactory(bean, factory,
						editable, false);
		section("").fields(Arrays.asList(fields).stream()
				.map(Field::getPropertyName).collect(Collectors.toList()));
		buildWidget(bean);
		return this;
	}

	public class ContentViewSectionsDialogBuilder {
		private boolean noGlass = false;

		private String caption;

		private String okButtonName="OK";

		private String cancelButtonName="Cancel";

		public ContentViewSectionsDialogBuilder
				okButtonName(String okButtonName) {
			this.okButtonName = okButtonName;
			return this;
		}

		public ContentViewSectionsDialogBuilder
				cancelButtonName(String cancelButtonName) {
			this.cancelButtonName = cancelButtonName;
			return this;
		}

		public ContentViewSectionsDialogBuilder caption(String caption) {
			this.caption = caption;
			return this;
		}

		public ContentViewSectionsDialogBuilder noGlass() {
			noGlass = true;
			return this;
		}

		public void show() {
			ClientUtils.createEditContentViewWidgets(actionListener, caption,
					"", beanViews.get(0), noGlass, true, true, true, true,
					okButtonName, cancelButtonName);
		}
	}

	public ContentViewSectionsDialogBuilder dialog() {
		return new ContentViewSectionsDialogBuilder();
	}

	public ContentViewSections
			actionListener(PermissibleActionListener actionListener) {
		this.actionListener = actionListener;
		return this;
	}
}
