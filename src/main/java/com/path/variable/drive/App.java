package com.path.variable.drive;

import com.path.variable.commons.properties.Configuration;
import com.path.variable.commons.slack.SlackHook;
import com.path.variable.drive.operations.Uploads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static com.path.variable.commons.properties.Configuration.getConfiguration;
import static com.path.variable.drive.operations.GoogleDriveUtils.clearDriveService;
import static com.path.variable.drive.operations.Uploads.findFolderId;
import static com.path.variable.drive.operations.Uploads.uploadFile;
import static java.lang.Thread.sleep;

public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private static final File TARGET_DIR = new File(getConfiguration().getString("upload.dir"));

    private static final SlackHook hook = new SlackHook(getConfiguration().getString("slack.hook"));

    public static void main(String[] args) throws IOException, InterruptedException {
        String folderId = findFolderId(Configuration.getConfiguration().getString("drive.folder"));
        int retry = 0;
        var maxRetry = getConfiguration().getInteger("max.retry", 2);
        while (true) {
            Optional.ofNullable(TARGET_DIR.listFiles())
                    .stream()
                    .flatMap(Arrays::stream)
                    .filter(Uploads::acquireLock)
                    .findFirst()
                    .ifPresent(f -> {
                        try {
                            uploadWithRetry(retry, maxRetry, f, folderId);
                        } catch (InterruptedException e) {
                            //silent catch
                        }
                    });
            sleep(1000);
        }
    }

    private static void uploadWithRetry(int retry, int maxRetry, File f, String folderId) throws InterruptedException {
        try {
            var file = uploadFile(folderId, f);
            LOG.info("Uploaded file {}", f.getName());
            hook.sendPlainText(String.format("File %s can be accessed at google drive.\n " +
                    "File location: https://drive.google.com/drive/u/1/folders/%s/%s",f.getName(), folderId, file.getId()));
            f.delete();
        } catch (IOException e) {
            LOG.error("There was an error while uploading the file to drive. {} retries left", maxRetry - retry, e);
            if (retry > maxRetry) {
                LOG.error("Reached maximum number of retries. Exiting program!");
                System.exit(1);
            }
            retry++;
            clearDriveService();
            sleep(1000);
            uploadWithRetry(retry, maxRetry, f, folderId);
        }
    }
}
