package cz.artique.jade.bookTrader;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSResponderDispatcher;

// chovani, ktere se stara o prodej knih
class SellBook extends SSResponderDispatcher {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SellBook(Agent a, MessageTemplate tpl) {
        super(a, tpl);
    }

    @Override
    protected Behaviour createResponder(ACLMessage initiationMsg) {
        return new SellBookResponder(myAgent, initiationMsg);
    }
}