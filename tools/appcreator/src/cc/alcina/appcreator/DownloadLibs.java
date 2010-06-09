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
			String tmpDir = getProject().getProperty(
					Constants.TMP_DOWNLOAD_DIR);
			File tmpDirFile = new File(FileUtils.translatePath(tmpDir));
			Mkdir mkdir;
			for (DownloadableFile downloadableFile : downloadableFileList
					.getFiles()) {
				String containerPath = null;
				String targetPath = targetDirectory + "/"
						+ downloadableFile.getTargetPath();
				Get get = new Get();
				File targetFile = new File(FileUtils.translatePath(targetPath));
				if (targetFile.exists()) {
					continue;
				}
				File downloadFile = targetFile;
				File dir = new File(PackageUtils.folderOf(targetPath));
				mkdir = new Mkdir();
				mkdir.setDir(dir);
				mkdir.execute();
				if (downloadableFile.getContainerPath() != null) {
					containerPath = containerDirectory + "/"
							+ downloadableFile.getContainerPath();
					downloadFile = new File(FileUtils
							.translatePath(containerPath));
				}
				if (!downloadFile.exists()) {
					log("Downloading " + PackageUtils.fileOf(targetPath));
					File tmpFile = new File(FileUtils.translatePath(tmpDir
							+ "/" + downloadFile.getName()));
					clearTmpDir();
					get.setDest(tmpFile);
					// get.setVerbose(true);
					try {
						get.setSrc(new URL(downloadableFile.getUrl()));
						get.execute();
						Move move = new Move();
						move.setFile(tmpFile);
						move.setTofile(downloadFile);
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
					PatternSet patternSet = new PatternSet();
					NameEntry nameEntry = patternSet.createInclude();
					nameEntry.setName("**/"
							+ downloadableFile.getExtractFileName());
					expand.addPatternset(patternSet);
					expand.execute();
					Move move = new Move();
					move.setFile(PackageUtils.findFile(tmpDirFile,
							downloadableFile.getExtractFileName()));
					move.setTofile(targetFile);
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
		File tmpDirFile = new File(FileUtils.translatePath(tmpDir));
		PackageUtils.clearFolder(tmpDirFile);
	}

	public void setContainerDirectory(String containerDirectory) {
		this.containerDirectory = containerDirectory;
	}

	public void setTargetDirectory(String targetDirectory) {
		this.targetDirectory = targetDirectory;
	}
}
