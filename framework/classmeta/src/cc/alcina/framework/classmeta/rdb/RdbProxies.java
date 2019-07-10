package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.WrappedObject.WrappedObjectHelper;
import cc.alcina.framework.entity.util.ShellWrapper;

@RegistryLocation(registryPoint = RdbProxies.class, implementationType = ImplementationType.SINGLETON)
public class RdbProxies {
    public static RdbProxies get() {
        return Registry.impl(RdbProxies.class);
    }

    Logger logger = LoggerFactory.getLogger(getClass());

    RdbEndpointSchema schema;

    List<Endpoint> endpoints = new ArrayList<>();

    public RdbProxies() {
    }

    public Endpoint endpointByName(String name) {
        return endpoints.stream().filter(e -> e.descriptor.name.equals(name))
                .findFirst().get();
    }

    public void start() {
        if ("disabled".length() > 90) {
            return;
        }
        String modelXml = null;
        try {
            modelXml = ResourceUtilities
                    .readClazzp("../schema/rdbEndpointSchema.xml");
        } catch (Exception e) {
        }
        if (modelXml == null) {
            Ax.out("No RdbEndpointSchema defined");
            schema = new RdbEndpointSchema();
            {
                RdbEndpointDescriptor descriptor = new RdbEndpointDescriptor();
                descriptor.jdwpHost = "127.0.0.1";
                descriptor.jdwpPort = 11001;
                descriptor.name = "ljda";
                descriptor.name = "ljda.jade.app.dev";
                descriptor.transportType = TransportType.shared_vm;
                descriptor.transportEndpointName = "jda";
                schema.endpointDescriptors.add(descriptor);
            }
            {
                RdbEndpointDescriptor descriptor = new RdbEndpointDescriptor();
                descriptor.jdwpHost = "jda";
                descriptor.jdwpPort = 5126;
                descriptor.jdwpAttach = true;
                descriptor.name = "jda";
                descriptor.name = "jda.jade.app.dev";
                schema.endpointDescriptors.add(descriptor);
            }
            Ax.out(WrappedObjectHelper.xmlSerialize(schema));
            System.exit(0);
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

    private void start(RdbEndpointDescriptor descriptor) {
        Endpoint endpoint = descriptor.jdwpAttach
                ? new DebuggeeEndpoint(descriptor)
                : new DebuggerEndpoint(descriptor);
        endpoints.add(endpoint);
        endpoint.launch();
    }

    public static class RdbEndpointDescriptor {
        public boolean jdwpAttach;

        public int jdwpPort;

        public String jdwpHost;

        public String name;

        public TransportType transportType;

        public String transportEndpointUrl;

        public String transportEndpointName;
    }

    @XmlRootElement
    public static class RdbEndpointSchema {
        public List<RdbEndpointDescriptor> endpointDescriptors = new ArrayList<>();
    }

    public enum TransportType {
        shared_vm, tcp_ip;
    }
}
