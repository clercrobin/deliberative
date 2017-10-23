package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR, DFS }
	
	/* Environment */
	private Topology topology;
	private TaskDistribution td;
	
	/* the properties of the agent */
	private Agent agent;
	private int capacity;
	private double costPerKm;
	private Vehicle vehicle;
	int size;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.costPerKm = this.vehicle.costPerKm();
		this.capacity = this.vehicle.capacity();
		
		// initialize the planner
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		

	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		for (Task task:tasks){
			System.out.println(task);
		}


		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = AstarPlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = BFSPlan(vehicle, tasks);
			break;
		case DFS:
			// ...
			plan = DFSPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}




	// Mon BFS classique est ce ASTAR sans ajouter les getEStimatedCostToFinish
	public class StateComparator implements Comparator<MyState>
	{
		@Override
		public int compare(MyState x, MyState y)
		{
			if (x.getTotalCost()/*+x.getEstimatedCostToFinish()*/ < y.getTotalCost()/*+y.getEstimatedCostToFinish()*/) {
				return -1;
			} else {
				return +1;
			}
		}
	}


	public double computeExpectedCost(MyState state, Task[] tasksArray_){
		double additionnal = 0;
		int [] taskstatus = state.getTasksStatus();
		City myCity = state.getCurrentCity();
		for (int j =0; j<this.size;j++){
			if (taskstatus[j]==0 ){
				// We have to consider picking up task i

				Task task_ = tasksArray_[j];
				if ((task_.weight + state.getLoad())>this.capacity){
					additionnal += this.costPerKm*(myCity.distanceTo(task_.pickupCity) +
							task_.pickupCity.distanceTo(task_.deliveryCity) /*+
							task_.deliveryCity.distanceTo(myCity)*/);
				} else {
					return Double.POSITIVE_INFINITY;
				}



			} else if (taskstatus[j]==1 ){
				// We have to consider picking up task i
				Task task_ = tasksArray_[j];

				additionnal += this.costPerKm*(myCity.distanceTo(task_.deliveryCity) /*+ task_.deliveryCity.distanceTo(myCity)*/);




			}

		}
		return additionnal;
	}


	public double computeEstimatedNextCost(MyState state, Task[] tasksArray_){
		double max_additionnal = 0;
		int [] taskstatus = state.getTasksStatus();
		City myCity = state.getCurrentCity();
		for (int j =0; j<this.size;j++){
			if (taskstatus[j]==0 ){
				// We have to consider picking up task i

				Task task_ = tasksArray_[j];
				if ((task_.weight + state.getLoad())>this.capacity){
					double thisAdditionnal = this.costPerKm*(myCity.distanceTo(task_.pickupCity) +
							task_.pickupCity.distanceTo(task_.deliveryCity));// +
							//task_.deliveryCity.distanceTo(myCity));
					if (max_additionnal>thisAdditionnal){
						max_additionnal =thisAdditionnal;
					}

				} else {
					return Double.POSITIVE_INFINITY;
				}



			} else if (taskstatus[j]==1 ){
				// We have to consider picking up task i
				Task task_ = tasksArray_[j];

				double thisAdditionnal = this.costPerKm*(myCity.distanceTo(task_.deliveryCity));
				if (max_additionnal>thisAdditionnal){
					max_additionnal =thisAdditionnal;
				}



			}

		}
		return max_additionnal;

	}

	private Plan AstarPlan(Vehicle vehicle, TaskSet tasks) {
		System.out.println("Astar algorithm starts");
		long startTime = System.currentTimeMillis();

		int size = tasks.size();
		this.size = size;

		double minCost = Double.POSITIVE_INFINITY;
		MyState minState = null;
		int [] tasksDone = new int [size];
		for (int i = 0; i<size;i++) tasksDone[i] = 2;

		Task[] tasksArray = tasks.toArray(new Task[size]);
		int iter = 0;

		//Hashtable <Integer,MyState> alreadyVisited = new Hashtable<Integer, MyState> (3^(size-1));
		Hashtable <Integer,MyState> alreadyVisited = new Hashtable<Integer, MyState> ();
		City currentCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currentCity);
		int [] taskStatus = new int [size];
		for (int i = 0; i<size;i++) taskStatus[i] = 0;
		MyState firstState = new MyState(null, null, currentCity, 0,  0, taskStatus);

		Comparator<MyState> comparator = new StateComparator();
		PriorityQueue<MyState> queue =
				new PriorityQueue<MyState>(100, comparator);
		queue.add(firstState);

		alreadyVisited.put(firstState.getId(), firstState);

		do {
			//System.out.println(stack.size());
			if (queue.isEmpty()) {
				break;
			}
			iter++;

			MyState currentState = queue.poll();

			taskStatus = currentState.getTasksStatus().clone();
			//System.out.println(Arrays.toString(taskStatus));

			if (Arrays.equals(taskStatus, tasksDone)){
				if (currentState.getTotalCost() < minCost){
					minState = currentState;
					//System.out.println(currentState.getCurrentCity().name);
					minCost = currentState.getTotalCost();
					break;
				}
			}

			for (int i = 0; i<size; i++) {
				if (taskStatus[i]==0 ){
					// We have to consider picking up task i
					int [] newTaskStatus = taskStatus.clone();
					newTaskStatus[i]=1;
					Task task = tasksArray[i];
					MyAction myAction = new MyAction(task, true);
					double newCost = currentState.getTotalCost()+ currentState.getCurrentCity().distanceTo(task.pickupCity)*this.costPerKm;
					int newLoad = currentState.getLoad() + task.weight;
					if (newLoad<=this.capacity){
						MyState nextState = new MyState(currentState, myAction, task.pickupCity,newCost,newLoad,newTaskStatus);

						int key = nextState.getId();
						if (alreadyVisited.containsKey(key)) {
							MyState oldState = alreadyVisited.get(key);
							if (oldState.getTotalCost() > newCost) {
								double expected = this.computeExpectedCost(nextState, tasksArray);
								nextState.setEstimatedCostToFinish(expected);
								alreadyVisited.replace(key,oldState,nextState);
								queue.add(nextState);
							}
						} else {
							double expected = this.computeExpectedCost(nextState, tasksArray);
							nextState.setEstimatedCostToFinish(expected);
							queue.add(nextState);
							alreadyVisited.put(nextState.getId(), nextState);
						}
						//stack.push(nextState);

					}


				}
				else if (taskStatus[i]==1){
					// We have to consider delivering task i
					int [] newTaskStatus = taskStatus.clone();
					newTaskStatus[i]=2;

					Task task = tasksArray[i];
					MyAction myAction = new MyAction(task, false);
					double newCost = currentState.getTotalCost()+ currentState.getCurrentCity().distanceTo(task.deliveryCity)*this.costPerKm;
					int newLoad = currentState.getLoad() - task.weight;

					MyState nextState = new MyState(currentState, myAction, task.deliveryCity,newCost,newLoad,newTaskStatus);
					int key = nextState.getId();
					if (alreadyVisited.containsKey(key)) {
						MyState oldState = alreadyVisited.get(key);
						if (oldState.getTotalCost() > newCost) {
							double expected = this.computeExpectedCost(nextState, tasksArray);
							nextState.setEstimatedCostToFinish(expected);
							alreadyVisited.replace(key,oldState,nextState);
							queue.add(nextState);
						}
					} else {
						double expected = this.computeExpectedCost(nextState, tasksArray);
						nextState.setEstimatedCostToFinish(expected);
						queue.add(nextState);
						alreadyVisited.put(nextState.getId(), nextState);
					}
					//stack.push(nextState);
				}
				//System.out.println(Arrays.toString(taskStatus));
			}

		} while (true);

		long endTime = System.currentTimeMillis();
		System.out.println(iter + " " + "Minumum Cost: " + minCost);
		System.out.println("Execution time: " + (endTime - startTime) + "");

		getPlanFromLastState(minState, plan); // We already have set the first city
		System.out.println(plan);
		//System.out.println("plandone");
		return plan;
	}

	private Plan DFSPlan(Vehicle vehicle, TaskSet tasks) {
		System.out.println("DFS algorithm starts");
		long startTime = System.currentTimeMillis();

		int size = tasks.size();

		double minCost = Double.POSITIVE_INFINITY;
		MyState minState = null;
		int [] tasksDone = new int [size];
		for (int i = 0; i<size;i++) tasksDone[i] = 2;

		Task[] tasksArray = tasks.toArray(new Task[size]);
		int iter = 0;

		//Hashtable <Integer,MyState> alreadyVisited = new Hashtable<Integer, MyState> (3^(size-1));
		Hashtable <Integer,MyState> alreadyVisited = new Hashtable<Integer, MyState> ();
		City currentCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currentCity);
		int [] taskStatus = new int [size];
		for (int i = 0; i<size;i++) taskStatus[i] = 0;
		MyState firstState = new MyState(null, null, currentCity, 0,  0, taskStatus);
		Stack<MyState> stack = new Stack <MyState>();
		stack.add(firstState);
		alreadyVisited.put(firstState.getId(), firstState);

		do {
			//System.out.println(stack.size());
			if (stack.isEmpty()) {
				break;
			}
			iter++;

			MyState currentState = stack.pop();

			taskStatus = currentState.getTasksStatus().clone();
			//System.out.println(Arrays.toString(taskStatus));

			if (Arrays.equals(taskStatus, tasksDone)){
				if (currentState.getTotalCost() < minCost){
					minState = currentState;
					//System.out.println(currentState.getCurrentCity().name);
					minCost = currentState.getTotalCost();
					break;
				}
			}

			for (int i = 0; i<size; i++) {
				if (taskStatus[i]==0 ){
					// We have to consider picking up task i
					int [] newTaskStatus = taskStatus.clone();
					newTaskStatus[i]=1;
					Task task = tasksArray[i];
					MyAction myAction = new MyAction(task, true);
					double newCost = currentState.getTotalCost()+ currentState.getCurrentCity().distanceTo(task.pickupCity)*this.costPerKm;
					int newLoad = currentState.getLoad() + task.weight;
					if (newLoad<=this.capacity){
						MyState nextState = new MyState(currentState, myAction, task.pickupCity,newCost,newLoad,newTaskStatus);
						int key = nextState.getId();
						if (alreadyVisited.containsKey(key)) {
							MyState oldState = alreadyVisited.get(key);
							if (oldState.getTotalCost() > newCost) {
								alreadyVisited.replace(key,oldState,nextState);
								stack.push(nextState);
							}
						} else {
							stack.push(nextState);
							alreadyVisited.put(nextState.getId(), nextState);
						}
						//stack.push(nextState);

					}


				}
				else if (taskStatus[i]==1){
					// We have to consider delivering task i
					int [] newTaskStatus = taskStatus.clone();
					newTaskStatus[i]=2;

					Task task = tasksArray[i];
					MyAction myAction = new MyAction(task, false);
					double newCost = currentState.getTotalCost()+ currentState.getCurrentCity().distanceTo(task.deliveryCity)*this.costPerKm;
					int newLoad = currentState.getLoad() - task.weight;

					MyState nextState = new MyState(currentState, myAction, task.deliveryCity,newCost,newLoad,newTaskStatus);
					int key = nextState.getId();
					if (alreadyVisited.containsKey(key)) {
						MyState oldState = alreadyVisited.get(key);
						if (oldState.getTotalCost() > newCost) {
							alreadyVisited.replace(key,oldState,nextState);
							stack.push(nextState);
						}
					} else {
						stack.push(nextState);
						alreadyVisited.put(nextState.getId(), nextState);
					}
					//stack.push(nextState);
				}
				//System.out.println(Arrays.toString(taskStatus));
			}

		} while (true);

		long endTime = System.currentTimeMillis();
		System.out.println(iter + " " + "Minumum Cost: " + minCost);
		System.out.println("Execution time: " + (endTime - startTime) + "");

		getPlanFromLastState(minState, plan); // We already have set the first city
		System.out.println(plan);
		//System.out.println("plandone");
		return plan;
	}

	private Plan BFSPlan(Vehicle vehicle, TaskSet tasks) {
		System.out.println("BFS algorithm starts");
		long startTime = System.currentTimeMillis();

		int size = tasks.size();

		double minCost = Double.POSITIVE_INFINITY;
		MyState minState = null;
		int [] tasksDone = new int [size];
		for (int i = 0; i<size;i++) tasksDone[i] = 2;

		Task[] tasksArray = tasks.toArray(new Task[size]);
		int iter = 0;

		//Hashtable <Integer,MyState> alreadyVisited = new Hashtable<Integer, MyState> (3^(size-1));
		Hashtable <Integer,MyState> alreadyVisited = new Hashtable<Integer, MyState> ();
		City currentCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currentCity);
		int [] taskStatus = new int [size];
		for (int i = 0; i<size;i++) taskStatus[i] = 0;
		MyState firstState = new MyState(null, null, currentCity, 0,  0, taskStatus);
		LinkedList<MyState> queue;
		queue = new LinkedList<MyState>();
		queue.add(firstState);
		alreadyVisited.put(firstState.getId(), firstState);

		do {
			//System.out.println(stack.size());
			if (queue.isEmpty()) {
				break;
			}
			iter++;

			MyState currentState = queue.poll();

			taskStatus = currentState.getTasksStatus().clone();
			//System.out.println(Arrays.toString(taskStatus));

			if (Arrays.equals(taskStatus, tasksDone)){
				if (currentState.getTotalCost() < minCost){
					minState = currentState;
					//System.out.println(currentState.getCurrentCity().name);
					minCost = currentState.getTotalCost();
					break;
				}
			}

			for (int i = 0; i<size; i++) {
				if (taskStatus[i]==0 ){
					// We have to consider picking up task i
					int [] newTaskStatus = taskStatus.clone();
					newTaskStatus[i]=1;
					Task task = tasksArray[i];
					MyAction myAction = new MyAction(task, true);
					double newCost = currentState.getTotalCost()+ currentState.getCurrentCity().distanceTo(task.pickupCity)*this.costPerKm;
					int newLoad = currentState.getLoad() + task.weight;
					if (newLoad<=this.capacity){
						MyState nextState = new MyState(currentState, myAction, task.pickupCity,newCost,newLoad,newTaskStatus);
						int key = nextState.getId();
						if (alreadyVisited.containsKey(key)) {
							MyState oldState = alreadyVisited.get(key);
							if (oldState.getTotalCost() > newCost) {
								alreadyVisited.replace(key,oldState,nextState);
								queue.add(nextState);
							}
						} else {
							queue.add(nextState);
							alreadyVisited.put(nextState.getId(), nextState);
						}
						//stack.push(nextState);

					}


				}
				else if (taskStatus[i]==1){
					// We have to consider delivering task i
					int [] newTaskStatus = taskStatus.clone();
					newTaskStatus[i]=2;

					Task task = tasksArray[i];
					MyAction myAction = new MyAction(task, false);
					double newCost = currentState.getTotalCost()+ currentState.getCurrentCity().distanceTo(task.deliveryCity)*this.costPerKm;
					int newLoad = currentState.getLoad() - task.weight;

					MyState nextState = new MyState(currentState, myAction, task.deliveryCity,newCost,newLoad,newTaskStatus);
					int key = nextState.getId();
					if (alreadyVisited.containsKey(key)) {
						MyState oldState = alreadyVisited.get(key);
						if (oldState.getTotalCost() > newCost) {
							alreadyVisited.replace(key,oldState,nextState);
							queue.add(nextState);
						}
					} else {
						queue.add(nextState);
						alreadyVisited.put(nextState.getId(), nextState);
					}
					//stack.push(nextState);
				}
				//System.out.println(Arrays.toString(taskStatus));
			}

		} while (true);

		long endTime = System.currentTimeMillis();
		System.out.println(iter + " " + "Minumum Cost: " + minCost);
		System.out.println("Execution time: " + (endTime - startTime) + "");

		getPlanFromLastState(minState, plan); // We already have set the first city
		System.out.println(plan);
		//System.out.println("plandone");
		return plan;
	}

	public void getPlanFromLastState(MyState state, Plan plan) {
		System.out.println(Arrays.toString(state.getTasksStatus()));
		MyState parentState = state.getPrevious();
		if (parentState != null) {
			getPlanFromLastState(parentState, plan);
			MyAction myAction = state.getLastAction();
			Task task = myAction.getTask();
			City parentCity = parentState.getCurrentCity();
			City currentCity = state.getCurrentCity();

			for (City city : parentCity.pathTo(currentCity))
				plan.appendMove(city);
			if (myAction.isPickup()) {
				plan.appendPickup(task);
			} else {
				plan.appendDelivery(task);
			}
		}
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
