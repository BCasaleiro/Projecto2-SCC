/*
Author: Fernando J. Barros
University of Coimbra
Department of Informatics Enginnering
3030 Coimbra, Portugal
Date: 20/2/2015
 */
package projecto;
import java.util.*;

interface RandomStream {
	abstract public double next();
}

class Discrete extends Uniform01 {
	private final double[] prob;
	private final double[] values;
	public Discrete(int stream, double[] values, double[] prob) {
		super(stream);
		double sum = 0.0;
		for (int i = 0; i < prob.length; ++i) sum += prob[i];
		assert(sum <= 1.0); 
		this.prob = prob;
		this.values = values;
	}
	@Override
	public double next() {
		double rnd = super.next();
		int i = 0;
		double sum = prob[i];
		while (sum < rnd) sum += prob[++i];
		return values[i];
	}
}

class Constant implements RandomStream {
	final private double value;
	public Constant(int value) {this.value = value;}
	@Override
	public double next() {return value;}
}

class Uniform01 implements RandomStream {
	final private Random rnd;
	public Uniform01(int stream) {rnd = new Random(stream);}
	@Override
	public double next() {return rnd.nextDouble();}
}

class Uniform extends Uniform01 {
	final private double a, b;
	public Uniform(int stream, double a, double b) {
		super(stream);
		assert (a < b);
		this.a = a;
		this.b = b;
	}
	@Override
	public double next() {return a + super.next() * (b - a);}
}

class Sequence implements RandomStream {
	final private double[] values;
	private int curr;
	public Sequence(double[] values) {
		this.values = values;
		curr = 0;
	}
	@Override
	public double next() {
		return values[curr++];
	}
}

class Exponential extends Uniform01 {
	final private double mean;
	public Exponential(int stream, double mean) {
		super(stream);
		this.mean = mean;
	}
	@Override
	public double next() {return -mean * Math.log(super.next());}
}

class Accumulate {
	private int value = 0;
	private double accum = 0.0;
	private double last = 0.0;
	public Accumulate(int value) {this.value = value;}
	public void inc(int d, double time) {set(value + d, time);}
	public void set(int v, double time) {
		double delta = time - last;
		accum += value * delta;
		this.value = v;
		last = time;
	}
	public double mean(double time) {return integral(time) / time;}
	public double integral(double time) {
		double delta = time - last;
		accum += value * delta;
		last = time;
		return accum;
	}
	public int value() {return value;}
	@Override
	public String toString() {return "" + value + " " + accum + " " + last;}
}

class Average {
        private double max = 0;
	private double sum;
	private int count;
	public Average() {clear();}
	public double mean() {return sum / count;}
	public void add(double value) {
                if(max < value){
                    max = value;
                }
		sum += value;
		++count;
	}
	public int count() {return count;}
	public final void clear() {
		sum = 0.0;
		count = 0;
	}
        
        public double getMax(){
            return max;
        }
	@Override
	public String toString() {return String.format("%.3f\t%d", mean(), count);}
}

class Tally {
	final private List<Double> values;
	public Tally() {values = new ArrayList<>();}
	public double mean() {
		double sum = 0.0;
		for (Double value: values) sum += value;
		return sum / values.size();
	}
	public double stdDev() {
		double sum = 0.0;
		double mean = mean();
		for (Double value: values) sum +=  Math.pow(mean - value, 2);
		return Math.sqrt(sum / (values.size() - 1));
	}
	public void add(double value) {values.add(value);}
	public void clear() {values.clear();}
	@Override
	public String toString() {return "" + values;}
}

abstract class Event implements Comparable<Event> {
	protected double time = 0;
	public Event() {}
	public void time(double time) {
		assert (time >= 0): "Event.time: Time error.";
		this. time = time;
	}
	public double time() {return time;}
	public abstract void execute();
	@Override
	public int compareTo(Event e) {
		if (time > e.time) return 1;
		return -1;
	}
	@Override
	public String toString() {return this.getClass().getSimpleName() + " " + String.format("%.2f", time);}
}

abstract class Model {
	private Simulator simulator;
	protected void simulator(Simulator simulator) {this.simulator = simulator;}
	public Model() {}
	abstract protected void init();
	protected final void cancel(Event e) {simulator.cancel(e);}
	protected final Event schedule(Event ev, double delta) {
		assert (delta >= 0): "Model.schedule, time error! " + delta;
		simulator.schedule(ev, delta);
		return ev;
	}
	protected final boolean reschedule(Event e, double delta) {
		assert (delta >= 0): "Model.reschedule, time error! " + delta;
		return simulator.reschedule(e, delta);
	}
	protected final void clear() {simulator.clear();}
}

final class EventList {
	protected final PriorityQueue<Event> eventList;
	public EventList() {
		this.eventList = new PriorityQueue<>(10);
	}
	public boolean empty() {return eventList.isEmpty();}
	public void clear() {eventList.clear();}
	public void schedule(Event e, double time) {
		e.time(time);
		eventList.add(e);
	}
	public boolean reschedule(Event e, double time) {
		boolean bool = eventList.remove(e);
		e.time(time);
		eventList.add(e);
		return bool;
	}
	public void cancel(Event e) {eventList.remove(e);}
	@Override
	public String toString() {return eventList.toString();}
	public double timeNext() {return eventList.peek().time();}
	public Event pop() {return eventList.poll();}
}

final class Simulator {
	private final Model model;
	double clock;
	private final EventList events;
	public Simulator(Model model) {
		this.model = model;
		clock = 0.0;
		events = new EventList();
	}
	protected void cancel(Event e) {events.cancel(e);}
	public Event schedule(Event e, double delta) {
		assert (delta >= 0): "Simulator.schedule, time error! " + delta;
		events.schedule(e, clock + delta);
		return e;
	}
	public boolean reschedule(Event e, double delta) {
		assert (delta >= 0): "Simulation.reschedule, time error! " + delta;
		return events.reschedule(e, clock + delta);
	}
	public void clear() {events.clear();}
	public final void run() {
		model.init();
		while (! events.empty()) {
			Event event = events.pop();
			double curr = event.time();
			assert (curr >= clock): "Simulation.run, time error. " + curr + " > " + clock;
			clock = curr;
			event.execute();
		}
	}
}