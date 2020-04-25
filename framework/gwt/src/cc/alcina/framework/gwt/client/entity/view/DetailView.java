package cc.alcina.framework.gwt.client.entity.view;

import java.beans.PropertyChangeEvent;

import com.google.gwt.user.client.ui.FlowPanel;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.VersionableDomainBase;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.view.ViewModel.DetailViewModel;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;

public abstract class DetailView<DVM extends DetailViewModel>
        extends AbstractViewModelView<DVM> {
    private static final String TOPIC_DETAIL_MODEL_OBJECT_SET = DetailView.class
            .getName() + "." + "TOPIC_DETAIL_MODEL_OBJECT_SET";

    public static TopicSupport<Entity> topicDetailModelObjectSet() {
        return new TopicSupport<>(TOPIC_DETAIL_MODEL_OBJECT_SET);
    }

    protected FlowPanel fp;

    protected FlowPanel toolbar;

    public DetailView() {
        this.fp = new FlowPanel();
        initWidget(fp);
        fp.setStyleName(getPanelCssName());
        new KeyboardActionHandler().setup(this, 'E', () -> {
            if (model.getPlace() != null
                    && model.getPlace() instanceof EntityPlace) {
                EntityPlace place = (EntityPlace) model.getPlace();
                if (place.getAction() == EntityAction.VIEW
                        || place.getAction() == null) {
                    AppController.get()
                            .doEdit((VersionableDomainBase) model.getModelObject());
                } else {
                    AppController.get()
                            .doView((VersionableDomainBase) model.getModelObject());
                }
            }
        });
    }

    public void displayDetail() {
        fp.clear();
        topicDetailModelObjectSet().publish(model.getModelObject());
        renderToolbar();
        renderNotificationBox();
        renderForm();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!model.isActive()) {
            return;
        }
        if ("modelObject".equals(evt.getPropertyName())) {
            if (model.getModelObject() == null) {
                clearDetail();
            } else {
                displayDetail();
            }
        }
    }

    protected void clearDetail() {
        fp.clear();
    }

    protected abstract String getPanelCssName();

    protected abstract void renderForm();

    protected void renderNotificationBox() {
        FlowPanel styledPanel = UsefulWidgetFactory
                .styledPanel("static-notification-box");
        renderNotificationBoxWidgets(styledPanel);
        if (styledPanel.getWidgetCount() > 0) {
            fp.add(styledPanel);
        }
    }

    protected void renderNotificationBoxWidgets(FlowPanel styledPanel) {
    }

    protected abstract void renderToolbar();
}
