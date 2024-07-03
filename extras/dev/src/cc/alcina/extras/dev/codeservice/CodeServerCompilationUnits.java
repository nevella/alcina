package cc.alcina.extras.dev.codeservice;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.extras.dev.codeservice.CodeService.Event;
import cc.alcina.extras.dev.codeservice.CodeService.PackageEvent;
import cc.alcina.extras.dev.codeservice.SourceFolder.SourcePackage;
import cc.alcina.extras.dev.console.code.CompilationUnits;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitCache;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.UnitType;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.util.DataFolderProvider;

/*
 * Models access to the source models (acts as a wrapper to CompilationUnits,
 * and tracks all source files)
 */
public class CodeServerCompilationUnits implements CodeService.Handler {
	CodeService codeService;

	List<SourceFolder> sourceFolders;

	ConcurrentHashMap<SourcePackage, PackageUnits> packageUnits = new ConcurrentHashMap<>();

	CompilationUnits compilationUnits;

	CodeServerCompilationUnits(CodeService codeService) {
		this.codeService = codeService;
		sourceFolders = new ArrayList<>();
		List<String> paths = codeService.sourceAndClassPaths;
		for (int idx = 0; idx < paths.size(); idx += 2) {
			sourceFolders
					.add(new SourceFolder(paths.get(idx), paths.get(idx + 1)));
		}
		compilationUnits = new CompilationUnits();
		File cacheFolder = DataFolderProvider.get()
				.getChildFile(getClass().getName());
		compilationUnits.cache = new CompilationUnitCache.Fs(cacheFolder);
	}

	/*
	 * Models info such as "what .java files are in this package"
	 */
	class PackageUnits {
		SourcePackage sourcePackage;

		PackageUnits(SourcePackage sourcePackage) {
			this.sourcePackage = sourcePackage;
			units = sourcePackage.listFiles().stream()
					.map(compilationUnits::ensureUnit)
					.collect(Collectors.toList());
		}

		public List<CompilationUnitWrapper> units;

		public Stream<UnitType> declaredTypes() {
			return units.stream().flatMap(cuw -> cuw.unitTypes.stream());
		}
	}

	@Override
	public void handle(Event event) {
		if (event instanceof PackageEvent) {
			handlePackageEvent((PackageEvent) event);
		}
	}

	void handlePackageEvent(PackageEvent event) {
		packageUnits.remove(event.sourcePackage);
	}

	public PackageUnits getPackageUnits(SourcePackage sourcePackage) {
		return packageUnits.computeIfAbsent(sourcePackage, PackageUnits::new);
	}

	public SourceFolder getSourceFolderFor(File file) {
		try {
			String canonicalPath = file.getCanonicalPath();
			return sourceFolders.stream()
					.filter(sf -> sf.containsPath(canonicalPath)).findFirst()
					.orElse(null);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public boolean isInSourcePath(String name) {
		String packageName = name.replaceFirst("(.+)\\.(.+)", "$1");
		SourcePackage sourcePackage = new SourcePackage(packageName);
		return packageUnits.containsKey(sourcePackage);
	}
}
