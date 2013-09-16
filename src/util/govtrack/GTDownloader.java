package util.govtrack;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import util.IOUtils;
import util.MiscUtils;

/**
 *
 * @author vietan
 */
public class GTDownloader {

    public static final String GOVTRACK_URL = "http://www.govtrack.us/data/us/";
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
