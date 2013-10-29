package main;

import core.AbstractRunner;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.cli.BasicParser;
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
public class Processor extends AbstractRunner {

    public static final String SENATOR_FILE = "s01112nw.txt";
    public static final String REPRESENTATIVE_FILE = "h01112nw.txt";
    public static final String NOMINATE_SCORE_FILE = "HANDSL01112D20_BSSE.txt";
    public static final String POLICY_AGENDA_CODEBOOK_FILE = "policy_agenda_codebook.txt";
    public static final String CONGRESSIONAL_BILL_PROJECT_TOPIC_FILE = "bills93-112-Sept232013.txt";
    public static final String HOUSE_REPUBLICAN_FILE = "112th-House-Republicans.txt";
    public static final String SENATE_REPUBLICAN_FILE = "112th-Senate-Republicans.txt";
    public static final String DEBATE_FOLDER = "debates"; // each turn as a document
    public static final String BILL_FOLDER = "bills"; // each bill summary as a document

    public static void main(String[] args) {
        try {
            // create the command line parser
            parser = new BasicParser();

            // create the Options
            options = new Options();

            addOption("folder", "Folder to store downloaded data");
            addOption("congress", "Congress number");
            addOption("processed-folder", "Processed folder");
            addOption("addinfo-folder", "Folder containing additional files");
            addOption("format-folder", "Format folder");
            addOption("tea-party-file", "Tea party annotation file");
            addOption("mode", "Mode of processing");

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
            } else if (mode.equals("format-debate-turns")) {
                formatDebateTurns();
            } else if (mode.equals("format-bill-summaries")) {
                formatBillSummaries();
            } else if (mode.equals("extract-republicans")) {
                extractRepublicans();
            } else if (mode.equals("format-debate-turns-tea-party")) {
                formatDebateTurnsWithTeaPartyAnnotations();
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
        File congBillsProjTopicFile = new File(addinfoFolder, CONGRESSIONAL_BILL_PROJECT_TOPIC_FILE);
        proc.loadCongressinalBillsProjectTopicLabels(congBillsProjTopicFile.getAbsolutePath());

        // select a subset of 'interesting' debates, following Thomas et. at. (EMNLP 06)
        ArrayList<GTDebate> selectedDebates = proc.selectDebates();

        // output
        File debateFolder = new File(processedFolder, DEBATE_FOLDER);
        File billFolder = new File(processedFolder, BILL_FOLDER);

        // - output legislators
        proc.outputLegislators(new File(processedFolder, "legislators.txt").getAbsolutePath());

        // - output debates (texts and info)
        proc.outputSelectedDebateTurns(debateFolder.getAbsolutePath(), selectedDebates);

        // - output bills (info, summary and subjects)
        IOUtils.createFolder(billFolder);
        proc.outputBills(new File(billFolder, "info.txt"));
        proc.outputBillSubjects(new File(billFolder, "subjects.txt"));
        proc.outputBillTopics(new File(billFolder, "topics.txt"));
        proc.outputBillSummaries(new File(billFolder, "summaries"));
    }

    /**
     * Format debate turns
     *
     * - Each turn in a debate is a document
     *
     * - The response variable associated with each document is the DW-NOMINATE
     * score of the corresponding speaker
     */
    private static void formatDebateTurns() throws Exception {
        if (verbose) {
            System.out.println("Formatting turns with NOMINATE reponses and labels ...");
        }

        String folder = cmd.getOptionValue("folder");
        String processedFolder = cmd.getOptionValue("processed-folder");
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 109); // default
        String outputFolder = cmd.getOptionValue("format-folder");
        IOUtils.createFolder(outputFolder);

        GTProcessor proc = new GTProcessor(folder, congressNo);
        proc.setVerbose(verbose);

        // load legislators
        HashMap<String, GTLegislator> legislators = proc.inputLegislators(
                new File(processedFolder, "legislators.txt").getAbsolutePath());

        // load debates
        File debateFolder = new File(processedFolder, DEBATE_FOLDER);
        ArrayList<GTDebate> debates = proc.inputDebates(debateFolder);

        ArrayList<String> docIds = new ArrayList<String>();
        ArrayList<String> docTexts = new ArrayList<String>();
        ArrayList<Double> docResponses = new ArrayList<Double>();
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

        // 3. output info
        writer = IOUtils.getBufferedWriter(new File(outputFolder, "info.txt"));
        for (int ii = 0; ii < docIds.size(); ii++) {
            writer.write(docIds.get(ii) + "\t" + docInfo.get(ii) + "\n");
        }
        writer.close();
    }

    /**
     * Format bill summaries
     *
     * - Each bill summary is a document
     *
     * - The set of subjects associated with each bill is the label
     */
    private static void formatBillSummaries() throws Exception {
        if (verbose) {
            System.out.println("Formatting bill summaries ...");
        }

        String folder = cmd.getOptionValue("folder");
        String processedFolder = cmd.getOptionValue("processed-folder");
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 109); // default
        String outputFolder = cmd.getOptionValue("format-folder");
        IOUtils.createFolder(outputFolder);

        GTProcessor proc = new GTProcessor(folder, congressNo);
        proc.setVerbose(verbose);

        // load bills
        File billFolder = new File(processedFolder, BILL_FOLDER);
        HashMap<String, GTBill> bills = proc.inputBills(new File(billFolder, "info.txt"));
        proc.inputBillSubjects(new File(billFolder, "subjects.txt"), bills);
        proc.inputBillTopics(new File(billFolder, "topics.txt"), bills);
        proc.inputBillSummaries(new File(billFolder, "summaries"), bills);

        // output
        BufferedWriter writer;

        if (verbose) {
            System.out.println("\nOutputing to " + outputFolder);
        }
        // output bill summaries
        File textFolder = new File(outputFolder, "texts");
        IOUtils.createFolder(textFolder);
        for (String billId : bills.keySet()) {
            GTBill bill = bills.get(billId);

            String summary = bill.getSummary();
            int firstPunct = summary.indexOf(".");

            writer = IOUtils.getBufferedWriter(new File(textFolder, billId + ".txt"));
            writer.write(bill.getOfficialTitle()
                    + " " + summary.substring(firstPunct + 1)
                    .replaceAll("\\(This measure has not been amended since "
                    + "it was introduced. The summary of that version is repeated here.\\)", "")
                    .trim()
                    + "\n");
            writer.close();
        }

        // output bill subjects
        writer = IOUtils.getBufferedWriter(new File(outputFolder, "subjects.txt"));
        for (String billId : bills.keySet()) {
            GTBill bill = bills.get(billId);
            writer.write(bill.getId());
            ArrayList<String> billSubjects = bill.getSubjects();
            for (String bs : billSubjects) {
                writer.write("\t" + bs);
            }
            writer.write("\n");
        }
        writer.close();

        // output bill major topics (from the Policy Agenda Codebook)
        writer = IOUtils.getBufferedWriter(new File(outputFolder, "topics.txt"));
        for (String billId : bills.keySet()) {
            GTBill bill = bills.get(billId);
            writer.write(bill.getId());

            String major = bill.getProperty(GTBill.MAJOR_TOPIC);
            if (major != null && !major.equals("null") && !major.equals("99")) {
                writer.write("\t" + bill.getProperty(GTBill.MAJOR_TOPIC));
            }
            writer.write("\n");
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

    /**
     * Format data with Tea Party annotations
     *
     * - Each turn in a debate is a document
     *
     * - Most of Republicans in the House are annotated with Tea Party score
     *
     * - The response variable associated with each document is the Tea Party
     * score of the corresponding speaker
     */
    private static void formatDebateTurnsWithTeaPartyAnnotations() throws Exception {
        String folder = cmd.getOptionValue("folder");
        String processedFolder = cmd.getOptionValue("processed-folder");
        int congressNo = CLIUtils.getIntegerArgument(cmd, "congress", 109); // default
        String outputFolder = cmd.getOptionValue("format-folder");
        String teaPartyAnnotationFile = cmd.getOptionValue("tea-party-file");
        IOUtils.createFolder(outputFolder);

        GTProcessor proc = new GTProcessor(folder, congressNo);
        proc.setVerbose(verbose);

        // load legislators
        HashMap<String, GTLegislator> legislators = proc.inputLegislators(
                new File(processedFolder, "legislators.txt").getAbsolutePath());

        // load Tea Party annotation for legislators
        proc.loadTeaPartyAnnotations(teaPartyAnnotationFile);

        // load debates
        File debateFolder = new File(processedFolder, DEBATE_FOLDER);
        ArrayList<GTDebate> debates = proc.inputDebates(debateFolder);

        File billFolder = new File(processedFolder, BILL_FOLDER);
        HashMap<String, GTBill> bills = proc.inputBills(new File(billFolder, "info.txt"));
        proc.inputBillTopics(new File(billFolder, "topics.txt"), bills);

        // output
        ArrayList<String> docIds = new ArrayList<String>();
        ArrayList<String> docTexts = new ArrayList<String>();
        ArrayList<Double> docResponses = new ArrayList<Double>();
        ArrayList<String> docPATopics = new ArrayList<String>();
        ArrayList<String> docInfo = new ArrayList<String>();
        for (GTDebate debate : debates) {
            int turnCount = -1;
            for (GTTurn turn : debate.getTurns()) {
                turnCount++;

                String speaker = turn.getSpeakerId();
                GTLegislator legislator = legislators.get(speaker);

                if (legislator == null) {
                    continue;
                }
                // skipping speakers without NOMINATE score
                if (!legislator.hasProperty(GTProcessor.NOMINATE_SCORE1)) {
                    System.out.println(legislator.toString() + " does not have NOMINATE score");
                    continue;
                }
                // skipping speakers without Tea Party annotation
                if (!legislator.hasProperty(GTLegislator.TP_SCORE)) {
                    continue;
                }

                GTRoll roll = debate.getAssociatedRoll();

                String id = debate.getId() + "_" + turnCount;
                String text = turn.getText();
                // response variable is the Tea Party score
                double response = Double.parseDouble(legislator.getProperty(GTLegislator.TP_SCORE));

                docIds.add(id);
                docTexts.add(text);
                docResponses.add(response);

                // policy agenda topics from congressional bill project
                GTBill bill = bills.get(roll.getBillId());
                String major = null;
                if (bill != null) {
                    major = bill.getProperty(GTBill.MAJOR_TOPIC);
                }
                docPATopics.add(major);

                docInfo.add(debate.getId() + "_" + turnCount + "\t"
                        + speaker + "\t"
                        + legislator.getParty() + "\t"
                        + roll.getVote(legislator.getId()) + "\t"
                        + legislator.getProperty(GTProcessor.NOMINATE_SCORE1) + "\t"
                        + legislator.getProperty(GTLegislator.FRESHMEN) + "\t"
                        + legislator.getProperty(GTLegislator.TP_SCORE) + "\t"
                        + legislator.getProperty(GTLegislator.FW_SCORE) + "\t"
                        + roll.getBillId() + "\t"
                        + roll.getTitle());
            }
        }

        // output
        if (verbose) {
            System.out.println("\nOutputing format data to " + outputFolder + " ...");
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
            writer.write(docIds.get(ii)
                    + "\t" + docResponses.get(ii)
                    + "\n");
        }
        writer.close();

        // 3. output info
        writer = IOUtils.getBufferedWriter(new File(outputFolder, "info.txt"));
        for (int ii = 0; ii < docIds.size(); ii++) {
            writer.write(docIds.get(ii) + "\t" + docInfo.get(ii) + "\n");
        }
        writer.close();

        // 4. output major topics from congressional bill project
        writer = IOUtils.getBufferedWriter(new File(outputFolder, "topics.txt"));
        for (int ii = 0; ii < docPATopics.size(); ii++) {
            if (docPATopics.get(ii) != null) {
                writer.write(docIds.get(ii) + "\t" + docPATopics.get(ii) + "\n");
            } else {
                writer.write(docIds.get(ii) + "\n");
            }
        }
        writer.close();
    }
}
