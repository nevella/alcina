package au.org.alcina;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class TemplateCreator {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new TemplateCreator().renameFiles("AlcinaTest","AlcinaTemplate");
			//replacePackageText(incomingPackageRoot,outgoingPackageRoot,"AlcinaTest","AlcinaTemplate");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<String> projectPaths = Arrays
			.asList(new String[] {
					"/Users/ouiji/Documents/current/barnet/src/projects/alcina/template/client",
					"/Users/ouiji/Documents/current/barnet/src/projects/alcina/template/entities",
					"/Users/ouiji/Documents/current/barnet/src/projects/alcina/template/server" });

	private static String incomingPackageRoot = "au.org.alcina.template";

	private static String outgoingPackageRoot = "au.org.alcina.template";

	// re
	private void replacePackageText(String templatePackageRoot,
			String targetTemplatePackageRoot, String templateName,
			String targetPrefix) throws Exception { 
		String tnlc = CommonUtils.infix(templateName);
		String tplc = CommonUtils.infix(targetPrefix);
		for (String path : projectPaths) {
			// get list of java files which have the annotation
			// copy, move
			Stack<File> fStack = new Stack<File>();
			fStack.push(new File(path));
			while (!fStack.isEmpty()) {
				File f = fStack.pop();
				File[] files = f.listFiles();
				for (File file : files) {
					if (file.isDirectory()) {
						fStack.push(file);
						continue;
					}
					if (file.getName().endsWith("class")||file.getName().endsWith(".jar")) {
						continue;
					}
					String s = ResourceUtilities.readFileToString(file);
					String orig = s;
					s = s.replace(templatePackageRoot,
							targetTemplatePackageRoot);
					s = s.replace(templateName,
							targetPrefix);
					s = s.replace(tnlc,
							tplc);
					if (!s.equals(orig)){
						ResourceUtilities.writeStringToFile(s, file);
					}
					
				}
			}
		}
	}

	private void renameFiles(String templateName, String targetPrefix)
			throws Exception {
		String tnlc = CommonUtils.infix(templateName);
		String tplc = CommonUtils.infix(targetPrefix);
		for (String path : projectPaths) {
			// get list of java files which have the annotation
			// copy, move
			Stack<File> fStack = new Stack<File>();
			fStack.push(new File(path));
			while (!fStack.isEmpty()) {
				File f = fStack.pop();
				File[] files = f.listFiles();
				for (File file : files) {
					if (file.isDirectory()) {
						fStack.push(file);
						continue;
					}
					if (file.getName().startsWith(templateName)) {
						file.renameTo(new File(file.getParent()
								+ "/"
								+ file.getName().replaceFirst(templateName,
										targetPrefix)));
					}
					if (file.getName().startsWith(tnlc)) {
						file.renameTo(new File(file.getParent() + "/"
								+ file.getName().replaceFirst(tnlc, tplc)));
					}
				}
			}
		}
	}
}
