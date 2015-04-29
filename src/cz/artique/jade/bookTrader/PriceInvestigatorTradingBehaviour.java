package cz.artique.jade.bookTrader;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.LinkedList;

import cz.artique.jade.bookTrader.ontology.BookInfo;

class PriceInvestigatorTradingBehaviour extends AbstractTradingBehaviour {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ArrayList<BookInfo> books;

    public PriceInvestigatorTradingBehaviour(Agent a, long period, ArrayList<BookInfo> books) {
        super(a, period);
        this.books = books;
    }

    @Override
    protected LinkedList<ArrayList<BookInfo>> getBooksToObtain() {
        LinkedList<ArrayList<BookInfo>> arrayList = new LinkedList<ArrayList<BookInfo>>();
        arrayList.add(books);
        return arrayList;
    }

    @Override
    protected void delegateMessage(ACLMessage buyBook) {
        getAgent().addBehaviour(new PriceReducerBehaviour(myAgent, buyBook, books.get(0)));
    }

}