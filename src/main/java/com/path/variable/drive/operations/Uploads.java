package com.path.variable.drive.operations;

import static java.net.URLConnection.getFileNameMap;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

public class Uploads {

    private Uploads() {}

    public static File uploadFile(String googleFolderIdParent, java.io.File file) throws IOException {

        var inputStreamContent = new FileContent(getMimeTypeByFilename(file.getName()), file);

        var fileMetadata = new File();
        fileMetadata.setName(file.getName());

        var parents = Collections.singletonList(googleFolderIdParent);
        fileMetadata.setParents(parents);

        Drive driveService = GoogleDriveUtils.getDriveService();
        var created =  driveService.files()
                                        .create(fileMetadata, inputStreamContent)
                                        .setFields("id, webContentLink, webViewLink, parents")
                                        .execute();

        long start = System.currentTimeMillis();
        while (!isFileUploaded(created.getId()) && (System.currentTimeMillis() - start < 60000 )) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // silent catch
            }
        }

        return created;
    }

    public static String findFolderId(String folderName) throws IOException {
        String query = "name contains '%s' and mimeType = 'application/vnd.google-apps.folder'";
        return GoogleDriveUtils.getDriveService().files()
                               .list()
                               .setQ(String.format(query, folderName))
                               .setPageSize(1)
                               .execute()
                               .getFiles()
                               .get(0)
                               .getId();
    }

    public static boolean isFileUploaded(String fileId) throws IOException {
        return GoogleDriveUtils.getDriveService()
                               .files()
                               .list()
                               .setOrderBy("modifiedTime desc")
                               .setPageSize(5)
                               .execute()
                               .getFiles()
                               .stream()
                               .anyMatch(f -> f.getId().equals(fileId));
    }

    public static boolean acquireLock(java.io.File file) {
        try(FileChannel ch = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
            FileLock lock = ch.tryLock()){
            return lock != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static String getMimeTypeByFilename(String fileName) {
        return getFileNameMap().getContentTypeFor(fileName);
    }
}
