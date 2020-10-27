package cc.alcina.extras.webdriver.google;

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
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

public class GoogleDriveAccessor {

    private JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private DriveAccess driveAccess;

    private Drive service;

    public File ensureFolder(String folderPath) throws IOException {
        String parentId = driveAccess.folderId;
        File file = null;
        ensureDrive();
        String pageToken = null;
        for (String part : folderPath.split("/")) {
            FileList result = service.files().list().setQ(Ax.format("mimeType = 'application/vnd.google-apps.folder' and '%s' in parents", parentId)).setSpaces("drive").setFields("nextPageToken, files(id, name)").setPageToken(pageToken).setPageSize(1).execute();
            if (result.getFiles().size() == 1) {
                file = result.getFiles().get(0);
            } else {
                File fileMetadata = new File();
                fileMetadata.setName(part);
                fileMetadata.setMimeType("application/vnd.google-apps.folder");
                fileMetadata.setParents(Collections.singletonList(parentId));
                file = service.files().create(fileMetadata).setFields("id").execute();
                Ax.out("Created folder: %s", part);
            }
            parentId = file.getId();
        }
        return file;
    }

    public File upload(File folder, byte[] bytes, String name) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setParents(Collections.singletonList(folder.getId()));
        String type = null;
        switch(name.replaceFirst(".+\\.(.+)", "$1")) {
            case "html":
                type = "text/html";
                break;
            case "png":
                type = "image/png";
                break;
            default:
                throw new UnsupportedOperationException();
        }
        ByteArrayContent mediaContent = new ByteArrayContent(type, bytes);
        File file = service.files().create(fileMetadata, mediaContent).setFields("id").execute();
        Ax.out("Uploaded file: %s", name);
        return file;
    }

    public GoogleDriveAccessor withDriveAccess(DriveAccess sheetAccess) {
        this.driveAccess = sheetAccess;
        return this;
    }

    private Credential getCredentials(NetHttpTransport transport) throws IOException {
        // Load client secrets.
        InputStream in = new FileInputStream(driveAccess.credentialsPath);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(transport, JSON_FACTORY, clientSecrets, driveAccess.scopes).setDataStoreFactory(new FileDataStoreFactory(new java.io.File(driveAccess.credentialsStorageLocalPath))).setAccessType("offline").build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    void ensureDrive() {
        if (service != null) {
            return;
        }
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            service = new Drive.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport)).setApplicationName(driveAccess.applicationName).build();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public static class DriveAccess {

        private List<String> scopes;

        private String applicationName;

        private String folderId;

        private String credentialsPath;

        private String credentialsStorageLocalPath;

        public DriveAccess withApplicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public DriveAccess withCredentialsPath(String credentialsPath) {
            this.credentialsPath = credentialsPath;
            return this;
        }

        public DriveAccess withCredentialsStorageLocalPath(String credentialsStorageLocalPath) {
            this.credentialsStorageLocalPath = credentialsStorageLocalPath;
            return this;
        }

        public DriveAccess withFolderId(String folderId) {
            this.folderId = folderId;
            return this;
        }

        public DriveAccess withScopes(List<String> scopes) {
            this.scopes = scopes;
            return this;
        }
    }
}
