package util.govtrack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import util.IOUtils;
import util.MiscUtils;

/**
 * This processing pipeline follows Thomas et. al. (EMNLP 06). Here are some
 * main points:
 *
 * - Each debate is associated with only 1 bill. In case multiple bills are
 * mentioned in a debate, the bill that is mentioned the most is kept.
 *
 * - Only bills that are have Yea/Nay ratio between [0.2, 0.8] are kept.
 *
 * @author vietan
 */
public class GTProcessor {

    public static final String EMPTY_SCORE = "100";
    public static final String NOMINATE_SCORE1 = "score1";
    public static final String NOMINATE_SCORE2 = "score2";
    public static final String DEMOCRAT = "Democrat";
    public static final String REPUBLICAN = "Republican";
    public static final String INDEPENDENT = "Independent";
    public static final int LASTNAME = 0;
    public static final int FIRSTNAME = 1;
    public static final int MIDDLENAME = 2;
    protected String folder;
    protected int congressNumber;
    protected HashMap<String, GTDebate> debates;
    protected HashMap<String, GTBill> bills;
    protected HashMap<String, GTRoll> rolls;
    protected HashMap<String, GTLegislator> legislators;
    protected HashMap<String, GTLegislator> icpsrLegislatorMap;
    public static HashMap<String, GTState> states;
    public static HashMap<Integer, String> stateCodeMap;
    public static HashMap<Integer, String> policyAgendaCodebook;
    protected File congressFolder;
    protected boolean verbose = true;

    public GTProcessor(String folder, int congNum) {
        this.folder = folder;
        this.congressNumber = congNum;
        GTProcessor.getStates();

        try {
            this.congressFolder = new File(this.folder, Integer.toString(this.congressNumber));
            if (!this.congressFolder.exists()) {
                IOUtils.createFolder(this.congressFolder);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception in initiating GTProcessor");
        }
    }

    public void setVerbose(boolean v) {
        this.verbose = v;
    }

    public HashMap<String, GTBill> getBills() {
        return this.bills;
    }

    private static void getStates() {
        states = new HashMap<String, GTState>();
        stateCodeMap = new HashMap<Integer, String>();
        for (String line : stateMaps) {
            int statecode = Integer.parseInt(line.substring(0, 2));
            String stateabbr = line.substring(3, 5);
            String statename = line.substring(6).trim();
            GTState state = new GTState(stateabbr);
            state.setStatecode(statecode);
            state.setStatename(statename);
            states.put(stateabbr, state);
            stateCodeMap.put(statecode, stateabbr);
        }
    }

    public HashMap<Integer, String> loadPolicyAgendaCodebook(String filepath) throws Exception {
        if (verbose) {
            System.out.println("\nLoading Policy Agenda Codebook from " + filepath);
        }

        policyAgendaCodebook = new HashMap<Integer, String>();
        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] sline = line.split("\t");
            int topicId = Integer.parseInt(sline[0]);
            String topicLabel = sline[1];
            policyAgendaCodebook.put(topicId, topicLabel);
        }
        reader.close();

        System.out.println("--- Codebook size: " + policyAgendaCodebook.size());
        return policyAgendaCodebook;
    }

    public void loadCongressinalBillsProjectTopicLabels(String filepath) throws Exception {
        if (verbose) {
            System.out.println("\nLoading topic labels from Congressional Bills Project "
                    + filepath);
        }

        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        reader.readLine(); // headers

        int numBillsLabeled = 0;
        while ((line = reader.readLine()) != null) {
            String[] sline = line.split("\t");
            if (sline[7] == null || sline[7].trim().isEmpty()) {
                System.out.println("Skipping line " + line);
                continue;
            }

            int congressNo = Integer.parseInt(sline[7]);
            if (congressNo != this.congressNumber) {
                continue;
            }

            String billNum = sline[2];
            String billType = sline[3];
            int major = Integer.parseInt(sline[10]);
            int minor = Integer.parseInt(sline[11]);

            String billId;
            if (billType.equals("HR")) {
                billId = "h-" + billNum;
            } else {
                billId = "s-" + billNum;
            }

            GTBill bill = this.bills.get(billId);
            if (bill != null) {
                bill.addProperty(GTBill.MAJOR_TOPIC, Integer.toString(major));
                bill.addProperty(GTBill.MINOR_TOPIC, Integer.toString(minor));
                numBillsLabeled++;
            }
        }
        reader.close();

        System.out.println("--- # bills labeled: " + numBillsLabeled);
    }

    public void processLegislators() throws Exception {
        File peopleFile = new File(new File(folder, Integer.toString(congressNumber)), "people.xml");
        if (!peopleFile.exists()) {
            throw new RuntimeException(peopleFile.getAbsolutePath() + " not found");
        }

        if (verbose) {
            System.out.println("\nProcessing legislators " + peopleFile.getAbsolutePath() + " ...");
        }

        this.icpsrLegislatorMap = new HashMap<String, GTLegislator>();
        this.legislators = new HashMap<String, GTLegislator>();
        Element docEle = getDocumentElement(peopleFile.getAbsolutePath());
        NodeList nodelist;
        Element el;

        int icpsrCount = 0;
        nodelist = docEle.getElementsByTagName("person");
        for (int i = 0; i < nodelist.getLength(); i++) {
            el = (Element) nodelist.item(i);
            String pid = el.getAttribute("id").trim();
            String lastname = el.getAttribute("lastname").trim();
            String firstname = el.getAttribute("firstname").trim();
            String middlename = el.getAttribute("middlename").trim();
            String nickname = el.getAttribute("nickname").trim();
            String state = el.getAttribute("state").trim();
            String icpsrid = el.getAttribute("icpsrid").trim();
            NodeList roleNodelist = el.getElementsByTagName("role");
            el = (Element) roleNodelist.item(0);

            if (el == null) {
                if (verbose) {
                    System.out.println("--- Skipping missing role"
                            + ". ID: " + pid
                            + ". last name: " + lastname
                            + ". first name: " + firstname
                            + ". middle name:" + middlename);
                }
                continue;
            }

            String party = el.getAttribute("party").trim();
            String type = el.getAttribute("type").trim();
            String distStr = el.getAttribute("district").trim();

            GTLegislator legislator = new GTLegislator(
                    pid, lastname, firstname, middlename,
                    type, party, state);
            if (!nickname.isEmpty()) {
                legislator.addProperty("nickname", nickname);
            }
            if (type.equals(GTLegislator.REP)) {
                int district = Integer.parseInt(distStr);
                if (district == 0) {
                    district = 1;
                }
                legislator.setDistrict(district);
            }

            if (!icpsrid.isEmpty()) {
                legislator.addProperty(GTLegislator.ICPSRID, icpsrid);
                this.icpsrLegislatorMap.put(icpsrid, legislator);
                icpsrCount++;
            }
            this.legislators.put(pid, legislator);
        }

        if (verbose) {
            System.out.println("--- Loaded " + legislators.size() + " legislators ...");
            System.out.println("--- # legislators having ICPSR IDs "
                    + this.icpsrLegislatorMap.size() + ". " + icpsrCount);
        }

        // get all debates involving each legislator
        for (GTDebate debate : this.debates.values()) {
            Set<String> speakerIds = new HashSet<String>();
            for (GTTurn turn : debate.getTurns()) {
                speakerIds.add(turn.getSpeakerId());
            }

            for (String speakerId : speakerIds) {
                GTLegislator legislator = this.legislators.get(speakerId);
                if (legislator == null) {
                    continue;
                }
                legislator.addDebateId(debate.getId());
            }
        }
    }

    public void getNOMINATEScores(String filepath) throws Exception {
        if (verbose) {
            System.out.println("\nLoading NOMINATE scores from " + filepath);
        }

        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        String[] sline;
        int count = 0;
        while ((line = reader.readLine()) != null) {
            sline = line.split("\t");

            int congressNum = Integer.parseInt(sline[0]);
            if (this.congressNumber != congressNum) {
                continue;
            }
            String icpsrid = sline[1];
            String score1 = sline[7];
            String score2 = sline[8];

            GTLegislator legislator = this.icpsrLegislatorMap.get(icpsrid);
            if (legislator == null) {
                if (verbose) {
                    System.out.println("--- --- Skipping " + line);
                }
                continue;
            }
            legislator.addProperty(NOMINATE_SCORE1, score1);
            legislator.addProperty(NOMINATE_SCORE2, score2);
            count++;
        }
        reader.close();

        if (verbose) {
            System.out.println("--- Loaded NOMINATE score for " + count + " legislators");
        }
    }

    /**
     * Some legislators from GovTrack have missing ICPSR ID. This is to fill in
     * those missing ICPSR IDs using external information from VoteView. Some
     * heuristics on name, party affiliation, district, state etc of legislators
     * are used to perform entity resolution.
     *
     * @param repFilepath File containing information about Representatives
     * @param senFilepath File containing information about Senators
     */
    public void getMissingICPSRIDs(String repFilepath, String senFilepath) throws Exception {
        String line;
        BufferedReader reader;

        // for Representatives
        if (verbose) {
            System.out.println("\nGetting missing ICPSR IDs ...");
            System.out.println("--- Filling missing ICPSR IDs for Representatives from file " + repFilepath);
        }
        int count = 0;
        File repFile = new File(repFilepath);
        if (repFile.exists()) {
            reader = IOUtils.getBufferedReader(repFilepath);
            while ((line = reader.readLine()) != null) {
                int congNum = Integer.parseInt(line.substring(0, 4).trim());
                if (congressNumber != congNum) {
                    continue;
                }

                String icpsrId = line.substring(5, 10).trim();
                int stateCode = Integer.parseInt(line.substring(11, 13).trim());
                int districtCode = Integer.parseInt(line.substring(13, 16).trim());
                int partyCode = Integer.parseInt(line.substring(25, 28).trim());
                String fullname = line.substring(41).trim();
                String[] names = getNames(fullname);

                String stateAbbre = stateCodeMap.get(stateCode);
                if (stateAbbre == null) { // skip states other than 50 states
                    continue;
                }

                if (this.icpsrLegislatorMap.containsKey(icpsrId)) {
                    continue;
                }

                GTLegislator matchLegislator = null;
                // find legislator
                for (GTLegislator legislator : this.legislators.values()) {
                    if (!legislator.getType().equals("rep")) // skip Senators
                    {
                        continue;
                    }
                    if (!legislator.getParty().equals(getParty(partyCode))) {
                        continue;
                    }
                    if (!legislator.getState().equals(stateAbbre) || legislator.getDistrict() != districtCode) {
                        continue;
                    }
                    String legLastname = legislator.getLastname().toLowerCase();
                    String legFirstname = legislator.getFirstname().toLowerCase();
                    String curLastname = names[LASTNAME].toLowerCase();
                    String curFirstname = names[FIRSTNAME].toLowerCase();

                    double lastnameDist = getEditDistance(legLastname, curLastname);
                    double firstnameDist = getEditDistance(legFirstname, curFirstname);
                    double firstNicknameDist = 1.0;
                    if (legislator.hasProperty("nickname")) {
                        firstNicknameDist = getEditDistance(
                                legislator.getProperty("nickname").toLowerCase(),
                                curFirstname);
                    }

                    if (lastnameDist > 0.3 && (firstnameDist > 0.3 && firstNicknameDist > 0.3)) {
                        if (verbose) {
                            System.out.println("\t\tDetecting a mismatch, check if reasonable"
                                    + ". lastname: " + legLastname + " vs. " + curLastname
                                    + ". firstname: " + legFirstname + " vs. " + curFirstname
                                    + ". type: " + legislator.getType()
                                    + ". party: " + legislator.getParty()
                                    + ". state: " + legislator.getState());
                        }
                        continue;
                    }
                    matchLegislator = legislator;
                }

                if (matchLegislator == null) {
                    count++;
                    continue;
                }
                matchLegislator.addProperty(GTLegislator.ICPSRID, icpsrId);
                icpsrLegislatorMap.put(icpsrId, matchLegislator);
            }
            reader.close();
            if (verbose) {
                System.out.println("--- Number of Representatives in "
                        + repFilepath + " don't match: " + count);
            }
        }

        // For Senators
        if (verbose) {
            System.out.println("--- Filling missing ICPSR IDs for Senators in file "
                    + senFilepath);
        }
        count = 0;
        File senFile = new File(senFilepath);
        if (senFile.exists()) {
            reader = IOUtils.getBufferedReader(senFilepath);
            while ((line = reader.readLine()) != null) {
                int congNum = Integer.parseInt(line.substring(0, 4).trim());
                if (congressNumber != congNum) {
                    continue;
                }

                String icpsrId = line.substring(5, 10).trim();
                int stateCode = Integer.parseInt(line.substring(11, 13).trim());
                int partyCode = Integer.parseInt(line.substring(25, 28).trim());
                String fullname = line.substring(41).trim();
                String[] names = getNames(fullname);

                String stateAbbre = stateCodeMap.get(stateCode);
                if (stateAbbre == null) // skip states other than 50 states
                {
                    continue;
                }

                if (this.icpsrLegislatorMap.containsKey(icpsrId)) {
                    continue;
                }

                GTLegislator matchLegislator = null;
                // find legislator
                for (GTLegislator legislator : this.legislators.values()) {
                    if (!legislator.getType().equals("sen")) // skip Representatives
                    {
                        continue;
                    }
                    if (!legislator.getParty().equals(getParty(partyCode))) {
                        continue;
                    }
                    if (!legislator.getState().equals(stateAbbre)) {
                        continue;
                    }
                    String legLastname = legislator.getLastname().toLowerCase();
                    String legFirstname = legislator.getFirstname().toLowerCase();
                    String curLastname = names[LASTNAME].toLowerCase();
                    String curFirstname = names[FIRSTNAME].toLowerCase();

                    double lastnameDist = getEditDistance(legLastname, curLastname);
                    double firstnameDist = getEditDistance(legFirstname, curFirstname);
                    double firstNicknameDist = 1.0;
                    if (legislator.hasProperty("nickname")) {
                        firstNicknameDist = getEditDistance(
                                legislator.getProperty("nickname").toLowerCase(),
                                curFirstname);
                    }

                    if (lastnameDist > 0.3 && (firstnameDist > 0.3 && firstNicknameDist > 0.3)) {
                        if (verbose) {
                            System.out.println("\t\tDetecting a mismatch, check if reasonable"
                                    + ". lastname: " + legLastname + " vs. " + curLastname
                                    + ". firstname: " + legFirstname + " vs. " + curFirstname
                                    + ". type: " + legislator.getType()
                                    + ". party: " + legislator.getParty()
                                    + ". state: " + legislator.getState());
                        }
                        continue;
                    }
                    matchLegislator = legislator;
                }

                if (matchLegislator == null) {
                    count++;
                    continue;
                }
                matchLegislator.addProperty(GTLegislator.ICPSRID, icpsrId);
                icpsrLegislatorMap.put(icpsrId, matchLegislator);
            }
            reader.close();
            if (verbose) {
                System.out.println("--- Number of Senators in "
                        + senFilepath + " don't match: " + count);
            }
        }

        if (verbose) {
            System.out.println("--- icpsr size = " + icpsrLegislatorMap.size());
        }
    }

    public void outputBillSummaries(File folder) throws Exception {
        if (verbose) {
            System.out.println("\nOutputing bill summaries " + folder);
        }

        BufferedWriter writer;
        IOUtils.createFolder(folder);
        for (GTBill bill : this.bills.values()) {
            File billSumFile = new File(folder, bill.getId());
            writer = IOUtils.getBufferedWriter(billSumFile);
            writer.write(bill.getSummary());
            writer.close();
        }
    }

    public void inputBillSummaries(File folder, HashMap<String, GTBill> billMap)
            throws Exception {
        if (verbose) {
            System.out.println("\nInputing bill summaries " + folder);
        }

        BufferedReader reader;
        String line;
        for (String billId : billMap.keySet()) {
            StringBuilder str = new StringBuilder();
            reader = IOUtils.getBufferedReader(new File(folder, billId));
            while ((line = reader.readLine()) != null) {
                str.append(" ").append(line);
            }
            reader.close();

            billMap.get(billId).setSummary(str.toString());
        }
    }

    public void outputBillSubjects(File file) throws Exception {
        if (verbose) {
            System.out.println("Outputing bill subjects " + file);
        }

        BufferedWriter writer = IOUtils.getBufferedWriter(file);
        for (GTBill bill : this.bills.values()) {
            writer.write(bill.getId() + "\t");
            for (String subject : bill.getSubjects()) {
                writer.write(subject + "\t");
            }
            writer.write("\n");
        }
        writer.close();
    }

    public void inputBillSubjects(File file, HashMap<String, GTBill> billMap)
            throws Exception {
        if (verbose) {
            System.out.println("\nInputing bill subjects " + file);
        }

        BufferedReader reader = IOUtils.getBufferedReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] sline = line.split("\t");
            String billId = sline[0];
            for (int i = 1; i < sline.length; i++) {
                billMap.get(billId).addSubject(sline[i]);
            }
        }
        reader.close();
    }

    public void outputBillTopics(File file) throws Exception {
        if (verbose) {
            System.out.println("\nOutputing bill topics from the Congressional"
                    + " Bill Project " + file);
        }

        BufferedWriter writer = IOUtils.getBufferedWriter(file);
        for (GTBill bill : this.bills.values()) {
            String majorTopicId = bill.getProperty(GTBill.MAJOR_TOPIC);
            String minorTopicId = bill.getProperty(GTBill.MINOR_TOPIC);

            String majorTopic = null;
            String minorTopic = null;
            if (majorTopicId != null) {
                majorTopic = policyAgendaCodebook.get(Integer.parseInt(majorTopicId));
            }
            if (minorTopicId != null) {
                minorTopic = policyAgendaCodebook.get(Integer.parseInt(minorTopicId));
            }

            writer.write(bill.getId()
                    + "\t" + majorTopicId
                    + "\t" + minorTopicId
                    + "\t" + majorTopic
                    + "\t" + minorTopic
                    + "\n");
        }
        writer.close();
    }

    public void inputBillTopics(File file, HashMap<String, GTBill> billMap)
            throws Exception {
        if (verbose) {
            System.out.println("\tInputing bill topics from " + file);
        }

        BufferedReader reader = IOUtils.getBufferedReader(file);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] sline = line.split("\t");
            String billId = sline[0];
            GTBill bill = billMap.get(billId);
            if (bill == null) {
                throw new RuntimeException("Bill " + billId + " not found");
            }
            String majorTopic = sline[3];
            if (majorTopic.equals("null")) {
                continue;
            }
            bill.addProperty(GTBill.MAJOR_TOPIC, sline[3]);
            bill.addProperty(GTBill.MINOR_TOPIC, sline[4]);
        }
        reader.close();
    }

    public void outputBills(File file) throws Exception {
        if (verbose) {
            System.out.println("\nOutputing bills " + file);
        }

        BufferedWriter writer = IOUtils.getBufferedWriter(file);
        for (String billId : this.bills.keySet()) {
            GTBill bill = this.bills.get(billId);
            writer.write(bill.getType()
                    + "\t" + bill.getNumber()
                    + "\t" + bill.getProperty(GTBill.MAJOR_TOPIC)
                    + "\t" + bill.getProperty(GTBill.MINOR_TOPIC)
                    + "\t" + bill.getTitle()
                    + "\t" + bill.getOfficialTitle()
                    + "\n");
        }
        writer.close();
    }

    public HashMap<String, GTBill> inputBills(File file) throws Exception {
        if (verbose) {
            System.out.println("\nInputing bills " + file);
        }

        HashMap<String, GTBill> billMap = new HashMap<String, GTBill>();
        BufferedReader reader = IOUtils.getBufferedReader(file);
        String line;
        String[] sline;
        while ((line = reader.readLine()) != null) {
            sline = line.split("\t");
            String type = sline[0];
            int number = Integer.parseInt(sline[1]);
            GTBill bill = new GTBill(type, number);
            if (!sline[2].equals("null")) {
                bill.addProperty(GTBill.MAJOR_TOPIC, sline[2]);
            }
            if (!sline[3].equals("null")) {
                bill.addProperty(GTBill.MINOR_TOPIC, sline[3]);
            }
            String title = sline[4];
            String officialTitle = sline[5];

            bill.setTitle(title);
            bill.setOfficialTitle(officialTitle);
            billMap.put(bill.getId(), bill);
        }
        reader.close();

        if (verbose) {
            System.out.println("--- Loaded " + billMap.size() + " bills");
        }
        return billMap;
    }

    public void outputLegislators(String filepath) throws Exception {
        if (verbose) {
            System.out.println("\nOutputing legislators with ICPSR IDs to " + filepath);
        }

        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (String icpsrId : this.icpsrLegislatorMap.keySet()) {
            GTLegislator legislator = this.icpsrLegislatorMap.get(icpsrId);
            String score1 = legislator.getProperty(NOMINATE_SCORE1);
            String score2 = legislator.getProperty(NOMINATE_SCORE2);
            if (score1 == null) {
                score1 = EMPTY_SCORE;
                score2 = EMPTY_SCORE;
            }
            writer.write(legislator.getId()
                    + "\t" + legislator.getProperty(GTLegislator.ICPSRID)
                    + "\t" + legislator.getParty()
                    + "\t" + legislator.getType()
                    + "\t" + legislator.getState()
                    + "\t" + legislator.getDistrict()
                    + "\t" + score1
                    + "\t" + score2
                    + "\t" + legislator.getLastname()
                    + "\t" + legislator.getFirstname()
                    + "\t" + legislator.getMiddlename()
                    + "\t" + legislator.getDebateIds()
                    + "\n");
        }
        writer.close();
    }

    public HashMap<String, GTLegislator> inputLegislators(String inputFilepath)
            throws Exception {
        if (verbose) {
            System.out.println("\nInputing legislators from file " + inputFilepath);
        }
        this.legislators = new HashMap<String, GTLegislator>();
        BufferedReader reader = IOUtils.getBufferedReader(inputFilepath);
        String line;
        String[] sline;
        while ((line = reader.readLine()) != null) {
            sline = line.split("\t");
            String lid = sline[0];
            String icpsrid = sline[1];
            String party = sline[2];
            String type = sline[3];
            String state = sline[4];
            int district = Integer.parseInt(sline[5]);
            String scoreStr1 = (sline[6]);
            String scoreStr2 = (sline[7]);
            String lastname = sline[8];
            String firstname = sline[9];
            String middlename = sline[10];

            GTLegislator legislator = new GTLegislator(lid, lastname, firstname,
                    middlename, party, state, district);
            legislator.setType(type);
            legislator.addProperty(GTLegislator.ICPSRID, icpsrid);
            if (!scoreStr1.equals(EMPTY_SCORE)) {
                legislator.addProperty(NOMINATE_SCORE1, scoreStr1);
                legislator.addProperty(NOMINATE_SCORE2, scoreStr2);
            }
            this.legislators.put(lid, legislator);
        }
        reader.close();

        // info for debug
        int numRep = 0;
        int numSen = 0;
        for (GTLegislator l : this.legislators.values()) {
            if (l.getType().equals(GTLegislator.REP)) {
                numRep++;
            } else if (l.getType().equals(GTLegislator.SEN)) {
                numSen++;
            } else {
                System.out.println("--- Neither Representative nor Senator " + l.toString());
            }
        }
        if (verbose) {
            System.out.println("--- Loaded " + this.legislators.size() + " legislators");
            System.out.println("--- --- # rep = " + numRep);
            System.out.println("--- --- # sen = " + numSen);
        }
        return this.legislators;
    }

    private static double getEditDistance(String str1, String str2) {
        int dist = StringUtils.getLevenshteinDistance(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        return (double) dist / maxLength;
    }

    private static String getParty(int partycode) {
        if (partycode == 100) {
            return DEMOCRAT;
        } else if (partycode == 200) {
            return REPUBLICAN;
        } else {
            return INDEPENDENT;
        }
    }

    private static String[] getNames(String fullname) {
        String[] names = new String[3];
        String[] sfullname = fullname.split(",");
        if (sfullname.length < 2) {
            throw new RuntimeException("Error while processing name " + fullname);
        }

        names[LASTNAME] = sfullname[0].trim();

        String[] firstMiddleNames = sfullname[1].trim().split(" ");
        names[FIRSTNAME] = sfullname[1].trim().split(" ")[0].trim();
        if (firstMiddleNames.length > 1) {
            names[MIDDLENAME] = sfullname[1].split(" ")[1].trim();
        }
        return names;
    }

    /**
     * Process raw debate data files downloaded from GovTrack.
     */
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

        NodeList nodelist;
        Element el;
        int count = 0;
        for (String filename : debateFilenames) {
            if (count % 100 == 0 && verbose) {
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

            Element docEle;
            try {
                docEle = getDocumentElement(debateFile.getAbsolutePath());
            } catch (Exception e) {
                if (verbose) {
                    System.out.println("--- --- Skipping problematic debate file "
                            + debateFile);
                    e.printStackTrace();
                }
                continue;
            }
            String title = docEle.getAttribute("title");
            String where = docEle.getAttribute("where");
            String debateId = IOUtils.removeExtension(filename);
            GTDebate debate = new GTDebate(debateId);
            debate.setTitle(title);
            debate.addProperty("where", where);

            // list of bill mentions
            nodelist = docEle.getElementsByTagName("bill");
            for (int i = 0; i < nodelist.getLength(); i++) {
                el = (Element) nodelist.item(i);
                String billType = el.getAttribute("type");
                String billNumber = el.getAttribute("number");
                debate.addBillMentioned(billType + "-" + billNumber);
            }

            // list of turns
            GTTurn preTurn = null;
            int turnCount = 0;
            nodelist = docEle.getElementsByTagName("speaking");
            for (int i = 0; i < nodelist.getLength(); i++) {
                el = (Element) nodelist.item(i);
                String speaker = el.getAttribute("speaker");
                String topic = el.getAttribute("topic");

                // get the actual texts
                StringBuilder text = new StringBuilder();
                NodeList nl = el.getElementsByTagName("paragraph");
                for (int j = 0; j < nl.getLength(); j++) {
                    text.append(nl.item(j).getTextContent()).append(" ");
                }

                // merge consecutive turns are from the same speaker
                if (preTurn != null && preTurn.getSpeakerId().equals(speaker)) {
                    String preTurnText = preTurn.getText();
                    preTurnText += " " + text.toString();
                    preTurn.setText(preTurnText);
                } else { // or create a new turn
                    GTTurn turn = new GTTurn(
                            debateId + "_" + turnCount,
                            speaker,
                            text.toString()
                            .replaceAll("\n", " ")
                            .replace("nbsp", "")
                            .replace("&", ""));
                    if (!topic.trim().isEmpty()) {
                        turn.addProperty("topic", topic);
                    }
                    debate.addTurn(turn);
                    preTurn = turn;
                    turnCount++;
                }
            }
            this.debates.put(debateId, debate);
        }
        if (verbose) {
            System.out.println("--- Loaded " + debates.size() + " debates");
        }

        // debug
//        HashMap<Integer, ArrayList<GTDebate>> debateBillMentionCount =
//                new HashMap<Integer, ArrayList<GTDebate>>();
//        for (GTDebate debate : this.debates.values()) {
//            int numBillsMentioned = debate.getBillsMentioned().size();
//            ArrayList<GTDebate> ds = debateBillMentionCount.get(numBillsMentioned);
//            if (ds == null) {
//                ds = new ArrayList<GTDebate>();
//            }
//            ds.add(debate);
//            debateBillMentionCount.put(numBillsMentioned, ds);
//        }
//        int maxCount = -1;
//        for (int ii : debateBillMentionCount.keySet()) {
//            int c = debateBillMentionCount.get(ii).size();
//            System.out.println(ii + "\t" + c);
//            if (maxCount < ii) {
//                maxCount = ii;
//            }
//        }
//
//        System.out.println("maxC = " + maxCount);
//        ArrayList<GTDebate> maxDs = debateBillMentionCount.get(maxCount);
//        for (GTDebate maxD : maxDs) {
//            System.out.println(maxD.toString());
//        }
    }

    public void processBills() {
        File billFolder = new File(this.congressFolder, "bills");
        if (!billFolder.exists()) {
            throw new RuntimeException(billFolder + " not found.");
        }

        if (verbose) {
            System.out.println("\nProcessing bills " + billFolder);
        }

        this.bills = new HashMap<String, GTBill>();
        String[] billFilenames = billFolder.list();
        int count = 0;
        for (String billFilename : billFilenames) {
            if (count % 100 == 0 && verbose) {
                System.out.println("--- Processing bill file "
                        + count + " / " + billFilenames.length);
            }
            count++;

            File billFile = new File(billFolder, billFilename);
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

            // add bill
            this.bills.put(bill.getId(), bill);
        }

        // store the list of debates that discuss each bill
        for (GTDebate debate : this.debates.values()) {
            String debateId = debate.getId();

            Set<String> billsMentionedSet = new HashSet<String>();
            for (String billId : debate.getBillsMentioned()) {
                billsMentionedSet.add(billId);
            }

            for (String billId : billsMentionedSet) {
                GTBill bill = this.bills.get(billId);
                if (bill == null) {
                    continue;
                }
                bill.addDebateId(debateId);
            }
        }

        if (verbose) {
            System.out.println("--- Loaded " + this.bills.size() + " bills.");
        }
    }

    /**
     * Process raw roll files downloaded from GovTrack
     */
    public void processRolls() {
        File rollFolder = new File(this.congressFolder, "rolls");
        if (!rollFolder.exists()) {
            throw new RuntimeException(rollFolder + " not found");
        }

        if (verbose) {
            System.out.println("\nProcessing rolls " + rollFolder);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

        this.rolls = new HashMap<String, GTRoll>();
        String[] rollFilenames = rollFolder.list();
        int count = 0;
        int stepSize = MiscUtils.getRoundStepSize(rollFilenames.length, 10);
        for (String rollFilename : rollFilenames) {
            if (count % stepSize == 0 && verbose) {
                System.out.println("--- Processing roll file "
                        + count + " / " + rollFilenames.length);
            }
            count++;

            File rollFile = new File(rollFolder, rollFilename);
            Element docEle;
            try {
                docEle = getDocumentElement(rollFile.getAbsolutePath());
            } catch (Exception e) {
                if (verbose) {
                    System.out.println("--- --- Skipping problematic roll file "
                            + rollFile);
                    e.printStackTrace();
                }
                continue;
            }
            NodeList nodelist;
            Element element;

            // create new roll
            String rollId = IOUtils.removeExtension(rollFilename);
            GTRoll roll = new GTRoll(rollId);

            roll.setWhere(docEle.getAttribute("where"));
            roll.setRoll(Integer.parseInt(docEle.getAttribute("roll")));

            // datetime
            String datetime = docEle.getAttribute("datetime").replaceAll("T", " ");
            int lastHyphenIndex = datetime.lastIndexOf("-");
            datetime = datetime.substring(0, lastHyphenIndex);
            long timeInMillisSinceEpoch = 0L;
            try {
                timeInMillisSinceEpoch = sdf.parse(datetime).getTime();
            } catch (Exception e) {
                System.out.println("Ill-formatted datetime.");
                e.printStackTrace();
            }
            long date = TimeUnit.MILLISECONDS.toMinutes(timeInMillisSinceEpoch);
            roll.setDate(date);

            // get bill associated with this
            nodelist = docEle.getElementsByTagName("bill");
            if (nodelist.getLength() == 0) {
                continue;
            }
            element = (Element) nodelist.item(0);
            String billType = element.getAttribute("type");
            int billNum = Integer.parseInt(element.getAttribute("number"));
            String billId = billType + "-" + billNum;
            GTBill bill = this.bills.get(billId);
            if (bill == null) {
                continue;
            }
            roll.setBillId(billId);
            roll.setTitle(bill.getOfficialTitle());
            bill.addRollId(roll.getId());

            // roll category
            nodelist = docEle.getElementsByTagName("category");
            element = (Element) nodelist.item(0);
            String category = element.getFirstChild().getNodeValue();
            roll.addProperty("category", category);

            // roll result
            nodelist = docEle.getElementsByTagName("result");
            element = (Element) nodelist.item(0);
            String result = element.getFirstChild().getNodeValue();
            roll.addProperty("result", result);

            nodelist = docEle.getElementsByTagName("voter");
            for (int i = 0; i < nodelist.getLength(); i++) {
                Element el = (Element) nodelist.item(i);
                String pid = el.getAttribute("id");
                String v = el.getAttribute("vote");
                roll.putVote(pid, v);
            }

            this.rolls.put(rollId, roll);
        }

        if (verbose) {
            System.out.println("--- Loaded " + rolls.size() + " votes");
        }
    }

    /**
     * Get the main vote associated with a given bill. This vote is selected as
     * follow
     *
     * 1. If the bill contains a vote having result as "Bill Passed", select it
     *
     * 2. If there is no "Bill Passed" vote, consider votes with category
     * "passage"
     *
     * 2a. If the bill contains no "passage" vote, discard it
     *
     * 2b. If the bill contains only one "passage" vote, select the vote
     *
     * 2c. If the bill contains more than one "passage" votes
     *
     * 2c1. Among these "passage" votes, if a vote starts with "On Passage"
     * select it
     *
     * 2c2. Otherwise, select none and return null
     *
     * @param bill The given bill
     */
    protected String getMainRollId(GTBill bill) {
        if (bill.getRollIds() == null) {
            return null;
        }

        String mainRollId = null;
        for (String rid : bill.getRollIds()) {
            String result = this.rolls.get(rid).getProperty("result");
            if (result.equals("Bill Passed")) {
                mainRollId = rid;
            }
        }

        if (mainRollId == null) {
            ArrayList<String> passageRollIds = new ArrayList<String>();
            for (String rid : bill.getRollIds()) {
                GTRoll roll = this.rolls.get(rid);
                if (roll.getProperty("category").equals("passage")) {
                    passageRollIds.add(rid);
                }
            }

            if (passageRollIds.isEmpty()) {
                return null;
            } else if (passageRollIds.size() == 1) {
                mainRollId = passageRollIds.get(0);
            } else {
                for (String rid : passageRollIds) {
                    GTRoll roll = this.rolls.get(rid);
                    if (roll.getTitle().startsWith("On Passage")) {
                        mainRollId = rid;
                        break;
                    }

                }
            }
        }

        return mainRollId;
    }

    /**
     * Select a subset of interesting debates, following the strategy suggested
     * by Thomas et. al. (EMNLP 06).
     */
    public ArrayList<GTDebate> selectDebates() throws Exception {
        if (verbose) {
            System.out.println("\nSelecting debates ...");
        }

        int count = 0;
        int hcount = 0;
        int scount = 0;

        // Debates are grouped according to bills. There might be multiple 
        // debates about the same bill.
        HashMap<String, ArrayList<GTDebate>> groupedDebateByBill = new HashMap<String, ArrayList<GTDebate>>();

        for (GTDebate debate : this.debates.values()) {
            // bill associated with this debate. A debate can be about more
            // than one bill. If there is no bill associated with this debate,
            // discard it. If more than one bill associated with it, choose
            // the bill mentioned the most during the debate. To break tie, take
            // the bill appeared first in the debate. (This is to follow the 
            // way convote did).
            String billId = debate.getBillAssociatedWith();

            if (billId == null) { // skip debate that has no bill associated with
                continue;
            }
            GTBill bill = this.bills.get(billId);
            if (bill == null) {
                continue;
            }
//            skip bills that have no topic label from the Congressional bill project
//            if (bill.getProperty("major") == null) { 
//                continue;
//            }
            if (bill.getRollIds() == null) { // skip debate that has no roll-call vote
                continue;
            }
            // select the main vote associated with the bill
            String rollId = this.getMainRollId(bill);
            if (rollId == null) { // if we can't select a main vote for this bill, discard it
                continue;
            }
            debate.setAssociatedVote(this.rolls.get(rollId));

            // group debates according to their associated bill
            ArrayList<GTDebate> groupedDebates = groupedDebateByBill.get(billId);
            if (groupedDebates == null) {
                groupedDebates = new ArrayList<GTDebate>();
            }
            groupedDebates.add(debate);
            groupedDebateByBill.put(billId, groupedDebates);

            // counts for debugging
            count++;
            if (bill.getType().startsWith("h")) {
                hcount++;
            } else if (bill.getType().startsWith("s")) {
                scount++;
            } else {
                System.out.println(debate.getId()
                        + ". " + bill.getId()
                        + ". " + bill.getType()
                        + ". " + rollId);
            }
        }

        if (verbose) {
            System.out.println("--- Done pre-selecting. Discard debates that");
            System.out.println("--- 1. have no bill associated with");
            System.out.println("--- 2. have no roll-call vote");
            System.out.println("--- 3. have no main vote for the associated bill "
                    + "(since for a bill, there might be multiple votes)");

            System.out.println("--- Pre-selected " + count + " debates. "
                    + "In House " + hcount + ". In Senate " + scount);
            System.out.println("--- Groups of debates " + groupedDebateByBill.size());
        }

        // select only interesting groups of debates by only keeping groups for
        // which at least 20% of the turns voted YEA and at least 20% of the turns
        // voted NAY
        hcount = 0;
        scount = 0;
        ArrayList<GTDebate> selectedDebates = new ArrayList<GTDebate>();
        for (String billId : groupedDebateByBill.keySet()) {
            ArrayList<GTDebate> groupedDebates = groupedDebateByBill.get(billId);
            int yeaCount = 0;
            int nayCount = 0;
            for (GTDebate debate : groupedDebates) {
                GTRoll roll = debate.getAssociatedRoll();
                for (GTTurn turn : debate.getTurns()) {
                    String speakerId = turn.getSpeakerId();
                    String vote = roll.getVote(speakerId);
                    if (vote == null) {
                        continue;
                    }
                    if (vote.equals(GTRoll.YEA)) {
                        yeaCount++;
                    } else if (vote.equals(GTRoll.NAY)) {
                        nayCount++;
                    }
                }
            }

            // only consider interesting groups of debates
            double ratio = (double) yeaCount / (yeaCount + nayCount);
            if (ratio > 0.2 && ratio < 0.8) {
                for (GTDebate debate : groupedDebates) {
                    selectedDebates.add(debate);

                    if (debate.getId().startsWith("h")) {
                        hcount++;
                    } else if (debate.getId().startsWith("s")) {
                        scount++;
                    } else {
                        System.out.println("Neither House nor Senate: " + debate.getId());
                    }
                }
            }
        }

        if (verbose) {
            System.out.println("--- Done selecting. Only keep interesting debates that"
                    + " have the YeaToNayRatio in the range (0.2, 0.8). This range was"
                    + " also used in Thomas et al's EMNLP 06 paper.");
            System.out.println("--- Final selected debates: " + selectedDebates.size());
            System.out.println("--- House debates = " + hcount + ". Senate debates = " + scount);
        }
        return selectedDebates;
    }

    public void outputSelectedDebateTurns(
            String outputFolderpath,
            ArrayList<GTDebate> selectedDebates) throws Exception {
        if (verbose) {
            System.out.println("\nOutputing selected debates to folder " + outputFolderpath);
        }

        File debateTextFolder = new File(outputFolderpath, "texts");
        File debateInfoFolder = new File(outputFolderpath, "info");

        IOUtils.createFolder(debateTextFolder);
        IOUtils.createFolder(debateInfoFolder);

        BufferedWriter writer;
        for (GTDebate debate : selectedDebates) {
            // output debate texts
            writer = IOUtils.getBufferedWriter(new File(debateTextFolder, debate.getId() + ".txt"));
            for (GTTurn turn : debate.getTurns()) {
                writer.write(turn.getSpeakerId() + ":\t" + turn.getText() + "\n");
            }
            writer.close();

            // output debate info
            writer = IOUtils.getBufferedWriter(new File(debateInfoFolder, debate.getId() + ".info"));
            GTRoll roll = debate.getAssociatedRoll();
            writer.write(roll.getId()
                    + "\t" + roll.getWhere()
                    + "\t" + roll.getRoll()
                    + "\t" + roll.getBillId()
                    + "\t" + roll.getProperty("category")
                    + "\t" + roll.getProperty("result")
                    + "\t" + roll.getTitle()
                    + "\n");
            for (String pid : roll.getVotes().keySet()) {
                writer.write(pid + "\t" + roll.getVote(pid) + "\n");
            }
            writer.close();
        }
    }

    public ArrayList<GTDebate> inputDebates(File inputFolder) throws Exception {
        File debateTextFolder = new File(inputFolder, "texts");
        File debateInfoFolder = new File(inputFolder, "info");
        if (!debateTextFolder.exists() || !debateInfoFolder.exists()) {
            throw new RuntimeException("Exception while loading debates. "
                    + debateTextFolder.getAbsolutePath()
                    + " and "
                    + debateInfoFolder.getAbsolutePath());
        }

        if (verbose) {
            System.out.println("\nInputing debates from folder " + debateTextFolder
                    + " and " + debateInfoFolder);
        }

        String[] filenames = debateTextFolder.list();
        BufferedReader reader;
        String line;
        String[] sline;
        ArrayList<GTDebate> debateList = new ArrayList<GTDebate>();
        for (String filename : filenames) {
            String debateId = IOUtils.removeExtension(filename);
            GTDebate debate = new GTDebate(debateId);

            // read in the debate texts
            reader = IOUtils.getBufferedReader(new File(debateTextFolder, filename));
            int count = 0;
            while ((line = reader.readLine()) != null) {
                sline = line.split(":\t");
                if (sline.length < 2) {
                    System.out.println(line + ". " + filename);
                }
                String lid = sline[0];
                String text = sline[1];
                GTTurn turn = new GTTurn(debateId + "_" + count, lid, text);
                count++;

                debate.addTurn(turn);
            }
            reader.close();

            // read in debate metadata
            reader = IOUtils.getBufferedReader(new File(debateInfoFolder, debateId + ".info"));
            line = reader.readLine(); // first line
            sline = line.split("\t");
            String rid = sline[0];
            String where = sline[1];
            int rollNum = Integer.parseInt(sline[2]);
            String billId = sline[3];
            String category = sline[4];
            String result = sline[5];
            String title = sline[6];
            for (int i = 7; i < sline.length; i++) {
                title += " " + sline[i];
            }
            GTRoll roll = new GTRoll(rid);
            roll.setWhere(where);
            roll.setBillId(billId);
            roll.setRoll(rollNum);
            roll.addProperty("category", category);
            roll.addProperty("result", result);
            roll.setTitle(title);

            while ((line = reader.readLine()) != null) {
                sline = line.split("\t");
                String pid = sline[0];
                String vote = sline[1];
                roll.putVote(pid, vote);
            }
            reader.close();

            debate.setAssociatedVote(roll);
            debateList.add(debate);
        }

        if (verbose) {
            System.out.println("--- Loaded " + debateList.size() + " debates");

            int houseNumDebates = 0;
            int senateNumDebates = 0;
            double houseAvgNumTurns = 0.0;
            double senateAvgNumTurns = 0.0;

            for (GTDebate debate : debateList) {
                if (debate.getId().startsWith("h")) {
                    houseNumDebates++;
                    houseAvgNumTurns += debate.getNumTurns();
                } else if (debate.getId().startsWith("s")) {
                    senateNumDebates++;
                    senateAvgNumTurns += debate.getNumTurns();
                } else {
                    System.out.println("--- --- Neither house nor senate " + debate.getId());
                }
            }
            System.out.println("--- # House debates  = " + houseNumDebates
                    + ". Avg num turns = " + (houseAvgNumTurns / houseNumDebates));
            System.out.println("--- # Senate debates = " + senateNumDebates
                    + ". Avg num turns = " + (senateAvgNumTurns / senateNumDebates));
        }

        return debateList;
    }

    public void loadTeaPartyAnnotations(String file) throws Exception {
        if (verbose) {
            System.out.println("Loading Tea Party annotation from " + file);
        }

        if (this.legislators == null) {
            throw new RuntimeException("Legislator list is null");
        }

        HashMap<String, GTLegislator> icpsrIDMap = new HashMap<String, GTLegislator>();
        for (GTLegislator legislator : this.legislators.values()) {
            String icpsrId = legislator.getProperty(GTLegislator.ICPSRID);
            if (icpsrId == null) {
                continue;
            }
            icpsrIDMap.put(icpsrId, legislator);
        }

        BufferedReader reader = IOUtils.getBufferedReader(file);
        String line;
        String[] sline;
        reader.readLine(); // header
        int count = 0;
        while ((line = reader.readLine()) != null) {
            sline = line.split("\t");
            if (sline.length == 0) {
                break;
            }

            String icpsrId = sline[0];
            String freshmen = sline[6];
            String tpScore = sline[11];
            String fwScore = sline[14];

            GTLegislator legislator = icpsrIDMap.get(icpsrId);
            if (legislator == null) {
                continue;
            }

            legislator.addProperty(GTLegislator.FRESHMEN, freshmen);
            legislator.addProperty(GTLegislator.TP_SCORE, tpScore);
            legislator.addProperty(GTLegislator.FW_SCORE, fwScore);
            count++;
        }
        reader.close();
        if (verbose) {
            System.out.println("--- Load Tea Party annotations. " + count);
        }
    }

    public void debug() throws Exception {
        System.out.println("Debugging ...");

        int count = 0;
        int hcount = 0;
        int scount = 0;
        int mainIdCount = 0;
        int nomainIdCount = 0;
        int nomainIdNotFoundCount = 0;

        System.out.println(">>> # debates: " + this.debates.size());

        for (GTDebate debate : this.debates.values()) {
            // 1. choose interesting debate
            // a. number of turns > n
            // b. at least a certain number of legislators from both parties 
            // participated in the debate
            if (debate.getTurns().size() > 5) {
                continue;
            }

            // 2. bill associated with this debate. A debate can be about more
            // than one bill. If there is no bill associated with this debate,
            // discard it. If more than one bill associated with it, choose
            // the bill mentioned the most during the debate. To break tie, take
            // the bill appeared first in the debate. (This is to follow the 
            // way convote did).
            String billId = debate.getBillAssociatedWith();
            if (billId == null) { // skip debate that has no bill associated with
                continue;
            }
            GTBill bill = this.bills.get(billId);
            if (bill == null) {
                continue;
            }
            if (bill.getRollIds() == null) { // skip if the associated bill has no roll-call vote
                continue;
            }
            count++;

            // 3. vote associated with the bill
            String rollId = this.getMainRollId(bill);
            if (rollId == null) {
                nomainIdCount++;
                if (bill.getRollIds() != null) {
                    nomainIdNotFoundCount++;

                    System.out.println("bill " + bill.getId() + ": " + bill.getTitle());
                    for (String rid : bill.getRollIds()) {
                        GTRoll roll = this.rolls.get(rid);
                        System.out.println("\t" + roll.getId()
                                + ". " + roll.getProperty("category")
                                + ", " + roll.getTitle());
                    }
                    System.out.println();
                }
            } else {
                mainIdCount++;
                if (bill.getType().startsWith("h")) {
                    hcount++;
                } else if (bill.getType().startsWith("s")) {
                    scount++;
                }
            }
        }

        System.out.println("total number of bills considered = " + count);
        System.out.println("hcount = " + hcount + ". scount = " + scount);
        System.out.println("mainIdCount = " + mainIdCount
                + ". noMainIdCount = " + nomainIdCount
                + ". notfound = " + nomainIdNotFoundCount);
    }

    protected Element getDocumentElement(String filepath) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        //Using factory get an instance of document builder
        DocumentBuilder db = dbf.newDocumentBuilder();
        //parse using builder to get DOM representation of the XML file
        Document dom = db.parse(filepath);
        Element docEle = dom.getDocumentElement(); //get the root element
        return docEle;
    }
    protected static String[] stateMaps = {
        "41 AL ALABAMA",
        "81 AK ALASKA",
        "61 AZ ARIZONA",
        "42 AR ARKANSAS",
        "71 CA CALIFORNIA",
        "62 CO COLORADO",
        "01 CT CONNECTICUT",
        "11 DE DELAWARE",
        "43 FL FLORIDA",
        "44 GA GEORGIA",
        "82 HI HAWAII",
        "63 ID IDAHO",
        "21 IL ILLINOIS",
        "22 IN INDIANA",
        "31 IA IOWA",
        "32 KS KANSAS",
        "51 KY KENTUCKY",
        "45 LA LOUISIANA",
        "02 ME MAINE",
        "52 MD MARYLAND",
        "03 MA MASSACHUSETTS",
        "23 MI MICHIGAN",
        "33 MN MINNESOTA",
        "46 MS MISSISSIPPI",
        "34 MO MISSOURI",
        "64 MT MONTANA",
        "35 NE NEBRASKA",
        "65 NV NEVADA",
        "04 NH NEW HAMSHIRE",
        "12 NJ NEW JERSEY",
        "66 NM NEW MEXICO",
        "13 NY NEW YORK",
        "47 NC NORTH CAROLINA",
        "36 ND NORTH DAKOTA",
        "24 OH OHIO",
        "53 OK OKLAHOMA",
        "72 OR OREGON",
        "14 PA PENNSYLVANIA",
        "05 RI RHODE ISLAND",
        "48 SC SOUTH CAROLINA",
        "37 SD SOUTH DAKOTA",
        "54 TN TENNESSEE",
        "49 TX TEXAS",
        "67 UT UTAH",
        "06 VT VERMONT",
        "40 VA VIRGINIA",
        "73 WA WASHINGTON",
        "56 WV WEST VIRGINA",
        "25 WI WISCONSIN",
        "68 WY WYOMING",
        "55 DC DISTRICT OF COLUMBIA"
    };
}
