package core;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Set;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import util.IOUtils;

/**
 *
 * @author vietan
 */
public abstract class AbstractTokenizeDataset extends AbstractDataset {

    public static final String wordVocabExt = ".wvoc";
    public static final String speakerVocabExt = ".svoc";
    public static final String numDocDataExt = ".dat";
    public static final String numSentDataExt = ".sent-dat";
    public static final String docIdExt = ".docid";
    public static final String docTextExt = ".text";
    public static final String docInfoExt = ".docinfo";
    protected String folder; // main folder of the dataset
    protected String formatFolder = "format/";
    protected Set<String> stopwords;
    protected Tokenizer tokenizer;
    protected CorpusProcessor corpProc;

    public AbstractTokenizeDataset(
            String name,
            String folder) {
        super(name);
        this.folder = folder;
        try {
            // initiate tokenizer
            InputStream tokenizeIn = new FileInputStream(CorpusProcessor.TokenizerFilePath);
            TokenizerModel tokenizeModel = new TokenizerModel(tokenizeIn);
            this.tokenizer = new TokenizerME(tokenizeModel);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public AbstractTokenizeDataset(
            String name, // dataset name
            String folder, // dataset folder
            CorpusProcessor corpProc// corpus processor
            ) {
        super(name);
        this.folder = folder;

        this.corpProc = corpProc;

        try {
            // initiate tokenizer
            InputStream tokenizeIn = new FileInputStream(CorpusProcessor.TokenizerFilePath);
            TokenizerModel tokenizeModel = new TokenizerModel(tokenizeIn);
            this.tokenizer = new TokenizerME(tokenizeModel);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected void outputDataPreprocessingConfigurations() throws Exception {
        BufferedWriter writer = IOUtils.getBufferedWriter(folder);
        writer.write(this.corpProc.getSettings() + "\n");
        writer.close();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getFolder() {
        return this.folder;
    }

    public String getDatasetFolderPath() {
        return this.folder + getName() + "/";
    }

    public String getFormatPath() {
        return this.getDatasetFolderPath() + formatFolder;
    }

    public void setFormatFolder(String ff) {
        this.formatFolder = ff;
    }
}
