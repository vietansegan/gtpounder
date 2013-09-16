package util;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author vietan
 */
public class IOUtils {

    public static ArrayList<String> loadVocab(String filepath) throws Exception {
        ArrayList<String> voc = new ArrayList<String>();
        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        while ((line = reader.readLine()) != null) {
            voc.add(line);
        }
        reader.close();
        return voc;
    }

    public static int[][] loadLDACFile(String filepath) throws Exception {
        BufferedReader reader = IOUtils.getBufferedReader(filepath);

        ArrayList<int[]> wordList = new ArrayList<int[]>();
        String line;
        String[] sline;
        while ((line = reader.readLine()) != null) {
            sline = line.split(" ");

            int numTypes = Integer.parseInt(sline[0]);
            int[] types = new int[numTypes];
            int[] counts = new int[numTypes];

            int numTokens = 0;
            for (int ii = 0; ii < numTypes; ++ii) {
                String[] entry = sline[ii + 1].split(":");
                int count = Integer.parseInt(entry[1]);
                int id = Integer.parseInt(entry[0]);
                numTokens += count;
                types[ii] = id;
                counts[ii] = count;
            }

            int[] gibbsString = new int[numTokens];
            int index = 0;
            for (int ii = 0; ii < numTypes; ++ii) {
                for (int jj = 0; jj < counts[ii]; ++jj) {
                    gibbsString[index++] = types[ii];
                }
            }
            wordList.add(gibbsString);
        }
        reader.close();
        int[][] words = wordList.toArray(new int[wordList.size()][]);
        return words;
    }

    public static ZipOutputStream getZipOutputStream(String outptuFile) throws Exception {
        File f = new File(outptuFile);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
        return out;
    }

    public static ZipInputStream getZipInputStream(String inputFile) throws Exception {
        File f = new File(inputFile);
        ZipInputStream in = new ZipInputStream(new FileInputStream(f));
        return in;
    }

    public static BufferedReader getBufferedReader(String filepath)
            throws FileNotFoundException, UnsupportedEncodingException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filepath), "UTF-8"));
        return in;
    }

    public static BufferedReader getBufferedReader(File file)
            throws FileNotFoundException, UnsupportedEncodingException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        return in;
    }

    public static BufferedWriter getBufferedWriter(String filepath)
            throws FileNotFoundException, UnsupportedEncodingException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "UTF-8"));
        return out;
    }

    public static BufferedWriter getBufferedWriter(File file)
            throws FileNotFoundException, UnsupportedEncodingException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        return out;
    }

    public static BufferedWriter getBufferedWriterAppend(String filepath)
            throws FileNotFoundException, UnsupportedEncodingException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, true), "UTF-8"));
        return out;
    }

    /**
     * Create a folder if it does not exist
     */
    public static void createFolder(String dir) {
        File folder = new File(dir);
        createFolder(folder);
    }

    /**
     * Create a folder if it does not exist
     */
    public static void createFolder(File folder) {
        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Method that makes an empty folder. If the folder does not exist, create
     * it.
     *
     * @param dir the String indicates the directory to the folder
     */
    public static void makeEmptyFolder(String dir) {
        try {
            File folder = new File(dir);
            if (!folder.exists()) {
                folder.mkdirs();
            } else {
                deleteFolderContent(dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Delete a file
     *
     * @param filepath The directory of the file to be deleted
     */
    public static void deleteFile(String filepath) {
        try {
            File aFile = new File(filepath);
            if (aFile.exists()) {
                aFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Method that deletes all the files in a given folder
     *
     * @param dir Directory of the folder to be deleted
     */
    public static void deleteFolderContent(String dir) {
        try {
            File folder = new File(dir);
            if (folder.isDirectory()) {
                String[] children = folder.list();
                for (int i = 0; i < children.length; i++) {
                    String tempFilename = dir.concat("/").concat(children[i]);
                    File tempF = new File(tempFilename);
                    tempF.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Method that returns all the file names in a folder
     *
     * @param dir Directory of the folder
     * @return A String array consists of all the file names in that folder
     */
    public static String[] getFilesFromFolder(String dir) {
        String[] subFolderName = null;
        try {
            File folder = new File(dir);
            if (folder.isDirectory()) {
                subFolderName = folder.list();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return subFolderName;
    }

    /**
     * Method that returns the file name without the extension
     *
     * @param oriFilename A file name with extension (eg: filename.ext)
     * @return The file name without the extension (eg: filename)
     */
    public static String removeExtension(String oriFilename) {
        int dotAt = oriFilename.lastIndexOf(".");
        return oriFilename.substring(0, dotAt);
    }

    /**
     * Return the file name from a given file path
     *
     * @param filepath The given file path
     * @return The file name
     */
    public static String getFilename(String filepath) {
        String[] dirs = filepath.split("/");
        return dirs[dirs.length - 1];
    }

    /**
     * Copy files from one folder to another
     */
    public static void copyFile(String sourceFile, String destinationFile) {
        try {
            File f1 = new File(sourceFile);
            File f2 = new File(destinationFile);
            InputStream in = new FileInputStream(f1);

            //For Overwrite the file.
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Output top words for each topic
     *
     * @param topicWordDistr 2D array containing topical word distributions
     * @param vocab List of tokens in the vocabulary
     * @param numTopWord Number of top words to output
     * @param filepath Path to the output file
     */
    public static void outputTopWords(double[][] topicWordDistr, ArrayList<String> vocab,
            int numTopWord, String filepath) throws Exception {

        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (int t = 0; t < topicWordDistr.length; t++) {
            // sort words
            double[] bs = topicWordDistr[t];
            ArrayList<RankingItem<Integer>> rankedWords = new ArrayList<RankingItem<Integer>>();
            for (int i = 0; i < bs.length; i++) {
                rankedWords.add(new RankingItem<Integer>(i, bs[i]));
            }
            Collections.sort(rankedWords);

            // output top words
            writer.write("Topic " + (t + 1));
            for (int i = 0; i < Math.min(numTopWord, vocab.size()); i++) {
                writer.write("\t" + vocab.get(rankedWords.get(i).getObject()));
            }
            writer.write("\n\n");
        }
        writer.close();
    }

    /**
     * Output top words for each topic
     *
     * @param topicWordDistr array list containing topical word distributions
     * @param vocab List of tokens in the vocabulary
     * @param numTopWord Number of top words to output
     * @param filepath Path to the output file
     */
    public static void outputTopWords(ArrayList<double[]> topicWordDistr, ArrayList<String> vocab,
            int numTopWord, String filepath) throws Exception {

        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (int t = 0; t < topicWordDistr.size(); t++) {
            // sort words
            double[] bs = topicWordDistr.get(t);
            ArrayList<RankingItem<Integer>> rankedWords = new ArrayList<RankingItem<Integer>>();
            for (int i = 0; i < bs.length; i++) {
                rankedWords.add(new RankingItem<Integer>(i, bs[i]));
            }
            Collections.sort(rankedWords);

            // output top words
            writer.write("Topic " + (t + 1));
            for (int i = 0; i < Math.min(numTopWord, vocab.size()); i++) {
                writer.write("\t" + vocab.get(rankedWords.get(i).getObject()));
            }
            writer.write("\n");
        }
        writer.close();
    }

    public static void outputLogLikelihoods(ArrayList<Double> logLhoods, String filepath)
            throws Exception {
        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (int i = 0; i < logLhoods.size(); i++) {
            writer.write(i + "\t" + logLhoods.get(i) + "\n");
        }
        writer.close();
    }

    public static ArrayList<RankingItem<String>> getSortedVocab(double[] distr, ArrayList<String> vocab) {
        if (distr.length != vocab.size()) {
            throw new RuntimeException("In IOUtils: dimensions mismatched");
        }
        ArrayList<RankingItem<String>> sortedVocab = new ArrayList<RankingItem<String>>();
        for (int i = 0; i < distr.length; i++) {
            sortedVocab.add(new RankingItem<String>(vocab.get(i), distr[i]));
        }
        Collections.sort(sortedVocab);
        return sortedVocab;
    }

    public static void outputTopWordsCummProbs(double[][] topicWordDistr, ArrayList<String> vocab,
            int numTopWord, String filepath) throws Exception {
        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (int t = 0; t < topicWordDistr.length; t++) {
            // sort words
            double[] bs = topicWordDistr[t];
            ArrayList<RankingItem<Integer>> rankedWords = new ArrayList<RankingItem<Integer>>();
            for (int i = 0; i < bs.length; i++) {
                rankedWords.add(new RankingItem<Integer>(i, bs[i]));
            }
            Collections.sort(rankedWords);

            // output top words
            writer.write("Topic " + (t + 1));
            double cumm_prob = 0;
            for (int i = 0; i < Math.min(numTopWord, vocab.size()); i++) {
                cumm_prob += rankedWords.get(i).getPrimaryValue();
                writer.write("\t" + vocab.get(rankedWords.get(i).getObject())
                        + ", " + rankedWords.get(i).getPrimaryValue()
                        + ", " + cumm_prob);
            }
            writer.write("\n");
        }
        writer.close();
    }

    /**
     * Output top words for each topic with indices
     *
     * @param topicIndices List of topic indices
     * @param topicWordDistr 2D array containing topical word distributions
     * @param vocab List of tokens in the vocabulary
     * @param numTopWord Number of top words to output
     * @param filepath Path to the output file
     */
    public static void outputTopWords(ArrayList<Integer> topicIndices,
            double[][] topicWordDistr, ArrayList<String> vocab,
            int numTopWord, String filepath) throws Exception {

        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (int t = 0; t < topicWordDistr.length; t++) {
            // sort words
            double[] bs = topicWordDistr[t];
            ArrayList<RankingItem<Integer>> rankedWords = new ArrayList<RankingItem<Integer>>();
            for (int i = 0; i < bs.length; i++) {
                rankedWords.add(new RankingItem<Integer>(i, bs[i]));
            }
            Collections.sort(rankedWords);

            // output top words
            writer.write("Topic " + topicIndices.get(t));
            for (int i = 0; i < Math.min(numTopWord, vocab.size()); i++) {
                writer.write("\t" + vocab.get(rankedWords.get(i).getObject()));
            }
            writer.write("\n");
        }
        writer.close();
    }

    /**
     * Output latent variable values
     *
     * @param distrs 2D array containing the variable values
     * @param filepath Path to the output file
     */
    public static void outputDistributions(double[][] distrs, String filepath)
            throws Exception {
        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        StringBuilder line;
        for (double[] var : distrs) {
            line = new StringBuilder();
            for (double v : var) {
                line.append(Double.toString(v)).append(" ");
            }
            writer.write(line.toString().trim() + "\n");
        }
        writer.close();
    }

    /**
     * Input latent variable values
     *
     * @param filepath Path to the input file
     */
    public static double[][] inputDistributions(String filepath)
            throws Exception {
        ArrayList<double[]> distr_list = new ArrayList<double[]>();
        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] sline = line.split(" ");
            double[] distr = new double[sline.length];
            for (int i = 0; i < distr.length; i++) {
                distr[i] = Double.parseDouble(sline[i]);
            }
            distr_list.add(distr);
        }
        reader.close();

        double[][] distrs = new double[distr_list.size()][];
        for (int i = 0; i < distrs.length; i++) {
            distrs[i] = distr_list.get(i);
        }
        return distrs;
    }

    public static void outputDistribution(double[] distr, String filepath)
            throws Exception {
        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (double d : distr) {
            writer.write(d + " ");
        }
        writer.close();
    }

    public static double[] inputDistribution(String filepath) throws Exception {
        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String[] sline = reader.readLine().split(" ");
        reader.close();
        double[] distr = new double[sline.length];
        for (int i = 0; i < distr.length; i++) {
            distr[i] = Double.parseDouble(sline[i]);
        }
        return distr;
    }

    /**
     * Output latent variable assignments
     */
    public static void outputLatentVariableAssignment(int[][] var, String filepath)
            throws Exception {
        StringBuilder outputLine;
        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        for (int[] var_line : var) {
            if (var_line.length == 0) {
                writer.write("\n");
            } else {
                outputLine = new StringBuilder();
                outputLine.append(Integer.toString(var_line.length)).append("\t");
                for (int v : var_line) {
                    outputLine.append(Integer.toString(v)).append(" ");
                }
                writer.write(outputLine.toString().trim() + "\n");
            }
        }
        writer.close();
    }

    /**
     * Input latent variable assignments
     */
    public static int[][] inputLatentVariableAssignment(String filepath)
            throws Exception {
        ArrayList<int[]> list = new ArrayList<int[]>();
        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        String[] sline;
        while ((line = reader.readLine()) != null) {
            if (line.equals("")) {
                list.add(new int[0]);
                continue;
            }

            sline = line.split("\t")[1].split(" ");
            int[] assignments = new int[sline.length];
            for (int i = 0; i < assignments.length; i++) {
                assignments[i] = Integer.parseInt(sline[i]);
            }
            list.add(assignments);
        }
        reader.close();

        int[][] latentVar = new int[list.size()][];
        for (int i = 0; i < latentVar.length; i++) {
            latentVar[i] = list.get(i);
        }
        return latentVar;
    }

    public static void outputLatentVariables(double[][] vars, String filepath)
            throws Exception {
        BufferedWriter writer = IOUtils.getBufferedWriter(filepath);
        StringBuilder line;
        for (double[] var : vars) {
            line = new StringBuilder();
            for (double v : var) {
                line.append(Double.toString(v)).append(" ");
            }
            writer.write(line.toString().trim() + "\n");
        }
        writer.close();
    }

    public static double[][] inputLatentVariables(String filepath)
            throws Exception {
        ArrayList<double[]> var_list = new ArrayList<double[]>();
        BufferedReader reader = IOUtils.getBufferedReader(filepath);
        String line;
        while ((line = reader.readLine()) != null) {
            String[] sline = line.split(" ");
            double[] distr = new double[sline.length];
            for (int i = 0; i < distr.length; i++) {
                distr[i] = Double.parseDouble(sline[i]);
            }
            var_list.add(distr);
        }
        reader.close();

        double[][] vars = new double[var_list.size()][];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = var_list.get(i);
        }
        return vars;
    }

    public static void metaSummarize(ArrayList<String> singleRunFilepaths, String outputFolderpath) throws Exception {
        BufferedReader reader;
        String line;
        String[] sline;
        HashMap<String, HashMap<String, ArrayList<Double>>> metaSummary =
                new HashMap<String, HashMap<String, ArrayList<Double>>>();
        ArrayList<String> measurementNames = new ArrayList<String>();
        ArrayList<String> modelNames = new ArrayList<String>();

        // input
        for (int j = 0; j < singleRunFilepaths.size(); j++) {
            String singleRunFilepath = singleRunFilepaths.get(j);
            reader = getBufferedReader(singleRunFilepath);

            // header - first line
            line = reader.readLine();
            sline = line.split("\t");
            if (metaSummary.isEmpty()) { // for the first file
                for (int i = 1; i < sline.length; i++) {
                    metaSummary.put(sline[i], new HashMap<String, ArrayList<Double>>());
                    measurementNames.add(sline[i]);
                }
            }

            // from 2nd line onwards
            while ((line = reader.readLine()) != null) {
                sline = line.split("\t");
                String modelName = sline[0];

                if (j == 0) {
                    modelNames.add(modelName);
                }

                for (int i = 1; i < sline.length; i++) {
                    double perfValue = Double.parseDouble(sline[i]);
                    String measurementName = measurementNames.get(i - 1);

                    HashMap<String, ArrayList<Double>> measurementTable =
                            metaSummary.get(measurementName);
                    ArrayList<Double> modelPerfList = measurementTable.get(modelName);
                    if (modelPerfList == null) {
                        modelPerfList = new ArrayList<Double>();
                    }
                    modelPerfList.add(perfValue);
                    measurementTable.put(modelName, modelPerfList);
                    metaSummary.put(measurementName, measurementTable);
                }
            }
            reader.close();
        }

        // output
        BufferedWriter writer;
        for (String measurement : metaSummary.keySet()) {
            writer = getBufferedWriter(outputFolderpath + measurement + ".txt");
            HashMap<String, ArrayList<Double>> measurementTable = metaSummary.get(measurement);

//            for(String modelName : modelNames){
//                writer.write(modelName);
//                ArrayList<Double> values = measurementTable.get(modelName);
//                for(Double value : values)
//                    writer.write("\t" + value);
//                writer.write("\n");
//            }

            // write header
            for (int i = 0; i < modelNames.size(); i++) {
                writer.write(modelNames.get(i) + "\t");
            }
            writer.write("\n");

            // write contents
            for (int j = 0; j < measurementTable.get(modelNames.get(0)).size(); j++) {
                for (int i = 0; i < modelNames.size(); i++) {
                    writer.write(measurementTable.get(modelNames.get(i)).get(j) + "\t");
                }
                writer.write("\n");
            }
            writer.close();
        }
    }
}
