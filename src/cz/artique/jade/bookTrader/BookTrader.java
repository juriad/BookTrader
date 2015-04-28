package cz.artique.jade.bookTrader;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import cz.artique.jade.bookTrader.ontology.BookOntology;

/**
 * Created by Martin Pilat on 16.4.14.
 *
 * Jednoducha (testovaci) verze obchodujiciho agenta. Agent neobchoduje nijak rozumne, stara se pouze o to, aby
 * nenabizel knihy, ktere nema. (Ale stejne se muze stat, ze obcas nejakou knihu zkusi prodat dvakrat, kdyz o ni pozadaji
 * dva agenti rychle po sobe.)
 */
public class BookTrader extends Agent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    Codec codec = new SLCodec();
    Ontology onto = BookOntology.getInstance();

    @Override
    protected void setup() {
        super.setup();

        // napred je potreba rict agentovi, jakym zpusobem jsou zpravy kodovany, a jakou pouzivame ontologii
        this.getContentManager().registerLanguage(codec);
        this.getContentManager().registerOntology(onto);

        // popis sluzby book-trader
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-trader");
        sd.setName("book-trader");

        // popis tohoto agenta a sluzeb, ktere nabizi
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(this.getAID());
        dfd.addServices(sd);

        // zaregistrovani s DF
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // pridame chovani, ktere bude cekat na zpravu o zacatku obchodovani
        addBehaviour(new StartTradingBehaviour(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

}
