package cc.alcina.framework.servlet.google;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.api.services.sheets.v4.SheetsScopes;

import cc.alcina.framework.servlet.google.SheetAccessor.SheetAccess;

public class SheetAccessorConfiguration {
	public String name;

	public String spreadSheetId;

	public String credentialsPath;

	public String credentialsStorageLocalPath;

	public String sheetName;

	public SheetAccess asSheetAccess() {
		return new SheetAccess().withApplicationName(name)
				.withCredentialsPath(credentialsPath)
				.withCredentialsStorageLocalPath(credentialsStorageLocalPath)
				.withSpreadSheetId(spreadSheetId).withSheetName(sheetName)
				.withScopes(Arrays.asList(SheetsScopes.SPREADSHEETS).stream()
						.collect(Collectors.toList()));
	}
}
