package cc.alcina.framework.entity.gwt.reflection.jdk;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.google.gwt.core.ext.CachedGeneratorResult;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.dev.resource.ResourceOracle;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.gwt.reflection.impl.typemodel.TypeOracle;

class GeneratorContextImpl implements GeneratorContext {
	JdkReflectionGenerator generator;

	TypeOracle typeOracle;

	PropertyOracle propertyOracle;

	GeneratorContextImpl(JdkReflectionGenerator jdkReflectionGenerator) {
		this.generator = jdkReflectionGenerator;
		typeOracle = new TypeOracle();
		typeOracle.setTypeProvider(jdkReflectionGenerator.typeProvider);
		propertyOracle = new PropertyOracleImpl(this);
	}

	@Override
	public boolean checkRebindRuleAvailable(String sourceTypeName) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'checkRebindRuleAvailable'");
	}

	@Override
	public void commit(TreeLogger logger, PrintWriter pw) {
		pw.flush();
		pw.close();
	}

	@Override
	public void commitArtifact(TreeLogger logger, Artifact<?> artifact)
			throws UnableToCompleteException {
		// NOOP (not sure if reachability is already written?)
	}

	@Override
	public GeneratedResource commitResource(TreeLogger logger, OutputStream os)
			throws UnableToCompleteException {
		throw new UnsupportedOperationException(
				"Unimplemented method 'commitResource'");
	}

	@Override
	public CachedGeneratorResult getCachedGeneratorResult() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'getCachedGeneratorResult'");
	}

	@Override
	public PropertyOracle getPropertyOracle() {
		return propertyOracle;
	}

	@Override
	public ResourceOracle getResourcesOracle() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getResourcesOracle'");
	}

	@Override
	public TypeOracle getTypeOracle() {
		return typeOracle;
	}

	@Override
	public boolean isGeneratorResultCachingEnabled() {
		// TODO - jdk reflection - revisit
		return false;
	}

	@Override
	public boolean isProdMode() {
		return false;
	}

	@Override
	public PrintWriter tryCreate(TreeLogger logger, String packageName,
			String simpleName) {
		File generationSrcFolder = generator.attributes.generationSrcFolder();
		File packageFolder = SEUtilities.getChildFile(generationSrcFolder,
				packageName.replace(".", "/"));
		packageFolder.mkdirs();
		String unitFileName = Ax.format("%s.java", simpleName);
		File unitFile = SEUtilities.getChildFile(packageFolder, unitFileName);
		try {
			return new PrintWriter(unitFile);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	@Override
	public OutputStream tryCreateResource(TreeLogger logger, String partialPath)
			throws UnableToCompleteException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'tryCreateResource'");
	}

	@Override
	public boolean tryReuseTypeFromCache(String typeName) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'tryReuseTypeFromCache'");
	}
}
