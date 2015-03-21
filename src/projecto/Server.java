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
    static int id = 0;
	private double arrivalTick;
	private double serviceTick;
	private double endTick;
        
        private double actTick;
        private int cashierLine = -1;
        public int id_tk;
        
	public Token(double arrivalTick) {
            this.arrivalTick = this.serviceTick = arrivalTick;
            this.id_tk = id++;
            System.out.print("Token criado com id " + id_tk);
        }
	public double waitTime() {return serviceTick - arrivalTick;}
	public double cycleTime(double time) {return time - arrivalTick;}
	public double cycleTime() {return endTick - arrivalTick;}
	public void arrivalTick(double arrivalTick) {this.arrivalTick = arrivalTick;}
	public double arrivalTick() {return arrivalTick;}
	public double serviceTick() {return serviceTick;}
	public void serviceTick(double serviceTick) {this.serviceTick = serviceTick;}
	public void endTick(double endTick) {this.endTick = endTick;}
        
    public double getActTick() {
	return actTick;
    }

    public void incActTick(double actTick) {
	this.actTick += actTick;
    }

    public int getCashierLine() {
        return cashierLine;
    }

    public void setCashierLine(int cashierLine) {
        this.cashierLine = cashierLine;
    }
    
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
            double n = model.groupSize.next();
            int rot;
            System.out.println("Tamanho do grupo: " + n);
            for (int i = 0; i < (int)n; i++) {
                Token client = new Token(time);
                rot = (int)model.route.next();
                System.out.print("\tRota: " + rot);
                
                switch(rot) {
                    case 1:
                        if (model.restHotFood.value() > 0) {
                            model.restHotFood.inc(-1, time);
                            model.schedule(new DepartureHotFood(model, client), model.stHotFood.next());
                            System.out.println("\tentra");
                            break;
                        } else {
                            System.out.println("\tfila de espera");
                            model.queueHotFood.inc(1, time);
                            model.lineHotFood.add(client);
                            break;
                        }
                    case 2:
                        if (model.restSandwich.value() > 0) {
                                model.restSandwich.inc(-1, time);
                                model.schedule(new DepartureSandwich(model, client), model.stSandwich.next());
                                break;
                        }
                        else {
                                model.queueSandwich.inc(1, time);
                                model.lineSandwich.add(client);
                                break;
                        }
                    case 3:
                        model.schedule(new DepartureDrinks(model, client), model.stDrinks.next());
                        break;
                    default:
                        System.out.println("ERRO");
                }
            }
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
            System.out.format("Id: " + client.id_tk  +"\tDepartureHotFood: %.2f\t%.2f\t%.2f\n", client.arrivalTick(), client.serviceTick(), time);
            client.incActTick(model.actHotFood.next());
            model.schedule(new DepartureDrinks(model, client), model.stDrinks.next());
            
            if (model.queueHotFood.value() > 0) {
                model.hf++;
                model.queueHotFood.inc(-1, time);
                client = model.lineHotFood.remove(0);
		client.serviceTick(time);
		model.delayTimeHotFood.add(client.waitTime());
		model.schedule(this, model.stHotFood.next());
            } else {
		model.restHotFood.inc(1, time);
            }
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
            System.out.format("Id: " + client.id_tk  +"\tDepartureSandwich: %.2f\t%.2f\t%.2f\n", client.arrivalTick(), client.serviceTick(), time);
            client.incActTick(model.actSandwich.next());
            model.schedule(new DepartureDrinks(model, client), model.stDrinks.next());
            
            if (model.queueSandwich.value() > 0) {
                model.sw++;
                    model.queueSandwich.inc(-1, time);
                    client = model.lineSandwich.remove(0);
                    client.serviceTick(time);
                    model.delayTimeSandwich.add(client.waitTime());
                    model.schedule(this, model.stSandwich.next());
		} else {
                    model.restSandwich.inc(1, time);
		}
        }
}

final class DepartureDrinks extends Event {
	private final Server model;
        private int indexMin;
	public DepartureDrinks(Server model, Token client) {
		super();
		this.model = model;
		this.client = client;
	}
	private Token client = null;
	@Override
	public void execute() {
            System.out.format("Id: " + client.id_tk  +"\tDepartureDrinks: %.2f\t%.2f\t%.2f\n", client.arrivalTick(), client.serviceTick(), time);
            if (model.restCashier.value() > 0) {
                model.dk++;
                indexMin = 0;
                model.restCashier.inc(-1, time);
                client.incActTick(model.actDrinks.next());
                
                for(int i = 0; i < model.lineCashier.size(); i++) {
                    if(model.lineCashier.get(i).size() < model.lineCashier.get(indexMin).size()){
                        indexMin = i;
                    }
                }
                
                model.lineCashier.get(indexMin).add(client);
                client.setCashierLine(indexMin);
                
                model.schedule(new DepartureCashier(model, client), client.getActTick());
            } else {
                indexMin = 0;
                model.queueCashier.inc(1, time);
                
                for(int i = 0; i < model.lineCashier.size(); i++) {
                    if(model.lineCashier.get(i).size() < model.lineCashier.get(indexMin).size()){
                        indexMin = i;
                    }
                }
                
                model.lineCashier.get(indexMin).add(client);
                client.setCashierLine(indexMin);
                
            }
	}
}

final class DepartureCashier extends Event {
	private final Server model;
	public DepartureCashier(Server model, Token client) {
		super();
		this.model = model;
		this.client = client;
	}
	private Token client = null;
	@Override
	public void execute() {
            System.out.format("Id: " + client.id_tk  +"\tDepartureCashier: %.2f\t%.2f\t%.2f\n", client.arrivalTick(), client.serviceTick(), time);
            if (model.queueCashier.value() > 0) {
		model.queueCashier.inc(-1, time);
		
                client = model.lineCashier.get(client.getCashierLine()).remove(0);
		
                client.serviceTick(time);
		model.delayTimeCashier.add(client.waitTime());
                model.schedule(this, client.getActTick());
            } else {
                model.restCashier.inc(1, time);
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
		System.out.println("Hot Food:\tqueue: " + model.queueHotFood.mean(time) + "\trest: " + model.restHotFood.mean(time) + "\tdelay: " + model.delayTimeHotFood.mean() + "\tMax delay: " + model.delayTimeHotFood.getMax() + "\tcostumers: " + model.hf + "\tmax queue: " + model.queueHotFood.getMax());
                System.out.println("Sandwich:\tqueue: " + model.queueSandwich.mean(time) + "\trest: " + model.restSandwich.mean(time) + "\tdelay: " + model.delayTimeSandwich.mean() + "\tMax delay: " + model.delayTimeSandwich.getMax() + "\tcostumers: " + model.sw + "\tmax queue: " + model.queueSandwich.getMax());
                System.out.println("Cashier:\tqueue: " + model.queueCashier.mean(time) + "\trest: " + model.restCashier.mean(time) + "\tdelay: " + model.delayTimeCashier.mean() + "\tMax delay: " + model.delayTimeCashier.getMax() + "\tcostumers: " + model.dk + "\tmax queue: " + model.queueCashier.getMax());
		model.clear();
	}
}

final class Server extends Model {
        static int hf = 0;
        static int dk = 0;
        static int sw = 0;
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
        
        final List<List<Token>> lineCashier;
        final Uniform stCashier;
        final Uniform actCashier;
        
        final Accumulate queueCashier;
        final Accumulate restCashier;
        final Average delayTimeCashier;
        
        final Exponential arrival;
        
        final Discrete groupSize = new Discrete(1, new double[]{1.0, 2.0, 3.0, 4.0}, new double[]{0.5, 0.3, 0.1, 0.1});
        final Discrete route = new Discrete(1, new double[]{1.0, 2.0, 3.0}, new double[]{0.8, 0.15, 0.05});
        
	public Server(int nWorkersHotFood, int nWorkersSandwich, int nCashiers) {
		super();
		this.queueHotFood = new Accumulate(0);
		this.restHotFood = new Accumulate(nWorkersHotFood);
                this.delayTimeHotFood = new Average();
		this.lineHotFood = new ArrayList<>();
                this.stHotFood = new Uniform(1, 50/nWorkersHotFood,120/nWorkersHotFood);
                this.actHotFood = new Uniform(1, 20, 40);
                
                this.queueSandwich = new Accumulate(0);
		this.restSandwich = new Accumulate(nWorkersSandwich);
                this.delayTimeSandwich = new Average();
		this.lineSandwich = new ArrayList<>();
                this.stSandwich = new Uniform(1, 60/nWorkersSandwich,180/nWorkersSandwich);
                this.actSandwich = new Uniform(1, 5, 15);
                
                this.stDrinks = new Uniform(1, 5, 20);
                this.actDrinks = new Uniform(1, 5, 10);
                
                this.lineCashier = new ArrayList<>();
                
                this.queueCashier = new Accumulate(0);
                this.restCashier = new Accumulate(nCashiers);
                this.delayTimeCashier = new Average();
                
                for(int i = 0; i < nCashiers; i++){
                    this.lineCashier.add(new ArrayList<>());
                }
                this.stCashier = new Uniform(1, 50,120);
                this.actCashier = new Uniform(1, 20, 40);
                
		arrival = new Exponential(1, 30);
	}
        
	@Override
	protected void init() {
		schedule(new Arrival(this), arrival.next());
		schedule(new Stop(this), 5400);
	}
	
        @Override
	public String toString() {return "" + queueHotFood.value() + " " + restHotFood.value();}
}
