package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.AsyncCallback;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.module.login.LoginRequest;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@RegistryLocation(registryPoint = ReflectiveLoginRemoteServiceAsync.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
@Registration.Singleton
public class ReflectiveLoginRemoteServiceAsync extends ReflectiveRemoteServiceAsync {

    public static ReflectiveLoginRemoteServiceAsync get() {
        return Registry.impl(ReflectiveLoginRemoteServiceAsync.class);
    }

    public void login(LoginRequest request, AsyncCallback<LoginResponse> callback) {
        call("login", new Class[] { LoginRequest.class }, callback, request);
    }
}
