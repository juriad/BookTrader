package cz.artique.jade.bookTrader;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.util.ArrayList;
import java.util.Vector;

import cz.artique.jade.bookTrader.ontology.BookInfo;
import cz.artique.jade.bookTrader.ontology.ChooseFrom;
import cz.artique.jade.bookTrader.ontology.Offer;

public class PriceReducerBehaviour extends ContractNetInitiator {

    private BookInfo book;

    public PriceReducerBehaviour(Agent a, ACLMessage cfp, BookInfo book) {
        super(a, cfp);
        this.book = book;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // zpracovani nabidek od prodavajicich
    @Override
    protected void handleAllResponses(Vector responses, Vector acceptances) {

        double minimumFitness = Double.MAX_VALUE;

        for (Object funckingObject : responses) {
            ACLMessage response = (ACLMessage) funckingObject;

            ContentElement ce = null;
            try {
                if (response.getPerformative() == ACLMessage.REFUSE) {
                    continue;
                }

                ce = getAgent().getContentManager().extractContent(response);

                ChooseFrom cf = (ChooseFrom) ce;
                ArrayList<Offer> offers = cf.getOffers();
                for (Offer o : offers) {
                    double fitness = o.getMoney();
                    boolean skipOffer = false;
                    if (o.getBooks() != null) {
                        for (BookInfo bi : o.getBooks()) {
                            Double est = Library.LIBRARY.getEstimatedPrice(bi, false);
                            if (est == null) {
                                skipOffer = true;
                                break;
                            } else {
                                fitness += est;
                            }
                        }
                    }
                    if (skipOffer) {
                        continue;
                    }
                    minimumFitness = fitness < minimumFitness ? fitness : minimumFitness;
                }
            } catch (Codec.CodecException e) {
                e.printStackTrace();
            } catch (OntologyException e) {
                e.printStackTrace();
            }

            ACLMessage acc = response.createReply();
            acc.setPerformative(ACLMessage.REJECT_PROPOSAL);
            acceptances.add(acc);
        }

        if (minimumFitness < Double.MAX_VALUE) {
            Library.LIBRARY.updateBookEstimate(book, minimumFitness);
        }
    }
}
