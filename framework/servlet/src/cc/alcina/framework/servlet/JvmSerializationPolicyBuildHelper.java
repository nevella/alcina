package cc.alcina.framework.servlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JvmSerializationPolicyBuildHelper {
	private static final String nocachejs = ".nocache.js";

	public static void main(String [] args) throws Exception
	{
		if (args.length != 2) {
			System.err.println("You must provide the build path and the module base");
		}
		retrieveSerializationPolicies(args[0], args[1]);
	}

	public static void retrieveSerializationPolicies(String buildPath, String moduleBase) {
		String moduleNoCacheJs = moduleBase + nocachejs;
		String moduleBuildPath = buildPath + File.separator + moduleBase;
		List<String> guessAllGwtPolicyName = guessAllGwtPolicyName(moduleBuildPath, moduleNoCacheJs);

		String policyNames = "<html><head/>";
		for (String policyName : guessAllGwtPolicyName) {
			policyNames += policyName + ",";
		}
		policyNames += "</html>";
		writeTextFile(policyNames, moduleBuildPath + File.separator + "policyNames.html");
	}

	private static List<String> guessAllGwtPolicyName(String buildPath, String nocachejs) {
		String filePath = buildPath + File.separator + nocachejs;
		String responseText = readTextFile(new File(filePath));
		List<String> findGwtNames = findGwtNames(responseText);
		String firstCacheJS = findGwtNames.iterator().next();
		String cacheurl = firstCacheJS + ".cache.js";
		filePath = buildPath + File.separator + cacheurl;
		String responseCache = readTextFile(new File(filePath));
		List<String> findRpcNames = findGwtNames(responseCache);
		List<String> gwtRpcValidatedList = new ArrayList<String>();
		for (Iterator<String> iterator = findRpcNames.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			gwtRpcValidatedList.add("'" + string + "'");
		}
		return gwtRpcValidatedList;
	}

	private static List<String> findGwtNames(String responseText) {
		List<String> result = new ArrayList<String>();
		// 32 chars surrounded by apostrophe
		String regex = "\'([A-Z0-9]){32}\'";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(responseText);
		while (matcher.find()) {
			String temp = matcher.group();
			temp = temp.replace("\'", "");
			result.add(temp);
		}
		return result;
	}

	static private String readTextFile(File aFile) {
		StringBuilder contents = new StringBuilder();

		try {
			//use buffering, reading one line at a time
			//FileReader always assumes default encoding is OK!
			BufferedReader input =  new BufferedReader(new FileReader(aFile));
			try {
				String line = null; //not declared within while loop
				/*
				 * readLine is a bit quirky :
				 * it returns the content of a line MINUS the newline.
				 * it returns null only for the END of the stream.
				 * it returns an empty String if two newlines appear in a row.
				 */
				while (( line = input.readLine()) != null){
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
				}
			}
			finally {
				input.close();
			}
		}
		catch (IOException ex){
			System.out.println(ex.toString());
		}

		return contents.toString();
	}

	public static void writeTextFile(String text, String filePath) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
			out.write(text);
			out.close();
		}
		catch (IOException ex)
		{
			System.out.println(ex.toString());
		}
	}

}
