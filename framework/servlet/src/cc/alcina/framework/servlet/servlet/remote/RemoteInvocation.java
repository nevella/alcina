package cc.alcina.framework.servlet.servlet.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager.PostTransactionEntityResolver;
import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationProxy.RemoteInvocationProxyInterceptor;

/**
 * org.apache.http.client is in gwt-dev - so we don't require it in eclipse
 */
@SuppressWarnings("deprecation")
@RegistryLocation(registryPoint = RemoteInvocation.class, implementationType = ImplementationType.INSTANCE)
public class RemoteInvocation {
	private Object interceptionResult;

	public PostAndClient getHttpPost(URI uri) throws Exception {
		int timeoutSecs = 120;
		PostAndClient postAndClient = new PostAndClient();
		HttpParams params = new BasicHttpParams();
		int timeoutMs = timeoutSecs * 1000;
		params.setParameter("http.socket.timeout", timeoutMs * 4);
		params.setParameter("http.connection.timeout", timeoutMs);
		postAndClient.client = new DefaultHttpClient(params);
		postAndClient.post = new HttpPost(uri);
		return postAndClient;
	}

	public Object getInterceptionResult() {
		return interceptionResult;
	}

	public void hookParams(String methodName, Object[] args,
			RemoteInvocationParameters params) {
		for (RemoteInvocationProxyInterceptor interceptor : Registry
				.impls(RemoteInvocationProxyInterceptor.class)) {
			interceptor.hookParams(methodName, args, params);
		}
	}

	public Object invoke(String methodName, Object[] args,
			RemoteInvocationParameters params) throws Exception,
			URISyntaxException, IOException, UnsupportedEncodingException,
			ClientProtocolException, ClassNotFoundException {
		try {
			LooseContext.pushWithBoolean(
					KryoUtils.CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER, false);
			LooseContext.set(KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER,
					true);
			hookParams(methodName, args, params);
			String address = ResourceUtilities
					.getBundledString(RemoteInvocation.class, "address");
			PostAndClient png = getHttpPost(new URI(address));
			// params.username = ResourceUtilities
			// .getBundledString(DevRemoter.class, "username");
			params.asRoot = PermissionsManager.get().isRoot();
			ClientInstance clientInstance = PermissionsManager.get()
					.getClientInstance();
			if (clientInstance != null) {
				if (clientInstance.getId() == 0) {
					clientInstance = EntityLayerObjects.get()
							.getServerAsClientInstance();
				}
				params.clientInstanceId = clientInstance.getId();
				params.clientInstanceAuth = clientInstance.getAuth();
			}
			params.methodName = methodName;
			params.args = args == null ? new Object[0] : args;
			List<NameValuePair> qparams = new ArrayList<NameValuePair>();
			qparams.add(new BasicNameValuePair(
					RemoteInvocationServlet.REMOTE_INVOCATION_PARAMETERS,
					KryoUtils.serializeToBase64(params)));
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(qparams);
			png.post.setEntity(entity);
			HttpParams httpParams = png.client.getParams();
			int timeoutMs = 120 * 1000;
			httpParams.setParameter("http.socket.timeout", timeoutMs);
			httpParams.setParameter("http.connection.timeout", timeoutMs);
			HttpResponse response = png.client.execute(png.post);
			InputStream content = response.getEntity().getContent();
			ArrayList container = KryoUtils.deserializeFromStream(content,
					ArrayList.class);
			Object object = container.get(0);
			if (object instanceof Exception) {
				((Exception) object).printStackTrace();
				throw new Exception("Remote exception");
			}
			if (methodName.equals("transformInPersistenceContext")) {
				ThreadlocalTransformManager.get()
						.setPostTransactionEntityResolver(
								(PostTransactionEntityResolver) container
										.get(1));
			}
			customiseResult(object);
			return object;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			LooseContext.pop();
		}
	}

	public boolean tryInterception(Object proxy, Method method, Object[] args)
			throws Throwable {
		for (RemoteInvocationProxyInterceptor interceptor : Registry
				.impls(RemoteInvocationProxyInterceptor.class)) {
			if (interceptor.handles(proxy, method, args)) {
				interceptionResult = interceptor.invoke(proxy, method, args);
				return true;
			}
		}
		return false;
	}

	protected void customiseResult(Object obj) {
	}

	class PostAndClient {
		public HttpPost post;

		public HttpGet get;

		public DefaultHttpClient client;
	}
}
