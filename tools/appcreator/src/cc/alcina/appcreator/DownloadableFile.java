package cc.alcina.appcreator;


public class DownloadableFile {
	private String url;
	private String url2;
	private String tocUrl;
	private String targetPath;
	private LicenseType licenseType;
	private String extractFileName;
	private String containerPath;
	public String getUrl() {
		return this.url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl2() {
		return this.url2;
	}
	public void setUrl2(String url2) {
		this.url2 = url2;
	}
	public String getTocUrl() {
		return this.tocUrl;
	}
	public void setTocUrl(String tocUrl) {
		this.tocUrl = tocUrl;
	}
	public String getTargetPath() {
		return this.targetPath;
	}
	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}
	public LicenseType getLicenseType() {
		return this.licenseType;
	}
	public void setLicenseType(LicenseType licenseType) {
		this.licenseType = licenseType;
	}
	public void setExtractFileName(String extractFileName) {
		this.extractFileName = extractFileName;
	}
	public String getExtractFileName() {
		return extractFileName;
	}
	public void setContainerPath(String containerPath) {
		this.containerPath = containerPath;
	}
	public String getContainerPath() {
		return containerPath;
	}
	
}
