package gopher.io;


import java.io.File;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * This is used to figure out where VPVGui will store the viewpoint files. For instance, with linux
 * this would be /home/username/.gopher/...
 */
public class Platform {

    /**
     * Get path to directory where HRMD-gui stores global settings.
     * The path depends on underlying operating system. Linux, Windows and OSX
     * currently supported.
     * @return File to directory
     */
    public static File getGopherDir() {
        CurrentPlatform platform = figureOutPlatform();

        File linuxPath = new File(System.getProperty("user.home") + File.separator + ".gopher");
        File windowsPath = new File(System.getProperty("user.home") + File.separator + "gopher");
        File osxPath = new File(System.getProperty("user.home") + File.separator + ".gopher");

        switch (platform) {
            case LINUX: return linuxPath;
            case WINDOWS: return windowsPath;
            case OSX: return osxPath;
            case UNKNOWN: return null;
            default:
                Alert a = new Alert(AlertType.ERROR);
                a.setTitle("Find GUI config dir");
                a.setHeaderText(null);
                a.setContentText(String.format("Unrecognized platform. %s", platform.toString()));
                a.showAndWait();
                return null;
        }
    }

    /**
     * Get the absolute path to the viewpoint file, which is a serialized Java file (suffix {@code .ser}).
     * @param basename The plain viewpoint name, e.g., human37cd4
     * @return the absolute path,e.g., /home/user/data/immunology/human37cd4.ser
     */
    public static String getAbsoluteProjectPath(String basename) {
        File dir = getGopherDir();
        return new String(dir + File.separator + basename + ".ser");
    }

    /**
     * Get the absolute path to the log file.
     * @return the absolute path,e.g., /home/user/.gopher/gopher.log
     */
    public static String getAbsoluteLogPath() {
        File dir = getGopherDir();
        return new String(dir + File.separator +  "gopher.log");
    }

    /* Based on this post: http://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/ */
    private static CurrentPlatform figureOutPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return CurrentPlatform.LINUX;
        } else if (osName.contains("win")) {
            return CurrentPlatform.WINDOWS;
        } else if (osName.contains("mac")) {
            return CurrentPlatform.OSX;
        } else {
            return CurrentPlatform.UNKNOWN;
        }
    }


    private enum CurrentPlatform {
        LINUX("Linux"),
        WINDOWS("Windows"),
        OSX("Os X"),
        UNKNOWN("Unknown");

        private String name;

        CurrentPlatform(String n) {this.name = n; }

        @Override
        public String toString() { return this.name; }
    }

}
