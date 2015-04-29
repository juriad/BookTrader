package cz.artique.jade.bookTrader;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import cz.artique.jade.bookTrader.ontology.BookInfo;

public class PriceScannerBehaviourPlanner extends TickerBehaviour {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private HashMap<String, PriceInvestigatorTradingBehaviour> map = new HashMap<String, PriceInvestigatorTradingBehaviour>();
    private long investigatorPeriod;

    public PriceScannerBehaviourPlanner(Agent a, long period, long investigatorPeriod) {
        super(a, period);
        this.investigatorPeriod = investigatorPeriod;
    }

    @Override
    protected void onTick() {
        HashSet<String> knownBooks = Library.LIBRARY.getKnownBooks();
        for (String string : knownBooks) {
            if (!map.containsKey(string)) {
                ArrayList<BookInfo> books = new ArrayList<BookInfo>();
                BookInfo bookInfo = new BookInfo();
                bookInfo.setBookName(string);
                books.add(bookInfo);
                getAgent().addBehaviour(new PriceInvestigatorTradingBehaviour(getAgent(), investigatorPeriod, books));
            }
        }
    }
}
