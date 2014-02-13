package util.freedomworks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.IOUtils;

/**
 *
 * @author vietan
 */
public class FWDownloader {
    
    public static final String FREEDOMWORKS_URL = "http://congress.freedomworks.org";
    private String congressType; // house or senate
    private int year;
    private FWYear yearVotes;
    
    public FWDownloader(String congressType, int year) {
        this.congressType = congressType;
        this.year = year;
    }
    
    public void output(File folder) throws Exception {
        this.output(folder.toString());
    }
    
    public void output(String folder) throws Exception {
        outputLegislator(new File(folder, year + "speaker-scores.txt"));
    }
    
    public void outputLegislator(File filepath) throws Exception {
        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (int lid : yearVotes.getLegislatorIDs()) {
            FWLegislator legislator = yearVotes.getLegislator(lid);
            int score = yearVotes.getLegislatorScore(lid);
            writer.write(lid
                    + "\t" + legislator.getProperty(FWLegislator.NAME)
                    + "\t" + legislator.getProperty(FWLegislator.ROLE)
                    + "\t" + score
                    + "\n");
        }
        writer.close();
    }
    
    public FWYear downloadFreedomWorksScores() throws Exception {
        yearVotes = new FWYear(year);
        
        String urlString = FREEDOMWORKS_URL + "/keyvotes/" + congressType + "/" + year + "/print";
        System.out.println("Downloading from URL: " + urlString);
        URL url = new URL(urlString);
        if (!urlExists(url)) {
            System.out.println("--- URL does not exist: " + urlString);
            return null;
        }

        // read the html file
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder rawContent = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            rawContent.append(line).append("\n");
        }
        in.close();
        
        Document doc = Jsoup.parse(rawContent.toString());

        // get keyvote descriptions
        getKeyBills(doc, yearVotes);

        // get actual votes
        getVotes(doc, yearVotes);
        
        System.out.println("# bills: " + yearVotes.getBills().size());
        System.out.println("# legislators: " + yearVotes.getLegislators().size());
        System.out.println("# votes: " + yearVotes.getVotes().size());
        
        return yearVotes;
    }
    
    private void getKeyBills(Document doc, FWYear yearVotes) {
        for (Element e : doc.select("div")) {
            if (e.attr("class").equals("keyvote-description")) {
                int bid = Integer.parseInt(e.attr("rel"));
                
                FWBill bill = yearVotes.getBill(bid);
                if (bill == null) {
                    bill = new FWBill(bid);
                }
                bill.addProperty(FWBill.FW_VOTE_PREFERRED, e.attr("data-preferred"));
                bill.addProperty(FWBill.TITLE, e.select("h4").first().text());
                
                for (Element ee : e.select("div")) {
                    String cls = ee.attr("class");
                    if (cls.equals("meta")) {
                        for (Element eee : ee.select("span")) {
                            String eeeClass = eee.attr("class");
                            if (eeeClass.equals("type")) {
                                bill.addProperty(FWBill.ROLL_CALL, eee.text());
                            } else if (eeeClass.equals("bill")) {
                                bill.addProperty(FWBill.BILL, eee.text());
                            }
                        }
                    } else if (cls.equals("summary")) {
                        bill.addProperty(FWBill.SUMMARY, ee.text());
                    }
                }
                yearVotes.putBill(bid, bill);
            }
        }
    }
    
    private void getVotes(Document doc, FWYear yearVotes) {
        Elements elements = doc.select("tr");
        for (Element e : elements) {
            String legIdStr = e.attr("rel").trim();
            if (legIdStr.isEmpty()) {
                continue;
            }
            int lid = Integer.parseInt(legIdStr);
            FWLegislator legislator = yearVotes.getLegislator(Integer.parseInt(legIdStr));
            if (legislator == null) {
                legislator = new FWLegislator(lid);
                yearVotes.putLegislator(lid, legislator);
            }
            
            for (Element ee : e.select("td")) {
                String eeClass = ee.attr("class");
                if (eeClass.equals("legislator clear-block")) {
                    String role = ee.select("span").first().text();
                    String name = ee.select("a").first().text().replaceAll(role, "");
                    legislator.addProperty(FWLegislator.ROLE, role);
                    legislator.addProperty(FWLegislator.NAME, name);
                } else if (eeClass.equals("votes")) {
                    for (Element eea : ee.select("a")) {
                        FWVote.VoteType vt = FWVote.getVoteType(eea.attr("class"));
                        int bid = Integer.parseInt(eea.attr("rel"));
                        FWBill bill = yearVotes.getBill(bid);
                        
                        FWVote vote = new FWVote(legislator, bill, year, vt);
                        yearVotes.addVote(vote);
                    }
                } else if (eeClass.equals("score")) {
                    String scoreStr = ee.select("span").first().text();
                    
                    int score = FWYear.NA_SCORE;
                    if (!scoreStr.equals("N/A")) {
                        score = Integer.parseInt(ee.select("span").first().text());
                    }
                    yearVotes.putLegislatorScore(lid, score);
                }
            }
        }
    }
    
    private static boolean urlExists(URL url) throws Exception {
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        huc.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD"); 
        huc.connect();
        int code = huc.getResponseCode();
        if (code == 404) {
            return false;
        }
        return true;
    }
}