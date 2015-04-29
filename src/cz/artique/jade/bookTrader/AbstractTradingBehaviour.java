package cz.artique.jade.bookTrader;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import cz.artique.jade.bookTrader.ontology.BookInfo;
import cz.artique.jade.bookTrader.ontology.BookOntology;
import cz.artique.jade.bookTrader.ontology.SellMeBooks;

public abstract class AbstractTradingBehaviour extends TickerBehaviour {

    private Codec codec = new SLCodec();
    private Ontology onto = BookOntology.getInstance();

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AbstractTradingBehaviour(Agent a, long period) {
        super(a, period);
    }

    protected abstract LinkedList<ArrayList<BookInfo>> getBooksToObtain();

    protected LinkedList<ArrayList<BookInfo>> queue = new LinkedList<ArrayList<BookInfo>>();

    protected abstract void delegateMessage(ACLMessage buyBook);

    @Override
    protected void onTick() {
        try {
            // najdeme si ostatni prodejce a pripravime zpravu
            ServiceDescription sd = new ServiceDescription();
            sd.setType("book-trader");
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.addServices(sd);

            DFAgentDescription[] traders = DFService.search(myAgent, dfd);

            ACLMessage buyBook = new ACLMessage(ACLMessage.CFP);
            buyBook.setLanguage(codec.getName());
            buyBook.setOntology(onto.getName());
            buyBook.setReplyByDate(new Date(System.currentTimeMillis() + 5000));

            for (DFAgentDescription dfad : traders) {
                if (dfad.getName().equals(myAgent.getAID()))
                    continue;
                buyBook.addReceiver(dfad.getName());
            }

            if (queue.isEmpty()) {
                System.out.println("Regenerating queue.");
                queue = getBooksToObtain();
            }
            if (queue.isEmpty()) {
                return;
            }

            ArrayList<BookInfo> books = queue.removeFirst();

            if (this instanceof RegularTradingBehaviour) {
                System.out.print("Trying to buy: ");
                for (BookInfo bookInfo : books) {
                    System.out.print(bookInfo.getBookName() + ", ");
                }
                System.out.println();
            } else {
                System.out.println("Trying to estimate price of " + books.get(0).getBookName());
            }

            SellMeBooks smb = new SellMeBooks();
            smb.setBooks(books);

            getAgent().getContentManager().fillContent(buyBook, new Action(myAgent.getAID(), smb));
            delegateMessage(buyBook);
        } catch (Codec.CodecException e) {
            e.printStackTrace();
        } catch (OntologyException e) {
            e.printStackTrace();
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}