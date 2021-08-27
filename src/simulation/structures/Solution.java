package simulation.structures;

import simulation.assets.objects.Device;
import simulation.assets.objects.Satellite;

import java.util.ArrayList;
import java.util.List;

/**
 * This class stores a solution found by the GradientSearch Algorithm
 **/
public class Solution {

    private final int nOfPlanes;
    private final int nOfSatsPerPlane;
    private final double inclination;
    private final double mcg;
    private final List<Device> devices;
    private final List<Satellite> satellites;
    private final int[] discardedSolutions;

    public Solution(int nOfPlanes, int nOfSatsPerPlane, double inclination, double mcg, List<Device> devices,
                    List<Satellite> satellites, int[] discardedSolutions) {
        this.nOfPlanes = nOfPlanes;
        this.nOfSatsPerPlane = nOfSatsPerPlane;
        this.inclination = inclination;
        this.mcg = mcg;
        this.devices = new ArrayList<>(devices);
        this.satellites = new ArrayList<>(satellites);
        this.discardedSolutions = discardedSolutions.clone();
    }

    public int getnOfPlanes() {
        return nOfPlanes;
    }

    public int getnOfSatsPerPlane() {
        return nOfSatsPerPlane;
    }

    public double getInclination() {
        return inclination;
    }

    public double getMcg() {
        return mcg;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public List<Satellite> getSatellites() {
        return satellites;
    }

    public int[] getDiscardedSolutions() {
        return discardedSolutions;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder(nOfPlanes + "," +
                nOfSatsPerPlane + "," +
                inclination + "," +
                mcg);

        for (int discarded : discardedSolutions) {
            sb.append(",");
            sb.append(discarded);
        }

        return sb.toString();

    }
}
