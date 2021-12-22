package cc.alcina.extras.dev.console.code;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;

public class CompilationUnits {
	public static final String CONTEXT_COMP_UNITS = CompilationUnits.class
			.getName() + ".CONTEXT_COMP_UNITS";

	public static Set<String> invalidSuperclassFqns = new LinkedHashSet<>();

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
			StringMap classPaths,
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
			SingletonCache<CompilationUnits> cache, StringMap classPaths,
			BiFunction<CompilationUnits, CompilationUnitWrapper, CompilationUnitWrapperVisitor> visitorCreator,
			boolean refresh) throws Exception {
		if (!refresh && cache.get() != null) {
			return cache.get();
		}
		CompilationUnits units = new CompilationUnits();
		for (String classPath : classPaths.keySet()) {
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
					CompilationUnitWrapper unit = new CompilationUnitWrapper(
							file);
					unit.unit().accept(visitorCreator.apply(units, unit), null);
					units.units.add(unit);
					unit.declarations.stream().filter(d -> d.hasFlags())
							.forEach(d -> {
								units.declarations.put(d.qualifiedSourceName,
										d);
								Ax.out("%s :: %s", d.typeFlags,
										d.clazz().getSimpleName());
							});
					counter.tick();
				}
			}
		}
		cache.set(units);
		cache.persist();
		return units;
	}

	static String fqn(CompilationUnitWrapper unit, ClassOrInterfaceType n) {
		try {
			return fqn0(unit, n);
		} catch (Error e) {
			Ax.err(unit.file.getName());
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

	public Map<String, ClassOrInterfaceDeclarationWrapper> declarations = new LinkedHashMap<>();

	public CompilationUnits() {
		CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ClassLoaderTypeSolver(getClass().getClassLoader()));
		this.solver = JavaParserFacade.get(typeSolver);
	}

	public ClassOrInterfaceDeclarationWrapper
			declarationWrapperForClass(Class<?> clazz) {
		return declByFqn(clazz.getCanonicalName());
	}

	public ClassOrInterfaceDeclarationWrapper declByFqn(String typeFqn) {
		return declarations.get(typeFqn);
	}

	public SolverUtils solverUtils() {
		return new SolverUtils();
	}

	public void writeDirty(boolean test) {
		File outDir = new File("/tmp/refactor");
		SEUtilities.deleteDirectory(outDir);
		outDir.mkdirs();
		units.stream().filter(u -> u.dirty)
				.forEach(u -> u.writeTo(test ? outDir : null));
	}

	public static class ClassOrInterfaceDeclarationWrapper {
		private transient ClassOrInterfaceDeclaration declaration;

		public Set<TypeFlag> typeFlags = new LinkedHashSet<>();

		public String name;

		public CompilationUnitWrapper unitWrapper;

		public String qualifiedSourceName;

		public String qualifiedBinaryName;

		public String superclassFqn;

		public boolean invalid;

		public ClassOrInterfaceDeclarationWrapper() {
		}

		public ClassOrInterfaceDeclarationWrapper(CompilationUnitWrapper unit,
				ClassOrInterfaceDeclaration n) {
			this.unitWrapper = unit;
			this.name = n.getNameAsString();
			qualifiedSourceName = fqn(unit, n, false);
			qualifiedBinaryName = fqn(unit, n, true);
			if (qualifiedSourceName == null) {
				invalid = true;
				return;
			}
			NodeList<ClassOrInterfaceType> extendedTypes = n.getExtendedTypes();
			if (extendedTypes.size() > 1) {
				throw new UnsupportedOperationException();
			} else if (extendedTypes.size() == 1) {
				ClassOrInterfaceType exType = extendedTypes.get(0);
				try {
					superclassFqn = fqn(unit, exType);
				} catch (Exception e) {
					invalidSuperclassFqns.add(exType.getNameAsString());
					Ax.out("%s: %s", e.getMessage(), exType.getNameAsString());
				}
			}
		}

		public Class clazz() {
			return Reflections.classLookup()
					.getClassForName(qualifiedBinaryName);
		}

		public void dirty() {
			unitWrapper.dirty = true;
		}

		public void dirty(String initialSource, String ensuredSource) {
			if (!Objects.equals(initialSource, ensuredSource)) {
				dirty();
			}
		}

		public void ensureImport(Class<?> clazz) {
			unitWrapper.ensureImport(clazz);
		}

		public void ensureImport(String name) {
			unitWrapper.ensureImport(name);
		}

		public ClassOrInterfaceDeclaration getDeclaration() {
			if (declaration == null) {
				unitWrapper.unit().accept(new VoidVisitorAdapter<Void>() {
					@Override
					public void visit(ClassOrInterfaceDeclaration node,
							Void arg) {
						if (Objects.equals(qualifiedSourceName,
								fqn(unitWrapper, node, false))) {
							declaration = node;
						}
						super.visit(node, arg);
					}
				}, null);
			}
			return declaration;
		}

		public boolean hasFlag(TypeFlag flag) {
			return typeFlags.contains(flag);
		}

		public boolean hasFlags() {
			return typeFlags.size() > 0;
		}

		public boolean hasSuperclass(String fqn) {
			if (Objects.equals(fqn, superclassFqn)) {
				return true;
			}
			if (superclassFqn == null) {
				return false;
			}
			ClassOrInterfaceDeclarationWrapper compUnitClassDec = compUnits().declarations
					.get(superclassFqn);
			if (compUnitClassDec == null) {
				return false;
			}
			return compUnitClassDec.hasSuperclass(fqn);
		}

		public boolean isAssignableFrom(Class<?> from) {
			return from.isAssignableFrom(clazz());
		}

		public String resolveFqn(Type type) {
			return fqn(unitWrapper, type.asString());
		}

		public String resolveFqnOrNull(String genericPart) {
			try {
				return fqn(unitWrapper, genericPart);
			} catch (Exception e) {
				return null;
			}
		}

		public String resolveFqnOrNull(Type type) {
			try {
				return resolveFqn(type);
			} catch (Exception e) {
				return null;
			}
		}

		public void setDeclaration(ClassOrInterfaceDeclaration declaration) {
			this.declaration = declaration;
		}

		public void setFlag(TypeFlag flag) {
			setFlag(flag, true);
		}

		public void setFlag(TypeFlag flag, boolean set) {
			if (flag == null) {
				return;
			}
			if (set) {
				typeFlags.add(flag);
			} else {
				typeFlags.remove(flag);
			}
		}

		public String simpleName() {
			return qualifiedSourceName.replaceFirst(".+\\.", "");
		}

		@Override
		public String toString() {
			return Ax.format("%s: %s", typeFlags, name);
		}

		public <T> T typedInstance() {
			return (T) Reflections.newInstance(clazz());
		}

		private CompilationUnits compUnits() {
			return LooseContext.get(CONTEXT_COMP_UNITS);
		}
	}

	public static class CompilationUnitWrapper {
		public File file;

		public List<ClassOrInterfaceDeclarationWrapper> declarations = new ArrayList<>();

		transient CompilationUnit unit;

		public boolean dirty;

		public CompilationUnitWrapper() {
		}

		public CompilationUnitWrapper(File file) {
			this.file = file;
		}

		public ClassOrInterfaceDeclarationWrapper
				declarationWrapperFor(ClassOrInterfaceDeclaration n) {
			unit();
			return declarations.stream().filter(
					d -> d.qualifiedSourceName.endsWith(n.getNameAsString()))
					.findFirst().get();
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

		public boolean hasFlag(TypeFlag flag) {
			return declarations.stream().anyMatch(w -> w.hasFlag(flag));
		}

		public boolean hasFlags() {
			return declarations.stream()
					.anyMatch(ClassOrInterfaceDeclarationWrapper::hasFlags);
		}

		public void removeImport(Class<?> clazz) {
			new ArrayList<>(unit.getImports()).stream()
					.filter(i -> i.getNameAsString().equals(clazz.getName()))
					.forEach(ImportDeclaration::remove);
		}

		public CompilationUnit unit() {
			try {
				if (unit == null) {
					unit = StaticJavaParser.parse(file);
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			return unit;
		}

		public void writeTo(File outDir) {
			if (outDir == null) {
				outDir = file.getParentFile();
			}
			File outFile = SEUtilities.getChildFile(outDir, file.getName());
			try {
				ResourceUtilities.writeStringToFile(unit.toString(), outFile);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
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
