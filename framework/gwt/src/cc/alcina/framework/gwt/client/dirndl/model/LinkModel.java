package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Optional;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.actions.PermissibleActionHandler.DefaultPermissibleActionHandler;
import cc.alcina.framework.common.client.actions.instances.NonstandardObjectAction;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour.TopicBehaviour;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;
import cc.alcina.framework.gwt.client.dirndl.model.LinkModel.LinkModelRendererPrimaryClassName;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.BasePlace;

//FIXME - dirndl.2 - baseplace should implement a  'link provider' interface
// and various subtypes should be subclasses...
@Bean
@LinkModelRendererPrimaryClassName("-ol-primary")
public class LinkModel extends Model {
	private BasePlace place;

	private boolean withoutLink;

	private boolean primaryAction;

	private NonstandardObjectAction objectAction;

	private String text;

	private String className;

	private String href;

	private boolean newTab;

	public LinkModel() {
	}

	public LinkModel(Entity entity) {
		withPlace(EntityPlace.forEntity(entity));
		withText(TextProvider.get().getObjectName(entity));
	}

	public void addTo(List<LinkModel> actions) {
		actions.add(this);
	}

	public String getClassName() {
		return this.className;
	}

	public String getHref() {
		return this.href;
	}

	public NonstandardObjectAction getObjectAction() {
		return this.objectAction;
	}

	public BasePlace getPlace() {
		return this.place;
	}

	public String getText() {
		return text;
	}

	public boolean isNewTab() {
		return this.newTab;
	}

	public boolean isPrimaryAction() {
		return this.primaryAction;
	}

	public boolean isWithoutLink() {
		return this.withoutLink;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public void setNewTab(boolean newTab) {
		this.newTab = newTab;
	}

	public void setObjectAction(NonstandardObjectAction objectAction) {
		this.objectAction = objectAction;
	}

	public void setPlace(BasePlace place) {
		this.place = place;
	}

	public void setPrimaryAction(boolean primaryAction) {
		this.primaryAction = primaryAction;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setWithoutLink(boolean withoutLink) {
		this.withoutLink = withoutLink;
	}

	public LinkModel withActionRef(Class<? extends ActionRef> clazz) {
		return withPlace(new ActionRefPlace(clazz));
	}

	public LinkModel withClassName(String className) {
		this.className = className;
		return this;
	}

	public LinkModel withHref(String href) {
		this.href = href;
		return this;
	}

	public LinkModel withNewTab(boolean newTab) {
		this.newTab = newTab;
		return this;
	}

	public LinkModel
			withNonstandardObjectAction(NonstandardObjectAction objectAction) {
		this.objectAction = objectAction;
		return this;
	}

	public LinkModel withPlace(BasePlace place) {
		this.place = place;
		return this;
	}

	public LinkModel withPrimaryAction(boolean primaryAction) {
		this.primaryAction = primaryAction;
		return this;
	}

	public LinkModel withText(String text) {
		this.text = text;
		return this;
	}

	public LinkModel withWithoutLink(boolean withoutLink) {
		this.withoutLink = withoutLink;
		return this;
	}

	@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = LinkModel.class)
	public static class LinkModelRenderer extends LeafNodeRenderer {
		@Override
		public Widget render(Node node) {
			LinkModel model = model(node);
			Widget rendered = super.render(node);
			rendered.getElement().setInnerText(getText(node));
			if (model.isWithoutLink() && model.getText() != null) {
				return rendered;
			}
			NonstandardObjectAction objectAction = model(node)
					.getObjectAction();
			if (objectAction != null) {
				EntityPlace currentPlace = (EntityPlace) Client.currentPlace();
				rendered.getElement().setAttribute("href", "#");
				rendered.addDomHandler(
						e -> DefaultPermissibleActionHandler.handleAction(
								(Widget) e.getSource(), objectAction,
								currentPlace.provideEntity()),
						ClickEvent.getType());
				return rendered;
			}
			BasePlace place = model.getPlace();
			if (place == null) {
				if (model.getHref() != null) {
					rendered.getElement().setAttribute("href", model.getHref());
				}
				if (model.isNewTab()) {
					rendered.getElement().setAttribute("target", "_blank");
				}
			} else {
				rendered.getElement().setAttribute("href",
						place.toHrefString());
				if (place instanceof ActionRefPlace) {
					ActionRefPlace actionRefPlace = (ActionRefPlace) place;
					Optional<ActionHandler> actionHandler = actionRefPlace
							.getActionHandler();
					if (actionHandler.isPresent()) {
						rendered.getElement().setAttribute("href", "#");
						rendered.addDomHandler(
								evt -> actionHandler.get().handleAction(node,
										evt, actionRefPlace),
								ClickEvent.getType());
					}
					Optional<TopicBehaviour> actionTopic = actionRefPlace
							.getActionTopic();
					if (actionTopic.isPresent()) {
						rendered.addDomHandler(event -> {
							TopicBehaviour behaviour = actionTopic.get();
							Context context = NodeEvent.Context
									.newTopicContext(event, node);
							TopicEvent.fire(context, behaviour.topic(),
									behaviour.payloadTransformer(), null,
									false);
						}, ClickEvent.getType());
					}
				}
			}
			if (model.isPrimaryAction()) {
				LinkModelRendererPrimaryClassName primaryClassName = node
						.annotation(LinkModelRendererPrimaryClassName.class);
				if (primaryClassName != null) {
					rendered.addStyleName(primaryClassName.value());
				}
			}
			return rendered;
		}

		private BasePlace getPlace(Node node) {
			return model(node).getPlace();
		}

		private LinkModel model(Node node) {
			return (LinkModel) node.getModel();
		}

		@Override
		protected String getTag(Node node) {
			return model(node).isWithoutLink() ? "span" : "a";
		}

		protected String getText(Node node) {
			LinkModel model = model(node);
			if (model.getText() != null) {
				return model.getText();
			}
			NonstandardObjectAction objectAction = model.getObjectAction();
			if (objectAction != null) {
				return objectAction.getDisplayName();
			}
			return getPlace(node) == null ? "<null text>"
					: getPlace(node).toNameString();
		}
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Inherited
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface LinkModelRendererPrimaryClassName {
		String value();
	}
}