import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Greg on 29/01/2017.
 */
public class FileGrabber {

    public void downloadFile(File file) {
        try {
            downloadFromUrl(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getSitePath(File file) {
        return Updater.SITE_URL + file.getPath().replaceAll("\\\\", "/").substring(Updater.DIRECTORY.length(), file.getPath().length()) + "?" + (System.currentTimeMillis() / 1000L);
    }

    private void downloadFromUrl(File file) throws IOException {
        //Reset
        Updater.progressBar.setValue(0);
        String uri = getSitePath(file);
        String localFilename = file.getPath();
        InputStream is = null;
        FileOutputStream fos = null;

        try {
            //Connect
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            //Response Codes
            if(conn.getResponseCode() != 200)
                throw new IOException("Webserver response: " + conn.getResponseCode() + "; Error downloading file: " + uri + " to location: " + localFilename);

            //Initiating
            int fileSize = conn.getContentLength();
            is = conn.getInputStream();
            fos = new FileOutputStream(localFilename);

            //Set text
            Updater.downloadingText.setText("Downloading " + file.getName() + "...");

            //Download
            byte[] buffer = new byte[4096];
            int len;
            int bytesDownloaded = 0;
            while ((len = is.read(buffer)) > 0) {
                bytesDownloaded += len;

                fos.write(buffer, 0, len);

                Updater.progressBar.setIndeterminate(false);
                Updater.progressBar.setValue((bytesDownloaded / fileSize) * 100);
            }
            is.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        }
    }
}
