package cz.artique.jade.bookTrader;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import cz.artique.jade.bookTrader.ontology.BookInfo;
import cz.artique.jade.bookTrader.ontology.BookOntology;
import cz.artique.jade.bookTrader.ontology.ChooseFrom;
import cz.artique.jade.bookTrader.ontology.Chosen;
import cz.artique.jade.bookTrader.ontology.MakeTransaction;
import cz.artique.jade.bookTrader.ontology.Offer;

// vlastni chovani, ktere se stara o opratreni knihy
class ObtainBook extends ContractNetInitiator {

    private Codec codec = new SLCodec();
    private Ontology onto = BookOntology.getInstance();

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Runnable callback;

    public ObtainBook(Agent a, ACLMessage cfp, Runnable callback) {
        super(a, cfp);
        this.callback = callback;
    }

    private Offer bestOffer; // musime si pamatovat, co jsme nabidli
    private ArrayList<BookInfo> shouldReceive; // pamatujeme si, i co nabidl prodavajici nam

    // prodavajici nam posila nasi objednavku, zadame vlastni pozadavek na poslani platby
    @Override
    protected void handleInform(ACLMessage inform) {
        try {
            // vytvorime informace o transakci a posleme je prostredi
            MakeTransaction mt = new MakeTransaction();

            mt.setSenderName(myAgent.getName());
            mt.setReceiverName(inform.getSender().getName());
            mt.setTradeConversationID(inform.getConversationId());

            if (bestOffer.getBooks() == null)
                bestOffer.setBooks(new ArrayList<BookInfo>());

            mt.setSendingBooks(bestOffer.getBooks());
            mt.setSendingMoney(bestOffer.getMoney());

            if (shouldReceive == null)
                shouldReceive = new ArrayList<BookInfo>();

            mt.setReceivingBooks(shouldReceive);
            mt.setReceivingMoney(0.0);

            ServiceDescription sd = new ServiceDescription();
            sd.setType("environment");
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.addServices(sd);

            DFAgentDescription[] envs = DFService.search(myAgent, dfd);

            ACLMessage transReq = new ACLMessage(ACLMessage.REQUEST);
            transReq.addReceiver(envs[0].getName());
            transReq.setLanguage(codec.getName());
            transReq.setOntology(onto.getName());
            transReq.setReplyByDate(new Date(System.currentTimeMillis() + 5000));

            getAgent().getContentManager().fillContent(transReq, new Action(envs[0].getName(), mt));
            getAgent().addBehaviour(new SendBook(myAgent, transReq));
            callback.run();
        } catch (UngroundedException e) {
            e.printStackTrace();
        } catch (OntologyException e) {
            e.printStackTrace();
        } catch (Codec.CodecException e) {
            e.printStackTrace();
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // zpracovani nabidek od prodavajicich
    @Override
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        double bestFitness = Double.MAX_VALUE;
        bestOffer = null;
        ACLMessage bestResponse = null;
        shouldReceive = null;

        // find best
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
                    if (o.getMoney() > Library.LIBRARY.getMoney()) {
                        continue;
                    }

                    double fitness = o.getMoney();
                    boolean foundAll = true;
                    if (o.getBooks() != null) {
                        for (BookInfo bi : o.getBooks()) {
                            BookInfo bookInfoByBookName = Library.LIBRARY.getMyBookInfo(bi);
                            if (bookInfoByBookName != null) {
                                bi.setBookID(bookInfoByBookName.getBookID());
                                Double estimatedPrice = Library.LIBRARY.getEstimatedPrice(bi, false);
                                if (estimatedPrice == null) {
                                    foundAll = false;
                                    break;
                                }
                                fitness += estimatedPrice;
                            } else {
                                foundAll = false;
                                break;
                            }
                        }
                    }

                    if (foundAll) {
                        if (fitness < bestFitness) {
                            bestFitness = fitness;
                            bestOffer = o;
                            bestResponse = response;
                            shouldReceive = cf.getWillSell();
                        }
                    }
                }
            } catch (Codec.CodecException e) {
                e.printStackTrace();
            } catch (OntologyException e) {
                e.printStackTrace();
            }
        }

        // check best
        double estPrice = 0;
        if (shouldReceive != null) {
            for (BookInfo bi : shouldReceive) {
                Double est = Library.LIBRARY.getEstimatedPrice(bi, false);
                if (est != null) {
                    estPrice += est;
                } else {
                    bestResponse = null;
                }
            }
        } else {
            bestResponse = null;
        }

        if (estPrice <= bestFitness) {
            bestResponse = null;
        }

        // accept / reject
        for (Object funckingObject : responses) {
            ACLMessage response = (ACLMessage) funckingObject;
            ACLMessage acc = response.createReply();
            if (response.equals(bestResponse)) {
                acc.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                Chosen ch = new Chosen();
                ch.setOffer(bestOffer);
                try {
                    getAgent().getContentManager().fillContent(acc, ch);
                } catch (CodecException e) {
                    e.printStackTrace();
                } catch (OntologyException e) {
                    e.printStackTrace();
                }

            } else {
                acc.setPerformative(ACLMessage.REJECT_PROPOSAL);
            }
            acceptances.add(acc);
        }
    }
}