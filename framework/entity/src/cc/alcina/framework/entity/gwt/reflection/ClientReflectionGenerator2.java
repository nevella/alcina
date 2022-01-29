package cc.alcina.framework.entity.gwt.reflection;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.totsp.gwittir.rebind.beans.IntrospectorFilter;
import com.totsp.gwittir.rebind.beans.IntrospectorFilterHelper;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.util.Ax;

public class ClientReflectionGenerator2 extends Generator {
	private IntrospectorFilter filter;

	private long start;

	private String packageName = getClass().getPackageName();

	private String moduleName;

	private PrintWriter printWriter;

	private ClassSourceFileComposerFactory composerFactory;

	private TreeLogger logger;

	private GeneratorContext context;

	private String typeName;

	private String implementationName;

	private Map<PrintWriter, BiWriter> wrappedWriters = new HashMap<PrintWriter, BiWriter>();

	@Override
	public String generate(TreeLogger logger, GeneratorContext context,
			String typeName) throws UnableToCompleteException {
		try {
			this.logger = logger;
			this.context = context;
			this.typeName = typeName;
			setupEnvironment();
			if (printWriter == null) {
				return packageName + "." + implementationName;
			}
			commit(context, logger, printWriter, true);
			System.out.format("Client reflection generation  [%s] -  %s ms\n",
					filter.getModuleName(), System.currentTimeMillis() - start);
			filter.generationComplete();
			return packageName + "." + implementationName;
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	private void commit(GeneratorContext context, TreeLogger logger,
			PrintWriter printWriter, boolean end) {
		if (end) {
			int debug = 3;
		}
		context.commit(logger, printWriter);
		if (wrappedWriters.containsKey(printWriter)) {
			String text = wrappedWriters.get(printWriter).getStringWriter()
					.toString();
			if (end) {
				Ax.out("ClientReflectionGeneration - context: %s - hash: %s",
						moduleName, text.hashCode());
			}
			// System.out.println(text);
		}
	}

	private void setupEnvironment() throws NotFoundException {
		filter = IntrospectorFilterHelper.getFilter(context);
		start = System.currentTimeMillis();
		// scan for reflectable annotations etc
		String superClassName = null;
		JClassType generatingType = context.getTypeOracle().getType(typeName);
		if (generatingType.isInterface() != null) {
			throw new UnsupportedOperationException("TODO - registry");
			// intrType = context.getTypeOracle()
			// .getType(ClientReflector2.class.getName());
		}
		ReflectionModule module = generatingType
				.getAnnotation(ReflectionModule.class);
		moduleName = module.value();
		filter.setModuleName(moduleName);
		implementationName = String.format("ClientReflector_%s_Impl",
				moduleName);
		superClassName = getQualifiedSourceName(generatingType);
		composerFactory = new ClassSourceFileComposerFactory(this.packageName,
				implementationName);
		printWriter = context.tryCreate(logger, packageName,
				implementationName);
	}

	private String getQualifiedSourceName(JType jType) {
		if (jType.isTypeParameter() != null) {
			return jType.isTypeParameter().getBaseType()
					.getQualifiedSourceName();
		} else {
			return jType.getQualifiedSourceName();
		}
	}
}
