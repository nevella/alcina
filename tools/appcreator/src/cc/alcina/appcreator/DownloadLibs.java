package cc.alcina.appcreator;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Get;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.taskdefs.Move;
import org.apache.tools.ant.taskdefs.Pack;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;
import org.apache.tools.ant.util.FileUtils;

public class DownloadLibs extends Task {
	private DownloadableFileList downloadableFileList;

	private String targetDirectory;

	private String containerDirectory;

	public void add(DownloadableFileList downloadableFileList) {
		this.downloadableFileList = downloadableFileList;
	}

	@Override
	public void execute() throws BuildException {
		try {
			String tmpDir = getProject()
					.getProperty(Constants.TMP_DOWNLOAD_DIR);
			File tmpDirFile = new File(PackageUtils.translatePath(tmpDir));
			Mkdir mkdir;
			for (DownloadableFile downloadableFile : downloadableFileList
					.getFiles()) {
				String containerPath = null;
				String targetPath = targetDirectory + "/"
						+ downloadableFile.getTargetPath();
				Get get = new Get();
				get.setProject(getProject());
				get.setTaskName("get");
				File targetFile = new File(
						PackageUtils.translatePath(targetPath));
				if (targetFile.exists()) {
					continue;
				}
				File downloadFile = targetFile;
				File dir = new File(PackageUtils.folderOf(targetPath));
				mkdir = new Mkdir();
				mkdir.setProject(getProject());
				mkdir.setDir(dir);
				mkdir.setTaskName("mkdir");
				log("" + dir);
				mkdir.execute();
				if (downloadableFile.getContainerPath() != null) {
					containerPath = containerDirectory + "/"
							+ downloadableFile.getContainerPath();
					downloadFile = new File(
							PackageUtils.translatePath(containerPath));
				}
				if (!downloadFile.exists()) {
					log("Downloading " + PackageUtils.fileOf(targetPath));
					File tmpFile = new File(PackageUtils.translatePath(tmpDir
							+ "/" + downloadFile.getName()));
					clearTmpDir();
					get.setDest(tmpFile);
					String libsLocalPath = getProject().getProperty(
							(Constants.ALCINA_DOWNLOAD_LIBS_LOCAL));
					String url = downloadableFile.getUrl();
					if (libsLocalPath != null) {
						url = url.replace(
								"http://alcina.cc/files/framework/lib",
								libsLocalPath);
					}
					String deployLocalPath = getProject().getProperty(
							(Constants.ALCINA_DOWNLOAD_DEPLOY_LOCAL));
					if (deployLocalPath != null) {
						url = url.replace(
								"http://alcina.cc/files/framework/deploy",
								deployLocalPath);
					}
					// get.setVerbose(true);
					try {
						get.setSrc(new URL(url));
						get.execute();
						Move move = new Move();
						move.setProject(getProject());
						move.setFile(tmpFile);
						move.setTofile(downloadFile);
						move.setTaskName("move");
						move.execute();
						if (!PackageUtils.isNullOrEmpty(downloadableFile
								.getTocUrl())) {
							log("*** File download implies acceptance of Terms and Conditions at "
									+ downloadableFile.getTocUrl());
						}
					} catch (BuildException be) {
						System.out.println(be);
						throw be;
						// get.setSrc(new URL(df.getUrl2()));
						// get.execute();
					}
				}
				if (downloadFile != targetFile) {
					clearTmpDir();
					Expand expand = new Expand();
					expand.setSrc(downloadFile);
					expand.setDest(tmpDirFile);
					expand.setProject(getProject());
					expand.setTaskName("expand");
					PatternSet patternSet = new PatternSet();
					NameEntry nameEntry = patternSet.createInclude();
					nameEntry.setName("**/"
							+ downloadableFile.getExtractFileName());
					expand.addPatternset(patternSet);
					expand.execute();
					Move move = new Move();
					move.setProject(getProject());
					move.setFile(PackageUtils.findFile(tmpDirFile,
							downloadableFile.getExtractFileName()));
					move.setTofile(targetFile);
					move.setTaskName("move");
					move.execute();
					clearTmpDir();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (e instanceof BuildException) {
				BuildException be = (BuildException) e;
				throw be;
			}
			throw new BuildException(e);
		}
	}

	private void clearTmpDir() {
		String tmpDir = getProject().getProperty(Constants.TMP_DOWNLOAD_DIR);
		File tmpDirFile = new File(PackageUtils.translatePath(tmpDir));
		PackageUtils.clearFolder(tmpDirFile);
	}

	public void setContainerDirectory(String containerDirectory) {
		this.containerDirectory = containerDirectory;
	}

	public void setTargetDirectory(String targetDirectory) {
		this.targetDirectory = targetDirectory;
	}
}
