package cc.alcina.framework.servlet.component.featuretree;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.servlet.component.featuretree.place.FeaturePlace;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.dom.AbstractUi;
import cc.alcina.framework.servlet.dom.Environment;
import cc.alcina.framework.servlet.dom.RemoteUi;

/**
 * A remote component that models the jvm-visible feature tree
 *
 *
 * FIXME - featuretree - leaves with no status should be open.
 */
@Feature.Ref(Feature_FeatureTree.class)
public class FeatureTree {
	@Registration(RemoteComponent.class)
	public static class Component implements RemoteComponent {
		@Override
		public String getPath() {
			return "/feature-tree";
		}

		@Override
		public Class<? extends RemoteUi> getUiType() {
			return FeatureTree.Ui.class;
		}
	}

	public static class Ui extends AbstractUi<FeaturePlace> {
		public static Ui get() {
			return (Ui) Environment.get().ui;
		}

		@Override
		public Client createClient() {
			return new TypedPlaceClient(FeaturePlace.class);
		}

		public String getMainCaption() {
			return "feature tree";
		}

		@Override
		public void init() {
		}

		@Override
		protected DirectedLayout render0() {
			injectCss("res/css/styles.css");
			Client.get().initAppHistory();
			DirectedLayout layout = new DirectedLayout();
			layout.render(new Page()).getRendered().appendToRoot();
			return layout;
		}

		@Override
		public Class<? extends cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext>
				getAppCommandContext() {
			return CommandContext.class;
		}
	}

	public interface CommandContext extends
			cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext {
	}
}
