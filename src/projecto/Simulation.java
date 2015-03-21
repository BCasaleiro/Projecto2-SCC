package projecto;

public class Simulation {

    public Simulation() {
    }
    
    public void start() {
        Model model = new Server(1, 1, 3);
	Simulator simulator = new Simulator(model);
	model.simulator(simulator);
	simulator.run();
    }
}
