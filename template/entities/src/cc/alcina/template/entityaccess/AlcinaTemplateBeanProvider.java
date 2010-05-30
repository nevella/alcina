package cc.alcina.template.entityaccess;


import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.template.AlcinaTemplate;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;


/**
 * @author nreddel@barnet.com.au
 * 
 */
@AlcinaTemplate
public class AlcinaTemplateBeanProvider {

	/**
	 * This will be called by a class in another jar, so the context will be
	 * that of the other jar. Uses JNDI default settings. JBoss specific
	 */

	private static AlcinaTemplateBeanProvider theInstance;

	public static AlcinaTemplateBeanProvider get() {
		if (theInstance == null) {
			theInstance = new AlcinaTemplateBeanProvider();
		}
		return theInstance;
	}

	private Context context;

	private static final String JNDIPrefix = "alcinaTemplate_server/";

	public void appShutdown() {
		theInstance = null;
	}
	
	private AlcinaTemplateBeanProvider() {
		Properties properties = new Properties();
		properties.put("java.naming.factory.initial",
				"org.jnp.interfaces.NamingContextFactory");
		properties.put("java.naming.factory.url.pkgs",
				"=org.jboss.naming:org.jnp.interfaces");
		properties.put("java.naming.provider.url", ResourceUtilities.singleton().getBundledString(AlcinaTemplateBeanProvider.class, "jndiUrl"));
		try {
			context = new InitialContext(properties);
		} catch (NamingException e) {
			throw new WrappedRuntimeException(e, SuggestedAction.CANCEL_STARTUP);

		}

	}

	public AlcinaTemplatePersistenceLocal getAlcinaTemplatePersistenceBean()
			{
		AlcinaTemplatePersistenceLocal beanLocal = null;
		try {
			beanLocal = (AlcinaTemplatePersistenceLocal) context
					.lookup(JNDIPrefix + AlcinaTemplatePersistence.LocalJNDIName);
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
