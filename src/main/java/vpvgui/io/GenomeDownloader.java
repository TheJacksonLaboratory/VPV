package vpvgui.io;

import javafx.scene.control.ProgressIndicator;
import javafx.stage.DirectoryChooser;
import org.apache.log4j.Logger;
import vpvgui.exception.DownloadFileNotFoundException;
import vpvgui.gui.ErrorWindow;
import vpvgui.model.DataSource;


import java.io.File;

public class GenomeDownloader {
    static Logger logger = Logger.getLogger(GenomeDownloader.class.getName());
    /** genome build symbol, e.g., hg19, mm10. */
    private String genomebuild=null;
    /** URL to download the genome from UCSC. */
    private String url=null;

    private String currentStatus="uninitialized";



    public GenomeDownloader(String build) {
        this.genomebuild=build;
        logger.debug("Constructor of GenomeDownloader, build="+build);
        try {
            this.url=getGenomeURL(build);
            logger.debug("Setting url to "+url);
        } catch (DownloadFileNotFoundException e){
            logger.error(e,e);
        }
    }
    /** @return a simple message summarizing how much work has been completed. */
    public String getStatus() {
        return currentStatus;
    }


    public void setDownloadDirectoryAndDownloadIfNeeded(String directory, String basename, ProgressIndicator pi) {
        Operation op = new GenomeDownloadOperation(directory);
        Downloader downloadTask = new Downloader(directory, this.url, basename, pi);
        if (downloadTask.needToDownload(op)) {
            Thread th = new Thread(downloadTask);
            th.setDaemon(true);
            th.start();
        } else {
            currentStatus=String.format("Genome %s was already downloaded",this.genomebuild);
        }
    }


    /**
     * This method uses {@link DataSource} as a source for collections
     * of paths and names that represent the sets of data we will need
     * to analyze any one genome.
     * @param gb The genome build
     */
    public String getGenomeURL(String gb) throws DownloadFileNotFoundException {
        if (gb.equals("hg19")) {
            return  DataSource.createUCSChg19().getGenomeURL();
        } else if (gb.equals("hg38")) {
            return DataSource.createUCSChg38().getGenomeURL();
        } else if (gb.equals("mm9")) {
            return DataSource.createUCSCmm9().getGenomeURL();
        } else if (gb.equals("mm10")) {
            return  DataSource.createUCSCmm10().getGenomeURL();
        } else {
            throw new DownloadFileNotFoundException(String.format("Attempt to get URL for unknown genome build: %s.", gb));
        }
    }

}