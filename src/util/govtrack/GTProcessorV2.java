package util.govtrack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import util.IOUtils;
import util.MiscUtils;

/**
 *
 * @author vietan
 */
public class GTProcessorV2 extends GTProcessor {

    public GTProcessorV2(String folder, int congNum) {
        super(folder, congNum);
    }

    // === Processing debates ==================================================
    @Override
    public void processDebates() {
        File crFolder = new File(this.congressFolder, "cr");
        if (!crFolder.exists()) {
            throw new RuntimeException(crFolder + " not found.");
        }
        if (verbose) {
            System.out.println("Processing debates " + crFolder.getAbsolutePath() + " ...");
        }

        this.debates = new HashMap<String, GTDebate>();
        String[] debateFilenames = crFolder.list();


        int count = 0;
        int stepSize = MiscUtils.getRoundStepSize(debateFilenames.length, 10);
        for (String filename : debateFilenames) {
            if (count % stepSize == 0 && verbose) {
                System.out.println("--- Processing debate file "
                        + count + " / " + debateFilenames.length);
            }
            count++;

            File debateFile = new File(crFolder, filename);
            if (debateFile.length() == 0) {
                if (verbose) {
                    System.out.println("--- --- Skipping empty file " + debateFile);
                }
                continue;
            }

            GTDebate debate = processSingleDebate(debateFile);
            this.debates.put(debate.getId(), debate);
        }

        if (verbose) {
            System.out.println("--- Loaded " + debates.size() + " debates");
            int numDebatesMentioningBill = 0;
            for (GTDebate debate : debates.values()) {
                if (debate.getBillAssociatedWith() != null) {
                    numDebatesMentioningBill++;
                }
            }
            System.out.println("--- --- # debates having bill(s) mentioned by speakers: "
                    + numDebatesMentioningBill);
        }
    }

    public GTDebate processSingleDebate(File debateFile) {
        Element docEle;
        NodeList nodelist;
        Element el;
        try {
            docEle = getDocumentElement(debateFile.getAbsolutePath());
        } catch (Exception e) {
            if (verbose) {
                System.out.println("--- --- Skipping problematic debate file "
                        + debateFile);
                e.printStackTrace();
            }
            return null;
        }

        String title = docEle.getAttribute("title");
        String where = docEle.getAttribute("where");
        String debateId = IOUtils.removeExtension(debateFile.getName());
        GTDebate debate = new GTDebate(debateId);
        debate.setTitle(title);
        debate.addProperty("where", where);

        // list of turns
        GTTurn preTurn = null;
        int turnCount = 0;
        nodelist = docEle.getElementsByTagName("speaking");
        for (int i = 0; i < nodelist.getLength(); i++) {
            el = (Element) nodelist.item(i);
            String speaker = el.getAttribute("speaker");
            String topic = el.getAttribute("topic");

            // get contents
            StringBuilder text = new StringBuilder();
            ArrayList<String> paraBillsMentioned = new ArrayList<String>();
            NodeList nl = el.getElementsByTagName("paragraph");
            for (int j = 0; j < nl.getLength(); j++) {
                Element turnEle = (Element) nl.item(j);

                // text
                String paraText = turnEle.getTextContent();
                if (filterOut(paraText)) {
                    continue;
                }
                text.append(paraText).append(" ");

                // bills mentioned
                NodeList billNl = turnEle.getElementsByTagName("bill");
                for (int ii = 0; ii < billNl.getLength(); ii++) {
                    Element billEl = (Element) billNl.item(ii);
                    String billType = billEl.getAttribute("type");
                    String billNumber = billEl.getAttribute("number");
                    String billId = billType + "-" + billNumber;
                    paraBillsMentioned.add(billId);
                    debate.addBillMentioned(billId);
                }
            }

            // merge consecutive turns are from the same speaker
            if (preTurn != null && preTurn.getSpeakerId().equals(speaker)) {
                String preTurnText = preTurn.getText();
                preTurnText += " " + text.toString();
                preTurn.setText(preTurnText);
                preTurn.addBillsMentioned(paraBillsMentioned);
            } else { // or create a new turn
                GTTurn turn = new GTTurn(
                        debateId + "_" + turnCount,
                        speaker,
                        (text.toString()));
                turn.setBillsMentioned(paraBillsMentioned);
                if (!topic.trim().isEmpty()) {
                    turn.addProperty("topic", topic);
                }
                debate.addTurn(turn);
                preTurn = turn;
                turnCount++;
            }
        }

        // estimate main bill mentioned for each turn
        if (debate.getBillsMentioned().size() > 0) {
            estimateTurnMainBillMentioned(debate);
        }

        return debate;
    }

    /**
     * Fill in the main bill mentioned for every turn in the debate. If a turn
     * does not explicitly mention a bill, the bill mentioned in the most recent
     * turn is used to associate with this turn.
     */
    public void estimateTurnMainBillMentioned(GTDebate debate) {
        String firstBillMentioned = null;
        int firstTurnMentioning = -1;
        for (int ii = 0; ii < debate.getNumTurns(); ii++) {
            GTTurn turn = debate.getTurn(ii);
            String billAssoc = turn.getBillAssociatedWith();
            if (billAssoc != null) {
                firstBillMentioned = billAssoc;
                firstTurnMentioning = ii;
            }
        }

        for (int ii = 0; ii <= firstTurnMentioning; ii++) {
            debate.getTurn(ii).setMainBillMentioned(firstBillMentioned);
        }

        String curBillMentioned = firstBillMentioned;
        for (int ii = firstTurnMentioning + 1; ii < debate.getNumTurns(); ii++) {
            GTTurn turn = debate.getTurn(ii);
            String billAssoc = turn.getBillAssociatedWith();
            if (billAssoc == null) {
                turn.setMainBillMentioned(curBillMentioned);
            } else {
                turn.setMainBillMentioned(billAssoc);
                curBillMentioned = billAssoc;
            }
        }
    }

    protected boolean filterOut(String paraText) {
        if (paraText.contains("I yield")) {
            return true;
        }
        return false;
    }

    protected String procecessText(String text) {
        return text.replaceAll("\n", " ")
                .replace("nbsp", "")
                .replace("&", "")
                .replace("Madam Speaker", "")
                .replace("Mr. Speaker", "");
    }
    // === End processing debates ==============================================

    // === Start processing bills ==============================================
    @Override
    public void processBills() {
        File billFolder = new File(this.congressFolder, "bills");
        if (!billFolder.exists()) {
            throw new RuntimeException(billFolder + " not found.");
        }

        File billTextFolder = new File(this.congressFolder, "bills.html");
        if (!billTextFolder.exists()) {
            throw new RuntimeException(billTextFolder + " not found.");
        }

        if (verbose) {
            System.out.println("\nProcessing bills " + billFolder);
        }

        this.bills = new HashMap<String, GTBill>();
        String[] billFilenames = billFolder.list();
        int count = 0;
        int stepSize = MiscUtils.getRoundStepSize(billFilenames.length, 10);
        for (String billFilename : billFilenames) {
            // debug
            if (count % stepSize == 0 && verbose) {
                System.out.println("--- Processing bill file "
                        + count + " / " + billFilenames.length);
            }
            count++;

            File billFile = new File(billFolder, billFilename);
            File billTextFile = new File(billTextFolder, billFilename.replaceAll("xml", "txt"));
            if (!billTextFile.exists()) {
                System.out.println("--- --- Skipping bill " + billTextFile
                        + ". No text found.");

                continue;
            }
            Element docEle;
            try {
                docEle = getDocumentElement(billFile.getAbsolutePath());
            } catch (Exception e) {
                if (verbose) {
                    System.out.println("--- --- Skipping problematic bill file "
                            + billFile);
                    e.printStackTrace();
                }
                continue;
            }
            NodeList nodelist;
            Element element;

            // create bill
            String billType = docEle.getAttribute("type");
            int billNumber = Integer.parseInt(docEle.getAttribute("number"));
            GTBill bill = new GTBill(billType, billNumber);

            // titles
            nodelist = docEle.getElementsByTagName("title");
            for (int ii = 0; ii < nodelist.getLength(); ii++) {
                element = (Element) nodelist.item(ii);
                String type = element.getAttribute("type");
                String title = element.getTextContent();
                if (type.equals("popular")) {
                    bill.setTitle(title);
                } else if (type.equals("official")) {
                    bill.setOfficialTitle(title);
                }
            }

            // subjects (labels) of this bill
            ArrayList<String> subjects = new ArrayList<String>();
            nodelist = docEle.getElementsByTagName("term");
            for (int i = 0; i < nodelist.getLength(); i++) {
                element = (Element) nodelist.item(i);
                subjects.add(element.getAttribute("name"));
            }
            bill.setSubjects(subjects);

            // bill summary
            nodelist = docEle.getElementsByTagName("summary");
            element = (Element) nodelist.item(0);
            String summary = element.getTextContent();
            bill.setSummary(summary);

            // bill text
            String billText = inputBillText(billTextFile);
            bill.setText(billText);

            // add bill
            this.bills.put(bill.getId(), bill);
        }

        // store the list of debate turns that discuss each bill
        for (GTDebate debate : this.debates.values()) {
            for (GTTurn turn : debate.getTurns()) {
                String billAssoc = turn.getBillAssociatedWith();
                if (billAssoc == null) {
                    continue;
                }

                GTBill bill = this.bills.get(billAssoc);
                if (bill == null) {
                    continue;
                }
                bill.addDebateId(turn.getId());
            }
        }

        if (verbose) {
            System.out.println("--- Loaded " + this.bills.size() + " bills.");
            int numBillsHaveTurn = 0;
            for (GTBill bill : this.bills.values()) {
                if (bill.getSpeechIds() != null && bill.getSpeechIds().size() > 0) {
                    numBillsHaveTurn++;
                }
            }
            System.out.println("--- --- # bills get mentioned: " + numBillsHaveTurn);
        }
    }

    private String inputBillText(File file) {
        StringBuilder str = new StringBuilder();
        try {
            BufferedReader reader = IOUtils.getBufferedReader(file);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                str.append(line.trim()).append(" ");
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception while reading file "
                    + file);
        }

        return str.toString();
    }
    // =========================================================================

    @Override
    public ArrayList<GTDebate> selectDebates() throws Exception {
        System.out.println("Selecting debates ...");
        System.out.println("--- Total # debates: " + debates.size());
        ArrayList<GTDebate> selectedDebates = new ArrayList<GTDebate>();
        for (GTDebate debate : this.debates.values()) {
            if (debate.getNumTurns() > 1) {
                selectedDebates.add(debate);
            }
        }
        System.out.println("--- # selected debates: " + selectedDebates.size());
        return selectedDebates;
    }

    public ArrayList<GTBill> selectBills() throws Exception {
        System.out.println("Selecting bills ...");
        System.out.println("--- Total # bills: " + this.bills.size());
        ArrayList<GTBill> selectedBills = new ArrayList<GTBill>();
        for (GTBill bill : this.bills.values()) {
            selectedBills.add(bill);
        }
        System.out.println("--- # selected bills: " + selectedBills.size());
        return selectedBills;
    }

    /**
     * Select bills by roll-call votes. Only consider bills that are voted.
     */
    public ArrayList<GTBill> selectBillsByVotes() throws Exception {
        System.out.println("Selecting bills by roll-call votes ...");
        System.out.println("# raw bills: " + bills.size());
        int numBillsWithMainRollCall = 0;
        int numBillsWithRollCall = 0;
        ArrayList<GTBill> selectedBills = new ArrayList<GTBill>();
        for (GTBill bill : this.bills.values()) {
            if (bill.getRollIds() != null && bill.getRollIds().size() > 0) {
                numBillsWithRollCall++;
                selectedBills.add(bill);
            }

            String rollID = this.getMainRollId(bill);
            if (rollID == null) {
                continue;
            }

            numBillsWithMainRollCall++;
        }

        System.out.println("# bills with roll-call(s): " + numBillsWithRollCall
                + "\t" + selectedBills.size());
        System.out.println("# bills with main roll-call: " + numBillsWithMainRollCall);

        // debug
        for (GTBill bill : selectedBills) {
            System.out.println(bill.getId()
                    + "\t" + getMainRollId(bill)
                    + "\t" + bill.getRollIds().toString());

        }
        return selectedBills;
    }

    public void outputBillTexts(File billFolder, ArrayList<GTBill> selectedBills) throws Exception {
        System.out.println("Outputing bill texts to " + billFolder);
        IOUtils.createFolder(billFolder);

        BufferedWriter writer;
        for (GTBill bill : selectedBills) {
            writer = IOUtils.getBufferedWriter(new File(billFolder, bill.getId()));
            writer.write(bill.getText().trim() + "\n" + bill.getSummary().trim());
            writer.close();
        }
    }

    public void outputDebateTurnText(File debateTurnTextFolder, 
            ArrayList<GTDebate> selectedDebates) throws Exception {
        System.out.println("Outputing debate turns to " + debateTurnTextFolder);
        IOUtils.createFolder(debateTurnTextFolder);

        BufferedWriter writer;
        for (GTDebate debate : selectedDebates) {
            writer = IOUtils.getBufferedWriter(new File(debateTurnTextFolder, debate.getId()));
            for (int ii = 0; ii < debate.getNumTurns(); ii++) {
                GTTurn turn = debate.getTurn(ii);
                writer.write(turn.getId() 
                        + "\t" + turn.getMainBillMentioned()
                        + "\t" + turn.getSpeakerId()
                        + "\t" + turn.getText() + "\n");
            }
            writer.close();
        }
    }

    public void outputDebateTurnSubjects(File debateTurnSubjFile) throws Exception {
        System.out.println("Outputing debate turn subject to " + debateTurnSubjFile);
        BufferedWriter writer = IOUtils.getBufferedWriter(debateTurnSubjFile);
        for (GTDebate debate : this.debates.values()) {
            for (int ii = 0; ii < debate.getNumTurns(); ii++) {
                GTTurn turn = debate.getTurn(ii);
                String majorbill = turn.getMainBillMentioned();
                GTBill bill = this.bills.get(majorbill);
                if (bill == null) {
                    continue;
                }
                ArrayList<String> subjects = bill.getSubjects();
                if (subjects == null || subjects.isEmpty()) {
                    continue;
                }
                writer.write(turn.getId());
                for (String subject : subjects) {
                    writer.write("\t" + subject);
                }
                writer.write("\n");
            }
        }
        writer.close();
    }
}
