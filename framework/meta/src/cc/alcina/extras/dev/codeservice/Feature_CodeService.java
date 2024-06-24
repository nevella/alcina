package cc.alcina.extras.dev.codeservice;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.meta.Feature;

/**
 * <p>
 * The CodeService marshalls source files and source file events, collating and
 * emitting them to provide a basis for incremental companion code generation.
 * 
 * <p>
 * Initial intended clients are PackagePropertiesGenerator (generates
 * {@link TypedProperty.Container} companion files to fill in the missing
 * Class.properties model in the JVM}); ReflectiveRpcGenerator (generates async
 * interfaces for alcina/gwt rpc apis) and ConfigurableSorter (a java sourcefile
 * sorter)
 */
public interface Feature_CodeService extends Feature {
}
