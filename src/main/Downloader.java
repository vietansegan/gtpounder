package main;

import core.AbstractRunner;
import java.io.File;
import java.net.URL;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import util.CLIUtils;
import util.IOUtils;
import util.govtrack.GTDownloader;

/**
 *
 * @author vietan
 */
public class Downloader extends AbstractRunner {
    
    public static void main(String[] args) {
        try {
            // create the command line parser
            parser = new BasicParser();

            // create the Options
            options = new Options();
            
            addOption("folder", "Folder to store downloaded data");
            addOption("congress", "Congress number");

            options.addOption("help", false, "Help");

            cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                CLIUtils.printHelp("java -cp 'dist/gtcrawler.jar' main.Downloader -help", options);
                return;
            }

            download();

//            downloadExternalResources();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void download() throws Exception {
        System.out.println("Start downloading ...");

        String folder = cmd.getOptionValue("folder");
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 112);

        GTDownloader gtDownloader = new GTDownloader(folder, congressNo);
        gtDownloader.downloadPeopleXML();
        gtDownloader.downloadCR();

//        gtDownloader.downloadIndexCrPerson(); // no longer valid

        gtDownloader.downloadRolls();

//        gtDownloader.downloadVoteAllIndex();
//        gtDownloader.downloadBillsIndex();
//        gtDownloader.downloadIndexCrBill(); // no longer valid

        gtDownloader.downloadBills();
    }

    private static void downloadExternalResources() throws Exception {
        System.out.println("Start downloading external resources ...");

        String folder = CLIUtils.getStringArgument(cmd, "folder", "L:/Dropbox/Datasets/govtrack/addinfo/");
        IOUtils.createFolder(folder);
        String voteviewUrl = "ftp://voteview.com";

        URL repFileUrl = new URL(new URL(voteviewUrl), "h01112nw.txt‎");
        File repFile = new File(folder, "h01112nw.txt‎");


        System.out.println("Downloading " + repFileUrl + " to " + repFile);
        FileUtils.copyURLToFile(repFileUrl, repFile);
    }
}
