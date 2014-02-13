package main;

import core.AbstractRunner;
import java.io.File;
import java.net.URL;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import util.CLIUtils;
import util.IOUtils;
import util.freedomworks.FWDownloader;
import util.govtrack.GTDownloader;
import util.govtrack.GTProcessor;

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
            addOption("type", "Download type");
            addOption("year", "Year");
            addOption("congress-type", "Congress type: house or senate");

            options.addOption("help", false, "Help");

            cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                CLIUtils.printHelp("java -cp 'dist/gtcrawler.jar' main.Downloader -help", options);
                return;
            }

            String type = CLIUtils.getStringArgument(cmd, "type", "fw-score");
            
            if (type.equals("all")) {
                download();
            } else if (type.equals("external")) {
                downloadExternalResources();
            } else if (type.equals("bill-text")) {
                downloadBillTexts();
            } else if (type.equals("bill-html")) {
                downloadBillTextInHtmls();
            } else if (type.equals("fw-score")) {
                downloadFreedomWorksScores();
            } else {
                throw new RuntimeException("Download type " + type + " is not supported");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private static void downloadFreedomWorksScores() throws Exception {
        System.out.println("Start downloading FreedomWorks scores ...");
        String congressType = CLIUtils.getStringArgument(cmd, "congress-type", "house");
        int year = CLIUtils.getIntegerArgument(cmd, "year", 2012);
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 112);
        String folder = cmd.getOptionValue("folder");
        
        File congressFolder = new File(folder, Integer.toString(congressNo));
        IOUtils.createFolder(congressFolder);
        FWDownloader downloader = new FWDownloader(congressType, year);
        downloader.downloadFreedomWorksScores();
        downloader.output(congressFolder);
    }

    private static void downloadBillTextInHtmls() throws Exception {
        System.out.println("Start downloading bill texts ...");
        String folder = cmd.getOptionValue("folder");
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 112);

        GTProcessor proc = new GTProcessor(folder, congressNo);
        proc.processDebates();
        proc.processBills();

        GTDownloader gtDownloader = new GTDownloader(folder, congressNo);
        gtDownloader.downloadBillHtmls(proc.getBills());
    }

    private static void downloadBillTexts() throws Exception {
        System.out.println("Start downloading bill texts ...");
        String folder = cmd.getOptionValue("folder");
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 112);

        GTProcessor proc = new GTProcessor(folder, congressNo);
        proc.processDebates();
        proc.processBills();

        GTDownloader gtDownloader = new GTDownloader(folder, congressNo);
        gtDownloader.downloadBillTexts(proc.getBills());
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
