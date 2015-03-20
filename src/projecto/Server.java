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
            double n = new Discrete(1, new double[]{1, 2, 3, 4}, new double[]{0.5, 0.3, 0.1, 0.1}).next();
            int route;
            for (int i = 0; i < n; i++) {
                Token client = new Token(time);
                route = (int) new Discrete(1, new double[]{1, 2, 3}, new double[]{0.8, 0.15, 0.05}).next();
                
                switch(route) {
                    case 1:
                        if (model.restHotFood.value() > 0) {
                                model.restHotFood.inc(-1, time);
                                model.schedule(new DepartureHotFood(model, client), model.stHotFood.next());
                        }
                        else {
                                model.queueHotFood.inc(1, time);
                                model.lineHotFood.add(client);
                        }
                        break;
                    case 2:
                        if (model.restSandwich.value() > 0) {
                                model.restSandwich.inc(-1, time);
                                model.schedule(new DepartureSandwich(model, client), model.stSandwich.next());
                        }
                        else {
                                model.queueSandwich.inc(1, time);
                                model.lineSandwich.add(client);
                        }
                        break;
                    case 3:
                        model.schedule(new DepartureDrinks(model, client), model.stDrinks.next());
                        break;
                    default:
                        System.out.println("ERRO");
                }
            }
            
		Token client = new Token(time);
		model.schedule(this, model.arrival.next());
	}
}

final class DepartureHotFood extends Event {
	private final Server model;
	public DepartureHotFood(Server model, Token client) {
		super();
		this.model = model;
		this.client = client;
	}
	private Token client = null;
	@Override
	public void execute() {
            System.out.format("%.2f\t%.2f\t%.2f\n", client.arrivalTick(), client.serviceTick(), time);
            model.actSandwich.next();
            model.schedule(new DepartureDrinks(model, client), model.stDrinks.next());
        }
}

final class DepartureSandwich extends Event {
	private final Server model;
	public DepartureSandwich(Server model, Token client) {
		super();
		this.model = model;
		this.client = client;
	}
	private Token client = null;
	@Override
	public void execute() {
            System.out.format("%.2f\t%.2f\t%.2f\n", client.arrivalTick(), client.serviceTick(), time);
            model.actSandwich.next();
            model.schedule(new DepartureDrinks(model, client), model.stDrinks.next());
        }
}

final class DepartureDrinks extends Event {
	private final Server model;
	public DepartureDrinks(Server model, Token client) {
		super();
		this.model = model;
		this.client = client;
	}
	private Token client = null;
	@Override
	public void execute() {
		System.out.format("%.2f\t%.2f\t%.2f\n", client.arrivalTick(), client.serviceTick(), time);
		if (model.queueSandwich.value() > 0) {
			model.queueSandwich.inc(-1, time);
			client = model.lineSandwich.remove(0);
			client.serviceTick(time);
			model.delayTimeSandwich.add(client.waitTime());
			model.schedule(this, model.stSandwich.next());
		}
		else {
			model.restSandwich.inc(1, time);
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
    
	final Accumulate queueHotFood;
        final Accumulate restHotFood;
        final Average delayTimeHotFood;
        final List<Token> lineHotFood;
        final Uniform stHotFood;
        final Uniform actHotFood;
        
        final Accumulate queueSandwich;
        final Accumulate restSandwich;
        final Average delayTimeSandwich;
        final List<Token> lineSandwich;
        final Uniform stSandwich;
        final Uniform actSandwich;
        
        final Uniform stDrinks;
        final Uniform actDrinks;
        
        final Accumulate queueCashier;
        final Accumulate restCashier;
        final Average delayTimeCashier;
        final List<Token> lineCashier;
        final Uniform stCashier;
        final Uniform actCashier;
        
        final Exponential arrival;
        
	public Server(int n) {
		super();
		this.queueHotFood = new Accumulate(0);
		this.restHotFood = new Accumulate(n);
                this.delayTimeHotFood = new Average();
		this.lineHotFood = new ArrayList<>();
                this.stHotFood = new Uniform(1, 50,120);
                this.actHotFood = new Uniform(1, 20, 40);
                
                this.queueSandwich = new Accumulate(0);
		this.restSandwich = new Accumulate(n);
                this.delayTimeSandwich = new Average();
		this.lineSandwich = new ArrayList<>();
                this.stSandwich = new Uniform(1, 60,180);
                this.actSandwich = new Uniform(1, 5, 15);
                
                this.stDrinks = new Uniform(1, 5, 20);
                this.actDrinks = new Uniform(1, 5, 10);
                
                this.queueCashier = new Accumulate(0);
		this.restCashier = new Accumulate(n);
                this.delayTimeCashier = new Average();
		this.lineCashier = new ArrayList<>();
                this.stCashier = new Uniform(1, 50,120);
                this.actCashier = new Uniform(1, 20, 40);
                
		arrival = new Exponential(1, 30);
	}
        
	@Override
	protected void init() {
		schedule(new Arrival(this), arrival.next());
		schedule(new Stop(this), 5400);
	}
	
        //@Override
	//public String toString() {return "" + queue.value() + " " + rest.value();}
}
