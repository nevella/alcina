package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.WrappedObject.WrappedObjectHelper;
import cc.alcina.framework.entity.util.ShellWrapper;

public class RdbProxies {
    Logger logger = LoggerFactory.getLogger(getClass());

    RdbEndpointSchema schema;

    List<RdbProxy> proxies = new ArrayList<>();

    public RdbProxies() {
        if ("disabled".length() > 90) {
            return;
        }
        String modelXml = null;
        try {
            modelXml = ResourceUtilities
                    .readClazzp("../schema/rdbProxySchema.xml");
        } catch (Exception e) {
        }
        if (modelXml == null) {
            Ax.out("No RdbProxySchema defined");
            schema = new RdbEndpointSchema();
        } else {
            schema = WrappedObjectHelper.xmlDeserialize(RdbEndpointSchema.class,
                    modelXml);
        }
        schema.endpointDescriptors.forEach(this::start);
        try {
            new ShellWrapper().runBashScript(
                    "/usr/bin/java -jar /g/alcina/lib/framework/dev/eclipse_remote_control_client.jar execute_command ljda.jade DEBUG");
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    private void start(RdbEndpointDescriptor proxyDescriptor) {
        RdbProxy rdbProxy = new RdbProxy(proxyDescriptor);
        proxies.add(rdbProxy);
        rdbProxy.start();
    }

    @XmlRootElement
    public static class RdbEndpointSchema {
        public List<RdbEndpointDescriptor> endpointDescriptors = new ArrayList<>();
    }
//FIXME - add transport type 
    public static class RdbEndpointDescriptor {
        public int internalDebuggerPort;

        public int externalDebuggerAttachToPort;

        public String remoteHost;

        public int remoteJdwpPort;

        public String name;

        public String fullName;
    }
}
