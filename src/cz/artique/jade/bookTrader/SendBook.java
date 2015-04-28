package cz.artique.jade.bookTrader;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import cz.artique.jade.bookTrader.ontology.AgentInfo;
import cz.artique.jade.bookTrader.ontology.BookOntology;
import cz.artique.jade.bookTrader.ontology.GetMyInfo;

// po dokonceni obchodu (prostredi poslalo info) si aktualizujeme vlastni seznam knih a cile
class SendBook extends AchieveREInitiator {

    private Codec codec = new SLCodec();
    private Ontology onto = BookOntology.getInstance();

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SendBook(Agent a, ACLMessage msg) {
        super(a, msg);
    }

    @Override
    protected void handleInform(ACLMessage inform) {

        try {
            ACLMessage getMyInfo = new ACLMessage(ACLMessage.REQUEST);
            getMyInfo.setLanguage(codec.getName());
            getMyInfo.setOntology(onto.getName());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("environment");
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.addServices(sd);

            DFAgentDescription[] envs = DFService.search(myAgent, dfd);

            getMyInfo.addReceiver(envs[0].getName());
            getAgent().getContentManager().fillContent(getMyInfo, new Action(envs[0].getName(), new GetMyInfo()));

            ACLMessage myInfo = FIPAService.doFipaRequestClient(myAgent, getMyInfo);

            Result res = (Result) getAgent().getContentManager().extractContent(myInfo);

            AgentInfo ai = (AgentInfo) res.getValue();

            Library.LIBRARY.update(ai.getBooks(), ai.getGoals(), ai.getMoney());
        } catch (OntologyException e) {
            e.printStackTrace();
        } catch (FIPAException e) {
            e.printStackTrace();
        } catch (Codec.CodecException e) {
            e.printStackTrace();
        }

    }
}