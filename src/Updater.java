import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.FileChannel;

public class Updater {

    public static String DIRECTORY = System.getProperty("user.home") + "/.fury/";
    public static String SITE_URL = "http://furyps.com/play/client/";

    private static PrintStream out;
    //TODO change to update list
    private static String[] fileLocations = new String[] {
            "fury_client.jar",
            "cache/main_file_sprites.dat", "cache/main_file_sprites.idx",
            "cache/images/background.png", "cache/images/logo.png", "cache/images/icons/16.png",
            "cache/images/icons/32.png", "cache/images/icons/48.png"
    };

    private static File[] files;
    private static File cache = new File(DIRECTORY + "/cache/");
    public static boolean updateLoop = true;

    private static JFrame download = new JFrame(), loading = new JFrame();
    public static JProgressBar progressBar = new JProgressBar(0, 100);
    private static FileGrabber downloader = new FileGrabber();
    public static JLabel downloadingText = new JLabel("Loading...");

    private static void createDownloadGUI() {
        setGUIDefaults(download);

        //Not the proper way to do alignment..
        JPanel text = new JPanel();
        text.add(downloadingText);
        download.add(text, BorderLayout.PAGE_START);

        JPanel bar = new JPanel();
        bar.add(progressBar);
        download.add(bar, BorderLayout.CENTER);

        download.pack();
    }

    private static void setGUIDefaults(JFrame frame) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(350, 105));
        frame.setSize(new Dimension(350, 105));
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
    }

    private static void createLoadingGUI() {
        setGUIDefaults(loading);

        JPanel card3 = new JPanel();
        card3.add(new JLabel("<html><br>Checking for updates...</html>"));
        loading.add(card3, BorderLayout.CENTER);

        loading.pack();
        loading.setVisible(true);
    }

    private static void createGUI() {

        createDownloadGUI();

        createLoadingGUI();

    }

    private static void loadClient() {
        System.out.println("Loading Client...");
        try {ProcessBuilder pb = new ProcessBuilder("java", "-jar", DIRECTORY + "fury_client.jar");
            pb.directory(new File(DIRECTORY));
            Process p = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*try {
            System.out.println(System.getProperty("java.home") + "/bin/java.exe -Xmx512m -jar " + DIRECTORY + "client.jar");
            Runtime.getRuntime().exec(System.getProperty("java.home") + "/bin/java.exe -Xmx512m -jar " + DIRECTORY + "client.jar").waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        closeLogger();
        System.exit(0);
        Runtime.getRuntime().halt(0);
    }

    private static void loadUpdateFiles() {
        files = new File[fileLocations.length];

        for (int i = 0; i < fileLocations.length; i++) {
            File f = new File(DIRECTORY + fileLocations[i]);
            files[i] = f;
        }
    }

    public static void copyByTransfer(File sFile, File tFile) throws IOException {
        FileInputStream fInput = new FileInputStream(sFile);
        FileOutputStream fOutput = new FileOutputStream(tFile);
        FileChannel fReadChannel = fInput.getChannel();
        FileChannel fWriteChannel = fOutput.getChannel();

        fReadChannel.transferTo(0, fReadChannel.size(), fWriteChannel);

        fReadChannel.close();
        fWriteChannel.close();
        fInput.close();
        fOutput.close();
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        System.out.println("Starting...");

        //Load GUI's
        EventQueue.invokeAndWait(() -> createGUI());

        System.out.println("Setting up logger");
        loadLogger();

        System.out.println("Checking for old directory");
        CacheHandler.handleOldCache();

        System.out.println("Directory loaded: ");
        System.out.println(DIRECTORY);


        System.out.println("Checking cache exists...");
        boolean exists = cacheExists();

        if (!exists) {
            System.out.println("Creating cache location.");
            markCacheIfNonExistent();
        }

        File images = new File(DIRECTORY + "cache/images/");
        File background = new File(DIRECTORY + "cache/images/loading/");
        if(!images.exists() || !background.exists()) {
            if(!images.exists())
                images.mkdirs();

            images = new File(DIRECTORY + "/cache/images.zip");
            System.out.println("Downloading images...");
            downloadFile(images);
        }

        images = new File(DIRECTORY + "cache/images/icons/");
        if(!images.exists())
            images.mkdirs();

        System.out.println("Loading update files...");
        loadUpdateFiles();

        System.out.println("Checking files...");

        int timeout = 0;
        while (updateLoop) {
            int existingFiles = getExistingFiles();
            System.out.println("Files checked: " + files.length + " exist: " + existingFiles);

            if (existingFiles == files.length) {
                System.out.println("Checking existing file versions...");
                checkFileVersions();
            } else if (existingFiles != 0) {
                System.out.println("Checking for missing files");
                downloadMissingFiles();
            } else {
                System.out.println("Downloading cache files...");
                downloadCache();
            }

            //Just a precaution
            timeout++;
            if (timeout > 10) {
                System.err.println("Timeout: Failed to download file.");
                break;
            }
        }
        download.dispose();
        loadClient();
        System.exit(0);
    }

    private static void loadLogger() {
        File log = new File(DIRECTORY + "logs/launcher.log");
        log.getParentFile().mkdirs();
        try {
            out = new PrintStream(new FileOutputStream(log));
            System.setOut(out);
            System.setErr(out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void closeLogger() {
        if(out != null) {
            out.flush();
            out.close();
            System.setOut(null);
            System.setErr(null);
        }
    }

    public static int getExistingFiles() {
        int existingFiles = 0;
        for (File f : files)
            if (f.exists())
                existingFiles++;
        return existingFiles;
    }

    private static void downloadMissingFiles() {
        for (File file : files) {
            if (!file.exists()) {
                downloadFile(file);
            }
        }
    }

    private static void downloadCache() {
        for (File file : files)
            downloadFile(file);

        updateLoop = false;
    }

    private static void updateFile(File file) {
        downloadFile(file);
    }

    private static void downloadFile(File file) {
        if (!download.isVisible()) {
            loading.dispose();
            download.setVisible(true);
        }
        System.out.println("Downloading file: " + file.getName() + " override: " + file.exists());
        downloader.downloadFile(file);
    }

    private static void checkFileVersions() {
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String loc = fileLocations[i];
            UpdateScript script = new UpdateScript(loc, file.getPath());
            System.out.println("Checking; " + file.getName());
            if (script.isSuccessful()) {
                if (script.requiresUpdate())
                    updateFile(file);
            } else {
                System.out.println("Error checking update script; " + loc + "; " + file.getPath());
            }
        }
        updateLoop = false;
    }

    private static boolean cacheExists() {
        return cache.exists();
    }

    private static void markCacheIfNonExistent() {
        if (!cache.isDirectory())
            cache.mkdirs();
    }
}