package cc.alcina.framework.gwt.client.dirndl.model.component;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Copy;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.CopyToClipboard;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.model.HeadingActions;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay.Attributes;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;

/*
 * A rich viewer for strings in forms/tables. Note that because this is used to
 * replace a String model, the rendered tag will be from the originating
 * property (it doesn't implement Model.ResetDirecteds to avoid clobbering table
 * containment, etc)
 * 
 * So all styles should be applied to inner
 */
public class StringArea extends Model.Fields
		implements DomEvents.Click.Handler, ModelEvents.Closed.Handler {
	@Directed(tag = "string-value")
	public String value;

	AnnotationLocation location;

	public StringArea(AnnotationLocation location, String value) {
		this.location = location;
		this.value = value;
	}

	static class InnerResolver extends ContextResolver {
		@Override
		protected Object resolveModel(AnnotationLocation location,
				Object model) {
			return model;
		}
	}

	@Directed(tag = "string-area-expanded")
	@DirectedContextResolver(InnerResolver.class)
	class Expanded extends Model.All implements ModelEvents.Copy.Handler {
		HeadingActions headingActions;

		String value;

		Expanded() {
			value = StringArea.this.value;
			headingActions = new HeadingActions("Expanded");
			headingActions.withModelActions(ModelEvents.Copy.class);
		}

		@Override
		public void onCopy(Copy event) {
			event.reemitAs(this, CopyToClipboard.class, value);
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
			overlay.close();
		}
	}

	@Override
	public void onClosed(Closed event) {
		overlay = null;
	}
}
