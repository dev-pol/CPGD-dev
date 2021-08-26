package simulation.structures;

public class Event {

    private long time;
    private int who;

    public Event(long time, int who) {
        this.time = time;
        this.who = who;
    }

    public long getTime() {
        return time;
    }

    public int getWho() {
        return who;
    }

}
