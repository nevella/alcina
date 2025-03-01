package cc.alcina.framework.gwt.client.dirndl.model.component;

import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Copy;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.CopyToClipboard;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.model.HeadingActions;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay.Attributes;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

/*
 * A rich viewer for strings in forms/tables. Note that because this is used to
 * replace a String model, the rendered tag will be from the originating
 * property (it doesn't implement Model.ResetDirecteds to avoid clobbering table
 * containment, etc)
 * 
 * So all styles should be applied to inner
 */
@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class StringArea extends Model.Fields
		implements DomEvents.Click.Handler, ModelEvents.Closed.Handler {
	@Directed(tag = "string-value")
	public String renderedValue;

	String value;

	AnnotationLocation location;

	boolean json;

	public StringArea(AnnotationLocation location, String value) {
		this.location = location;
		this.value = value;
		this.renderedValue = Ax.trim(value, 300);
	}

	static class InnerResolver extends ContextResolver {
		@Override
		protected Object resolveModel(AnnotationLocation location,
				Object model) {
			return model;
		}
	}

	public static class PreWrap extends ModelEvent<Object, PreWrap.Handler> {
		@Override
		public void dispatch(PreWrap.Handler handler) {
			handler.onPreWrap(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPreWrap(PreWrap event);
		}
	}

	static PackageProperties._StringArea_Expanded _StringArea_Expanded_properties = PackageProperties.stringArea_expanded;

	@Directed(tag = "string-area-expanded")
	@DirectedContextResolver(InnerResolver.class)
	@TypedProperties
	class Expanded extends Model.All
			implements ModelEvents.Copy.Handler, PreWrap.Handler {
		HeadingActions headingActions;

		String value;

		@Binding(
			type = Type.STYLE_ATTRIBUTE,
			to = "whiteSpace",
			transform = Binding.WhiteSpacePreWrapPre.class)
		boolean preWrap;

		@Binding(
			type = Type.STYLE_ATTRIBUTE,
			to = "fontFamily",
			transform = Binding.MonospaceInherit.class)
		boolean fixedWidth;

		@SuppressWarnings("deprecation")
		Expanded() {
			value = StringArea.this.value;
			headingActions = new HeadingActions("Expanded");
			if (value.startsWith("http")) {
				headingActions.actions.add(new Link().withText("Navigate")
						.withHref(value).withTargetBlank());
			}
			headingActions.actions.add(Link.of(ModelEvents.Copy.class));
			headingActions.actions.add(Link.of(PreWrap.class));
			if (value.startsWith("[") || value.startsWith("{")) {
				try {
					JsonValue jsonValue = Json.instance().parse(value);
					value = JsonUtil.stringify(jsonValue, 2);
					fixedWidth = true;
				} catch (JsonException e) {
				}
			}
			if (value.startsWith("<")) {
				// assume ml, don't format
				fixedWidth = true;
			}
			if (value.length() > 60 && value.indexOf("\n") == -1) {
				preWrap = true;
			}
		}

		@Override
		public void onCopy(Copy event) {
			event.reemitAs(this, CopyToClipboard.class, value);
		}

		@Override
		public void onPreWrap(PreWrap event) {
			_StringArea_Expanded_properties.preWrap.set(this, !preWrap);
		}
	}

	/**
	 * Transform all Strings to StringArea
	 */
	public static class StringAreaResolver extends ContextResolver {
		@Override
		public Object resolveModel(AnnotationLocation location, Object model) {
			if (location.property != null
					&& location.property.getOwningType() == StringArea.class) {
				return model;
			}
			if (model instanceof String) {
				return new StringArea(location, (String) model);
			} else {
				return model;
			}
		}
	}

	Overlay overlay = null;

	@Override
	public void onClick(Click event) {
		if (overlay == null) {
			Expanded expanded = new Expanded();
			Attributes attributes = Overlay.attributes();
			attributes.dropdown(Position.CENTER,
					provideElement().getBoundingClientRect(), this, expanded);
			attributes.withLogicalParent(this);
			overlay = attributes.create();
			overlay.open();
		} else {
			// FIXME - double dispatch/listen?
			// overlay.close();
		}
	}

	@Override
	public void onClosed(Closed event) {
		overlay = null;
	}
}
