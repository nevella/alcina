package cc.alcina.framework.servlet.servlet.remote;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.transform.TransformPersisterInPersistenceContext;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.EntityLocatorMap;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationProxy.RemoteInvocationProxyInterceptor;

/**
 * org.apache.http.client is in gwt-dev - so we don't require it in eclipse
 */
@SuppressWarnings("deprecation")
@RegistryLocation(registryPoint = RemoteInvocation.class, implementationType = ImplementationType.INSTANCE)
@Registration(RemoteInvocation.class)
public class RemoteInvocation {
	private Object interceptionResult;

	private String remoteAddress;

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

	public String getRemoteAddress() {
		return this.remoteAddress;
	}

	public void hookParams(String methodName, Object[] args,
			RemoteInvocationParameters params) {
		Registry.query(RemoteInvocationProxyInterceptor.class).implementations()
				.forEach(interceptor -> interceptor.hookParams(methodName, args,
						params));
	}

	public Object invoke(String methodName, Object[] args,
			RemoteInvocationParameters params) throws Exception {
		try {
			LooseContext.pushWithBoolean(
					KryoUtils.CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER, false);
			LooseContext.set(KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER,
					true);
			hookParams(methodName, args, params);
			String address = Ax.blankTo(getRemoteAddress(), ResourceUtilities
					.getBundledString(RemoteInvocation.class, "address"));
			PostAndClient png = getHttpPost(new URI(address));
			// params.username = ResourceUtilities
			// .getBundledString(DevRemoter.class, "username");
			params.asRoot = PermissionsManager.get().isRoot();
			params.methodName = methodName;
			boolean transformMethod = methodName
					.equals("transformInPersistenceContext");
			ClientInstance clientInstance = PermissionsManager.get()
					.getClientInstance();
			if (transformMethod) {
				TransformPersistenceToken token = (TransformPersistenceToken) args[1];
				if (LooseContext.is(
						TransformPersisterInPersistenceContext.CONTEXT_REPLAYING_FOR_LOGS)) {
					// force usage of the remote client instance (and remove all
					// refs to the clientInstance - assuming that this is
					// bootstrapping a devconsole)
					clientInstance = null;
					token.getRequest().setClientInstance(null);
				}
			}
			if (clientInstance != null) {
				if (clientInstance.getId() == 0) {
					clientInstance = EntityLayerObjects.get()
							.getServerAsClientInstance();
				}
				params.clientInstanceId = clientInstance.getId();
				params.clientInstanceAuth = clientInstance.getAuth();
			}
			params.args = args == null ? new Object[0] : args;
			params.context = new LinkedHashMap<>();
			LooseContext.getContext().properties.forEach((k, v) -> {
				if (v == null || GraphProjection
						.isPrimitiveOrDataClass(v.getClass())) {
					params.context.put(k, v);
				}
			});
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
			ArrayList container = deserializeResult(content);
			Object object = container.get(0);
			if (object instanceof Exception) {
				((Exception) object).printStackTrace();
				throw new Exception("Remote exception");
			}
			if (transformMethod) {
				// will be invalid if replaying
				if (!LooseContext.is(
						TransformPersisterInPersistenceContext.CONTEXT_REPLAYING_FOR_LOGS)) {
					EntityLocatorMap returned = ((DomainTransformLayerWrapper) container
							.get(0)).locatorMap;
					EntityLocatorMap sent = ((TransformPersistenceToken) params.args[1])
							.getLocatorMap();
					sent.merge(returned);
				}
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

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public boolean tryInterception(Object proxy, Method method, Object[] args)
			throws Throwable {
		Optional<RemoteInvocationProxyInterceptor> first = Registry
				.query(RemoteInvocationProxyInterceptor.class).implementations()
				.filter(interceptor -> interceptor.handles(proxy, method, args))
				.findFirst();
		if (first.isPresent()) {
			interceptionResult = first.get().invoke(proxy, method, args);
			return true;
		} else {
			return false;
		}
	}

	protected void customiseResult(Object obj) {
	}

	protected ArrayList deserializeResult(InputStream content) {
		return KryoUtils.deserializeFromStream(content, ArrayList.class);
	}

	class PostAndClient {
		public HttpPost post;

		public HttpGet get;

		public DefaultHttpClient client;
	}
}
