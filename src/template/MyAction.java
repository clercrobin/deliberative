package template;

import logist.task.Task;
import logist.topology.Topology;

public class MyAction {

    private boolean delivery;
    private boolean pickup;
    private Task task;

    public MyAction(Task task_, boolean pickup_) {
        this.pickup = pickup_;
        this.delivery = !pickup_;
        this.task = task_;
    }

    public boolean isDelivery() {
        return delivery;
    }

    public void setDelivery(boolean delivery) {
        this.delivery = delivery;
    }

    public boolean isPickup() {
        return pickup;
    }

    public void setPickup(boolean pickup) {
        this.pickup = pickup;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }
}
