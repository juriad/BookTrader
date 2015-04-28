package cz.artique.jade.bookTrader;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;

import cz.artique.jade.bookTrader.ontology.BookInfo;

class RegularTradingBehaviour extends AbstractTradingBehaviour {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public RegularTradingBehaviour(Agent a, long period) {
        super(a, period);
    }

    @Override
    protected ArrayList<BookInfo> getBooksToObtain() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void delegateMessage(ACLMessage buyBook) {
        getAgent().addBehaviour(new ObtainBook(myAgent, buyBook));
    }
}