package java.lang;

import java.util.Map;
import java.util.LinkedHashMap;

public class Package{
	
	static Map<String,Package> packageByName=new LinkedHashMap<>();
	
	static Package getPackage(String name){
		return packageByName.computeIfAbsent(name, Package::new);
	}
	private final String name;

	public Package(String name) {
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
}