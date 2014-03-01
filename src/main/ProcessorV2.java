package main;

import java.io.File;
import java.util.ArrayList;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.Options;
import util.CLIUtils;
import util.IOUtils;
import util.govtrack.GTBill;
import util.govtrack.GTDebate;
import util.govtrack.GTProcessorV2;

/**
 * This use GTProcessorV2 instead of GTProcessor in Processor.
 *
 * @author vietan
 */
public class ProcessorV2 extends Processor {

    public static String getHelpString() {
        return "java -cp 'dist/gtpounder.jar' " + ProcessorV2.class.getName() + " -help";
    }

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
                CLIUtils.printHelp(getHelpString(), options);
                return;
            }

            String mode = CLIUtils.getStringArgument(cmd, "mode", "process");
            verbose = cmd.hasOption("v");

            if (mode.equals("process")) {
                process();
            } else {
                throw new RuntimeException("Processing mode " + mode + " is not supported.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
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

        GTProcessorV2 proc = new GTProcessorV2(folder, congressNo);
        proc.setVerbose(verbose);
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

        // load topic annotation from the Congressional Bills project
        // - load Policy Agenda codebook
        File policyAgendaCodebookFile = new File(addinfoFolder, POLICY_AGENDA_CODEBOOK_FILE);
        proc.loadPolicyAgendaCodebook(policyAgendaCodebookFile.getAbsolutePath());

        // - load topics labeled by the Congressional Bills project using topics
        // from the Policy Agenda codebook
        File congBillsProjTopicFile = new File(addinfoFolder, CONGRESSIONAL_BILL_PROJECT_TOPIC_FILE);
        proc.loadCongressinalBillsProjectTopicLabels(congBillsProjTopicFile.getAbsolutePath());

        // load Tea Party anno  tation for legislators
        File houseRepublicanFile = new File(addinfoFolder, HOUSE_REPUBLICAN_FILE);
        proc.loadTeaPartyHouse(houseRepublicanFile.getAbsolutePath());

        // output
        // - output legislators
        proc.outputLegislators(new File(processedFolder, "legislators.txt").getAbsolutePath());

        // output bills
        ArrayList<GTBill> selectedBills = proc.selectBills();
        File billFolder = new File(processedFolder, BILL_FOLDER);
        proc.outputSelectedBills(billFolder, selectedBills);

        // output debates
        ArrayList<GTDebate> selectedDebates = proc.selectDebates();
        File debateTurnFolder = new File(processedFolder, DEBATE_FOLDER);
        proc.outputSelectedDebates(debateTurnFolder, selectedDebates);
    }
}
