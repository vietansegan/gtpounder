package main;

import java.io.File;
import java.util.ArrayList;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import util.CLIUtils;
import util.IOUtils;
import util.govtrack.GTDebate;
import util.govtrack.GTProcessor;

/**
 *
 * @author vietan
 */
public class Processor {

    public static final String SENATOR_FILE = "s01112nw.txt";
    public static final String REPRESENTATIVE_FILE = "h01112nw.txt";
    public static final String NOMINATE_SCORE_FILE = "HANDSL01111E20_BSSE.txt";
    public static final String POLICY_AGENDA_CODEBOOK_FILE = "policy_agenda_codebook.txt";
    public static final String CONGRESSIONAL_BILL_PROJECT_TOPIC_FILE = "bills93-111.csv";
    private static CommandLineParser parser;
    private static Options options;
    private static CommandLine cmd;

    public static void main(String[] args) {
        try {
            // create the command line parser
            parser = new BasicParser();

            // create the Options
            options = new Options();

            options.addOption(OptionBuilder.withLongOpt("folder")
                    .withDescription("Folder to store downloaded data")
                    .hasArg()
                    .withArgName("")
                    .create());

            options.addOption(OptionBuilder.withLongOpt("congress")
                    .withDescription("Congress number")
                    .hasArg()
                    .withArgName("")
                    .create());

            options.addOption(OptionBuilder.withLongOpt("processed-folder")
                    .withDescription("Processed folder")
                    .hasArg()
                    .withArgName("")
                    .create());

            options.addOption(OptionBuilder.withLongOpt("addinfo-folder")
                    .withDescription("Folder containing additional files")
                    .hasArg()
                    .withArgName("")
                    .create());

            options.addOption("help", false, "Help");

            cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                CLIUtils.printHelp("java -cp 'dist/gtcrawler.jar' main.Processor -help", options);
                return;
            }

            process();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void process() throws Exception {
        String folder = CLIUtils.getStringArgument(cmd, "folder", "L:/Dropbox/Datasets/govtrack/");
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 109);
        String processedFolder = CLIUtils.getStringArgument(cmd, "processed-folder", "L:/Dropbox/Datasets/govtrack/109/processed");
        IOUtils.createFolder(processedFolder);
        
        GTProcessor proc = new GTProcessor(folder, congressNo);

        proc.processDebates();
        proc.processBills();
        proc.processRolls();
        proc.processLegislators();
        String addinfoFolder = CLIUtils.getStringArgument(cmd, "addinfo-folder", "L:/Dropbox/DynamicHierarchicalModel/data/convote/addinfo");
        File senFile = new File(addinfoFolder, SENATOR_FILE);
        File repFile = new File(addinfoFolder, REPRESENTATIVE_FILE);
        File nominateFile = new File(addinfoFolder, NOMINATE_SCORE_FILE);

        proc.getMissingICPSRIDs(repFile.getAbsolutePath(), senFile.getAbsolutePath());
        proc.getNOMINATEScores(nominateFile.getAbsolutePath());

        File policyAgendaCodebookFile = new File(addinfoFolder, POLICY_AGENDA_CODEBOOK_FILE);
        proc.loadPolicyAgendaCodebook(policyAgendaCodebookFile.getAbsolutePath());

        File congressionalBillsProjectTopicFile = new File(addinfoFolder, CONGRESSIONAL_BILL_PROJECT_TOPIC_FILE);
        proc.loadCongressinalBillsProjectTopicLabels(congressionalBillsProjectTopicFile.getAbsolutePath());

        ArrayList<GTDebate> selectedDebates = proc.selectDebates();
        
        proc.outputLegislators(new File(processedFolder, "legislators.txt").getAbsolutePath());
        proc.outputSelectedDebate(new File(processedFolder, "filter").getAbsolutePath(), selectedDebates);
        proc.outputBills(new File(processedFolder, "bills.txt").getAbsolutePath());
        proc.outputBillSubjects(new File(processedFolder, "bills-subjects.txt").getAbsolutePath());
    }
}
