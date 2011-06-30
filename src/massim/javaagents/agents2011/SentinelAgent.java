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

public class SentinelAgent extends Agent {

	private int myHealth = 1;

	public SentinelAgent(String name, String team) {
		super(name, team);
	}

	public void handlePercept(Percept p) {
	}

	@Override
	public Action step() {

		handleMessages();
		handlePercepts();

		Action act = null;

		// 1. recharging
		act = planRecharge();
		if (act != null)
			return act;

		act = planBuyShield();
		if (act != null)
			return act;

		// 3.Parry if necessary
		act = planParry();
		if (act != null)
			return act;

		// 2. surveying if necessary
		act = planSurvey();
		if (act != null)
			return act;

		// 4. buying battery with a certain probability
		//act = planBuyBattery();
		//if (act != null)
			//return act;

		// 5. (almost) random walking
		act = planRandomWalk();
		if (act != null)
			return act;

		return Util.skipAction();

	}

	private void handleMessages() {

		// handle messages... believe everything the others say
		Collection<Message> messages = getMessages();
		for (Message msg : messages) {
			println(msg.sender + " told me " + msg.value);
			String predicate = ((LogicBelief) msg.value).getPredicate();
			if (containsBelief((LogicBelief) msg.value)) {
				println("I already knew that");
			} else {
				println("that was new to me");
				if (predicate.equals("probedVertex")
						|| predicate.equals("surveyedEdge")) {
					addBelief((LogicBelief) msg.value);
					println("I will keep that in mind");
					continue;
				}
				println("but I am not interested in that gibberish");
			}
		}

	}

	private void handlePercepts() {

		String position = null;
		Vector<String> neighbors = new Vector<String>();

		// check percepts
		Collection<Percept> percepts = getAllPercepts();
		// if ( gatherSpecimens ) processSpecimens(percepts);
		removeBeliefs("visibleEntity");
		removeBeliefs("visibleEdge");
		for (Percept p : percepts) {
			if (p.getName().equals("step")) {
				println(p);
			} else if (p.getName().equals("visibleEntity")) {
				LogicBelief b = Util.perceptToBelief(p);
				if (containsBelief(b) == false) {
					addBelief(b);
				} else {
				}
			} else if (p.getName().equals("visibleEdge")) {
				LogicBelief b = Util.perceptToBelief(p);
				if (containsBelief(b) == false) {
					addBelief(b);
				} else {
				}
			} else if (p.getName().equals("probedVertex")) {
				LogicBelief b = Util.perceptToBelief(p);
				if (containsBelief(b) == false) {
					println("I perceive the value of a vertex that I have not known before");
					addBelief(b);
					broadcastBelief(b);
				} else {
					// println("I already knew " + b);
				}
			} else if (p.getName().equals("surveyedEdge")) {
				LogicBelief b = Util.perceptToBelief(p);
				if (containsBelief(b) == false) {
					println("I perceive the weight of an edge that I have not known before");
					addBelief(b);
					broadcastBelief(b);
				} else {
					// println("I already knew " + b);
				}
			} else if (p.getName().equals("health")) {
				Integer health = new Integer(p.getParameters().get(0)
						.toString());
				println("my health is " + health);
				if (health.intValue() == 0) {
					println("my health is zero. asking for help");
					broadcastBelief(new LogicBelief("iAmDisabled"));
				}
			} else if (p.getName().equals("position")) {
				position = p.getParameters().get(0).toString();
				removeBeliefs("position");
				addBelief(new LogicBelief("position", position));
			} else if (p.getName().equals("energy")) {
				Integer energy = new Integer(p.getParameters().get(0)
						.toString());
				removeBeliefs("energy");
				addBelief(new LogicBelief("energy", energy.toString()));
			} else if (p.getName().equals("maxEnergy")) {
				Integer maxEnergy = new Integer(p.getParameters().get(0)
						.toString());
				removeBeliefs("maxEnergy");
				addBelief(new LogicBelief("maxEnergy", maxEnergy.toString()));
			} else if (p.getName().equals("money")) {
				Integer money = new Integer(p.getParameters().get(0).toString());
				removeBeliefs("money");
				addBelief(new LogicBelief("money", money.toString()));
			} else if (p.getName().equals("achievement")) {
				println("reached achievement " + p);
			}
		}

		// again for checking neighbors
		this.removeBeliefs("neighbor");
		for (Percept p : percepts) {
			if (p.getName().equals("visibleEdge")) {
				String vertex1 = p.getParameters().get(0).toString();
				String vertex2 = p.getParameters().get(1).toString();
				if (vertex1.equals(position))
					addBelief(new LogicBelief("neighbor", vertex2));
				if (vertex2.equals(position))
					addBelief(new LogicBelief("neighbor", vertex1));
			}
		}
	}

	private Action planParry() {
		Collection<Percept> percepts = getAllPercepts();
		for (Percept p : percepts) {
			if (p.getName().equals("health")) {
				Integer health = new Integer(p.getParameters().get(0)
						.toString());
				if (health - myHealth < 0)
					return Util.parryAction();
			}
		}
		return null;
	}

	private Action planRecharge() {

		LinkedList<LogicBelief> beliefs = null;

		beliefs = getAllBeliefs("energy");
		if (beliefs.size() == 0) {
			println("strangely I do not know my energy");
			return Util.skipAction();
		}
		int energy = new Integer(beliefs.getFirst().getParameters()
				.firstElement()).intValue();

		beliefs = getAllBeliefs("maxEnergy");
		if (beliefs.size() == 0) {
			println("strangely I do not know my maxEnergy");
			return Util.skipAction();
		}
		int maxEnergy = new Integer(beliefs.getFirst().getParameters()
				.firstElement()).intValue();

		// if has the goal of being recharged...
		if (goals.contains(new LogicGoal("beAtFullCharge"))) {
			if (maxEnergy == energy) {
				println("I can stop recharging. I am at full charge");
				removeGoals("beAtFullCharge");
			} else {
				println("recharging...");
				return Util.rechargeAction();
			}
		}
		// go to recharge mode if necessary
		else {
			if (energy < maxEnergy / 3) {
				println("I need to recharge");
				goals.add(new LogicGoal("beAtFullCharge"));
				return Util.rechargeAction();
			}
		}

		return null;

	}

	private Action planSurvey() {

		println("I know " + getAllBeliefs("visibleEdge").size()
				+ " visible edges");
		println("I know " + getAllBeliefs("surveyedEdge").size()
				+ " surveyed edges");

		// get all neighbors
		LinkedList<LogicBelief> visible = getAllBeliefs("visibleEdge");
		LinkedList<LogicBelief> surveyed = getAllBeliefs("surveyedEdge");

		String position = getAllBeliefs("position").get(0).getParameters()
				.firstElement();

		int unsurveyedNum = 0;
		int adjacentNum = 0;

		for (LogicBelief v : visible) {

			String vVertex0 = v.getParameters().elementAt(0);
			String vVertex1 = v.getParameters().elementAt(1);

			boolean adjacent = false;
			if (vVertex0.equals(position) || vVertex1.equals(position))
				adjacent = true;

			if (adjacent == false)
				continue;
			adjacentNum++;

			boolean isSurveyed = false;
			for (LogicBelief s : surveyed) {
				String sVertex0 = s.getParameters().elementAt(0);
				String sVertex1 = s.getParameters().elementAt(1);
				if (sVertex0.equals(vVertex0) && sVertex1.equals(vVertex1)) {
					isSurveyed = true;
					break;
				}
				if (sVertex0.equals(vVertex1) && sVertex1.equals(vVertex0)) {
					isSurveyed = true;
					break;
				}
			}
			if (isSurveyed == false)
				unsurveyedNum++;

		}

		println("" + unsurveyedNum + " out of " + adjacentNum
				+ " adjacent edges are unsurveyed");

		if (unsurveyedNum > 0) {
			println("I will survey");
			return Util.surveyAction();
		}

		return null;

	}

	/**
	 * Buy a battery with a given probability
	 * 
	 * @return
	 */
	private Action planBuyBattery() {

		LinkedList<LogicBelief> beliefs = this.getAllBeliefs("money");
		if (beliefs.size() == 0) {
			println("strangely I do not know our money.");
			return null;
		}

		LogicBelief moneyBelief = beliefs.get(0);
		int money = new Integer(moneyBelief.getParameters().get(0)).intValue();

		if (money < 10) {
			println("we do not have enough money.");
			return null;
		}
		println("we do have enough money.");

		println("I am going to buy a battery");

		return Util.buyAction("battery");

	}

	private Action planBuyShield() {

		LinkedList<LogicBelief> beliefs = this.getAllBeliefs("money");
		if (beliefs.size() == 0) {
			println("strangely I do not know our money.");
			return null;
		}

		LogicBelief moneyBelief = beliefs.get(0);
		int money = new Integer(moneyBelief.getParameters().get(0)).intValue();

		if (money < 10 || myHealth == 3) {
			println("we do not have enough money or we have enough health.");
			return null;
		}

		int flag = ((int)Math.random() % 12);
		switch (flag) {
		case 0:
			println("I am going to buy a battery");
			return Util.buyAction("battery");
		case 1:
			println("I am going to buy a sensor");
			return Util.buyAction("sensor");
		case 2:
			println("I am going to buy a shield");
			return Util.buyAction("shield");
		default:
			break;
		}
		return null;
	}

	private Action planRandomWalk() {

		LinkedList<LogicBelief> beliefs = getAllBeliefs("neighbor");
		Vector<String> neighbors = new Vector<String>();
		for (LogicBelief b : beliefs) {
			neighbors.add(b.getParameters().firstElement());
		}

		if (neighbors.size() == 0) {
			println("strangely I do not know any neighbors");
			return Util.skipAction();
		}

		// goto neighbors
		Collections.shuffle(neighbors);
		String neighbor = neighbors.firstElement();
		println("I will go to " + neighbor);
		return Util.gotoAction(neighbor);

	}
}
