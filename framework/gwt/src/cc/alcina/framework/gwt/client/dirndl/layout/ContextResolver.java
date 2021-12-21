package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.TreeResolver;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

@ClientInstantiable
public class ContextResolver extends AnnotationLocation.Resolver {

	protected TreeResolver<Directed> directedResolver;

	protected ContextResolver parent;

	public ContextResolver() {
		this(null);
	}

	public ContextResolver(ContextResolver parent) {
		this.parent = parent;
		init();
	}

	public void beforeRender() {
		// FIXME - dirndl 1.0 - Registry.get.push(this)
		// TODO Auto-generated method stub
	}

	

	public <A extends Annotation> TreeResolver<A>
			getTreeResolver(Class<A> clazz) {
		if (clazz == Directed.class) {
			return (TreeResolver<A>) directedResolver;
		}
		throw new UnsupportedOperationException();
	}

	public PropertyReflector resolveDirectedReflector(PropertyReflector propertyReflector) {
		return propertyReflector.hasAnnotation(Directed.class)?propertyReflector:null;
	}

	public Object resolveModel(Object model) {
		return model;
	}

	public <T> T resolveRenderContextProperty(String key) {
		return null;
	}


	protected void init() {
		directedResolver = parent != null ? parent.directedResolver
				: new TreeResolver<>(Directed.class, Directed::merge);
	}
}