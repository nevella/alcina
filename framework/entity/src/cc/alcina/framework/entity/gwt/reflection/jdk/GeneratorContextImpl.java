package cc.alcina.framework.entity.gwt.reflection.jdk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Set;

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
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.gwt.reflection.impl.typemodel.TypeOracle;
import cc.alcina.framework.entity.util.FileUtils;

class GeneratorContextImpl implements GeneratorContext {
	JdkReflectionGenerator generator;

	TypeOracle typeOracle;

	PropertyOracle propertyOracle;

	Set<String> printWriterPaths = new LinkedHashSet<>();

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
		File packageFolder = FileUtils.child(generationSrcFolder,
				packageName.replace(".", "/"));
		packageFolder.mkdirs();
		String unitFileName = Ax.format("%s.java", simpleName);
		File unitFile = FileUtils.child(packageFolder, unitFileName);
		StringWriter stringWriter = new StringWriter();
		try {
			printWriterPaths.add(unitFile.getCanonicalPath());
			return new LazyPrintWriter(stringWriter, unitFile);
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

	class LazyPrintWriter extends PrintWriter {
		private StringWriter stringWriter;

		private File file;

		public LazyPrintWriter(StringWriter stringWriter, File file)
				throws FileNotFoundException {
			super(stringWriter);
			this.stringWriter = stringWriter;
			this.file = file;
		}

		@Override
		public void close() {
			super.close();
			String out = stringWriter.toString();
			Io.write().string(out).withNoUpdateIdentical(true).toFile(file);
		}
	}
}
