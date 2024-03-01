package cc.alcina.framework.entity.gwt.reflection.jdk;

import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.ClientReflections;
import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider;
import cc.alcina.framework.common.client.reflection.impl.ForName;

public class BuildTimeReflections {
	public static void init() {
		ForName.setImpl(new ForNameImplBuildTime());
		ClassReflectorProvider.setImpl(new ClassReflectorProviderImpl());
	}

	static class ClassReflectorProviderImpl
			implements ClassReflectorProvider.Impl {
		@Override
		public ClassReflector getClassReflector(Class clazz) {
			return ClientReflections.getClassReflector(clazz);
		}
	}
}
