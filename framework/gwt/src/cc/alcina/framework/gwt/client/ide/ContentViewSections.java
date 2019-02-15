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

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.OkAction;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.widget.GridFormCellRenderer;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.util.ClientUtils.EditContentViewWidgets;
import cc.alcina.framework.gwt.client.widget.typedbinding.EnumeratedBinding;

public class ContentViewSections {
    public List<ContentViewSection> sections = new ArrayList<>();

    public List<PaneWrapperWithObjects> beanViews = new ArrayList<>();

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

    private String tableStyleName;

    private boolean editable;

    private boolean autoSave = true;

    private PermissibleActionListener createListener;

    private PermissibleActionListener actionListener;

    private boolean cancelButton;

    private boolean noAdminOverride;

    private String okButtonName;

    public ContentViewSections actionListener(
            PermissibleActionListener actionListener) {
        this.actionListener = actionListener;
        return this;
    }

    public ContentViewSections addCreateListener(
            PermissibleActionListener createListener) {
        this.createListener = createListener;
        return this;
    }

    public ContentViewSections allFields(Object bean) {
        return allFields(bean, o -> true);
    }

    public ContentViewSections allFields(Object bean, Predicate<Field> filter) {
        BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
        Field[] fields = GwittirBridge.get()
                .fieldsForReflectedObjectAndSetupWidgetFactory(bean, factory,
                        editable, false);
        section("").fields(Arrays.asList(fields).stream().filter(filter)
                .map(Field::getPropertyName).collect(Collectors.toList()));
        buildWidget(bean);
        return this;
    }

    public Widget buildWidget(Object bean) {
        FlowPanel fp = new FlowPanel();
        fp.setStyleName("content-view-sections");
        fp.addAttachHandler(captionColEqualiser);
        for (int idx = 0; idx < sections.size(); idx++) {
            ContentViewSection section = sections.get(idx);
            if (Ax.notBlank(section.name)) {
                InlineLabel sectionLabel = new InlineLabel(section.name);
                SimplePanel holder = new SimplePanel(sectionLabel);
                holder.setStyleName("section-label");
                fp.add(holder);
            }
            ContentViewFactory contentViewFactory = new ContentViewFactory();
            if (idx < sections.size() - 1) {
                contentViewFactory.setNoButtons(true);
            }
            if (!autoSave) {
                contentViewFactory.okButtonName("Create");
            }
            if (okButtonName != null) {
                contentViewFactory.okButtonName(okButtonName);
            }
            contentViewFactory.fieldFilter(section).fieldOrder(section)
                    .editableFieldFilter(section.editableFieldFilter())
                    .fieldPostReflectiveSetupModifier(
                            section.fieldPostReflectiveSetupModifier)
                    .noCaption();
            contentViewFactory.horizontalGrid(section.horizontalGrid);
            contentViewFactory.setCellRenderer(section.gridFormCellRenderer);
            contentViewFactory.setCancelButton(cancelButton);
            contentViewFactory.editable(editable)
                    .actionListener(new MultiListener()).autoSave(autoSave)
                    .doNotClone(true).additionalProvisional(null)
                    .doNotPrepare(false);
            contentViewFactory.setTableStyleName(tableStyleName);
            PaneWrapperWithObjects beanView = contentViewFactory
                    .createBeanView(bean);
            if (cancelButton) {
                beanView.setFireOkButtonClickAsOkActionEvent(true);
            }
            beanView.setAlwaysDisallowOkIfInvalid(noAdminOverride);
            beanViews.add(beanView);
            fp.add(beanView);
        }
        return fp;
    }

    public ContentViewSections cancelButton(boolean cancelButton) {
        this.cancelButton = cancelButton;
        return this;
    }

    public ContentViewSectionsDialogBuilder dialog() {
        return new ContentViewSectionsDialogBuilder();
    }

    public ContentViewSections editable() {
        this.editable = true;
        return this;
    }

    public ContentViewSections editable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public String getOkButtonName() {
        return this.okButtonName;
    }

    public String getTableStyleName() {
        return this.tableStyleName;
    }

    public boolean isAutoSave() {
        return this.autoSave;
    }

    public boolean isEditable() {
        return this.editable;
    }

    public ContentViewSections noAdminOverride() {
        this.noAdminOverride = true;
        return this;
    }

    public ContentViewSection section(String name) {
        ContentViewSection section = new ContentViewSection(name);
        sections.add(section);
        return section;
    }

    public ContentViewSections setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
        return this;
    }

    public void setOkButtonName(String okButtonName) {
        this.okButtonName = okButtonName;
    }

    public void setTableStyleName(String tableStyleName) {
        this.tableStyleName = tableStyleName;
    }

    public class ContentViewSection
            implements Comparator<Field>, Predicate<Field> {
        private boolean horizontalGrid;

        public List<String> fieldNames;

        public String name;

        private List<String> editableFieldNames;

        private Consumer<Field> fieldPostReflectiveSetupModifier;

        private GridFormCellRenderer gridFormCellRenderer;

        public ContentViewSection(String name) {
            this.name = name;
        }

        public ContentViewSection cellRenderer(
                GridFormCellRenderer gridFormCellRenderer) {
            this.gridFormCellRenderer = gridFormCellRenderer;
            return this;
        }

        @Override
        public int compare(Field o1, Field o2) {
            return fieldNames.indexOf(o1.getPropertyName())
                    - fieldNames.indexOf(o2.getPropertyName());
        }

        public Predicate<String> editableFieldFilter() {
            return s -> editableFieldNames == null
                    || editableFieldNames.contains(s);
        }

        public ContentViewSection editableFields(String... editableFieldNames) {
            this.editableFieldNames = Arrays.asList(editableFieldNames);
            return this;
        }

        public ContentViewSection fieldBindings(
                List<EnumeratedBinding> enumeratedBindings) {
            this.fieldNames = enumeratedBindings.stream()
                    .map(EnumeratedBinding::getPath)
                    .collect(Collectors.toList());
            return this;
        }

        public ContentViewSection fieldPostReflectiveSetupModifier(
                Consumer<Field> fieldPostReflectiveSetupModifier) {
            this.fieldPostReflectiveSetupModifier = fieldPostReflectiveSetupModifier;
            return this;
        }

        public ContentViewSection fields(
                EnumeratedBinding... enumeratedBindings) {
            return fieldBindings(Arrays.asList(enumeratedBindings));
        }

        public ContentViewSection fields(List<String> fieldNames) {
            this.fieldNames = fieldNames;
            return this;
        }

        public ContentViewSection fields(String... fieldNames) {
            this.fieldNames = Arrays.asList(fieldNames);
            return this;
        }

        public ContentViewSection horizontalGrid() {
            this.horizontalGrid = true;
            return this;
        }

        @Override
        public boolean test(Field t) {
            return fieldNames.contains(t.getPropertyName());
        }
    }

    public class ContentViewSectionsDialogBuilder {
        private boolean noGlass = false;

        private String caption;

        private String okButtonName = "OK";

        private String cancelButtonName = "Cancel";

        private boolean withCancel = true;

        private String className;

        private boolean withOk = true;

        public ContentViewSectionsDialogBuilder actionListener(
                PermissibleActionListener actionListener) {
            ContentViewSections.this.actionListener = actionListener;
            return this;
        }

        public ContentViewSectionsDialogBuilder cancelButtonName(
                String cancelButtonName) {
            this.cancelButtonName = cancelButtonName;
            return this;
        }

        public ContentViewSectionsDialogBuilder caption(String caption) {
            this.caption = caption;
            return this;
        }

        public ContentViewSectionsDialogBuilder className(String className) {
            this.className = className;
            return this;
        }

        public ContentViewSectionsDialogBuilder noGlass() {
            noGlass = true;
            return this;
        }

        public ContentViewSectionsDialogBuilder okButtonName(
                String okButtonName) {
            this.okButtonName = okButtonName;
            return this;
        }

        public EditContentViewWidgets show() {
            return ClientUtils.createEditContentViewWidgets(null, caption, "",
                    beanViews.get(0), noGlass, true, true, withOk, withCancel,
                    okButtonName, cancelButtonName, className);
        }

        public ContentViewSectionsDialogBuilder withCancel(boolean withCancel) {
            this.withCancel = withCancel;
            return this;
        }

        public ContentViewSectionsDialogBuilder withOk(boolean withOk) {
            this.withOk = withOk;
            return this;
        }
    }

    class MultiListener implements PermissibleActionListener {
        @Override
        public void vetoableAction(PermissibleActionEvent evt) {
            if (evt.getAction().getClass() == OkAction.class || !cancelButton) {
                if (createListener != null) {
                    createListener.vetoableAction(evt);
                }
            }
            if (actionListener != null) {
                actionListener.vetoableAction(evt);
            }
        }
    }
}
