package cc.alcina.framework.common.client.collections;

import java.util.Arrays;
import java.util.Collection;

public class IsClassesFilter implements CollectionFilter {
	private Collection<Class> classes;

	private boolean checkingClassObjects;

	public IsClassesFilter(Class... classes) {
		this.classes = Arrays.asList(classes);
	}

	public IsClassesFilter(Collection<Class> classes) {
		this.classes = classes;
	}

	@Override
	public boolean allow(Object o) {
		return o != null && (checkingClassObjects ? classes.contains(o)
				: classes.contains(o.getClass()));
	}

	public boolean isCheckingClassObjects() {
		return this.checkingClassObjects;
	}

	public void setCheckingClassObjects(boolean checkingClassObjects) {
		this.checkingClassObjects = checkingClassObjects;
	}
}