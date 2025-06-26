#!/bin/bash
# run :: export DEBUG=true &&  ./launch.alcina.devconsole.sh for tcp/JDWP debugging
# run :: ./launch.alcina.devconsole.sh --http-port=5401 to listen on a non-default port
CLASSPATH=/g/jade/server/lib/war/httpclient-4.5.13.jar:\
/g/alcina/subproject/console/bin:\
/g/alcina/lib/framework/entity/commons-lang-2.5.jar:\
/g/alcina/lib/extras/jetty/jetty-all-uber.jar:\
/g/alcina/extras/dev/bin:\
/g/alcina/framework/jvmclient/bin:\
/g/alcina/framework/meta/bin:\
/g/alcina/framework/story/bin:\
/g/alcina/extras/dev/bin:\
/g/alcina/extras/jsdom/bin:\
/g/alcina/extras/patches/nekohtml/bin:\
/g/alcina/bin:\
/g/alcina/lib/framework/entity/nekohtml.jar:\
/g/alcina/lib/extras/cluster/ant.jar:\
/g/alcina/lib/framework/common/persistence-api.jar:\
/g/alcina/lib/framework/common/jaxb-api.jar:\
/g/alcina/lib/framework/entity/xercesImpl-2.12.jar:\
/g/alcina/lib/framework/entity/xalan-2.7.2.jar:\
/g/alcina/lib/framework/entity/reload4j-1.2.21.jar:\
/g/alcina/lib/framework/gwt/gwtx-1.5.2.jar:\
/g/alcina/lib/framework/gwt/gwt-dev.jar:\
/g/alcina/lib/framework/gwt/gwt-html5-database.jar:\
/g/alcina/lib/framework/gwt/validation-api-1.0.0.GA-sources.jar:\
/g/alcina/lib/framework/gwt/validation-api-1.0.0.GA.jar:\
/g/alcina/lib/framework/impl/jboss-hibernate/hibernate-commons-annotations-4.0.4.Final.jar:\
/g/alcina/lib/framework/impl/jboss-hibernate/hibernate-core-5.3.20.Final-patched.jar:\
/g/alcina/lib/framework/gwt/gwt-user.jar:\
/g/alcina/lib/framework/impl/jboss-hibernate/hibernate-entitymanager-4.3.7.Final.jar:\
/g/alcina/lib/framework/servlet/jakarta.activation-1.2.1.jar:\
/g/alcina/lib/framework/servlet/jakarta.mail-1.6.7.jar:\
/g/alcina/lib/framework/entity/javax.ejb-api-3.2.jar:\
/g/alcina/lib/framework/gwt/requestfactory-server.jar:\
/g/alcina/lib/framework/impl/jboss-hibernate/jboss-vfs-3.2.5.Final.jar:\
/g/alcina/lib/framework/entity/fastutil-7.0.6.jar:\
/g/alcina/lib/framework/entity/kryo/kryo-4.0.0-javadoc.jar:\
/g/alcina/lib/framework/entity/kryo/kryo-4.0.0-sources.jar:\
/g/alcina/lib/framework/entity/kryo/kryo-4.0.0.jar:\
/g/alcina/lib/framework/entity/kryo/minlog-1.3.0.jar:\
/g/alcina/lib/framework/entity/kryo/objenesis-2.1.jar:\
/g/alcina/lib/framework/entity/kryo/reflectasm-1.10.1-shaded.jar:\
/g/alcina/lib/framework/common/jackson-annotations-2.7.1.jar:\
/g/alcina/lib/framework/entity/jackson-core-2.7.1.jar:\
/g/alcina/lib/framework/entity/jackson-databind-2.7.1.jar:\
/g/alcina/lib/framework/gwt/gwt-dev-patch.jar:\
/g/alcina/lib/framework/classmeta/jna-5.10.0.jar:\
/g/alcina/lib/framework/classmeta/barbary-watchservice-2.1-SNAPSHOT.jar:\
/g/alcina/lib/framework/classmeta/jna-platform-5.10.0.jar:\
/g/alcina/lib/extras/dev/httpclient-4.5.jar:\
/g/alcina/lib/extras/dev/httpcore-4.4.3.jar:\
/g/alcina/lib/framework/entity/slf4j-api-1.7.36.jar:\
/g/alcina/lib/framework/entity/commons-pool2-2.6.0.jar:\
/g/alcina/lib/framework/gwt/gwt-elemental.jar:\
/g/alcina/lib/extras/webdriver/selenium-server-4.9.1.jar:\
/g/alcina/lib/framework/entity/javax.transaction-api-1.3.jar:\
/g/alcina/lib/framework/classmeta/tools.jar:\
/g/alcina/lib/framework/classmeta/javassist-3.18.1-GA.jar:\
/g/alcina/lib/framework/servlet/scrypt-1.4.0-patched.jar:\
/g/alcina/lib/framework/google/google-api-client-1.25.0.jar:\
/g/alcina/lib/framework/google/google-api-client-jackson2-1.25.0.jar:\
/g/alcina/lib/framework/google/google-api-client-java6-1.25.0.jar:\
/g/alcina/lib/framework/google/google-api-services-sheets-v4-rev565-1.25.0-sources.jar:\
/g/alcina/lib/framework/google/google-api-services-sheets-v4-rev565-1.25.0.jar:\
/g/alcina/lib/framework/google/google-http-client-1.44.1.jar:\
/g/alcina/lib/framework/google/google-http-client-jackson2-1.44.1.jar:\
/g/alcina/lib/framework/google/google-oauth-client-1.35.0.jar:\
/g/alcina/lib/framework/google/google-oauth-client-java6-1.35.0.jar:\
/g/alcina/lib/framework/google/google-oauth-client-jetty-1.35.0.jar:\
/g/alcina/lib/framework/google/gson-2.1.jar:\
/g/alcina/lib/framework/google/protobuf-java-2.4.1.jar:\
/g/alcina/lib/framework/google/google-api-services-drive-v3-rev161-1.25.0.jar:\
/g/alcina/lib/framework/servlet/cluster/kafka-clients-2.7.1.jar:\
/g/alcina/lib/framework/entity/commons-lang-2.5.jar:\
/g/alcina/lib/extras/webdriver/commonmark-0.18.1-SNAPSHOT.jar:\
/g/alcina/lib/extras/webdriver/commonmark-ext-gfm-tables-0.18.1-SNAPSHOT.jar:\
/g/alcina/lib/framework/entity/javax.ws.rs-api-2.1.jar:\
/g/alcina/lib/framework/entity/jol-core-0.17-SNAPSHOT.jar:\
/g/alcina/lib/framework/entity/slf4j-reload4j-1.7.36.jar:\
/g/alcina/lib/framework/classmeta/javaparser-core-generators-3.24.9.jar:\
/g/alcina/lib/framework/classmeta/javaparser-core-metamodel-generator-3.24.9.jar:\
/g/alcina/lib/framework/classmeta/javaparser-core-serialization-3.24.9.jar:\
/g/alcina/lib/framework/classmeta/javaparser-core-testing-3.24.9.jar:\
/g/alcina/lib/framework/classmeta/javaparser-core-testing-bdd-3.24.9.jar:\
/g/alcina/lib/framework/classmeta/javaparser-symbol-solver-core-3.24.9.jar:\
/g/alcina/lib/framework/classmeta/javaparser-symbol-solver-testing-3.24.9.jar
DEBUG_ARGS=
if [ -z ${DEBUG} ]; then
    :
else
	DEBUG_ARGS=-agentlib:jdwp=transport=dt_socket,suspend=n,quiet=y,server=y,address=localhost:31201
fi
java  -Xms2000m -Xmx2000m -Xss4M -ea -XX:ParallelGCThreads=6 -Djava.awt.headless=true -Dawt.toolkit=sun.awt.HToolkit \
--add-opens java.logging/java.util.logging=ALL-UNNAMED --add-opens java.base/sun.net.www.protocol.jar=ALL-UNNAMED \
--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
--add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.util=ALL-UNNAMED  --add-opens java.base/java.io=ALL-UNNAMED  \
--add-opens java.base/java.util=ALL-UNNAMED -Dfile.encoding=UTF-8 \
-classpath $CLASSPATH \
$DEBUG_ARGS \
cc.alcina.extras.dev.console.alcina.AlcinaDevConsole $@