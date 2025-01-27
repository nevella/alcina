package cc.alcina.extras.dev.console.code;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;

public class CompilationUnits {
	public static final String CONTEXT_COMP_UNITS = CompilationUnits.class
			.getName() + ".CONTEXT_COMP_UNITS";

	public static Set<String> invalidSuperclassFqns = new LinkedHashSet<>();

	public static final transient String CONTEXT_LOG_SUPERCLASS_FQN_EXCEPTIONS = CompilationUnits.class
			.getName() + ".CONTEXT_LOG_SUPERCLASS_FQN_EXCEPTIONS";

	/*
	 * Marker interface - computed data associated with a persistent unit,
	 * invalidated on change
	 */
	public static abstract class PersistentUnitData {
		public long fileSize;

		public long fileModificationTime;

		public int version;

		public String path;

		public abstract int currentVersion();

		// for serialization
		protected PersistentUnitData() {
		}

		protected void putFile(File file) {
			putPath(file.getPath());
		}

		protected void putPath(String path) {
			this.version = currentVersion();
			this.path = path;
		}

		public PersistentUnitData(String path) {
			putPath(path);
		}

		public PersistentUnitData(File file) {
			this(file.getPath());
		}

		public boolean isCurrent() {
			if (path == null) {
				return false;
			}
			File file = getFile();
			return fileSize == file.length()
					&& fileModificationTime == file.lastModified()
					&& version == currentVersion();
		}

		public void updateMetadata() {
			File file = getFile();
			fileSize = file.length();
			fileModificationTime = file.lastModified();
			version = currentVersion();
		}

		protected File getFile() {
			return new File(path);
		}

		protected abstract void compute(File file,
				CompilationUnits compilationUnits);
	}

	static String fqn(CompilationUnitWrapper unit, ClassOrInterfaceType n) {
		try {
			return fqn0(unit, n);
		} catch (Error e) {
			Ax.err(unit.getFile().getName());
			Ax.simpleExceptionOut(e);
			return null;
		}
	}

	static String fqn(CompilationUnitWrapper unit, String nameAsString) {
		if (nameAsString.equals(nameAsString.toLowerCase())) {
			// primitive
			return nameAsString;
		}
		nameAsString = nameAsString.replaceFirst("<.+>", "");
		String f_nameAsString = nameAsString;
		Optional<ImportDeclaration> importDecl = unit.unit().getImports()
				.stream()
				.filter(id -> id.getNameAsString().endsWith(f_nameAsString))
				.findFirst();
		if (importDecl.isPresent()) {
			return importDecl.get().getNameAsString();
		} else {
			try {
				Class<?> clazz = Class.forName("java.lang." + nameAsString);
				return clazz.getName();
			} catch (Exception e) {
				try {
					Class<?> clazz = Class
							.forName(unit.unit.getPackageDeclaration().get()
									.getNameAsString() + "." + nameAsString);
					return clazz.getName();
				} catch (Exception e2) {
					try {
						// inner
						String cn = unit.unit.getPackageDeclaration().get()
								.getNameAsString() + "."
								+ unit.unit.getType(0).getNameAsString() + "$"
								+ nameAsString;
						Class<?> clazz = Class.forName(cn);
						return clazz.getName().replace("$", ".");
					} catch (Exception e3) {
						throw new WrappedRuntimeException(e3);
					}
				}
			}
		}
	}

	static String fqn(CompilationUnitWrapper unit, TypeDeclaration n,
			boolean binary) {
		if (n.getParentNode().get() instanceof TypeDeclaration) {
			return fqn(unit, (TypeDeclaration) n.getParentNode().get(), binary)
					+ (binary ? "$" : ".") + n.getNameAsString();
		}
		return Ax.format("%s.%s",
				unit.unit().getPackageDeclaration().get().getNameAsString(),
				n.getNameAsString());
	}

	private static String fqn0(CompilationUnitWrapper unit,
			ClassOrInterfaceType n) {
		String nameAsString = n.getNameAsString();
		Optional<ImportDeclaration> importDecl = unit.unit().getImports()
				.stream()
				.filter(id -> id.getNameAsString().endsWith(nameAsString))
				.findFirst();
		if (importDecl.isPresent()) {
			return importDecl.get().getNameAsString();
		} else {
			try {
				Class<?> clazz = Class.forName("java.lang." + nameAsString);
				return clazz.getName();
			} catch (Exception e) {
				try {
					Class<?> clazz = Class
							.forName(unit.unit.getPackageDeclaration().get()
									.getNameAsString() + "." + nameAsString);
					return clazz.getName();
				} catch (Exception e2) {
					try {
						// inner
						String cn = unit.unit.getPackageDeclaration().get()
								.getNameAsString()
								+ "."
								+ ((ClassOrInterfaceDeclaration) n
										.getParentNode().get().getParentNode()
										.get()).getNameAsString()
								+ "$" + nameAsString;
						Class<?> clazz = Class.forName(cn);
						return clazz.getName().replace("$", ".");
					} catch (Exception e3) {
						throw new WrappedRuntimeException(e3);
					}
				}
			}
		}
	}

	public static String genericPart(String typeFqn) {
		String regex = ".+<([^,]+?)>";
		if (typeFqn.matches(regex)) {
			return typeFqn.replaceFirst(regex, "$1");
		}
		return null;
	}

	public static boolean isGetter(MethodDeclaration n) {
		return n.getName().asString().matches("get.+")
				&& n.getParameters().size() == 0;
	}

	public static CompilationUnits load(SingletonCache<CompilationUnits> cache,
			Collection<String> classPaths,
			BiFunction<CompilationUnits, CompilationUnitWrapper, CompilationUnitWrapperVisitor> visitorCreator,
			boolean refresh) throws Exception {
		CompilationUnits units = load0(cache, classPaths, visitorCreator,
				refresh);
		LooseContext.set(CompilationUnits.CONTEXT_COMP_UNITS, units);
		return units;
	}

	private static CompilationUnits load0(
			SingletonCache<CompilationUnits> cache,
			Collection<String> classPaths,
			BiFunction<CompilationUnits, CompilationUnitWrapper, CompilationUnitWrapperVisitor> visitorCreator,
			boolean refresh) throws Exception {
		if (!refresh && cache.get() != null) {
			return cache.get();
		}
		CompilationUnits units = new CompilationUnits();
		classPaths.parallelStream().forEach(classPath -> {
			List<File> files = SEUtilities.listFilesRecursive(classPath,
					new FileFilter() {
						@Override
						public boolean accept(File file) {
							return file.isDirectory()
									|| file.getName().endsWith(".java");
						}
					});
			int javaFileCount = (int) files.stream()
					.filter(f -> !f.isDirectory()).count();
			SystemoutCounter counter = new SystemoutCounter(50, 10,
					javaFileCount, true);
			Ax.out("%s: %s", classPath, javaFileCount);
			for (File file : files) {
				if (!file.isDirectory()) {
					try {
						CompilationUnitWrapper unit = new CompilationUnitWrapper();
						unit.compute(file, null);
						unit.unit().accept(visitorCreator.apply(units, unit),
								null);
						units.addUnit(unit);
					} catch (Throwable e) {
						Ax.out(e);
						Ax.err("Could not load unit: %s", file);
					}
					counter.tick();
				}
			}
		});
		cache.set(units);
		cache.persist();
		return units;
	}

	void addUnit(CompilationUnitWrapper unit) {
		synchronized (units) {
			if (fileUnits.containsKey(unit.getFile())) {
				return;
			}
			fileUnits.put(unit.getFile(), unit);
			units.add(unit);
			unit.unitTypes.stream().filter(d -> d.hasFlags()).forEach(d -> {
				declarations.put(d.qualifiedSourceName, d);
			});
			unit.unitTypes.forEach(d -> declarationsByName.add(d.name, d));
		}
	}

	synchronized void removeUnit(CompilationUnitWrapper unit) {
		synchronized (units) {
			units.remove(unit);
			fileUnits.remove(unit.getFile());
			unit.unitTypes.stream().filter(d -> d.hasFlags()).forEach(d -> {
				declarations.remove(d.qualifiedSourceName, d);
			});
			unit.unitTypes.forEach(d -> declarationsByName.remove(d.name, d));
		}
	}

	public transient JavaParserFacade solver;

	public List<CompilationUnitWrapper> units = new ArrayList<>();

	public Map<File, CompilationUnitWrapper> fileUnits = new LinkedHashMap<>();

	public Map<String, UnitType> declarations = new LinkedHashMap<>();

	private Multimap<String, List<UnitType>> declarationsByName = new Multimap<>();

	public CompilationUnits() {
		CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ClassLoaderTypeSolver(getClass().getClassLoader()));
		this.solver = JavaParserFacade.get(typeSolver);
	}

	public UnitType declarationByFqn(String typeFqn) {
		return declarations.get(typeFqn);
	}

	public List<UnitType> declarationByName(String simpleName) {
		return declarationsByName.get(simpleName);
	}

	public SolverUtils solverUtils() {
		return new SolverUtils();
	}

	public UnitType typeForClass(Class<?> clazz) {
		return declarationByFqn(clazz.getCanonicalName());
	}

	public void writeDirty(boolean test) {
		writeDirty(test, null, Integer.MAX_VALUE);
	}

	public void writeDirty(boolean test,
			Function<CompilationUnitWrapper, String> mapper, int writeLimit) {
		File outDir = new File("/tmp/refactor");
		SEUtilities.deleteDirectory(outDir, true);
		outDir.mkdirs();
		long dirtyCount = units.stream().filter(u -> u.dirty).count();
		Ax.out("Writing: %s/%s units dirty", dirtyCount, units.size());
		units.stream().filter(u -> u.dirty).limit(writeLimit).forEach(u -> {
			u.writeTo(test ? outDir : null, mapper);
		});
	}

	public void writeDirty(boolean test, int writeLimit) {
		writeDirty(test, null, writeLimit);
	}

	/*
	 * return the persistent data, if valid
	 */
	public <T extends PersistentUnitData> T ensure(Class<T> clazz, File file) {
		return cache.ensure(clazz, file, this);
	}

	public CompilationUnitWrapper ensureUnit(File file) {
		CompilationUnitWrapper wrapper = ensure(CompilationUnitWrapper.class,
				file);
		return wrapper;
	}

	public interface CompilationUnitCache {
		<T extends PersistentUnitData> T ensure(Class<T> clazz, File file,
				CompilationUnits compilationUnits);

		public static class Fs implements CompilationUnitCache {
			Map<Class<? extends PersistentUnitData>, FsObjectCache<? extends PersistentUnitData>> caches = new LinkedHashMap<>();

			public Fs(File root) {
				this.root = root;
				root.mkdirs();
			}

			public File root;

			@Override
			public <T extends PersistentUnitData> T ensure(Class<T> clazz,
					File file, CompilationUnits compilationUnits) {
				FsObjectCache<T> cache = getCache(clazz);
				T instance = (T) cache.get(file.getPath());
				if (instance != null) {
					if (!instance.isCurrent()) {
						instance = null;
					}
				}
				if (instance == null) {
					instance = Reflections.newInstance(clazz);
					instance.compute(file, compilationUnits);
					cache.persist(file.getPath(), instance);
				}
				return instance;
			}

			private <T extends PersistentUnitData> FsObjectCache<T>
					getCache(Class<T> clazz) {
				FsObjectCache<? extends PersistentUnitData> cache = caches
						.computeIfAbsent(clazz, key -> {
							File cacheRoot = FileUtils.child(root,
									NestedName.get(clazz));
							FsObjectCache<? extends PersistentUnitData> newCache = new FsObjectCache<>(
									cacheRoot, clazz, path -> {
										return null;
									});
							newCache.returnNullOnDeserializationException = true;
							return newCache;
						});
				return (FsObjectCache<T>) cache;
			}
		}
	}

	public CompilationUnitCache cache;

	/*
	 * A utility wrapper around a javaparser CompilationUnit, which handles
	 * caching
	 */
	public static class CompilationUnitWrapper extends PersistentUnitData {
		public List<UnitType> unitTypes = new ArrayList<>();

		public transient CompilationUnit unit;

		public boolean dirty;

		transient boolean preparedForModification;

		public CompilationUnitWrapper() {
		}

		public void ensureImport(Class<?> clazz) {
			ensureImport(clazz.getName().replace("$", "."));
		}

		public void ensureImport(String name) {
			if (unit().getImports().stream()
					.noneMatch(id -> id.getName().toString().equals(name))) {
				unit().addImport(name);
				dirty = true;
			}
		}

		public File getFile() {
			return new File(path);
		}

		public boolean hasFlag(TypeFlag flag) {
			return unitTypes.stream().anyMatch(w -> w.hasFlag(flag));
		}

		public boolean hasFlags() {
			return unitTypes.stream().anyMatch(UnitType::hasFlags);
		}

		void prepareForModification() {
			if (!preparedForModification) {
				preparedForModification = true;
				LexicalPreservingPrinter.setup(unit());
			}
		}

		public void removeImport(Class<?> clazz) {
			new ArrayList<>(unit.getImports()).stream()
					.filter(i -> i.getNameAsString().equals(clazz.getName()))
					.forEach(ImportDeclaration::remove);
		}

		public UnitType typeFor(ClassOrInterfaceDeclaration n) {
			unit();
			return unitTypes.stream().filter(
					d -> d.qualifiedSourceName.endsWith(n.getNameAsString()))
					.findFirst().get();
		}

		public CompilationUnit unit() {
			try {
				if (unit == null) {
					unit = StaticJavaParser.parse(getFile());
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			return unit;
		}

		public void writeTo(File outDir,
				Function<CompilationUnitWrapper, String> mapper) {
			if (outDir == null) {
				outDir = getFile().getParentFile();
			}
			File outFile = FileUtils.child(outDir, getFile().getName());
			try {
				String modified = mapper == null
						? LexicalPreservingPrinter.print(unit)
						: mapper.apply(this);
				Io.write().string(modified).toFile(outFile);
				Ax.out("wrote: %s", getFile().getName());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public static final transient int VERSION = 3;

		@Override
		public int currentVersion() {
			return VERSION;
		}

		@Override
		protected void compute(File file, CompilationUnits units) {
			this.path = file.getPath();
			ensureUnitTypeDeclarations();
			synchronized (units) {
				units.units.add(this);
				unitTypes.stream().filter(d -> d.hasFlags()).forEach(d -> {
					units.declarations.put(d.qualifiedSourceName, d);
				});
				unitTypes.forEach(d -> units.declarationsByName.add(d.name, d));
			}
			updateMetadata();
		}

		class EnsureUnitTypesAdapter extends VoidVisitorAdapter<Void> {
			@Override
			public void visit(ClassOrInterfaceDeclaration n, Void arg) {
				CompilationUnitWrapper wrapper = CompilationUnitWrapper.this;
				UnitType type = wrapper.ensureUnitType(n);
				type.setDeclaration(n);
				super.visit(n, arg);
			}
		}

		public void ensureUnitTypeDeclarations() {
			unit().accept(new EnsureUnitTypesAdapter(), null);
		}

		UnitType ensureUnitType(ClassOrInterfaceDeclaration decl) {
			String fqbn = CompilationUnits.fqn(this, decl, true);
			UnitType type = unitTypes.stream()
					.filter(ut -> ut.qualifiedBinaryName.equals(fqbn))
					.findFirst().orElse(null);
			if (type == null) {
				type = new UnitType(this, decl);
				unitTypes.add(type);
			}
			return type;
		}
	}

	public abstract static class CompilationUnitWrapperVisitor
			extends VoidVisitorAdapter<Void> {
		protected CompilationUnitWrapper unit;

		protected CompilationUnits units;

		public CompilationUnitWrapperVisitor(CompilationUnits units,
				CompilationUnitWrapper compUnit) {
			this.units = units;
			this.unit = compUnit;
		}
	}

	public class SolverUtils {
	}

	public interface TypeFlag {
	}
}
