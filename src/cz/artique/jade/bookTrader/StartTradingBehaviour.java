package cz.artique.jade.bookTrader;

import jade.content.ContentElement;
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
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import cz.artique.jade.bookTrader.ontology.AgentInfo;
import cz.artique.jade.bookTrader.ontology.BookOntology;
import cz.artique.jade.bookTrader.ontology.GetMyInfo;
import cz.artique.jade.bookTrader.ontology.StartTrading;

// ceka na zpravu o zacatku obchodovani a potom prida obchodovaci chovani
class StartTradingBehaviour extends AchieveREResponder {

    private Codec codec = new SLCodec();
    private Ontology onto = BookOntology.getInstance();

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public StartTradingBehaviour(Agent a, MessageTemplate mt) {
        super(a, mt);
    }

    @Override
    protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {

        try {
            ContentElement ce = getAgent().getContentManager().extractContent(request);

            if (!(ce instanceof Action)) {
                throw new NotUnderstoodException("");
            }
            Action a = (Action) ce;

            // dostali jsme info, ze muzeme zacit obchodovat
            if (a.getAction() instanceof StartTrading) {

                // zjistime si, co mame, a jake jsou nase cile
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

                // pridame chovani, ktere jednou za dve vteriny zkusi koupit vybranou knihu
                getAgent().addBehaviour(new RegularTradingBehaviour(myAgent, 2000));

                // pridame chovani, ktere se stara o prodej knih
                getAgent().addBehaviour(new SellBook(myAgent, MessageTemplate.MatchPerformative(ACLMessage.CFP)));

                // odpovime, ze budeme obchodovat (ta zprava se v prostredi ignoruje, ale je slusne ji poslat)
                ACLMessage reply = request.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                return reply;
            }

            throw new NotUnderstoodException("");

        } catch (Codec.CodecException e) {
            e.printStackTrace();
        } catch (OntologyException e) {
            e.printStackTrace();
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        return super.handleRequest(request);
    }
}