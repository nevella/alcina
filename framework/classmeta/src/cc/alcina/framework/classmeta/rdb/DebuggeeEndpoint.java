package cc.alcina.framework.classmeta.rdb;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;

public class DebuggeeEndpoint extends Endpoint {
    public DebuggeeEndpoint(RdbEndpointDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    protected boolean isDebuggee() {
        return true;
    }

    @Override
    protected boolean isDebugger() {
        return false;
    }
}
