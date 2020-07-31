package cc.alcina.framework.gwt.client.dirndl.layout;


import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.Reflections;


public class DirectedLayout {

	 Widget parent;
	public Widget render(Widget parent, Object model) {
		this.parent = parent;
		Node root = new Node();
		root.model = model;
		return root.render().orElse(null);
	}
	public static class Node{
		 Object model;
		 
		 Field field;

		 DirectedNodeRenderer renderer;

		private BeanDescriptor descriptor;

		 Directed directed;

		Optional<Widget> render(){
			this.descriptor = Reflections.beanDescriptorProvider().getDescriptor(model);
			renderer = resolveRenderer();
			Optional<Widget> result = renderer.render(this);
			if(!result.isPresent()) {
				/*
				 * passthrough wrapper model
				 */
				Preconditions.checkArgument(descriptor.getProperties().length==1);
			}
			return null;
			
		}

		private DirectedNodeRenderer resolveRenderer() {
			DirectedNodeRenderer renderer = null;
			directed = Reflections.classLookup().getAnnotationForClass(model.getClass(), Directed.class);
			if(directed!=null) {
				renderer=Reflections.classLookup().newInstance(directed.renderer());
			}
			return renderer;
		}
		
	}
}
