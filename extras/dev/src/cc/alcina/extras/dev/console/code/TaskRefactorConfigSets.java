package cc.alcina.extras.dev.console.code;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.ThreeWaySetResult;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Configuration.ConfigurationFile;
import cc.alcina.framework.entity.Configuration.PropertyTree;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.servlet.schedule.PerformerTask;

@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class TaskRefactorConfigSets extends PerformerTask {
	private List<String> classpathEntries = new ArrayList<>();

	private List<Configuration.ConfigurationFile> appPropertyFileEntries = new ArrayList<>();

	private String classpathConfigurationFileFilter = "Bundle\\.properties";

	private String classpathConfigurationFileIgnorePackageFilter = "ignore-no-packages";

	transient List<Configuration.ConfigurationFile> classpathConfigurationFiles = new ArrayList<>();

	private PropertyTree tree;

	private boolean refresh;

	private transient CompilationUnits compUnits;

	private List<String> seenKeys;

	private NameResolver nameResolver;

	List<String> preserveKeys;

	private List<String> removeKeys;

	private int dirtyWriteLimit = 0;

	private boolean justBundleTree;

	public void addProperties(String set, String path) {
		ConfigurationFile configurationFile = new Configuration.ConfigurationFile(
				null, new File(path), set);
		appPropertyFileEntries.add(configurationFile);
	}

	public List<Configuration.ConfigurationFile> getAppPropertyFileEntries() {
		return this.appPropertyFileEntries;
	}

	public String getClasspathConfigurationFileFilter() {
		return this.classpathConfigurationFileFilter;
	}

	public String getClasspathConfigurationFileIgnorePackageFilter() {
		return this.classpathConfigurationFileIgnorePackageFilter;
	}

	public List<String> getClasspathEntries() {
		return this.classpathEntries;
	}

	public int getDirtyWriteLimit() {
		return this.dirtyWriteLimit;
	}

	@AlcinaTransient
	public NameResolver getNameResolver() {
		return this.nameResolver;
	}

	public List<String> getPreserveKeys() {
		return this.preserveKeys;
	}

	public List<String> getRemoveKeys() {
		return this.removeKeys;
	}

	public boolean isJustBundleTree() {
		return this.justBundleTree;
	}

	public boolean isRefresh() {
		return this.refresh;
	}

	@Override
	public void run() throws Exception {
		locateConfigurationFiles();
		if (justBundleTree) {
			//
		} else {
			checkConfigurationFilesValid();
			scanCodeRefs();
		}
		populateTree();
		String csv = tree.asCsv();
		Io.write().string(csv).toPath("/tmp/tree.csv");
		if (justBundleTree) {
			//
		} else {
			List<String> keys = tree.allKeys();
			ThreeWaySetResult<String> split = CommonUtils.threeWaySplit(keys,
					seenKeys);
			Io.write()
					.string(split.firstOnly.stream()
							.collect(Collectors.joining("\n")))
					.toPath("/tmp/not-seen.txt");
		}
	}

	public void setAppPropertyFileEntries(
			List<Configuration.ConfigurationFile> appPropertyFileEntries) {
		this.appPropertyFileEntries = appPropertyFileEntries;
	}

	public void setClasspathConfigurationFileFilter(
			String classpathConfigurationFileFilter) {
		this.classpathConfigurationFileFilter = classpathConfigurationFileFilter;
	}

	public void setClasspathConfigurationFileIgnorePackageFilter(
			String classpathConfigurationFileIgnorePackageFilter) {
		this.classpathConfigurationFileIgnorePackageFilter = classpathConfigurationFileIgnorePackageFilter;
	}

	public void setClasspathEntries(List<String> classpathEntries) {
		this.classpathEntries = classpathEntries;
	}

	public void setDirtyWriteLimit(int dirtyWriteLimit) {
		this.dirtyWriteLimit = dirtyWriteLimit;
	}

	public void setJustBundleTree(boolean justBundleTree) {
		this.justBundleTree = justBundleTree;
	}

	public void setNameResolver(NameResolver nameResolver) {
		this.nameResolver = nameResolver;
	}

	public void setPreserveKeys(List<String> preserveKeys) {
		this.preserveKeys = preserveKeys;
	}

	public void setRefresh(boolean refresh) {
		this.refresh = refresh;
	}

	public void setRemoveKeys(List<String> removeKeys) {
		this.removeKeys = removeKeys;
	}

	private void checkConfigurationFilesValid() {
		List<ConfigurationFile> nonNamespaced = classpathConfigurationFiles
				.stream()
				.filter(ConfigurationFile::provideContainsNonNamespaced)
				.collect(Collectors.toList());
		if (nonNamespaced.size() > 0) {
			Ax.out(nonNamespaced);
			throw new UnsupportedOperationException();
		}
	}

	/*
	 * Traverse classpathentries
	 *
	 * Scan for matching filter
	 *
	 * Construct - relative path == package
	 *
	 *
	 */
	private void locateConfigurationFiles() {
		for (String path : classpathEntries) {
			Stack<String> folders = new Stack<>();
			folders.push(path);
			while (folders.size() > 0) {
				String folder = folders.pop();
				File[] files = new File(folder).listFiles();
				for (File file : files) {
					if (file.isDirectory()) {
						folders.push(file.getPath());
					} else {
						if (file.getName()
								.matches(classpathConfigurationFileFilter)) {
							ConfigurationFile configurationFile = new Configuration.ConfigurationFile(
									path, file, null);
							if (configurationFile.getPackageName().matches(
									classpathConfigurationFileIgnorePackageFilter)) {
							} else {
								classpathConfigurationFiles
										.add(configurationFile);
							}
						}
					}
				}
			}
		}
	}

	private void populateTree() {
		this.tree = new Configuration.PropertyTree();
		classpathConfigurationFiles.forEach(tree::add);
		appPropertyFileEntries.forEach(tree::add);
		Set<String> trulyRemove = CommonUtils.threeWaySplit(removeKeys,
				preserveKeys).firstOnly;
		tree.removeKeys(trulyRemove);
	}

	private void scanCodeRefs() throws Exception {
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, classpathEntries,
				DeclarationVisitor::new, isRefresh());
		long count = compUnits.declarations.values().stream()
				.filter(dec -> dec.hasFlag(Type.HasConfiguration)).count();
		Ax.out("count with config: %s", count);
		SourceHandler sourceHandler = new SourceHandler();
		compUnits.declarations.values().stream().filter(UnitType::exists)
				.forEach(dec -> sourceHandler.listConfigurationCalls(dec));
		this.seenKeys = sourceHandler.refs.stream()
				.flatMap(ref -> ref.keys.stream()).map(key -> key.toString())
				.collect(Collectors.toList());
		if (dirtyWriteLimit != 0) {
			sourceHandler.removeSuperfluousClassLiterals();
			compUnits.writeDirty(false, dirtyWriteLimit);
		}
	}

	public interface NameResolver {
		public UnitType resolve(List<UnitType> choices, Object source,
				String name);
	}

	static class DeclarationVisitor extends CompilationUnitWrapperVisitor {
		public DeclarationVisitor(CompilationUnits units,
				CompilationUnitWrapper compUnit) {
			super(units, compUnit);
		}

		@Override
		public void visit(ClassOrInterfaceDeclaration node, Void arg) {
			try {
				visit0(node, arg);
			} catch (VerifyError ve) {
				Ax.out("Verify error: %s", node.getName());
			}
		}

		private void visit0(ClassOrInterfaceDeclaration node, Void arg) {
			UnitType type = new UnitType(unit, node);
			type.setDeclaration(node);
			unit.declarations.add(type);
			boolean hasConfiguration = node.toString()
					.contains("Configuration.");
			if (hasConfiguration) {
				type.setFlag(Type.HasConfiguration);
			}
			super.visit(node, arg);
		}
	}

	class SourceHandler {
		List<Ref> refs = new ArrayList<>();

		public void listConfigurationCalls(UnitType type) {
			ConfigurationCallLister lister = new ConfigurationCallLister(type);
			type.getDeclaration().accept(lister, null);
		}

		public void removeSuperfluousClassLiterals() {
			refs.stream().filter(ref -> ref.superfluousExplicitClass)
					.forEach(Ref::removeExplicitClassRef);
		}

		class ConfigurationCallLister extends VoidVisitorAdapter<Void> {
			private UnitType type;

			public ConfigurationCallLister(UnitType type) {
				this.type = type;
			}

			@Override
			public void visit(ClassOrInterfaceDeclaration n, Void arg) {
				if (n != type.getDeclaration()) {
					// stop (don't visit inner classes)
				} else {
					super.visit(n, arg);
				}
			}

			@Override
			public void visit(MethodCallExpr expr, Void arg) {
				if (expr.toString()
						.matches("(?s)Configuration\\.(has|is|get|key).*")) {
					Ref ref = new Ref(expr, type);
					refs.add(ref);
				}
				super.visit(expr, arg);
			};
		}

		class Ref {
			List<Configuration.Key> keys = new ArrayList<>();

			UnitType type;

			boolean implicitClass;

			boolean nonLiteral = false;

			boolean superfluousExplicitClass;

			MethodCallExpr expr;

			private ClassExpr superfluousArgument;

			public Ref(MethodCallExpr expr, UnitType type) {
				this.expr = expr;
				this.type = type;
				int size = expr.getArguments().size();
				if (size == 0) {
					// is() called on .key()
					return;
				}
				UnitType classParamWrapper = null;
				implicitClass = size == 1;
				Expression keyNameExpr = null;
				if (implicitClass) {
					classParamWrapper = type;
					keyNameExpr = expr.getArgument(0);
				} else {
					keyNameExpr = expr.getArgument(1);
				}
				String keyName = null;
				if (keyNameExpr instanceof StringLiteralExpr) {
					keyName = ((StringLiteralExpr) keyNameExpr).getValue();
				} else if (keyNameExpr instanceof NameExpr) {
					Optional<FieldDeclaration> fieldByName = type
							.getDeclaration()
							.getFieldByName(keyNameExpr.toString());
					if (fieldByName.isPresent()) {
						VariableDeclarator variable = fieldByName.get()
								.getVariable(0);
						Optional<Expression> initializer = variable
								.getInitializer();
						if (initializer.isPresent()) {
							Expression initializerExpr = initializer.get();
							if (initializerExpr instanceof StringLiteralExpr) {
								keyName = ((StringLiteralExpr) initializerExpr)
										.getValue();
							}
						}
						if (keyName == null) {
							checkNonLiteral();
							nonLiteral = true;
							return;
						}
					} else {
						checkNonLiteral();
						nonLiteral = true;
						return;
					}
				} else {
					checkNonLiteral();
					nonLiteral = true;
					return;
				}
				if (!implicitClass) {
					ClassExpr argument = (ClassExpr) expr.getArgument(0);
					String className = argument.getType().asString();
					UnitType classDeclByName = className.equals(type.name)
							? type
							: declarationByName(className);
					if (classDeclByName == null) {
						throw new UnsupportedOperationException();
					} else {
						if (classDeclByName == type) {
							if (classDeclByName.toString()
									.contains("Connection")
									|| classDeclByName.toString()
											.contains("DomainStoreLoader")) {
								int debug = 3;
							}
							superfluousExplicitClass = true;
							superfluousArgument = argument;
						}
						classParamWrapper = classDeclByName;
					}
				}
				List<String> superClassNames = getSuperclassNames(
						classParamWrapper);
				String f_keyName = keyName;
				keys = superClassNames.stream()
						.map(n -> Configuration.Key
								.stringKey(Ax.format("%s.%s", n, f_keyName)))
						.collect(Collectors.toList());
			}

			private void checkNonLiteral() {
				String string = expr.toString();
				if (string.matches(
						".+\\.(contains||equals||withContextOverride).*")) {
					return;
				}
				switch (string) {
				case "Configuration.get(getKey(\"password\"))":
				case "Configuration.get(getKey(\"username\"))":
					return;
				case "Configuration.key(BarpubGallery.class, BarpubGallery.GENERATE)":
				case "Configuration.key(Gallery.class, Gallery.DEVICE)":
				case "Configuration.key(BarpubGallery.class, REPLACE_BASELINE)":
				case "Configuration.key(BarpubGallery.class, COMPARE)":
				case "Configuration.get(RemoteInvocationServlet.class, Ax.format(\"%s.permittedAddresses\", params.api))":
					Ax.err(string);
					break;
				default:
					throw new UnsupportedOperationException();
				}
			}

			private List<String>
					getSuperclassNames(UnitType classParamWrapper) {
				UnitType cursor = classParamWrapper;
				List<String> result = new ArrayList<>();
				while (true) {
					String name = cursor.getDeclaration().getNameAsString();
					if (name.equals("Object")) {
						break;
					}
					result.add(0, name);
					Optional<ClassOrInterfaceType> extended = cursor
							.getDeclaration().getExtendedTypes().stream()
							.findFirst();
					if (extended.isPresent()) {
						String extendedName = extended.get().getNameAsString();
						UnitType type = declarationByName(extendedName);
						if (type != null) {
							cursor = type;
						} else {
							break;
						}
					} else {
						break;
					}
				}
				return result;
			}

			UnitType declarationByName(String name) {
				List<UnitType> types = compUnits.declarationByName(name);
				if (types == null) {
					switch (name) {
					case "RemoteServiceServlet":
					case "AbstractHandler":
					case "HttpServlet":
					case "TokenSecured":
					case "GenericServlet":
					case "Application":
						// no key parent(s)
						break;
					default:
						throw new UnsupportedOperationException();
					}
					return null;
				} else if (types.size() == 1) {
					return types.get(0);
				} else {
					return nameResolver.resolve(types, SourceHandler.this,
							name);
				}
			}

			void removeExplicitClassRef() {
				superfluousArgument.remove();
				type.dirty();
			}
		}
	}

	enum Type implements TypeFlag {
		HasConfiguration
	}
}
