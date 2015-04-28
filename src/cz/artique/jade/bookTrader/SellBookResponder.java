package cz.artique.jade.bookTrader;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.SSContractNetResponder;

import java.util.ArrayList;
import java.util.Date;

import cz.artique.jade.bookTrader.ontology.BookInfo;
import cz.artique.jade.bookTrader.ontology.BookOntology;
import cz.artique.jade.bookTrader.ontology.ChooseFrom;
import cz.artique.jade.bookTrader.ontology.Chosen;
import cz.artique.jade.bookTrader.ontology.MakeTransaction;
import cz.artique.jade.bookTrader.ontology.Offer;
import cz.artique.jade.bookTrader.ontology.SellMeBooks;

class SellBookResponder extends SSContractNetResponder {

    Codec codec = new SLCodec();
    Ontology onto = BookOntology.getInstance();

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SellBookResponder(Agent a, ACLMessage cfp) {
        super(a, cfp);
    }

    @Override
    protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
        try {
            Action ac = (Action) getAgent().getContentManager().extractContent(cfp);

            SellMeBooks smb = (SellMeBooks) ac.getAction();
            ArrayList<BookInfo> books = smb.getBooks();

            ArrayList<BookInfo> sellBooks = new ArrayList<BookInfo>();

            double fitness = 0;
            for (BookInfo bi : books) {
                BookInfo bookInfoByBookName = Library.LIBRARY.getBookInfoByBookName(bi.getBookName());
                if (bookInfoByBookName == null) {
                    throw new RefuseException("");
                }
                Double estimatePrice = Library.LIBRARY.getEstimatedPrice(bi);
                if (estimatePrice == null) {
                    throw new RefuseException("");
                }
                fitness += estimatePrice;
                sellBooks.add(bookInfoByBookName);
            }

            ChooseFrom cf = new ChooseFrom();

            cf.setWillSell(sellBooks);
            cf.setOffers(generateOffers(fitness));

            // posleme nabidky
            ACLMessage reply = cfp.createReply();
            reply.setPerformative(ACLMessage.PROPOSE);
            reply.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
            getAgent().getContentManager().fillContent(reply, cf);

            return reply;
        } catch (UngroundedException e) {
            e.printStackTrace();
        } catch (Codec.CodecException e) {
            e.printStackTrace();
        } catch (OntologyException e) {
            e.printStackTrace();
        }

        throw new FailureException("");
    }

    private ArrayList<Offer> generateOffers(double fitness) {
        // TODO Auto-generated method stub
        return null;
    }

    // agent se rozhodl, ze nabidku prijme
    @Override
    protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {

        try {
            ChooseFrom cf = (ChooseFrom) getAgent().getContentManager().extractContent(propose);

            // pripravime info o transakci a zadame ji prostredi
            MakeTransaction mt = new MakeTransaction();

            mt.setSenderName(myAgent.getName());
            mt.setReceiverName(cfp.getSender().getName());
            mt.setTradeConversationID(cfp.getConversationId());

            if (cf.getWillSell() == null) {
                cf.setWillSell(new ArrayList<BookInfo>());
            }

            mt.setSendingBooks(cf.getWillSell());
            mt.setSendingMoney(0.0);

            Chosen c = (Chosen) getAgent().getContentManager().extractContent(accept);

            if (c.getOffer().getBooks() == null) {
                c.getOffer().setBooks(new ArrayList<BookInfo>());
            }

            mt.setReceivingBooks(c.getOffer().getBooks());
            mt.setReceivingMoney(c.getOffer().getMoney());

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

            ACLMessage reply = accept.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            return reply;

        } catch (UngroundedException e) {
            e.printStackTrace();
        } catch (OntologyException e) {
            e.printStackTrace();
        } catch (Codec.CodecException e) {
            e.printStackTrace();
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        throw new FailureException("");
    }
}