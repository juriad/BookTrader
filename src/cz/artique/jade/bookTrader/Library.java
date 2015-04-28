package cz.artique.jade.bookTrader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import cz.artique.jade.bookTrader.ontology.BookInfo;
import cz.artique.jade.bookTrader.ontology.Goal;

public enum Library {
    LIBRARY;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private HashMap<String, Double> estimates;

    private HashSet<String> knownBooks;

    private HashMap<String, BookInfo> bookInfos;

    private ArrayList<BookInfo> books;

    private ArrayList<Goal> goals;

    private double money;

    public double getMoney() {
        return money;
    }

    public BookInfo getBookInfoByBookName(String bookName) {
        for (BookInfo b : books) {
            if (b.getBookName().equals(bookName)) {
                return b;
            }
        }
        return null;
    }

    public void update(ArrayList<BookInfo> books, ArrayList<Goal> goals, double money) {
        this.books = books;
        this.goals = goals;
        this.money = money;
    }

    public boolean isInGoals(BookInfo bi) {
        for (Goal g : goals) {
            if (g.getBook().getBookName().equals(bi.getBookName())) {
                return true;
            }
        }
        return false;
    }

    public Double getEstimatedPrice(BookInfo bi) {
        // TODO Auto-generated method stub
        return null;
    }

    public void updateBookEstimate(BookInfo book, double minimumFitness) {
        if (isInGoals(book)) {
            
        }
    }

}