package analysis;

import simulation.Simulation;
import simulation.assets.objects.Device;
import simulation.assets.objects.Satellite;
import simulation.structures.Event;
import simulation.structures.Interval;
import simulation.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class MultiGateway {

    private final Simulation simulation = new Simulation();
    private List<Device> devices;
    private List<Satellite> satellites;
    private List<Interval> currentIntervals = new ArrayList<>();
    private List<Interval> combinedIntervals = new ArrayList<>();
    private List<Interval> allAccesses = new ArrayList<>();
    private boolean includeCoverageGaps = false;
    private int povOption = 0;
    private double maxMCG = Double.MAX_VALUE;
    private long lastSimTime = 0;

    public MultiGateway() {

    }

    public MultiGateway(String dateStart, String dateEnd, double step, double visibilityThreshold) {
        this.simulation.setParams(dateStart, dateEnd, step, visibilityThreshold);
    }

    public MultiGateway(List<Device> gateways, List<Satellite> satellites) {
        this.devices = gateways;
        this.satellites = satellites;
    }

    public MultiGateway(List<Device> gateways, List<Satellite> satellites, String dateStart, String dateEnd, double step, double visibilityThreshold) {
        this.devices = gateways;
        this.satellites = satellites;
        this.simulation.setParams(dateStart, dateEnd, step, visibilityThreshold);
    }

    public MultiGateway(String gatewaysFile, String satellitesFile, String dateStart, String dateEnd, double step, double visibilityThreshold) {
        this(Utils.devicesFromFile(gatewaysFile), Utils.satellitesFromFile(satellitesFile), dateStart, dateEnd, step, visibilityThreshold);
    }

    public void setPovOption(int povOption) {
        this.povOption = povOption >= 1 ? 1 : 0;
    }

    public void setIncludeCoverageGaps(boolean includeCoverageGaps) {
        this.includeCoverageGaps = includeCoverageGaps;
    }

    public void setCurrentIntervals(List<Interval> currentIntervals) {
        this.currentIntervals = currentIntervals;
    }

    public void setAssets(List<Device> gateways, List<Satellite> satellites) {
        this.devices = gateways;
        this.satellites = satellites;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public void setSatellites(List<Satellite> satellites) {
        this.satellites = satellites;
    }

    public void addDevice(Device device) {
        devices.add(device);
    }

    public void addSatellite(Satellite satellite) {
        satellites.add(satellite);
    }

    public void setScenarioParams(String start, String end, double step, double th) {
        simulation.setParams(start, end, step, th);
    }

    public List<Interval> getCombinedIntervals() {
        return combinedIntervals;
    }

    public List<Interval> getAllAccesses() {
        return allAccesses;
    }

    public List<Interval> getCurrentIntervals() {
        return currentIntervals;
    }

    public double getMaxMCG() {
        return this.maxMCG;
    }

    public double getMaxMCGMinutes() {
        return Math.round((this.maxMCG / (60.0 * 1000.0)) * 100000.0) / 100000.0;
    }

    public long getLastSimTime() {
        return this.lastSimTime;
    }

    public void computeDevicesPOV() {

        long t0 = System.currentTimeMillis();

        setPovOption(0);

        allAccesses.clear();
        currentIntervals.clear();

        if (devices.isEmpty() || satellites.isEmpty()) {
            System.out.println("Check assets!");
            return;
        }

        for (Device device : devices) {

            currentIntervals.clear();

            for (Satellite satellite : satellites) {
                simulation.setAssets(device, satellite);
                simulation.computeAccess();
                synchronized (simulation) {
                    if (simulation.getIntervals().isEmpty()) {
                        allAccesses.add(new Interval(Utils.stamp2unix(simulation.getStartTime()), Utils.stamp2unix(simulation.getEndTime()),
                                new ArrayList<>(device.getId()), new ArrayList<>()));
                    } else {
                        currentIntervals.addAll(simulation.getIntervals());
                    }
                }
            }

            currentIntervals.sort((i1, i2) -> (int) (i1.getStart() - i2.getStart()));

            allAccesses.addAll(computeDevices2Constellation());

        }

        lastSimTime = System.currentTimeMillis() - t0;

    }

    public void computeSatellitesPOV() {

        setPovOption(1);

        allAccesses.clear();
        currentIntervals.clear();

        if (devices.isEmpty() || satellites.isEmpty()) {
            System.out.println("Check assets!");
            return;
        }

        for (Satellite satellite : satellites) {

            currentIntervals.clear();

            for (Device device : devices) {
                simulation.setAssets(device, satellite);
                simulation.computeAccess();
                synchronized (simulation) {
                    if (simulation.getIntervals().isEmpty()) {
                        allAccesses.add(new Interval(Utils.stamp2unix(simulation.getStartTime()), Utils.stamp2unix(simulation.getEndTime()),
                                new ArrayList<>(device.getId()), new ArrayList<>()));
                    } else {
                        currentIntervals.addAll(simulation.getIntervals());
                    }
                }
            }

            currentIntervals.sort((i1, i2) -> (int) (i1.getStart() - i2.getStart()));

            allAccesses.addAll(computeConstellation2Devices());

        }
    }

    /**
     *
     **/
    private List<Interval> computeConstellation2Devices() {

        if (currentIntervals.size() <= 1) {
            return currentIntervals;
        }

        combinedIntervals.clear();
        Set<Integer> inContact = new LinkedHashSet<>();
        List<Event> eventList = intervals2eventsSatPOV(currentIntervals);
        eventList = eventList.subList(1, eventList.size());

        var currentInterval = currentIntervals.get(0);
        inContact.add(currentInterval.getFirstFrom());

        for (Event event : eventList) {
            if (!inContact.contains(event.getWho())) {  // If I establish contact with a new asset
                inContact.add(event.getWho());
                if (includeCoverageGaps || !currentInterval.getFromAssets().isEmpty()) {
                    combinedIntervals.add(new Interval(currentInterval.getStart(), event.getTime(), currentInterval.getFromAssets(), currentInterval.getToAssets()));
                }
                currentInterval.addFrom(event.getWho());
            } else {    // If the asset is already in contact
                inContact.remove(event.getWho());
                combinedIntervals.add(new Interval(currentInterval.getStart(), event.getTime(), currentInterval.getFromAssets(), currentInterval.getToAssets()));
                currentInterval.removeFrom(event.getWho());
            }
            currentInterval.setStart(event.getTime());
        }

        return combinedIntervals;

    }

    /**
     *
     **/
    public List<Interval> computeDevices2Constellation() {

        if (currentIntervals.size() <= 1) {
            return currentIntervals;
        }

        combinedIntervals.clear();
        Set<Integer> inContact = new LinkedHashSet<>();
        List<Event> eventList = intervals2eventsDevicePOV(currentIntervals);
        eventList = eventList.subList(1, eventList.size());

        var currentInterval = currentIntervals.get(0);
        inContact.add(currentInterval.getFirstTo());

        for (Event event : eventList) {
            if (!inContact.contains(event.getWho())) {  // If I establish contact with a new asset
                inContact.add(event.getWho());
                if (includeCoverageGaps || !currentInterval.getToAssets().isEmpty()) {
                    combinedIntervals.add(new Interval(currentInterval.getStart(), event.getTime(), currentInterval.getFromAssets(), currentInterval.getToAssets()));
                }
                currentInterval.addTo(event.getWho());
            } else {    // If the asset is already in contact
                inContact.remove(event.getWho());
                combinedIntervals.add(new Interval(currentInterval.getStart(), event.getTime(), currentInterval.getFromAssets(), currentInterval.getToAssets()));
                currentInterval.removeTo(event.getWho());
            }
            currentInterval.setStart(event.getTime());
        }

        return combinedIntervals;

    }

    private List<Event> intervals2events(List<Interval> intervals) {

        List<Event> eventsList = new ArrayList<>();

        switch (povOption) {
            case 1:
                for (Interval interval : intervals) {
                    eventsList.add(new Event(interval.getStart(), interval.getFirstTo()));
                    eventsList.add(new Event(interval.getEnd(), interval.getFirstTo()));
                }
                break;
            case 0:
            default:
                for (Interval interval : intervals) {
                    eventsList.add(new Event(interval.getStart(), interval.getFirstFrom()));
                    eventsList.add(new Event(interval.getEnd(), interval.getFirstFrom()));
                }
                break;
        }

        eventsList.sort((e1, e2) -> (int) (e1.getTime() - e2.getTime()));
        return eventsList;

    }

    private List<Event> intervals2eventsSatPOV(List<Interval> intervals) {

        List<Event> eventsList = new ArrayList<>();

        for (Interval interval : intervals) {
            eventsList.add(new Event(interval.getStart(), interval.getFirstFrom()));
            eventsList.add(new Event(interval.getEnd(), interval.getFirstFrom()));
        }

        eventsList.sort((e1, e2) -> (int) (e1.getTime() - e2.getTime()));
        return eventsList;

    }

    private List<Event> intervals2eventsDevicePOV(List<Interval> intervals) {

        List<Event> eventsList = new ArrayList<>();

        for (Interval interval : intervals) {
            eventsList.add(new Event(interval.getStart(), interval.getFirstTo()));
            eventsList.add(new Event(interval.getEnd(), interval.getFirstTo()));
        }

        eventsList.sort((e1, e2) -> (int) (e1.getTime() - e2.getTime()));
        return eventsList;

    }

    /**
     * Returns a new filtered List containing only intervals that include contacts from/to at least N devices to
     * a single gateway
     **/
    public List<Interval> filterAtLeastNDevices(List<Interval> list, int n) {

        switch (povOption) {
            case 0:
                return list.stream().filter(interval -> interval.getToAssets().size() >= n).collect(Collectors.toList());
            case 1:
            default:
                return list.stream().filter(interval -> interval.getFromAssets().size() >= n).collect(Collectors.toList());
        }

    }

    /**
     * Returns a new filtered List containing only intervals that include contacts from/to at least N devices to
     * a single gateway
     **/
    public List<Interval> filterGetGaps(List<Interval> list) {

        switch (povOption) {
            case 0:
                return list.stream().filter(interval -> interval.getToAssets().isEmpty()).collect(Collectors.toList());
            case 1:
            default:
                return list.stream().filter(interval -> interval.getFromAssets().isEmpty()).collect(Collectors.toList());
        }

    }

    /**
     * computes the Maximum Coverage Gap for the computed accesses, unless they were not computed
     **/
    public void computeMaxMCG() {
        computeMaxMCG(allAccesses);
    }

    /**
     * Computes the Maximum Coverage Gap for a given list (MCG) in milliseconds
     *
     **/
    public void computeMaxMCG(List<Interval> list) {

        Interval maxMCGInterval = new Interval(simulation.getStartTimeUnix(), simulation.getEndTimeUnix());
        try {
            switch (povOption) {
                case 0:
                    maxMCGInterval = Collections.max(list.stream().filter(interval -> interval.getToAssets().isEmpty()).collect(Collectors.toList()),
                            (d1, d2) -> (int) (d1.getDuration() - d2.getDuration()));
                    break;
                case 1:
                default:
                    maxMCGInterval = Collections.max(list.stream().filter(interval -> interval.getFromAssets().isEmpty()).collect(Collectors.toList()),
                            (d1, d2) -> (int) (d1.getDuration() - d2.getDuration()));
                    break;
            }
        } catch (NoSuchElementException nse) {
            System.out.println("No such element exception");
            this.maxMCG = simulation.getTimeSpan() / (1000.0 * 60.0);
        }

        this.maxMCG = maxMCGInterval.getDuration();
    }


}
