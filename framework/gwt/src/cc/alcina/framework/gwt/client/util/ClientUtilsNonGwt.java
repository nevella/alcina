package cc.alcina.framework.gwt.client.util;

import com.google.gwt.user.client.rpc.StatusCodeException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;

//FIXME - post merge of localdom - remove (with prejudice)
public class ClientUtilsNonGwt {
    @RegistryLocation(registryPoint = ClientUtilsNonGwtImpl.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.DEFAULT_PRIORITY)
    @ClientInstantiable
    public static class ClientUtilsNonGwtImpl {
        public boolean maybeOffline(Throwable t) {
            while (t instanceof WrappedRuntimeException) {
                if (t == t.getCause() || t.getCause() == null) {
                    break;
                }
                t = t.getCause();
            }
            if (t.getMessage() != null && t.getMessage()
                    .contains("IOException while sending RPC request")) {
                return true;
            }
            if (t.getMessage() != null && t.getMessage()
                    .contains("IOException while receiving RPC response")) {
                return true;
            }
            if (t instanceof StatusCodeException) {
                if (AlcinaDebugIds
                        .hasFlag(AlcinaDebugIds.DEBUG_SIMULATE_OFFLINE)) {
                    return true;
                }
                StatusCodeException sce = (StatusCodeException) t;
                Registry.impl(ClientNotifications.class).log(
                        "** Status code exception: " + sce.getStatusCode());
                if (sce.getStatusCode() == 0) {
                    return true;
                }
                boolean internetExplorerErrOffline = BrowserMod
                        .isInternetExplorer() && sce.getStatusCode() > 600;
                if (internetExplorerErrOffline) {
                    return true;
                }
                // DNS error in Africa
                if (t.toString()
                        .contains("was not able to resolve the hostname")) {
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean maybeOffline(Throwable t) {
        ClientUtilsNonGwtImpl impl = Registry
                .implOrNull(ClientUtilsNonGwtImpl.class);
        return impl == null ? false : impl.maybeOffline(t);
    }
}
