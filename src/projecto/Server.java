/*
Author: Fernando J. Barros
University of Coimbra
Department of Informatics Enginnering
3030 Coimbra, Portugal
Date: 20/2/2015
 */
package projecto;

import java.util.ArrayList;
import java.util.List;

class Token {
	private double arrivalTick;
	private double serviceTick;
	private double endTick;
	public Token(double arrivalTick) {this.arrivalTick = this.serviceTick = arrivalTick;}
	public double waitTime() {return serviceTick - arrivalTick;}
	public double cycleTime(double time) {return time - arrivalTick;}
	public double cycleTime() {return endTick - arrivalTick;}
	public void arrivalTick(double arrivalTick) {this.arrivalTick = arrivalTick;}
	public double arrivalTick() {return arrivalTick;}
	public double serviceTick() {return serviceTick;}
	public void serviceTick(double serviceTick) {this.serviceTick = serviceTick;}
	public void endTick(double endTick) {this.endTick = endTick;}
	@Override
	public String toString() {return String.format("[%.2f]", arrivalTick);}
}

final class Arrival extends Event {
	private final Server model;
	public Arrival(Server model) {
		super();
		this.model = model;
	}
	@Override
	public void execute() {
		Token client = new Token(time);
		if (model.rest.value() > 0) {
			model.rest.inc(-1, time);
			model.schedule(new Departure(model, client), model.service.next());
		}
		else {
			model.queue.inc(1, time);
			model.line.add(client);
		}
		model.schedule(this, model.arrival.next());
	}
}

final class Departure extends Event {
	private final Server model;
	public Departure(Server model, Token client) {
		super();
		this.model = model;
		this.client = client;
	}
	private Token client = null;
	@Override
	public void execute() {
		System.out.format("%.2f\t%.2f\t%.2f\n", client.arrivalTick(), client.serviceTick(), time);
		if (model.queue.value() > 0) {
			model.queue.inc(-1, time);
			client = model.line.remove(0);
			client.serviceTick(time);
			model.delayTime.add(client.waitTime());
			model.schedule(this, model.service.next());
		}
		else {
			model.rest.inc(1, time);
		}
	}
}

final class Stop extends Event {
	private final Server model;
	public Stop(Server model) {
		super();
		this.model = model;
	}
	@Override
	public void execute() {
		System.out.format("%.2f\t%.2f\t%.2f\n",
			model.queue.mean(time),
			model.rest.mean(time), model.delayTime.mean()
		);
		model.clear();
	}
}

final class Server extends Model {
	final Accumulate queue;
	final Accumulate rest;
	final RandomStream service;
	final RandomStream arrival;
	final List<Token> line;
	final Average delayTime;
	public Server(int n) {
		super();
		this.queue = new Accumulate(0);
		this.rest = new Accumulate(n);
		this.line = new ArrayList<>();
		this.delayTime = new Average();
		double[] A = new double[]{0.4, 1.2, 0.5, 1.7, 0.2, 1.6, 0.2, 1.4, 1.9, 2.7, 0.5};
		double[] S = new double[]{2.0, 0.7, 0.2, 1.1, 3.7, 0.6, 0.9, 1.3, 1.1, 1.8, 0.8};
		arrival = new Sequence(A);
		service = new Sequence(S);
	}
	@Override
	protected void init() {
		schedule(new Arrival(this), arrival.next());
		schedule(new Stop(this), 9);
	}
	@Override
	public String toString() {return "" + queue.value() + " " + rest.value();}
}
