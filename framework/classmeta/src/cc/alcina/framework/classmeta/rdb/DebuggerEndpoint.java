package cc.alcina.framework.classmeta.rdb;

import cc.alcina.framework.classmeta.rdb.RdbProxies.RdbEndpointDescriptor;

class DebuggerEndpoint extends Endpoint {
    public DebuggerEndpoint(RdbEndpointDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    protected boolean isDebuggee() {
        return false;
    }

    @Override
    protected boolean isDebugger() {
        return true;
    }
}
