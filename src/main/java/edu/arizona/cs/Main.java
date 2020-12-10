package edu.arizona.cs;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    static IndexCreator index;
    static QueryEngine qe;
    public static void main(String[] args ) {
        boolean loop = true;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Hi! Welcome to Baby-IBM Watson!");
        while (loop) {
            printOption();
            String input = scanner.nextLine();
            int choice = Integer.valueOf(input);
            boolean validChoice = isValidChoice(input);
            if (!validChoice) {
                System.out.println("Try again. Pick a valid option, example: \n1\n");
                printOption();
                input = scanner.nextLine();
                choice = Integer.valueOf(input);
                validChoice = isValidChoice(input);
            }

            index = new IndexCreator(choice);
            printScoringMethod();
            input = scanner.nextLine();
            int scoreMethod = Integer.valueOf(input);
            validChoice = isValidChoice(input);
            if (!validChoice) {
                System.out.println("Try again. Pick a valid option, example: \n1\n");
                printScoringMethod();
                input = scanner.nextLine();
                choice = Integer.valueOf(input);
                validChoice = isValidChoice(input);
            }

            qe = new QueryEngine(index, choice, scoreMethod);
            printStatistics();
            loop = Integer.valueOf(scanner.nextLine()) == 1 ? true : false;

            try {
                index.getIndex().close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void printOption() {
        System.out.println("Do you want to use default (none),  lemmas, stemming?");
        System.out.println("Pick a choice: ");
        System.out.println(" (1) for default");
        System.out.println(" (2) for lemmatization");
        System.out.println(" (3) for stemming");
    }

    public static void printScoringMethod() {
        System.out.println("What scoring method would you like to use?");
        System.out.println("Pick a choice: ");
        System.out.println(" (1) for default (bm25)");
        System.out.println(" (2) for tf-idf");
        System.out.println(" (3) for boolean");
        System.out.println(" (4) for jelinek-mercer");
    }

    public static void printStatistics() {
        System.out.println("Mean Reciprocal Rank: " + qe.getMRR());
        System.out.println("% of correct answers with precision 1: " + qe.getPrecision());
        System.out.println("Again?");
        System.out.println("Pick a choice: ");
        System.out.println(" (1) for yes");
        System.out.println(" (2) for no");
    }

    public static boolean isValidChoice(String line) {
        int[] VALID_CHOICES = new int[]{1, 2, 3, 4};
        int intValue = Integer.valueOf(line);
        for (int num : VALID_CHOICES) {
            if (intValue == num) {
                return true;
            }
        } return false;
    }
}