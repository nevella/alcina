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
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;

public class CompilationUnits {
	public static final String CONTEXT_COMP_UNITS = CompilationUnits.class
			.getName() + ".CONTEXT_COMP_UNITS";

	public static Set<String> invalidSuperclassFqns = new LinkedHashSet<>();

	public static final transient String CONTEXT_LOG_SUPERCLASS_FQN_EXCEPTIONS = CompilationUnits.class
			.getName() + ".CONTEXT_LOG_SUPERCLASS_FQN_EXCEPTIONS";

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
						CompilationUnitWrapper unit = new CompilationUnitWrapper(
								file);
						unit.unit().accept(visitorCreator.apply(units, unit),
								null);
						synchronized (units) {
							units.units.add(unit);
							unit.declarations.stream().filter(d -> d.hasFlags())
									.forEach(d -> {
										units.declarations
												.put(d.qualifiedSourceName, d);
									});
							unit.declarations
									.forEach(d -> units.declarationsByName
											.add(d.name, d));
						}
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

	public transient JavaParserFacade solver;

	public List<CompilationUnitWrapper> units = new ArrayList<>();

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

	public static class CompilationUnitWrapper {
		public String path;

		public List<UnitType> declarations = new ArrayList<>();

		public transient CompilationUnit unit;

		public boolean dirty;

		transient boolean preparedForModification;

		public CompilationUnitWrapper() {
		}

		public CompilationUnitWrapper(File file) {
			this.setFile(file);
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
			return declarations.stream().anyMatch(w -> w.hasFlag(flag));
		}

		public boolean hasFlags() {
			return declarations.stream().anyMatch(UnitType::hasFlags);
		}

		public void removeImport(Class<?> clazz) {
			new ArrayList<>(unit.getImports()).stream()
					.filter(i -> i.getNameAsString().equals(clazz.getName()))
					.forEach(ImportDeclaration::remove);
		}

		public void setFile(File file) {
			this.path = file.getAbsolutePath();
		}

		public UnitType typeFor(ClassOrInterfaceDeclaration n) {
			unit();
			return declarations.stream().filter(
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
			File outFile = SEUtilities.getChildFile(outDir,
					getFile().getName());
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

		void prepareForModification() {
			if (!preparedForModification) {
				preparedForModification = true;
				LexicalPreservingPrinter.setup(unit());
			}
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
