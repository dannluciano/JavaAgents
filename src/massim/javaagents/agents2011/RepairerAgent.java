package massim.javaagents.agents2011;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Vector;

import apltk.interpreter.data.LogicBelief;
import apltk.interpreter.data.LogicGoal;
import apltk.interpreter.data.Message;
import eis.iilang.Action;
import eis.iilang.Percept;
import massim.javaagents.Agent;

public class RepairerAgent extends Agent {

	int rechargeSteps = 0;
	
	public RepairerAgent(String name, String team) {
		super(name, team);
	}

	@Override
	public void handlePercept(Percept p) {
	}

	@Override
	public Action step() {
		
		Action act = null;
		
		act = planRecharge();
		if ( act != null ) return act;

		
		/*if ( rechargeSteps > 0 ) {
			rechargeSteps --;
			println("recharging...");
			return Util.skipAction();
			//return Util.rechargeAction();
		}*/
		
		Collection<Message> messages = getMessages();
		Vector<String> needyAgents = new Vector<String>();
		for ( Message msg : messages ) {
				needyAgents.add(msg.sender);
		}
		
		if ( needyAgents.size() == 0 ) {
			println("nothing for me to do");
			return Util.skipAction();
			//goals.add(new LogicGoal("beAtFullCharge"));
			//return Util.rechargeAction();
		}

		println("some poor souls need my help " + needyAgents);
		
		Collection<Percept> percepts = getAllPercepts();
		String position = null;
		for ( Percept p : percepts ) {
			if ( p.getName().equals("lastActionResult") && p.getParameters().get(0).toProlog().equals("failed") ) {
				println("my previous action has failed. recharging...");
				rechargeSteps = 10;
				return Util.skipAction();
			} 
			if ( p.getName().equals("position") ) {
				position = p.getParameters().get(0).toString();
			}
		}
		
		// a needy one on the same vertex
		for ( Percept p : percepts ) {
			if ( p.getName().equals("visibleEntity") ) {
				String ePos = p.getParameters().get(1).toString();
				String eName = p.getParameters().get(0).toString();
				if ( ePos.equals(position) && needyAgents.contains(eName) ) {
					println("I am going to repair " + eName);
					Util.repairAction(eName);
				}
			}
		}
		
		// maybe on an adjacent vertex?
		Vector<String> neighbors = new Vector<String>();
		for ( Percept p : percepts ) {
			if ( p.getName().equals("visibleEdge") ) {
				String vertex1 = p.getParameters().get(0).toString();
				String vertex2 = p.getParameters().
				get(1).toString();
				if ( vertex1.equals(position) ) neighbors.add(vertex2);
				if ( vertex2.equals(position) ) neighbors.add(vertex1);
			}
		}
		for ( Percept p : percepts ) {
			if ( p.getName().equals("visibleEntity") ) {
				String ePos = p.getParameters().get(1).toString();
				String eName = p.getParameters().get(0).toString();
				if ( neighbors.contains(ePos) && needyAgents.contains(eName) ) {
					println("I am going to repair " + eName + ". move to " + ePos +" first.");
					Util.gotoAction(ePos);
				}
			}
		}
		
		// goto neighbors
		Collections.shuffle(neighbors);
		String neighbor = neighbors.firstElement();
		println("I will go to " + neighbor);
		return Util.gotoAction(neighbor);

	}

	private Action planRecharge() {

		LinkedList<LogicBelief> beliefs = null;
		
		beliefs =  getAllBeliefs("energy");
		if ( beliefs.size() == 0 ) {
				println("strangely I do not know my energy");
				return Util.skipAction();
		}		
		int energy = new Integer(beliefs.getFirst().getParameters().firstElement()).intValue();

		beliefs =  getAllBeliefs("maxEnergy");
		if ( beliefs.size() == 0 ) {
				println("strangely I do not know my maxEnergy");
				return Util.skipAction();
		}		
		int maxEnergy = new Integer(beliefs.getFirst().getParameters().firstElement()).intValue();

		// if has the goal of being recharged...
		if ( goals.contains(new LogicGoal("beAtFullCharge")) ) {
			if ( maxEnergy == energy ) {
				println("I can stop recharging. I am at full charge");
				removeGoals("beAtFullCharge");
			}
			else {
				println("recharging...");
				return Util.rechargeAction();
			}
		}
		// go to recharge mode if necessary
		else {
			if ( energy < maxEnergy / 4 ) {
				println("I need to recharge");
				goals.add(new LogicGoal("beAtFullCharge"));
				return Util.rechargeAction();
			}
		}	
		
		return null;
		
	}
	
}
