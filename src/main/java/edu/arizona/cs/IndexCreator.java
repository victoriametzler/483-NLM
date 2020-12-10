package edu.arizona.cs;

import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.Stack;

public class IndexCreator {
    int choice;
    String indexFilePath = "";
    public StandardAnalyzer stdAnlyzr;
    Directory index;
    IndexWriterConfig config;
    IndexWriter writer;
    Stack<Boolean> stack = new Stack<Boolean>();
    File allResources = new File("src/main/resources/wiki-subset-20140602");

    public IndexCreator(int choice) {
        this.choice = choice;
        buildIndex();
    }

    public void buildIndex() {
        updateFilePath();

        Path path = FileSystems.getDefault().getPath(indexFilePath);
        File dir = new File(indexFilePath);
        if (!dir.exists()) {
            dir.mkdir();
        }

        try {
            index = FSDirectory.open(path);
            //updateDirectory();
                if (!DirectoryReader.indexExists(index)) {
                updateDirectory();
            } else {
                stdAnlyzr = new StandardAnalyzer();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void updateFilePath() {
        if (choice == 1) {
            indexFilePath = "src/main/resources/default";
        } else if (choice == 2) {
            indexFilePath = "src/main/resources/lemma";
        } else if (choice == 3) {
            indexFilePath = "src/main/resources/stemm";
        }
    }

    public void updateDirectory() throws IOException {
        stdAnlyzr = new StandardAnalyzer();
        config = new IndexWriterConfig(stdAnlyzr);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        writer = new IndexWriter(index, config);
        for (File f : allResources.listFiles()) {
            checkFiles(f);
        }
        writer.close();
    }

    public void checkFiles(File file) {
        try {
            Scanner scanner = new Scanner(file);
            readFile(scanner);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public String[] readFile(Scanner scanner) {
        //title, categories, body
        String[] info = new String[]{"", "", ""};
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.length() > 4) {
                info = checkTitle(info, line);
            } else if (line.startsWith("CATEGORIES:")) {
                info[2] = line.substring(12);
            } else if(line.startsWith("=") && line.endsWith("=")) {
                info = checkHeaders(info, line);
            } else {
                info = removeTPL(info, line);
            }
        }
        scanner.close();
        return info;
    }

    public String[] checkTitle(String[] info, String line) {
        if (line.contains("[[") && line.contains("]]")) {
            if (isNewTitle(info[0])) {
                parseData(info);
            }
            info[0] = line.substring(2, line.length()-2);
            if (!stack.empty()) {
                stack.clear();
            }
        }
        return info;
    }

    public boolean isNewTitle(String line) {
        return !line.equals("") && !line.contains("Image:") && !line.contains("File:");
    }

    public String[] checkHeaders(String[] info, String line) {
        while (line.length() > 2 && line.startsWith("=") && line.endsWith("=")) {
            line = line.substring(1, line.length()-1);
        }
        if (!line.equals("References") && !line.equals("See also") && !line.equals("External links") && !line.equals("Further reading") && !line.equals("Notes")) {
            info[2] += line + " ";
        }
        return info;
    }

    public String[] removeTPL(String[] info, String line) {
        int opentpl = line.indexOf("[tpl]");
        int closetpl = line.indexOf("[/tpl]");
        if (opentpl != -1) {
            while (opentpl != -1) {
                info[2] += line.substring(0, opentpl);
                info[2] += " ";
                line = line.substring(closetpl+6);
                opentpl = line.indexOf("[tpl]");
                closetpl = line.indexOf("[/tpl]");
            }
        } else {
            info[2] += line;
            info[2] += " ";
        }
        return info;
    }

    public void parseData(String[] info) {
        String title = info[0].toLowerCase();
        String categories = info[1].toLowerCase().trim();
        String header = info[2].toLowerCase().trim();
        StringBuilder tempCategory = new StringBuilder();
        StringBuilder tempHeader = new StringBuilder();
        if (info[1].isEmpty() || info[1].equals("")) {
            info[1] = ".";
        }
        info[2] = info[2].trim();
        if (info[2].isEmpty() || info[2].equals("")) {
            info[2] = ".";
        }
        if (choice == 1) { //none
            tempCategory.append(categories);
            tempHeader.append(header);
        } else if (choice == 2) { //lemma
            StringBuilder[] sb = lemmatize(info);
            tempCategory = sb[0];
            tempHeader = sb[1];
        } else if (choice == 3) { //stem
            StringBuilder[] sb = stemmize(info);
            tempCategory = sb[0];
            tempHeader = sb[1];
        }

        Document document = new Document();
        document.add(new StringField("title", title, Field.Store.YES));
        document.add(new TextField("categories", tempCategory.toString().trim(), Field.Store.YES));
        document.add(new TextField("text", title + " " + tempCategory.toString().trim() + " " + tempHeader.toString().trim(), Field.Store.YES));

        try {
            writer.addDocument(document);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public StringBuilder[] lemmatize(String[] info) {
        StringBuilder tempCat = new StringBuilder();
        StringBuilder tempHead = new StringBuilder();
        for (String l : new Sentence(info[2]).lemmas()) {
            tempHead.append(l + " ");
        }
        for (String l : new Sentence(info[1]).lemmas()) {
            tempCat.append(l + " ");
        }
        StringBuilder[] sb = new StringBuilder[]{tempCat, tempHead};
        return sb;
    }

    public StringBuilder[] stemmize(String[] info) {
        StringBuilder tempCat = new StringBuilder();
        StringBuilder tempHead = new StringBuilder();
        PorterStemmer ps = new PorterStemmer();
        for (String s : new Sentence(info[1]).words()) {
            ps.setCurrent(s);
            ps.stem();
            String stemmed = ps.getCurrent();
            tempCat.append(stemmed + " ");
        }
        for (String s : new Sentence(info[2]).words()) {
            ps.setCurrent(s);
            ps.stem();
            String stemmed = ps.getCurrent();
            tempHead.append(stemmed + " ");
        }
        StringBuilder[] sb = new StringBuilder[]{tempCat, tempHead};
        return sb;
    }

    public Directory getIndex() {
        return index;
    }
}