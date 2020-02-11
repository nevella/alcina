/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

/**
 * 
 * @author Nick Reddel
 */
@ReflectionModule("Initial")
public abstract class ClientReflector implements ClassLookup {
    private static ClientReflector domainReflector;

    /**
     * Because ClientReflector is used from server code as well (via
     * TransformManager), we don't use a public static final INSTANCE generated
     * instance (see defined())
     * <p>
     * -- correction to this, it's minimally reffed - and that should probably
     * go
     * </p>
     * 
     */
    public static ClientReflector get() {
        if (domainReflector == null) {
            domainReflector = ClientReflectorFactory.create();
        }
        return domainReflector;
    }

    public static void register(ClientReflector r) {
        domainReflector = r;
    }

    protected Map<Class, ClientBeanReflector> gwbiMap = new HashMap<Class, ClientBeanReflector>();

    private List<ClientReflector> reflectors = new ArrayList<ClientReflector>();

    Set<Class> nullReflectors = new LinkedHashSet<>();

    private Map<Class, Object> templateInstances = new HashMap<Class, Object>();

    protected Map<String, Class> forNameMap = new HashMap<String, Class>();

    public ClientReflector() {
        {
            forNameMap.putAll(CommonUtils.stdAndPrimitivesMap);
        }
        reflectors.add(this);
    }

    public List<String> allInterestingAlcinaBeanProperties(Object bean) {
        return getWritableProperties(bean.getClass()).stream()
                .map(p -> p.getPropertyName()).sorted()
                .collect(Collectors.toList());
    }

    public ClientBeanReflector beanInfoForClass(Class clazz) {
        clazz = getBeanInfoClassResolver().resolveForBeanInfo(clazz);
        ClientBeanReflector beanReflector = gwbiMap.get(clazz);
        if (beanReflector == null) {
            if (!gwbiMap.containsKey(clazz)) {
                for (ClientReflector reflector : reflectors) {
                    reflector.initReflector(clazz);
                }
                beanReflector = gwbiMap.get(clazz);
                if (beanReflector == null) {
                    // so we don't look again (until next module registered)
                    gwbiMap.put(clazz, null);
                    nullReflectors.add(clazz);
                }
            }
        }
        return beanReflector;
    }

    @Override
    public String displayNameForObject(Object o) {
        ClientBeanReflector bi = beanInfoForClass(o.getClass());
        return (bi == null) ? null : bi.getObjectName(o);
    }

    @Override
    public List<String> getAnnotatedPropertyNames(Class clazz) {
        ClientBeanReflector bi = beanInfoForClass(clazz);
        Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
                .values();
        ArrayList<String> result = new ArrayList<String>();
        for (ClientPropertyReflector pr : prs) {
            if (pr.getDisplayInfo() != null) {
                result.add(pr.getPropertyName());
            }
        }
        return result;
    }

    @Override
    public <A extends Annotation> A getAnnotationForClass(Class targetClass,
            Class<A> annotationClass) {
        ClientBeanReflector beanInfo = ClientReflector.get()
                .beanInfoForClass(targetClass);
        return beanInfo == null ? null
                : beanInfo.getAnnotation(annotationClass);
    }

    @Override
    public Class getClassForName(String fqn) {
        Class clazz = forNameMap.get(fqn);
        if (clazz != null) {
            return clazz;
        }
        throw new WrappedRuntimeException(
                Ax.format("Class %s not reflect-instantiable", fqn),
                SuggestedAction.NOTIFY_ERROR);
    }

    @Override
    public Class getPropertyType(Class clazz, String propertyName) {
        return GwittirBridge.get().getPropertyForClass(clazz, propertyName)
                .getType();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getTemplateInstance(Class<T> clazz) {
        clazz = getBeanInfoClassResolver().resolveForBeanInfo(clazz);
        if (!templateInstances.containsKey(clazz)) {
            templateInstances.put(clazz,
                    Reflections.classLookup().newInstance(clazz, 0, 0));
        }
        return (T) templateInstances.get(clazz);
    }

    @Override
    public List<PropertyInfoLite> getWritableProperties(Class clazz) {
        List<PropertyInfoLite> infos = new ArrayList<PropertyInfoLite>();
        BeanDescriptor descriptor = GwittirBridge.get()
                .getDescriptorForClass(clazz);
        if (descriptor == null) {
            return infos;
        }
        for (Property p : descriptor.getProperties()) {
            if (p.getMutatorMethod() == null) {
                continue;
            }
            infos.add(new PropertyInfoLite(p.getType(), p.getName(),
                    p.getAccessorMethod(), p.getMutatorMethod(), clazz));
        }
        return infos;
    }

    public boolean isInstantiableClass(Class clazz) {
        try {
            if (CommonUtils.stdAndPrimitives.contains(clazz)) {
                return false;
            }
            getClassForName(clazz.getName());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public <T> T newInstance(Class<T> clazz) {
        return newInstance(clazz, 0, 0);
    }

    @Override
    public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
        for (ClientReflector reflector : reflectors) {
            T instance = reflector.newInstance0(clazz, objectId, localId);
            if (instance != null) {
                return instance;
            }
        }
        for (ClientReflector reflector : reflectors) {
            reflector.initialiseNewInstance(clazz);
            T instance = reflector.newInstance0(clazz, objectId, localId);
            if (instance != null) {
                return instance;
            }
        }
        throw new RuntimeException(
                "Class " + clazz + " not reflect-instantiable");
    }

    public void registerChild(ClientReflector child) {
        reflectors.add(0, child);
        child.gwbiMap = gwbiMap;
        forNameMap.putAll(child.forNameMap);
        gwbiMap.keySet().removeAll(nullReflectors);
        nullReflectors.clear();
    }

    private BeaninfoClassResolver getBeanInfoClassResolver() {
        return Registry.impl(BeaninfoClassResolver.class);
    }

    protected abstract void initialiseNewInstance(Class clazz);

    protected void initReflector(Class clazz) {
    }

    protected abstract <T> T newInstance0(Class<T> clazz, long objectId,
            long localId);

    @RegistryLocation(registryPoint = BeaninfoClassResolver.class, implementationType = ImplementationType.SINGLETON)
    @ClientInstantiable
    public static class BeaninfoClassResolver {
        public Class resolveForBeanInfo(Class clazz) {
            return clazz;
        }
    }
}
