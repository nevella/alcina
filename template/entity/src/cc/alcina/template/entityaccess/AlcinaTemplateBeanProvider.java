package cc.alcina.template.entityaccess;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;

/**
 * @author nreddel@barnet.com.au
 * 
 */
@RegistryLocation(registryPoint=AlcinaTemplateBeanProvider.class,implementationType=ImplementationType.SINGLETON)
public class AlcinaTemplateBeanProvider {
	public static AlcinaTemplateBeanProvider get() {
		return Registry.impl(AlcinaTemplateBeanProvider.class);
	}

	private Context context;

	private static final String JNDIPrefix = "java:global/alcinaTemplate_server/alcinaTemplate_entity/";

	private AlcinaTemplateBeanProvider() {
		Properties properties = new Properties();
		properties.put("java.naming.factory.initial",
				"org.jboss.as.naming.InitialContextFactory");
		properties.put("java.naming.factory.url.pkgs",
				"=org.jboss.naming:org.jnp.interfaces");
		properties.put("java.naming.provider.url", ResourceUtilities
				.getBundledString(AlcinaTemplateBeanProvider.class, "jndiUrl"));
		try {
			context = new InitialContext(properties);
		} catch (NamingException e) {
			throw new WrappedRuntimeException(e, SuggestedAction.CANCEL_STARTUP);
		}
	}

	public AlcinaTemplatePersistenceLocal getAlcinaTemplatePersistenceBean() {
		AlcinaTemplatePersistenceLocal beanLocal = null;
		try {
			beanLocal = (AlcinaTemplatePersistenceLocal) context
					.lookup(JNDIPrefix
							+ AlcinaTemplatePersistence.LocalJNDIName);
		} catch (NamingException e) {
			throw new WrappedRuntimeException(e, SuggestedAction.NOTIFY_ERROR);
		}
		return beanLocal;
	}

	public CommonPersistenceLocal getCommonPersistenceBean() {
		CommonPersistenceLocal beanLocal = null;
		try {
			beanLocal = (CommonPersistenceLocal) context.lookup(JNDIPrefix
					+ AlcinaTemplateCommonPersistence.LocalJNDIName);
		} catch (NamingException e) {
			throw new WrappedRuntimeException(e, SuggestedAction.NOTIFY_ERROR);
		}
		return beanLocal;
	}
}
