package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafNodeRenderer;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;
import cc.alcina.framework.gwt.client.place.BasePlace;

//FIXME - ert.dirndl.1 - baseplace should implement a  'link provider' interface
@Bean
public class LinkModel {
	private BasePlace place;

	private boolean withoutLink;

	private boolean primaryAction;

	public LinkModel withPrimaryAction(boolean primaryAction) {
		this.primaryAction = primaryAction;
		return this;
	}

	public LinkModel withPlace(BasePlace place) {
		this.place = place;
		return this;
	}

	public LinkModel withWithoutLink(boolean withoutLink) {
		this.withoutLink = withoutLink;
		return this;
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface LinkModelRendererPrimaryClassName {
		String value();
	}

	@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = LinkModel.class)
	public static class LinkModelRenderer extends LeafNodeRenderer {
		@Override
		public Widget render(Node node) {
			LinkModel model = (LinkModel) node.getModel();
			Widget rendered = super.render(node);
			rendered.getElement().setInnerText(getText(node));
			BasePlace place = model.getPlace();
			rendered.getElement().setAttribute("href", place.toHrefString());
			if (place instanceof ActionRefPlace) {
				ActionRefPlace actionRefPlace = (ActionRefPlace) place;
				Optional<ActionHandler> actionHandler = actionRefPlace
						.getActionHandler();
				if (actionHandler.isPresent()) {
					rendered.getElement().setAttribute("href", "#");
					rendered.addDomHandler(evt -> actionHandler.get()
							.handleAction(node, evt, actionRefPlace),
							ClickEvent.getType());
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
			return ((LinkModel) node.getModel()).getPlace();
		}

		@Override
		protected String getTag(Node node) {
			return ((LinkModel) node.getModel()).isWithoutLink() ? "span" : "a";
		}

		protected String getText(Node node) {
			return getPlace(node) == null ? "<null text>"
					: getPlace(node).toNameString();
		}
	}

	public BasePlace getPlace() {
		return this.place;
	}

	public boolean isWithoutLink() {
		return this.withoutLink;
	}

	public boolean isPrimaryAction() {
		return this.primaryAction;
	}
}