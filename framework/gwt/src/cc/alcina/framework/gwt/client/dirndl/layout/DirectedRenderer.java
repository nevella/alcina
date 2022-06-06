package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.RendererInput;

public abstract class DirectedRenderer {
	protected abstract DirectedLayout.Node render(DirectedLayout.RendererInput input);
	
	@Registration({DirectedRenderer.class,TextNodeRenderer.class})
	public static class Text extends DirectedRenderer{

		@Override
		protected Node render(RendererInput input) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	@Registration({DirectedRenderer.class,ContainerNodeRenderer.class})
	public static class Container extends DirectedRenderer{

		@Override
		protected Node render(RendererInput input) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	@Registration({DirectedRenderer.class,BindableNodeRenderer.class})
	public static class BindableRenderer extends DirectedRenderer{

		@Override
		protected Node render(RendererInput input) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
