package cz.artique.jade.bookTrader;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import cz.artique.jade.bookTrader.Library.ListTest;
import cz.artique.jade.bookTrader.ontology.BookInfo;
import cz.artique.jade.bookTrader.ontology.Goal;

class RegularTradingBehaviour extends AbstractTradingBehaviour {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public RegularTradingBehaviour(Agent a, long period) {
        super(a, period);
    }

    @Override
    protected LinkedList<ArrayList<BookInfo>> getBooksToObtain() {
        ArrayList<BookInfo> profitable = new ArrayList<BookInfo>();
        for (Goal g : Library.LIBRARY.getGoals()) {
            BookInfo book = g.getBook();
            if (Library.LIBRARY.haveBookTimes(book) > 0) {
                continue;
            }
            Double estimatedPrice = Library.LIBRARY.getEstimatedPrice(book, true);
            if (estimatedPrice == null) {
                continue;
            }

            double profit = g.getValue() - estimatedPrice;
            if (profit > 0) {
                profitable.add(book);
            }
        }
        System.out.println("There are " + profitable.size() + " books.");

        ArrayList<ArrayList<BookInfo>> all = Library.genSubsets(profitable, 2, new ListTest<BookInfo>() {
            @Override
            public boolean test(List<BookInfo> test) {
                return !test.isEmpty();
            }
        });
        Collections.sort(all, new Comparator<ArrayList<BookInfo>>() {
            @Override
            public int compare(ArrayList<BookInfo> o1, ArrayList<BookInfo> o2) {
                return -Double.compare(Library.getTotalProfit(o1), Library.getTotalProfit(o2));
            }
        });

        return new LinkedList<ArrayList<BookInfo>>(all);
    }

    @Override
    protected void delegateMessage(ACLMessage buyBook) {
        getAgent().addBehaviour(new ObtainBook(myAgent, buyBook, new Runnable() {
            @Override
            public void run() {
                queue.clear();
            }
        }));
    }
}