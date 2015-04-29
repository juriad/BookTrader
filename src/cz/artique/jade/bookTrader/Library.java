package cz.artique.jade.bookTrader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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

    private ArrayList<BookInfo> books;

    private ArrayList<Goal> goals;

    private double money;

    public double getMoney() {
        return money;
    }

    public BookInfo getMyBookInfo(BookInfo bi) {
        knownBooks.add(bi.getBookName());
        for (BookInfo b : books) {
            if (b.getBookName().equals(bi.getBookName())) {
                return b;
            }
        }
        return null;
    }

    public void update(ArrayList<BookInfo> books, ArrayList<Goal> goals, double money) {
        this.books = books;
        for (BookInfo bi : books) {
            knownBooks.add(bi.getBookName());
        }

        this.goals = goals;
        for (Goal g : goals) {
            knownBooks.add(g.getBook().getBookName());
        }

        this.money = money;
    }

    public Goal getGoal(BookInfo bi) {
        knownBooks.add(bi.getBookName());
        for (Goal g : goals) {
            if (g.getBook().getBookName().equals(bi.getBookName())) {
                return g;
            }
        }
        return null;
    }

    public Double getEstimatedPrice(BookInfo bi, boolean onlyNetwork) {
        knownBooks.add(bi.getBookName());
        if (!onlyNetwork) {
            Goal goal = getGoal(bi);
            if (goal != null) {
                return goal.getValue();
            }
        }
        return estimates.get(bi.getBookName());
    }

    public void updateBookEstimate(BookInfo book, double minimumFitness) {
        knownBooks.add(book.getBookName());
        estimates.put(book.getBookName(), minimumFitness);
    }

    public ArrayList<Goal> getGoals() {
        return goals;
    }

    public int haveBookTimes(BookInfo book) {
        knownBooks.add(book.getBookName());
        int count = 0;
        for (BookInfo b : books) {
            if (b.getBookName().equals(book.getBookName())) {
                count++;
            }
        }
        return count;
    }

    public static interface ListTest<E> {
        boolean test(List<E> test);
    }

    public static <E> ArrayList<ArrayList<E>> genSubsets(ArrayList<E> books, int maxSize, ListTest<E> test) {
        ArrayList<ArrayList<E>> all = new ArrayList<ArrayList<E>>();
        genSubsets(all, new ArrayList<E>(), books, 0, maxSize, test);
        return all;
    }

    private static <E> void genSubsets(ArrayList<ArrayList<E>> collector, ArrayList<E> partial, ArrayList<E> books, int offset, int maxSize, ListTest<E> test) {
        if (offset >= books.size() || partial.size() >= maxSize) {
            if (test.test(partial)) {
                collector.add(partial);
            }
        } else {
            @SuppressWarnings("unchecked")
            ArrayList<E> clone = (ArrayList<E>) partial.clone();
            clone.add(books.get(offset));
            genSubsets(collector, partial, books, offset + 1, maxSize, test);
            genSubsets(collector, clone, books, offset + 1, maxSize, test);
        }
    }

    public ArrayList<BookInfo> getBooks() {
        return books;
    }

    public static double getTotalProfit(ArrayList<BookInfo> o) {
        double profit = 0;
        for (BookInfo bi : o) {
            double goalPrice = Library.LIBRARY.getEstimatedPrice(bi, false);
            double estimatedPrice = Library.LIBRARY.getEstimatedPrice(bi, true);
            profit += goalPrice - estimatedPrice;
        }
        return profit;
    }

    public static double getTotalPrice(ArrayList<BookInfo> o) {
        double profit = 0;
        for (BookInfo bi : o) {
            double goalPrice = Library.LIBRARY.getEstimatedPrice(bi, false);
            profit += goalPrice;
        }
        return profit;
    }

    public HashSet<String> getKnownBooks() {
        return knownBooks;
    }

}