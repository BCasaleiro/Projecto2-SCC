package projecto;

public class Simulation {

    public Simulation() {
    }
    
    public void start() {
        Model model = new Server(1, 2, 2, 1);
	Simulator simulator = new Simulator(model);
	model.simulator(simulator);
	simulator.run();
    }
}
