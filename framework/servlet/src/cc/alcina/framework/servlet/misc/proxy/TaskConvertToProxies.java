package cc.alcina.framework.servlet.misc.proxy;

import java.util.List;

import cc.alcina.framework.common.client.job.Task;

/**
 * Analyse source, look for entrypoint accesses from accessing code (either
 * static calls or constructor calls)
 * 
 * 
 */
public class TaskConvertToProxies implements Task {
	String importMatcherRegex;

	String outputPackage;

	String outputPackagePath;

	List<String> pathsToScan;

	Class<?> classProxyImpl;

	String classProxyInterfacePackage;

	boolean dryRun;

	String inputPackagePrefix;

	boolean refreshCompilationUnits;

	public Class<?> getClassProxyImpl() {
		return this.classProxyImpl;
	}

	public String getClassProxyInterfacePackage() {
		return this.classProxyInterfacePackage;
	}

	public String getImportMatcherRegex() {
		return this.importMatcherRegex;
	}

	public String getInputPackagePrefix() {
		return this.inputPackagePrefix;
	}

	public String getOutputPackage() {
		return this.outputPackage;
	}

	public String getOutputPackagePath() {
		return this.outputPackagePath;
	}

	public List<String> getPathsToScan() {
		return this.pathsToScan;
	}

	public boolean isDryRun() {
		return this.dryRun;
	}

	public boolean isRefreshCompilationUnits() {
		return this.refreshCompilationUnits;
	}

	public void setClassProxyImpl(Class<?> classProxyImpl) {
		this.classProxyImpl = classProxyImpl;
	}

	public void
			setClassProxyInterfacePackage(String classProxyInterfacePackage) {
		this.classProxyInterfacePackage = classProxyInterfacePackage;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public void setImportMatcherRegex(String importMatcherRegex) {
		this.importMatcherRegex = importMatcherRegex;
	}

	public void setInputPackagePrefix(String inputPackagePrefix) {
		this.inputPackagePrefix = inputPackagePrefix;
	}

	public void setOutputPackage(String outputPackage) {
		this.outputPackage = outputPackage;
	}

	public void setOutputPackagePath(String outputPackagePath) {
		this.outputPackagePath = outputPackagePath;
	}

	public void setPathsToScan(List<String> pathsToScan) {
		this.pathsToScan = pathsToScan;
	}

	public void setRefreshCompilationUnits(boolean refreshCompilationUnits) {
		this.refreshCompilationUnits = refreshCompilationUnits;
	}
}
