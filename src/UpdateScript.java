import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Greg on 29/01/2017.
 */
public class UpdateScript {

    private static String UPDATER_URL = "http://furyps.com/play/client/updater.php";
    private String fileName;
    private String localPath;
    private long localModifiedTime, localCreateTime;
    private List<JsonElement> response = null;
    private boolean fileExists;

    public UpdateScript(String name, String localPath) {
        this.fileName = name;
        this.localPath = localPath;
        load();
    }

    public void load() {
        checkLocalFile();
        loadLocalFileDate();
        try {
            response = new ArrayList<>();
            String uri = UPDATER_URL + "?file=" + fileName + "&mod=" + localModifiedTime + "&create=" + localCreateTime;
            URL url = new URL(uri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(10000);

            connection.getResponseCode();
            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }

            if (connection.getResponseCode() == 200) {
                JsonParser jp = new JsonParser();
                JsonElement root = jp.parse(new InputStreamReader(stream));
                JsonObject rootobj = root.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : rootobj.entrySet()) {
                    response.add(entry.getValue());
                }
            } else {
                //Handle error
                System.out.println("Error checking file: " + fileName + "; website response code: " + connection.getResponseCode());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkLocalFile() {
        fileExists = new File(localPath).exists();
    }

    public boolean isSuccessful() {
        return response != null && !response.isEmpty() ? response.get(0).getAsBoolean() : false;
    }

    public boolean requiresUpdate() {
        return isSuccessful() ? response.get(1).getAsBoolean() : fileExists ? false : true;
    }

    public String getRequestDir() {
        return isSuccessful() ? response.get(2).getAsString() : "null";
    }

    public long getLocalDate() {
        return isSuccessful() ? response.get(3).getAsLong() : 0;
    }

    public long getHostDate() {
        return isSuccessful() ? response.get(4).getAsLong() : 0;
    }

    private void loadLocalFileDate() {
        localModifiedTime = (fileExists) ? getModifiedTime(localPath) : 0;
        localCreateTime = (fileExists) ? getCreationTime(localPath) : 0;
    }

    private static long getModifiedTime(String dir) {
        Path path = Paths.get(dir);
        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(path, BasicFileAttributes.class);
            return attr.lastModifiedTime().toMillis() / 1000;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static long getCreationTime(String dir) {
        Path path = Paths.get(dir);
        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(path, BasicFileAttributes.class);
            return attr.creationTime().toMillis() / 1000;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
