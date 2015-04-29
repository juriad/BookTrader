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
import java.util.List;

import cz.artique.jade.bookTrader.Library.ListTest;
import cz.artique.jade.bookTrader.ontology.BookInfo;
import cz.artique.jade.bookTrader.ontology.BookOntology;
import cz.artique.jade.bookTrader.ontology.ChooseFrom;
import cz.artique.jade.bookTrader.ontology.Chosen;
import cz.artique.jade.bookTrader.ontology.MakeTransaction;
import cz.artique.jade.bookTrader.ontology.Offer;
import cz.artique.jade.bookTrader.ontology.SellMeBooks;

class SellBookResponder extends SSContractNetResponder {

    private Codec codec = new SLCodec();
    private Ontology onto = BookOntology.getInstance();

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

            System.out.print("Somebody is requesting: ");

            ArrayList<BookInfo> sellBooks = new ArrayList<BookInfo>();

            double fitness = 0;
            for (BookInfo bi : books) {
                System.out.print(bi.getBookName() + ", ");
                BookInfo bookInfoByBookName = Library.LIBRARY.getMyBookInfo(bi);
                if (bookInfoByBookName == null) {
                    System.out.println("fail 1");
                    throw new RefuseException("");
                }
                Double estimatePrice = Library.LIBRARY.getEstimatedPrice(bi, false);
                if (estimatePrice == null) {
                    double inventedEstimate = Library.LIBRARY.inventEstimate(bi);
                    System.out.println("using invented estimate: " + inventedEstimate);
                    estimatePrice = inventedEstimate;
                }
                fitness += estimatePrice;
                sellBooks.add(bookInfoByBookName);
            }
            System.out.println();

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
        ArrayList<ArrayList<BookInfo>> all = Library.genSubsets(Library.LIBRARY.getBooks(), Library.LIBRARY.getBooks().size(), new ListTest<BookInfo>() {
            @Override
            public boolean test(List<BookInfo> test) {
                return true;
            }
        });
        ArrayList<Offer> offers = new ArrayList<Offer>();
        for (ArrayList<BookInfo> comb : all) {
            Double price = Library.getTotalPrice(comb, 0.2);
            if (price != null && fitness >= price) {
                Offer o = new Offer();
                o.setBooks(comb);
                o.setMoney(fitness - price);
                offers.add(o);
                System.out.print("offer: ");
                for (BookInfo bookInfo : comb) {
                    System.out.print(bookInfo.getBookName() + ", ");
                }
                System.out.println(" and " + (fitness - price));

                // TODO constant
            }
        }
        return offers;
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

            System.out.print("Our offer was accepted: ");
            for (BookInfo bi : cf.getWillSell()) {
                System.out.print(bi.getBookName() + ", ");
            }
            System.out.print(" for ");
            for (BookInfo bi : c.getOffer().getBooks()) {
                System.out.print(bi.getBookName() + ", ");
            }
            System.out.println(" and " + c.getOffer().getMoney());

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