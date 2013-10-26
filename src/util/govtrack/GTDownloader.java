package util.govtrack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import util.IOUtils;
import util.MiscUtils;

/**
 *
 * @author vietan
 */
public class GTDownloader {

    public static final String GOVTRACK_URL = "https://www.govtrack.us/data/us/";
    private int congressNumber;
    private File congressFolder;
    private URL congressURL;

    public GTDownloader(String folder, int congNum) {
        this.congressNumber = congNum;
        try {
            this.congressFolder = new File(folder, Integer.toString(congressNumber));
            this.congressURL = new URL(new URL(GOVTRACK_URL), Integer.toString(congressNumber));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception while initializing GTDownloader");
        }
        IOUtils.createFolder(this.congressFolder);
    }

    public void downloadBillHtmls(HashMap<String, GTBill> bills) throws Exception {
        System.out.println("Downloading bill htmls. # bills: " + bills.size());
        IOUtils.createFolder(new File(congressFolder, "bills.html"));
        int count = 0;
        int downloadCount = 0;
        for (GTBill bill : bills.values()) {
            if (count % 100 == 0) {
                System.out.println("\n--- bill " + count + " / " + bills.size());
            }
            count++;
            String billType = bill.getType();
            int billNumber = bill.getNumber();
            boolean success = downloadBillHtml(congressNumber, billType, billNumber);
            if (success) {
                downloadCount++;
            }
        }
        System.out.println("--- Downloaded " + downloadCount + " bill htmls");
    }

    /**
     * Read a HTML file, strip all HTML tags and output the text
     */
    private boolean downloadBillHtml(int congressNo, String billType, int billNumber) throws Exception {
        String billId = billType + billNumber;

        File outFile = new File(new File(this.congressFolder, "bills.html"), billId + ".txt");
        if (outFile.exists()) {
            System.out.println("--- Skipping already downloaded file");
            return true;
        }

        String urlString = GOVTRACK_URL + "bills.text/" + congressNo + "/" + billType + "/" + billId + ".html";
        System.out.println("Downloading from URL: " + urlString);
        URL url = new URL(urlString);
        if (!urlExists(url)) {
            System.out.println("--- URL does not exist: " + urlString);
            return false;
        }

        // read the html file
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder rawContent = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            rawContent.append(line).append("\n");
        }
        in.close();

        // strip html tags
        String strippedContent = removeHTML(rawContent.toString());

        // write text
        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
        writer.write(strippedContent.trim());
        writer.close();

        return true;
    }

    public static String removeHTML(String htmlString) {
        String noHTMLString = htmlString.replaceAll("\\<.*?\\>", " ");
        return StringEscapeUtils.unescapeHtml4(StringEscapeUtils.unescapeHtml3(noHTMLString));
    }

    public void downloadBillTexts(HashMap<String, GTBill> bills) throws Exception {
        System.out.println("Downloading bill texts. # bills: " + bills.size());
        int count = 0;
        int downloadCount = 0;
        for (GTBill bill : bills.values()) {
            if (count % 100 == 0) {
                System.out.println("\n--- bill " + count + " / " + bills.size());
            }
            count++;
            String billType = bill.getType();
            int billNumber = bill.getNumber();
            boolean success = downloadBillText(congressNumber, billType, billNumber);
            if (success) {
                downloadCount++;
            }
        }
        System.out.println("--- Downloaded " + downloadCount + " bill texts");
    }

    private boolean downloadBillText(int congressNo, String billType, int billNumber) throws Exception {
        String billId = billType + billNumber;

        File outFile = new File(new File(this.congressFolder, "bills.text"), billId + ".txt");
        if (outFile.exists()) {
            System.out.println("--- Skipping already downloaded file");
            return true;
        }

        String urlString = GOVTRACK_URL + "bills.text/" + congressNo + "/" + billType + "/" + billId + ".txt";
        System.out.println("Downloading from URL: " + urlString);
        URL url = new URL(urlString);
        if (!urlExists(url)) {
            System.out.println("--- URL does not exist: " + urlString);
            return false;
        }
        FileUtils.copyURLToFile(url, outFile);
        return true;
    }

    private boolean urlExists(URL url) throws Exception {
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        huc.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD"); 
        huc.connect();
        int code = huc.getResponseCode();
        if (code == 404) {
            return false;
        }
        return true;
    }

    public void downloadBills() throws Exception {
        System.out.println("Downloading files from " + GOVTRACK_URL + congressNumber + "/bills/");

        ArrayList<String> urls = getUrls(GOVTRACK_URL + congressNumber + "/" + "bills/");
        int count = 0;
        for (String urlFile : urls) {
            if (!urlFile.contains(".xml")) {
                continue;
            }
            System.out.println("--- Downloading bill " + urlFile
                    + ". " + count + " / " + urls.size());
            URL url = new URL(GOVTRACK_URL + congressNumber + "/bills/" + urlFile);
            File outFile = new File(new File(this.congressFolder, "bills"), urlFile);
            FileUtils.copyURLToFile(url, outFile);
            count++;
        }
        System.out.println("--- " + count + " files downloaded");
    }

    public void downloadRolls() throws Exception {
        System.out.println("Downloading files from " + GOVTRACK_URL + congressNumber + "/" + "rolls/");

        ArrayList<String> urls = getUrls(GOVTRACK_URL + congressNumber + "/" + "rolls/");
        int count = 0;
        for (String urlFile : urls) {
            if (!urlFile.contains(".xml")) {
                continue;
            }

            System.out.println("--- Downloading roll " + urlFile
                    + ". " + MiscUtils.formatDouble(100 * (double) count / urls.size())
                    + "%");
            URL url = new URL(GOVTRACK_URL + congressNumber + "/rolls/" + urlFile);
            File outFile = new File(new File(this.congressFolder, "rolls"), urlFile);
            FileUtils.copyURLToFile(url, outFile);
            count++;
        }
        System.out.println("--- " + count + " files downloaded");
    }

    public void downloadPeopleXML() throws Exception {
        URL url = new URL(this.congressURL, "people.xml");
        File outFile = new File(this.congressFolder, "people.xml");

        System.out.println("Downloading file " + url + " to " + outFile);
        FileUtils.copyURLToFile(url, outFile);
    }

    public void downloadCR() throws Exception {
        System.out.println("Downloading files from " + GOVTRACK_URL + congressNumber + "/" + "cr/");

        ArrayList<String> urls = getUrls(GOVTRACK_URL + congressNumber + "/" + "cr/");
        int count = 0;
        for (String urlFile : urls) {
            if (!urlFile.contains(".xml")) {
                continue;
            }

            System.out.println("--- Downloading cr " + urlFile
                    + ". " + MiscUtils.formatDouble(100 * (double) count / urls.size()));
            URL url = new URL(GOVTRACK_URL + congressNumber + "/cr/" + urlFile);
            File outFile = new File(new File(this.congressFolder, "cr"), urlFile);
            FileUtils.copyURLToFile(url, outFile);

            count++;
        }
        System.out.println("--- " + count + " files downloaded");
    }

    /**
     * Returns all urls found from a web page
     */
    public ArrayList<String> getUrls(String urlStr) throws Exception {
        System.out.println("Getting links from " + urlStr);

        ArrayList<String> urls = new ArrayList<String>();
        URL url = new URL(urlStr);
        Scanner in = new Scanner(url.openStream());
        while (in.hasNext()) {
            String line = in.nextLine();
            int hrefIndex = line.indexOf("href");
            if (hrefIndex == -1) {
                continue;
            }
            int startQuoteIndex = hrefIndex + 6;
            int endQuoteIndex = line.indexOf("\"", startQuoteIndex);
            urls.add(line.substring(startQuoteIndex, endQuoteIndex));
        }

        System.out.println("--- " + urls.size() + " urls retrieved");
        return urls;
    }
}
