package edu.arizona.cs;
import org.apache.lucene.document.Document;

public class ResultClass {
    Document DocName;
    double docScore = 0;

    public ResultClass(Document name, double docScore) {
        this.DocName = name;
        this.docScore = docScore;
    }
}
