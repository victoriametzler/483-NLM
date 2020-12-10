package edu.arizona.cs;

import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.tartarus.snowball.ext.DanishStemmer;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class QueryEngine {
    IndexCreator idx;
    int indexType;   //can be default, lemma, or stem
    int scoreMethod; // can be default, tf-idf, boolean, or jelinek-mercer
    int score = 0;
    int numCorrect = 0;
    double precision = 0.0;
    double rr = 0.0;
    double finalRR = 0.0;
    IndexSearcher ISearcher;
    IndexReader IReader;
    StandardAnalyzer stdAnlyzr = new StandardAnalyzer();

    public QueryEngine(IndexCreator idx, int indexType, int scoreMethod) {
        this.idx = idx;
        this.indexType = indexType;
        this.scoreMethod = scoreMethod;
        calculateScores();
    }

    public void calculateScores() {
        try {
            Scanner scanner = new Scanner((new File("src/main/resources/questions.txt")));
            readFile(scanner);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void readFile(Scanner scanner) {
        String category = "";
        String ans = "";
        String guess = "";
        while (scanner.hasNextLine()) {
            category = scanner.nextLine();
            guess = scanner.nextLine();
            ans = scanner.nextLine();
            scanner.nextLine();
            ArrayList<ResultClass> answers = parseQuery(category + " " + guess);
            score += 1;

            if (matches(answers, ans)) {
                numCorrect++;
                rr++;
            } else {
                updateMRR(ans, answers);
            }
        }
        precision = (double) numCorrect / score;
        finalRR = rr / score;
    }

    public ArrayList<ResultClass> parseQuery(String line) {
        ArrayList<ResultClass> answers = new ArrayList<>();
        stdAnlyzr = idx.stdAnlyzr;
        line = processChoices(line);
        int hits = 10;
        try {
            IReader = DirectoryReader.open(idx.index);
            ISearcher = new IndexSearcher(IReader);
            Query query = new QueryParser("text", stdAnlyzr).parse(line);
            processScoringMethod();

            TopDocs topDocs = ISearcher.search(query, hits);
            ScoreDoc[] matches = topDocs.scoreDocs;

            for (int i = 0; i < matches.length; i++) {
                int id = matches[i].doc;
                Document doc = ISearcher.doc(id);
                ResultClass rc = new ResultClass(doc, matches[i].score);
                answers.add(rc);
            }
            IReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return answers;
    }

    public boolean matches(ArrayList<ResultClass> answers, String ans) {
        System.out.println("curr: " + answers.get(0).DocName.get("title"));
        System.out.println("want: " + ans.toLowerCase());
        return (answers.size() > 0 && answers.get(0).DocName.get("title").toLowerCase().equals(ans.toLowerCase()));
    }

    public void updateMRR(String ans, ArrayList<ResultClass> answers) {
        for (int i = 0; i < answers.size(); i++) {
            if (answers.get(i).DocName.get("title").toLowerCase().equals(ans.toLowerCase())) {
                rr += (double) 1 / (i + 1);
                break;
            }
        }
    }

    public String processChoices(String guess) {
        String updatedGuess = "";
        if (indexType == 1) { //none
            for (String l : new Sentence(guess.toLowerCase()).lemmas()) {
                updatedGuess += l + " ";
            }
        } else if (indexType == 2) { //lemma
            PorterStemmer ps = new PorterStemmer();
            for (String s : new Sentence(guess.toLowerCase()).words()) {
                ps.setCurrent(s);
                ps.stem();
                String stem = ps.getCurrent();
                updatedGuess += stem + " ";
            }
        } else { //stem
            for (String skip : new Sentence(guess.toLowerCase()).words()) {
                updatedGuess += skip + " ";
            }
        }
        return updatedGuess;
    }

    public void processScoringMethod() {
        if (scoreMethod == 1) { //bm25
            ISearcher.setSimilarity(new BM25Similarity());
        } else if (scoreMethod == 2) { //tf-idf
            ISearcher.setSimilarity(new ClassicSimilarity());
        } else if (scoreMethod == 3) { //boolean
            ISearcher.setSimilarity(new BooleanSimilarity());
        } else { //jelinek-mercer
            ISearcher.setSimilarity(new LMJelinekMercerSimilarity((float) 0.69));
        }
    }

    public double getMRR() {
        return this.finalRR * 100;
    }

    public double getPrecision() {
        return this.precision * 100;
    }
}