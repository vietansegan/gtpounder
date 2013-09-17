package main;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import util.CLIUtils;
import util.IOUtils;
import util.govtrack.GTBill;
import util.govtrack.GTDebate;
import util.govtrack.GTLegislator;
import util.govtrack.GTProcessor;
import util.govtrack.GTRoll;
import util.govtrack.GTTurn;

/**
 *
 * @author vietan
 */
public class Processor {

    public static final String SENATOR_FILE = "s01112nw.txt";
    public static final String REPRESENTATIVE_FILE = "h01112nw.txt";
    public static final String NOMINATE_SCORE_FILE = "HANDSL01112D20_BSSE.txt";
    public static final String POLICY_AGENDA_CODEBOOK_FILE = "policy_agenda_codebook.txt";
    public static final String CONGRESSIONAL_BILL_PROJECT_TOPIC_FILE = "bills93-111.csv";
    private static CommandLineParser parser;
    private static Options options;
    private static CommandLine cmd;
    private static boolean verbose;

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

            options.addOption(OptionBuilder.withLongOpt("format-folder")
                    .withDescription("Format folder")
                    .hasArg()
                    .withArgName("")
                    .create());

            options.addOption(OptionBuilder.withLongOpt("mode")
                    .withDescription("Mode of processing")
                    .hasArg()
                    .withArgName("")
                    .create());

            options.addOption("help", false, "Help");
            options.addOption("v", false, "Verbose");

            cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                CLIUtils.printHelp("java -cp 'dist/gtpounder.jar' main.Processor -help", options);
                return;
            }

            String mode = CLIUtils.getStringArgument(cmd, "mode", "process");
            verbose = cmd.hasOption("v");

            if (mode.equals("process")) {
                process();
            } else if (mode.equals("format")) {
                format();
            } else if (mode.equals("extract-republicans")) {
                extractRepublicans();
            } else {
                throw new RuntimeException("Processing mode " + mode + " is not supported.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void process() throws Exception {
        if (verbose) {
            System.out.println("Processing ...");
        }

        String folder = cmd.getOptionValue("folder");
        String processedFolder = cmd.getOptionValue("processed-folder");
        String addinfoFolder = cmd.getOptionValue("addinfo-folder");
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 109); // default
        IOUtils.createFolder(processedFolder);

        GTProcessor proc = new GTProcessor(folder, congressNo);

        // load raw data from GovTrack
        proc.processDebates();
        proc.processBills();
        proc.processRolls();
        proc.processLegislators();

        // load additional information from external sources
        File senFile = new File(addinfoFolder, SENATOR_FILE);
        File repFile = new File(addinfoFolder, REPRESENTATIVE_FILE);
        File nominateFile = new File(addinfoFolder, NOMINATE_SCORE_FILE);

        // - entity resolution for legislators with missing ICPSR IDs
        proc.getMissingICPSRIDs(repFile.getAbsolutePath(), senFile.getAbsolutePath());

        // - load pre-computed NOMINATE scores for legislators
        proc.getNOMINATEScores(nominateFile.getAbsolutePath());

        // - load Policy Agenda codebook
        File policyAgendaCodebookFile = new File(addinfoFolder, POLICY_AGENDA_CODEBOOK_FILE);
        proc.loadPolicyAgendaCodebook(policyAgendaCodebookFile.getAbsolutePath());

        // - load topics labeled by the Congressional Bills project
        File congressionalBillsProjectTopicFile = new File(addinfoFolder, CONGRESSIONAL_BILL_PROJECT_TOPIC_FILE);
        proc.loadCongressinalBillsProjectTopicLabels(congressionalBillsProjectTopicFile.getAbsolutePath());

        // select a subset of 'interesting' debates, following Thomas et. at. (EMNLP 06)
        ArrayList<GTDebate> selectedDebates = proc.selectDebates();

        // output
        proc.outputLegislators(new File(processedFolder, "legislators.txt").getAbsolutePath());
        proc.outputSelectedDebate(new File(processedFolder, "filter").getAbsolutePath(), selectedDebates);
        proc.outputBills(new File(processedFolder, "bills.txt").getAbsolutePath());
        proc.outputBillSubjects(new File(processedFolder, "bills-subjects.txt").getAbsolutePath());
    }

    private static void format() throws Exception {
        if (verbose) {
            System.out.println("Formatting documents with NOMINATE reponses and labels ...");
        }

        String folder = cmd.getOptionValue("folder");
        String processedFolder = cmd.getOptionValue("processed-folder");
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 109); // default
        String outputFolder = cmd.getOptionValue("format-folder");
        IOUtils.createFolder(outputFolder);

        GTProcessor proc = new GTProcessor(folder, congressNo);
        proc.setVerbose(verbose);
        
        HashMap<String, GTLegislator> legislators = proc.inputLegislators(new File(processedFolder, "legislators.txt").getAbsolutePath());
        ArrayList<GTDebate> debates = proc.inputDebates(new File(processedFolder, "filter").getAbsolutePath());
        HashMap<String, GTBill> bills = proc.inputBills(new File(processedFolder, "bills.txt").getAbsolutePath());

        ArrayList<String> docIds = new ArrayList<String>();
        ArrayList<String> docTexts = new ArrayList<String>();
        ArrayList<Double> docResponses = new ArrayList<Double>();
        ArrayList<ArrayList<String>> docLabels = new ArrayList<ArrayList<String>>();
        ArrayList<String> docInfo = new ArrayList<String>();

        for (GTDebate debate : debates) {
            int turnCount = -1;
            for (GTTurn turn : debate.getTurns()) {
                turnCount++;

                String speaker = turn.getSpeakerId();
                GTLegislator legislator = legislators.get(speaker);
                // skipping speakers without NOMINATE score
                if (legislator == null) {
                    continue;
                }
                if (!legislator.hasProperty(GTProcessor.NOMINATE_SCORE1)) {
                    System.out.println(legislator.toString() + " does not have NOMINATE score");
                    continue;
                }

                GTRoll roll = debate.getAssociatedRoll();

                String id = debate.getId() + "_" + turnCount;
                String text = turn.getText();
                double response = Double.parseDouble(legislator.getProperty(GTProcessor.NOMINATE_SCORE1));

                docIds.add(id);
                docTexts.add(text);
                docResponses.add(response);

                docInfo.add(debate.getId() + "_" + turnCount + "\t"
                        + speaker + "\t"
                        + legislator.getParty() + "\t"
                        + roll.getVote(legislator.getId()) + "\t"
                        + legislator.getProperty(GTProcessor.NOMINATE_SCORE1) + "\t"
                        + roll.getBillId() + "\t"
                        + roll.getTitle());

                GTBill bill = bills.get(roll.getBillId());
                docLabels.add(bill.getSubjects());
            }
        }

        // output
        if (verbose) {
            System.out.println("\nOutputing format data to " + outputFolder);
        }
        BufferedWriter writer;
        // 1. output main texts
        File textFolder = new File(outputFolder, "texts");
        IOUtils.createFolder(textFolder);
        for (int ii = 0; ii < docIds.size(); ii++) {
            writer = IOUtils.getBufferedWriter(new File(textFolder, docIds.get(ii) + ".txt"));
            writer.write(docTexts.get(ii));
            writer.close();
        }

        // 2. output responses
        writer = IOUtils.getBufferedWriter(new File(outputFolder, "responses.txt"));
        for (int ii = 0; ii < docIds.size(); ii++) {
            writer.write(docIds.get(ii) + "\t" + docResponses.get(ii) + "\n");
        }
        writer.close();

        // 3. output labels
        writer = IOUtils.getBufferedWriter(new File(outputFolder, "labels.txt"));
        for (int ii = 0; ii < docIds.size(); ii++) {
            writer.write(docIds.get(ii) + "\t" + docLabels.get(ii).toString() + "\n");
        }
        writer.close();

        // 4. output info
        writer = IOUtils.getBufferedWriter(new File(outputFolder, "info.txt"));
        for (int ii = 0; ii < docIds.size(); ii++) {
            writer.write(docIds.get(ii) + "\t" + docInfo.get(ii) + "\n");
        }
        writer.close();
    }

    private static void extractRepublicans() throws Exception {
        String folder = cmd.getOptionValue("folder");
        String processedFolder = cmd.getOptionValue("processed-folder");
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 109); // default

        GTProcessor proc = new GTProcessor(folder, congressNo);
        HashMap<String, GTLegislator> legislators = proc.inputLegislators(
                new File(processedFolder, "legislators.txt").getAbsolutePath());

        File repFile = new File(processedFolder, congressNo + "-Republicans.txt");
        System.out.println("\nExtracting Republicans to " + repFile.getAbsolutePath());
        System.out.println("# " + legislators.size());

        BufferedWriter writer = IOUtils.getBufferedWriter(repFile);
        writer.write("ICPSR ID\tName\tState\tDistrict\tType\n");
        for (GTLegislator legislator : legislators.values()) {
            if (legislator.getParty().equals("Republican")) {
                writer.write(legislator.getProperty(GTLegislator.ICPSRID)
                        + "\t" + legislator.getFirstname()
                        + " " + legislator.getMiddlename()
                        + " " + legislator.getLastname()
                        + "\t" + legislator.getState()
                        + "\t" + legislator.getDistrict()
                        + "\t" + legislator.getType()
                        + "\n");
            }
        }
        writer.close();
    }
}
