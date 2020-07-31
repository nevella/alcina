package cc.alcina.framework.gwt.client.dirndl;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.gwittir.widget.GridForm;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;

public class RenderContextStyles {
	private RenderContext renderContext;

	public RenderContextStyles(RenderContext renderContext) {
		this.renderContext = renderContext;
	}

	@RegistryLocation(registryPoint = MultiRowTable.class, implementationType = ImplementationType.INSTANCE)
	@ClientInstantiable
	public static class MultiRowTable {
		protected RenderContext renderContext;

		public void applyTo(BoundTableExt table) {
		}

		private MultiRowTable withContext(RenderContext renderContext) {
			this.renderContext = renderContext;
			return this;
		}

		public void applyTo(PaneWrapperWithObjects paneWrapperWithObjects,
				boolean editable) {
		}

		public void applyTo(GridForm gridForm) {
		}
	}

	public MultiRowTable multiRowTable() {
		return Registry.impl(MultiRowTable.class).withContext(renderContext);
	}
}