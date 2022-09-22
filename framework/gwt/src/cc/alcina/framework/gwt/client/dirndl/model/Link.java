package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Objects;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelEvent;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

@Directed(receives = DomEvents.Click.class, bindings = {
		@Binding(from = "href", type = Type.PROPERTY),
		@Binding(from = "className", to = "class", type = Type.PROPERTY),
		@Binding(from = "innerHtml", type = Type.INNER_HTML),
		@Binding(from = "text", type = Type.INNER_TEXT),
		@Binding(from = "target", type = Type.PROPERTY),
		@Binding(from = "title", type = Type.PROPERTY) })
// TODO - check conflicting properties pre-render (e.g. inner, innterHtml)
// also document why this is a "non-standard" dirndl component (and merge
// with link)
public class Link extends Model.WithNode
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

	private Class<? extends ModelEvent> modelEvent;

	private String title;

	public Link() {
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

	@Directed
	public Object getInner() {
		return this.inner;
	}

	public String getInnerHtml() {
		return this.innerHtml;
	}

	public Class<? extends ModelEvent> getModelEvent() {
		return this.modelEvent;
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

	public String getTitle() {
		return this.title;
	}

	@Override
	public void onClick(Click event) {
		ClickEvent gwtEvent = (ClickEvent) event.getContext().gwtEvent;
		if (gwtEvent.getNativeButton() == NativeEvent.BUTTON_LEFT) {
			if (modelEvent != null) {
				WidgetUtils.squelchCurrentEvent();
				Context context = NodeEvent.Context.newTopicContext(event,
						node);
				ModelEvent.fire(context, modelEvent, null);
			} else if (runnable != null) {
				WidgetUtils.squelchCurrentEvent();
				runnable.run();
			} else {
				// propagate href
				if (!Objects.equals(tag, "a") && Ax.notBlank(href)) {
					History.newItem(href);
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

	public void setInner(Model inner) {
		this.inner = inner;
	}

	public void setInnerHtml(String innerHtml) {
		this.innerHtml = innerHtml;
	}

	public void setModelEvent(Class<? extends ModelEvent> modelEvent) {
		this.modelEvent = modelEvent;
	}

	public void setPlace(BasePlace place) {
		this.place = place;
		if (place != null) {
			setHref(place.toHrefString());
		}
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
		this.className = className;
		return this;
	}

	public Link withHref(String href) {
		this.href = href;
		return this;
	}

	public Link withInner(Object inner) {
		this.inner = inner;
		return this;
	}

	public Link withInnerHtml(String innerHtml) {
		this.innerHtml = innerHtml;
		return this;
	}

	public Link withModelEvent(Class<? extends ModelEvent> modelEvent) {
		this.modelEvent = modelEvent;
		return this;
	}

	public Link withNewTab(boolean newTab) {
		if (newTab) {
			setTarget("_blank");
		}
		return this;
	}

	public Link withPlace(BasePlace place) {
		setPlace(place);
		return this;
	}

	public Link withRunnable(Runnable runnable) {
		this.runnable = runnable;
		return this;
	}

	public Link withTag(String tag) {
		this.tag = tag;
		return this;
	}

	public Link withTarget(String target) {
		this.target = target;
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

	public Link withWithoutLink(boolean withoutLink) {
		if (withoutLink) {
			setHref("");
		}
		return this;
	}
}