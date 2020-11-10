package cc.alcina.framework.servlet.google;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.BatchUpdate;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class SheetAccessor {
	private JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	SheetAccess sheetAccess;

	private Sheets service;

	private Spreadsheet spreadsheet;

	public void bold(GridRange range) throws IOException {
		ensureSheets();
		ensureSpreadsheet();
		CellFormat cellFormat = new CellFormat();
		TextFormat textFormat = new TextFormat();
		textFormat.setBold(true);
		cellFormat.setTextFormat(textFormat);
		BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
		CellData cellData = new CellData();
		cellData.setUserEnteredFormat(cellFormat);
		RepeatCellRequest repeat = new RepeatCellRequest();
		repeat.setCell(cellData);
		repeat.setRange(range);
		String field = "userEnteredFormat.textFormat";
		repeat.setFields(field);
		batchUpdateSpreadsheetRequest.setRequests(
				Collections.singletonList(new Request().setRepeatCell(repeat)));
		BatchUpdate update = service.spreadsheets().batchUpdate(
				sheetAccess.spreadSheetId, batchUpdateSpreadsheetRequest);
		BatchUpdateSpreadsheetResponse response = update.execute();
	}

	public void ensureSpreadsheet() {
		if (spreadsheet != null) {
			return;
		}
		ensureSheets();
		try {
			spreadsheet = service.spreadsheets().get(sheetAccess.spreadSheetId)
					.setIncludeGridData(true).execute();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public List<RowData> getRowData(int sheetIdx) {
		ensureSpreadsheet();
		List<GridData> data = spreadsheet.getSheets().get(sheetIdx).getData();
		return data.get(0).getRowData();
	}

	public Sheet getSheet(int sheetIdx) {
		ensureSpreadsheet();
		return spreadsheet.getSheets().get(sheetIdx);
	}

	public Sheet getSheet(String name) {
		ensureSpreadsheet();
		return spreadsheet.getSheets().stream()
				.filter(sheet -> sheet.getProperties().getTitle().equals(name))
				.findFirst().get();
	}

	public void update(BatchUpdateValuesRequest batchUpdate) {
		try {
			service.spreadsheets().values()
					.batchUpdate(sheetAccess.spreadSheetId, batchUpdate)
					.execute();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void update(String range, List<List<Object>> values)
			throws IOException {
		ensureSheets();
		ValueRange body = new ValueRange().setValues(values);
		UpdateValuesResponse result = service.spreadsheets().values()
				.update(sheetAccess.spreadSheetId, range, body)
				.setValueInputOption("RAW").execute();
	}

	public SheetAccessor withSheetAccess(SheetAccess sheetAccess) {
		this.sheetAccess = sheetAccess;
		return this;
	}

	private Credential getCredentials(NetHttpTransport transport)
			throws IOException {
		// Load client secrets.
		InputStream in = new FileInputStream(sheetAccess.credentialsPath);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets
				.load(JSON_FACTORY, new InputStreamReader(in));
		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				transport, JSON_FACTORY, clientSecrets, sheetAccess.scopes)
						.setDataStoreFactory(
								new FileDataStoreFactory(new java.io.File(
										sheetAccess.credentialsStorageLocalPath)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder()
				.setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver)
				.authorize("user");
	}

	void ensureSheets() {
		if (service != null) {
			return;
		}
		try {
			final NetHttpTransport httpTransport = GoogleNetHttpTransport
					.newTrustedTransport();
			service = new Sheets.Builder(httpTransport, JSON_FACTORY,
					getCredentials(httpTransport))
							.setApplicationName(sheetAccess.applicationName)
							.build();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static class SheetAccess {
		private List<String> scopes;

		private String applicationName;

		private String spreadSheetId;

		private String credentialsPath;

		private String credentialsStorageLocalPath;

		private String sheetName;

		public String getSheetName() {
			return this.sheetName;
		}

		public SheetAccess withApplicationName(String applicationName) {
			this.applicationName = applicationName;
			return this;
		}

		public SheetAccess withCredentialsPath(String credentialsPath) {
			this.credentialsPath = credentialsPath;
			return this;
		}

		public SheetAccess withCredentialsStorageLocalPath(
				String credentialsStorageLocalPath) {
			this.credentialsStorageLocalPath = credentialsStorageLocalPath;
			return this;
		}

		public SheetAccess withScopes(List<String> scopes) {
			this.scopes = scopes;
			return this;
		}

		public SheetAccess withSheetName(String sheetName) {
			this.sheetName = sheetName;
			return this;
		}

		public SheetAccess withSpreadSheetId(String spreadSheetId) {
			this.spreadSheetId = spreadSheetId;
			return this;
		}
	}
}
