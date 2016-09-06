package cc.alcina.framework.entity.parser.structured;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Multimap;
@RegistryLocation(registryPoint=XmlTokens.class,implementationType=ImplementationType.SINGLETON)
public class XmlTokens {
	private Multimap<Class,List<XmlToken>> tokens=new Multimap<>();
	public static XmlTokens get() {
		XmlTokens singleton = Registry.checkSingleton(XmlTokens.class);
		if (singleton == null) {
			singleton = new XmlTokens();
			Registry.registerSingleton(XmlTokens.class, singleton);
		}
		return singleton;
	}
	public void register(Class clazz,XmlToken token){
		tokens.add(clazz, token);
	}
	public List<XmlToken> getTokens(Class<?> tokenClass) {
		return tokens.get(tokenClass);
	}
}
