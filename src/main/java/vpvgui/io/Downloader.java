package vpvgui.io;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import vpvgui.gui.ErrorWindow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Downloader extends Task<Void> {

    /** An error message, if an error occured */
    protected String errorMessage=null;

    /**
     * This is the absolute path to the place (directory) where the downloaded file will be
     * saved in the local filesystem.
     */
    private File localDir=null;

    /**
     * This is the full local path of the file we will download. It should be set to be identical
     * to {@link #localDir} except for the final file base name.
     */
    private File localFilePath=null;


    /** A reference to a ProgressIndicator that must have be
     * initialized in the GUI and not within this class.
     */
    private ProgressIndicator progress=null;

    /** This is the URL of the file we want to download */
    protected String urlstring=null;

    public Downloader(File path, String url, String basename) {
        this.localDir = path;
        this.urlstring=url;
        setLocalFilePath(basename);
        makeDirectoryIfNotExist();
    }

    public Downloader(String path, String url, String basename) {
        this(new File(path),url,basename);
    }

    public Downloader(String path, String url, String basename,  ProgressIndicator pi) {
        this(path,url,basename);
        this.progress = pi;
    }

    public Downloader(File path, String url, String basename,   ProgressIndicator pi) {
        this(path,url,basename);
        this.progress = pi;
    }


    protected File getLocalFilePath() { return  this.localFilePath; }

    protected void setLocalFilePath (String bname) {
        System.err.println("setLocalFilepath bname="+bname);
        this.localFilePath = new File(this.localDir + File.separator + bname);
        System.err.println("setLocalFilepath localFilePath="+localFilePath);
    }

   // public abstract boolean needToDownload();


    public boolean hasError() {
        return this.errorMessage != null;
    }

    public String getError() {
        return this.errorMessage;
    }

    /**
     * @param url Subclasses need to set this to the URL of the resource to be downloaded. Alternatively,
     * client code needs to set it.
     */
    public void setURL(String url) {
        this.urlstring=url;
    }

    /** This method will be used for each of the data sources to check
     * whether the requested file has already been downloaded.
     * @param operation A functor that implements the required data check.
     * @return
     */
    public boolean needToDownload(Operation operation){
        if (operation.execute())
            return true;
        else
            return false;
        }


    /**
     * This method downloads a file to the specified local file path. If the file already exists, it emits a warning
     * message and does nothing.
     */
    @Override
    protected Void call()  {
        if (progress!=null)
            progress.setProgress(1.000); /* show progress as 100% */
        System.out.println("[INFO] Downloading: \"" + urlstring + "\"");

        // The error handling can be improved with Java 7.
        String err = null;

        InputStream reader;
        FileOutputStream writer;

        int threshold = 0;
        int block = 250000;
        try {
            URL url = new URL(urlstring);
            URLConnection urlc = url.openConnection();
            reader = urlc.getInputStream();
            System.out.println("URL host : "+ url.getHost() + "\n reader available="+reader.available());
            System.out.println("1 localFilePath="+localFilePath);
            writer = new FileOutputStream(localFilePath);
            System.out.println("2 localFilePath="+localFilePath);
            byte[] buffer = new byte[153600];
            int totalBytesRead = 0;
            int bytesRead = 0;
            int size = urlc.getContentLength();
            if (progress!=null) { progress.setProgress(0.01); }
            System.out.println("size="+size);
            if (size >= 0)
                block = size /100;
            //System.err.println("0%       50%      100%");
            while ((bytesRead = reader.read(buffer)) > 0) {
                //System.out.println("bytesRead="+bytesRead + ", size="+size + ", threshold="+threshold +", totalBytesRead="+totalBytesRead + " gt?=+" + ( totalBytesRead > threshold));
                writer.write(buffer, 0, bytesRead);
                buffer = new byte[153600];
                totalBytesRead += bytesRead;
                if (size>0 && totalBytesRead > threshold) {
                    //System.err.print("=");
                    if (progress!=null) { progress.setProgress((double)totalBytesRead/size); }
                    threshold += block;
                }
            }
            //System.err.println();
            System.err.println("[INFO] Done. " + (new Integer(totalBytesRead).toString()) + "(" + size + ") bytes read.");
            writer.close();
        } catch (MalformedURLException e) {
            err = String.format("Could not interpret url: \"%s\"\n%s", urlstring, e.toString());
        } catch (IOException e) {
            progress.setProgress(0.00);
            ErrorWindow.display("Error downloading","um");
            err = String.format("IO Exception reading from URL: \"%s\"\n%s", urlstring, e.toString());
            System.err.println("err"+err);
            ErrorWindow.display("Error downloading",err);
        } catch (Exception e){
            err = e.getMessage();
        }
        if (err != null) {
            ErrorWindow.display("Error downloading",err);
            progress.setProgress(0.00);
            return null;

        }
        if (progress!=null) { progress.setProgress(1.000);/* show 100% completion */ }
        return null;
    }

    /**
     * This function creates a new directory to store the downloaded file. If the directory already exists, it
     *  does nothing.
     */
    protected void makeDirectoryIfNotExist() {
        System.out.println("localDir = "+ localDir);
        if (localDir==null) {
            return; // todo give user feedback
        }
        if (this.localDir.getParentFile().exists()) {
           return;// System.err.println(String.format("Cowardly refusing to create " + "directory \"%s\" since it already exists", this.localFilePath.getParent()));
        } else {
            this.localDir.mkdir();
        }
    }


}
