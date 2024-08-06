package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Collection;
import java.util.Objects;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionHandler.DefaultPermissibleActionHandler;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

@Directed(
	bindings = { @Binding(from = "href", type = Type.PROPERTY),
			@Binding(from = "className", type = Type.CLASS_PROPERTY),
			@Binding(from = "innerHtml", type = Type.INNER_HTML),
			@Binding(from = "text", type = Type.INNER_TEXT),
			@Binding(from = "target", type = Type.PROPERTY),
			@Binding(from = "title", type = Type.PROPERTY),
			@Binding(from = "id", type = Type.PROPERTY) })
// TODO - check conflicting properties pre-render (e.g. inner, innterHtml)
// also document why this is a "non-standard" dirndl component (and merge
// with link)
/*
 * This class did - momentarily - support withRunnable(runnable) - removed,
 * favouring withModelEvent.
 *
 * If the link itself must handle the event, rather than an ancestor (unlikely,
 * but possible), subclass Link and have the subclass implement the appropriate
 * ModelEvent.Handler
 */
public class Link extends Model implements DomEvents.Click.Handler, HasTag {
	public static Link of(Class<? extends ModelEvent> modelEvent) {
		return new Link().withModelEvent(modelEvent).withTextFromModelEvent();
	}

	public static Link button(Class<? extends ModelEvent> modelEvent) {
		return of(modelEvent).withTag("button");
	}

	private static final transient String INITIAL_HREF = "#";

	public static final transient String PRIMARY_ACTION = "primary-action";

	private String href = INITIAL_HREF;

	private String target;

	private Object inner;

	private String innerHtml;

	private String className;

	private String text;

	private BasePlace place;

	private String tag = "a";

	private Class<? extends ModelEvent> modelEvent;

	private String title;

	private String id;

	private Class<? extends PermissibleAction> nonStandardObjectAction;

	public Link() {
	}

	public void addTo(Collection<Link> hasLinks) {
		hasLinks.add(this);
	}

	public void addTo(HasLinks hasLinks) {
		hasLinks.add(this);
	}

	public String getClassName() {
		return this.className;
	}

	public String getHref() {
		return this.href;
	}

	public String getId() {
		return this.id;
	}

	@Directed
	// too unbounded for serialization
	@AlcinaTransient
	public Object getInner() {
		return this.inner;
	}

	public String getInnerHtml() {
		return this.innerHtml;
	}

	public Class<? extends ModelEvent> getModelEvent() {
		return this.modelEvent;
	}

	public Class<? extends PermissibleAction> getNonStandardObjectAction() {
		return this.nonStandardObjectAction;
	}

	// only rendered via href, but useful for equivalence testing
	public BasePlace getPlace() {
		return this.place;
	}

	public String getTag() {
		return this.tag;
	}

	public String getTarget() {
		return this.target;
	}

	public String getText() {
		return this.text;
	}

	public String getTitle() {
		return this.title;
	}

	@Override
	public void onClick(Click event) {
		ClickEvent gwtEvent = (ClickEvent) event.getContext().getGwtEvent();
		if (gwtEvent.getNativeButton() == NativeEvent.BUTTON_LEFT) {
			if (modelEvent != null) {
				WidgetUtils.squelchCurrentEvent();
				event.reemitAs(this, modelEvent);
			} else if (nonStandardObjectAction != null) {
				WidgetUtils.squelchCurrentEvent();
				DefaultPermissibleActionHandler.handleAction(
						((DirectedLayout.Node) event.getSource()).getRendered()
								.as(Widget.class),
						Reflections.newInstance(nonStandardObjectAction),
						((EntityPlace) Client.currentPlace()).provideEntity());
			} else {
				// propagate href (not squelched)
				if (Ax.notBlank(href)) {
					if (!Objects.equals(tag, "a")) {
						History.newItem(href);
					}
				}
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

	public void setId(String id) {
		this.id = id;
	}

	public void setInner(Object inner) {
		this.inner = inner;
	}

	public void setInnerHtml(String innerHtml) {
		this.innerHtml = innerHtml;
	}

	public void setModelEvent(Class<? extends ModelEvent> modelEvent) {
		this.modelEvent = modelEvent;
	}

	public void setNonStandardObjectAction(
			Class<? extends PermissibleAction> nonStandardObjectAction) {
		this.nonStandardObjectAction = nonStandardObjectAction;
	}

	public void setPlace(BasePlace place) {
		this.place = place;
		if (place != null) {
			if (text == null && inner == null) {
				setText(place.toTitleString());
			}
			if (Objects.equals(href, INITIAL_HREF)) {
				setHref(place.toHrefString());
			}
		}
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		FormatBuilder fb = new FormatBuilder().separator("\n");
		fb.appendIfNotBlankKv("tag", tag);
		fb.appendIfNotBlankKv("className", className);
		fb.appendIfNotBlankKv("href", href);
		fb.appendIfNotBlankKv("place ", place);
		return fb.toString();
	}

	public Link withClassName(String className) {
		setClassName(className);
		return this;
	}

	public Link withHref(String href) {
		setHref(href);
		return this;
	}

	public Link withId(String id) {
		setId(id);
		return this;
	}

	public Link withInner(Object inner) {
		setInner(inner);
		return this;
	}

	public Link withInnerHtml(String innerHtml) {
		setInnerHtml(innerHtml);
		return this;
	}

	public Link withModelEvent(Class<? extends ModelEvent> modelEvent) {
		setModelEvent(modelEvent);
		return this;
	}

	public Link withNewTab(boolean newTab) {
		if (newTab) {
			setTarget("_blank");
		}
		return this;
	}

	public Link withoutHref(boolean withoutHref) {
		if (withoutHref) {
			setHref("");
		}
		return this;
	}

	public Link withNonstandardObjectAction(
			Class<? extends PermissibleAction> nonStandardObjectAction) {
		setNonStandardObjectAction(nonStandardObjectAction);
		return this;
	}

	public Link withPlace(BasePlace place) {
		setPlace(place);
		return this;
	}

	public Link withTag(String tag) {
		setTag(tag);
		return this;
	}

	public Link withTarget(String target) {
		setTarget(target);
		return this;
	}

	public Link withTargetBlank() {
		return withTarget("_blank");
	}

	public Link withText(String text) {
		setText(text);
		return this;
	}

	public Link withTextFromModelEvent() {
		setText(ModelEvent.staticDisplayName(modelEvent));
		return this;
	}

	public Link withTitle(String title) {
		setTitle(title);
		return this;
	}

	public static class AnchorTransform
			implements ModelTransform<Object, Link> {
		@Override
		public Link apply(Object t) {
			return new Link().withId(t.toString());
		}
	}

	public static class HrefTransform implements ModelTransform<Object, Link> {
		@Override
		public Link apply(Object t) {
			if (t == null) {
				return null;
			}
			String url = t.toString();
			return new Link().withHref(url)
					.withText(CommonUtils.shortenPath(url, 60)).withTitle(url);
		}
	}

	public Link withText(Object textSource) {
		return withText(textSource.toString());
	}
}