package cc.alcina.framework.entity.parser.structured;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Multimap;


@Registration.Singleton
public class XmlTokens {
	public static XmlTokens get() {
		return Registry.impl(XmlTokens.class);
	}

	private Multimap<Class, List<XmlToken>> tokens = new Multimap<>();

	private Set<Class> instantiated = new HashSet<>();

	public synchronized List<XmlToken> getTokens(Class<?> tokenClass) {
		if (instantiated.add(tokenClass)) {
			Arrays.stream(tokenClass.getFields())
					.filter(f -> XmlToken.class.isAssignableFrom(f.getType()))
					.forEach(f -> {
						try {
							f.get(null);
						} catch (Exception e) {
							throw new WrappedRuntimeException(e);
						}
					});
		}
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
