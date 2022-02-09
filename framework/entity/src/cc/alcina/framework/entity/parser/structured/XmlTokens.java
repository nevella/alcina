package cc.alcina.framework.entity.parser.structured;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@RegistryLocation(registryPoint = XmlTokens.class, implementationType = ImplementationType.SINGLETON)
@Registration.Singleton
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

    private Set<Class> instantiated = new HashSet<>();

    public synchronized List<XmlToken> getTokens(Class<?> tokenClass) {
        if (instantiated.add(tokenClass)) {
            Arrays.stream(tokenClass.getFields()).filter(f -> XmlToken.class.isAssignableFrom(f.getType())).forEach(f -> {
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
