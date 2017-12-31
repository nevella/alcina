package cc.alcina.framework.entity.parser.structured;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Multimap;

@RegistryLocation(registryPoint = XmlTokens.class, implementationType = ImplementationType.SINGLETON)
public class XmlTokens {
	public static XmlTokens get() {
		XmlTokens singleton = Registry.checkSingleton(XmlTokens.class);
		if (singleton == null) {
			singleton = new XmlTokens();
			Registry.registerSingleton(XmlTokens.class, singleton);
		}
		return singleton;
	}

	private Multimap<Class, List<XmlToken>> tokens = new Multimap<>();

	public List<XmlToken> getTokens(Class<?> tokenClass) {
		return tokens.get(tokenClass);
	}

	public void register(Class clazz, XmlToken token) {
		if (token.matchOrderBefore() != null) {
			List<XmlToken> list = tokens.get(clazz);
			list.add(list.indexOf(token.matchOrderBefore()), token);
		} else {
			tokens.add(clazz, token);
		}
	}
}
