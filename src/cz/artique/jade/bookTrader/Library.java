package cz.artique.jade.bookTrader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import cz.artique.jade.bookTrader.ontology.BookInfo;
import cz.artique.jade.bookTrader.ontology.Goal;

public enum Library {
    LIBRARY;

    private static final double INVENTED_ESTIMATE_PRICE_DROP = 1.2;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private HashMap<String, Double> estimates = new HashMap<String, Double>();

    private HashMap<String, Double> inventedEstimates = new HashMap<String, Double>();

    private HashSet<String> knownBooks = new HashSet<String>();

    private ArrayList<BookInfo> books = new ArrayList<BookInfo>();

    private ArrayList<Goal> goals = new ArrayList<Goal>();

    private double money = 0;

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

    public double inventEstimate(BookInfo bi) {
        if (!inventedEstimates.containsKey(bi.getBookName())) {
            double price = 0;
            for (Goal g : goals) {
                price += g.getValue();
            }
            inventedEstimates.put(bi.getBookName(), price);
        } else {
            inventedEstimates.put(bi.getBookName(), inventedEstimates.get(bi.getBookName()) / INVENTED_ESTIMATE_PRICE_DROP);
        }
        return inventedEstimates.get(bi.getBookName());
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

    public static Double getTotalPrice(ArrayList<BookInfo> o, double coefForNonGoals) {
        double price = 0;
        for (BookInfo bi : o) {
            Double bookPrice = Library.LIBRARY.getEstimatedPrice(bi, false);
            if (bookPrice == null) {
                return null;
            }
            Goal goal = Library.LIBRARY.getGoal(bi);
            price += goal != null ? bookPrice : bookPrice * coefForNonGoals;
        }
        return price;
    }

    public HashSet<String> getKnownBooks() {
        return knownBooks;
    }

}