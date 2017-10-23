package template;

import logist.task.Task;
import logist.topology.Topology;

import java.util.Arrays;

public class MyState {

    private Topology.City currentCity;
    private int load;
    private double totalCost;
    private double estimatedCostToFinish;
    private int[] tasksStatus; // 0 for available, 1 for carried, 2 for delivered Or scind in three variables
    private MyState previous;
    private MyAction lastAction;
    private int id;



    public MyState(MyState previous_, MyAction lastAction_, Topology.City currentCity_, double totalCost_, int load_, int [] tasksStatus_) {
        this.previous = previous_;
        this.lastAction = lastAction_;
        this.currentCity = currentCity_;
        this.totalCost = totalCost_;
        this.load = load_;
        this.tasksStatus = tasksStatus_;
        this.id = generateId(tasksStatus_, currentCity_.id);
        this.estimatedCostToFinish = 0;
    }


    public Topology.City getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(Topology.City currentCity) {
        this.currentCity = currentCity;
    }

    public int getLoad() {
        return load;
    }

    public void setLoad(int load) {
        this.load = load;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public int[] getTasksStatus() {
        return tasksStatus;
    }

    public void setTasksStatus(int[] tasksStatus) {
        this.tasksStatus = tasksStatus;
    }

    public MyState getPrevious() {
        return previous;
    }

    public void setPrevious(MyState previous) {
        this.previous = previous;
    }

    public MyAction getLastAction() {
        return lastAction;
    }

    public void setLastAction(MyAction lastAction) {
        this.lastAction = lastAction;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MyState other = (MyState) obj;
        return ((currentCity == other.getCurrentCity())&& (Arrays.equals(tasksStatus,other.getTasksStatus())));
    }

    public int generateId(int [] tasksStatus, int currentCityId){
        int size = tasksStatus.length;
        int id = 0;
        for (int i = 0; i<size;i++){
            id += (tasksStatus[i]+1)*(10^i);
        }
        id += (10^(size+1))+currentCityId+1;
        return id;

    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    public double getEstimatedCostToFinish() {
        return estimatedCostToFinish;
    }

    public void setEstimatedCostToFinish(double estimatedCostToFinish) {
        this.estimatedCostToFinish = estimatedCostToFinish;
    }
}
