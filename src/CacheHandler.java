import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class CacheHandler {
    static File oldCache = new File(System.getProperty("user.home") + "/.onyx/");
    static File newCache = new File(System.getProperty("user.home") + "/.fury/");
    static File oldClient = new File(System.getProperty("user.home") + "/.fury/client.jar");

    public static void handleOldCache() {
        if (!newCache.exists())
            newCache.mkdir();
        if (oldCache.exists() && oldCache.isDirectory())
            move(oldCache, newCache);
        if(oldCache.exists())
            cleanUp(oldCache);
        if(oldClient.exists())
            oldClient.delete();
    }

    private static void cleanUp(File directory) {
        if(directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    cleanUp(file);
                    file.delete();
                }
            }
            directory.delete();
        }
    }

    private static boolean move(File sourceFile, File destFile) {
        System.out.println("Move: " + sourceFile + " to  "+ destFile);
        if(!destFile.exists())
            if(destFile.isFile())
                destFile.getParentFile().mkdirs();
            else
                destFile.mkdirs();
        if (sourceFile.isDirectory()) {
            for (File file : sourceFile.listFiles()) {
                move(file, new File(file.getPath().replace(".onyx", ".fury")));
            }
        } else {
            try {
                Files.move(Paths.get(sourceFile.getPath()), Paths.get(destFile.getPath()), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }
}
