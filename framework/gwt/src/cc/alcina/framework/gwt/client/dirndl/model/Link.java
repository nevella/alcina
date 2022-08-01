package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.actions.PermissibleActionHandler.DefaultPermissibleActionHandler;
import cc.alcina.framework.common.client.actions.instances.NonstandardObjectAction;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.EmitsTopic;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Link.LinkRendererPrimaryClassName;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

// FIXME - dirndl.2 - baseplace should implement a  'link provider' interface
// and various subtypes should be subclasses...
/**
 * Also, this class was a very early Dirndl member - can possibly be simplified
 * to 'ideal dirndl' (no transformer, just bindings and resolvers)
 *
 * In fact, that's underway - see Link.Wrapped (which will probably just become
 * Link)
 */
@LinkRendererPrimaryClassName("-ol-primary")
public class Link extends Model {
	private BasePlace place;

	private boolean withoutLink;

	private boolean primaryAction;

	private NonstandardObjectAction objectAction;

	private String text;

	private String className;

	private String href;

	private Class<? extends TopicEvent> topicClass;

	private boolean newTab;

	private String title;

	private Runnable runnable;

	public Link() {
	}

	public Link(Entity entity) {
		withPlace(EntityPlace.forEntity(entity));
		withText(TextProvider.get().getObjectName(entity));
	}

	public void addTo(List<Link> actions) {
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

	public Runnable getRunnable() {
		return this.runnable;
	}

	public String getText() {
		return text;
	}

	public String getTitle() {
		return this.title;
	}

	public Class<? extends TopicEvent> getTopicClass() {
		return topicClass;
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

	public void setRunnable(Runnable runnable) {
		this.runnable = runnable;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setWithoutLink(boolean withoutLink) {
		this.withoutLink = withoutLink;
	}

	public Link withActionRef(Class<? extends ActionRef> clazz) {
		return withPlace(new ActionRefPlace(clazz));
	}

	public Link withClassName(String className) {
		this.className = className;
		return this;
	}

	public Link withHref(String href) {
		this.href = href;
		return this;
	}

	public Link withNewTab(boolean newTab) {
		this.newTab = newTab;
		return this;
	}

	public Link
			withNonstandardObjectAction(NonstandardObjectAction objectAction) {
		this.objectAction = objectAction;
		return this;
	}

	public Link withPlace(BasePlace place) {
		this.place = place;
		return this;
	}

	public Link withPrimaryAction(boolean primaryAction) {
		this.primaryAction = primaryAction;
		return this;
	}

	public Link withRunnable(Runnable runnable) {
		this.runnable = runnable;
		return this;
	}

	public Link withText(String text) {
		this.text = text;
		return this;
	}

	public Link withTitle(String title) {
		this.title = title;
		return this;
	}

	public Link withTopic(Class<? extends TopicEvent> topicClass) {
		this.topicClass = topicClass;
		return this;
	}

	public Link withWithoutLink(boolean withoutLink) {
		this.withoutLink = withoutLink;
		return this;
	}

	public static class LinkRenderer extends LeafNodeRenderer {
		@Override
		public Widget render(Node node) {
			Link model = model(node);
			Widget rendered = super.render(node);
			rendered.getElement().setInnerText(getText(node));
			rendered.getElement().setTitle(model.getTitle());
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
				if (model.getTopicClass() != null) {
					rendered.addDomHandler(event -> {
						Context context = NodeEvent.Context
								.newTopicContext(event, node);
						TopicEvent.fire(context, model.getTopicClass(), null);
					}, ClickEvent.getType());
				}
				if (model.getRunnable() != null) {
					rendered.addDomHandler(event -> {
						model.getRunnable().run();
					}, ClickEvent.getType());
				}
			} else {
				rendered.getElement().setAttribute("href",
						place.toHrefString());
				if (model.isNewTab()) {
					rendered.getElement().setAttribute("target", "_blank");
				}
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
					Optional<EmitsTopic> emitsTopic = actionRefPlace
							.emitsTopic();
					if (emitsTopic.isPresent()
							&& !emitsTopic.get().hasValidation()) {
						rendered.addDomHandler(event -> {
							Class<? extends TopicEvent> type = emitsTopic.get()
									.value();
							Context context = NodeEvent.Context
									.newTopicContext(event, node);
							TopicEvent.fire(context, type, null);
						}, ClickEvent.getType());
					}
				}
			}
			if (model.isPrimaryAction()) {
				LinkRendererPrimaryClassName primaryClassName = node
						.annotation(LinkRendererPrimaryClassName.class);
				if (primaryClassName != null) {
					rendered.addStyleName(primaryClassName.value());
				}
			}
			return rendered;
		}

		private BasePlace getPlace(Node node) {
			return model(node).getPlace();
		}

		private Link model(Node node) {
			return (Link) node.getModel();
		}

		@Override
		protected String getTag(Node node) {
			return model(node).isWithoutLink() ? "span" : "a";
		}

		protected String getText(Node node) {
			Link model = model(node);
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
	public @interface LinkRendererPrimaryClassName {
		String value();
	}

	@Directed(receives = DomEvents.Click.class, emits = DomEvents.Click.class, bindings = {
			@Binding(from = "href", type = Type.PROPERTY),
			@Binding(from = "className", to = "class", type = Type.PROPERTY),
			@Binding(from = "innerHtml", type = Type.INNER_HTML),
			@Binding(from = "text", type = Type.INNER_TEXT),
			@Binding(from = "target", type = Type.PROPERTY) })
	// TODO - check conflicting properties pre-render (e.g. inner, innterHtml)
	// also document why this is a "non-standard" dirndl component (and merge
	// with link)
	public static class Wrapper extends Model
			implements DomEvents.Click.Handler, HasTag {
		private String href = "#";

		private String target;

		private Object inner;

		private String innerHtml;

		private String className;

		private String text;

		private transient Runnable runnable;

		private BasePlace place;

		private String tag = "a";

		public Wrapper() {
		}

		public String getClassName() {
			return this.className;
		}

		public String getHref() {
			return this.href;
		}

		@Directed
		public Object getInner() {
			return this.inner;
		}

		public String getInnerHtml() {
			return this.innerHtml;
		}

		// only rendered via href, but useful for equivalence testing
		public BasePlace getPlace() {
			return this.place;
		}

		public Runnable getRunnable() {
			return this.runnable;
		}

		public String getTarget() {
			return this.target;
		}

		public String getText() {
			return this.text;
		}

		@Override
		public void onClick(Click event) {
			if (runnable != null) {
				runnable.run();
				WidgetUtils.squelchCurrentEvent();
			} else {
				// propagate href
				if (!Objects.equals(tag, "a") && Ax.notBlank(href)) {
					History.newItem(href);
				}
			}
		}

		@Override
		public String provideTag() {
			return tag;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public void setHref(String href) {
			this.href = href;
		}

		public void setInner(Model inner) {
			this.inner = inner;
		}

		public void setInnerHtml(String innerHtml) {
			this.innerHtml = innerHtml;
		}

		public void setPlace(BasePlace place) {
			this.place = place;
		}

		public void setTarget(String target) {
			this.target = target;
		}

		public void setText(String text) {
			this.text = text;
		}

		public Wrapper withClassName(String className) {
			this.className = className;
			return this;
		}

		public Wrapper withHref(String href) {
			this.href = href;
			return this;
		}

		public Wrapper withInner(Object inner) {
			this.inner = inner;
			return this;
		}

		public Wrapper withInnerHtml(String innerHtml) {
			this.innerHtml = innerHtml;
			return this;
		}

		public Wrapper withPlace(BasePlace place) {
			this.place = place;
			this.href = place.toHrefString();
			return this;
		}

		public Wrapper withRunnable(Runnable runnable) {
			this.runnable = runnable;
			return this;
		}

		public Wrapper withTag(String tag) {
			this.tag = tag;
			return this;
		}

		public Wrapper withTarget(String target) {
			this.target = target;
			return this;
		}

		public Wrapper withText(String text) {
			this.text = text;
			return this;
		}
	}
}
