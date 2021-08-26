package testbench;

import analysis.MultiGateway;
import simulation.Simulation;
import simulation.assets.Asset;
import simulation.assets.objects.Device;
import simulation.assets.objects.Satellite;
import simulation.structures.Ephemeris;
import simulation.structures.Interval;
import simulation.structures.OrbitalElements;
import simulation.utils.Reports;
import simulation.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Bench {

    public static void main(String[] args) {

//        example1();
//        example2();
//        example3();
//        example4();
//        example5();
//        example6();
//        example7();
        example8();
//        example9();
//        example10();

    }

    public static void example10() {

        System.out.println("Example 10 - Test overlay intervals computing");

        var multiGatewayAnalysis = new MultiGateway("devices.csv", "satellites.csv",
                "2020-03-20T11:00:00.000", "2020-03-21T11:00:00.000", 60D, 5);

        List<Interval> intervals = new ArrayList<>();

        intervals.add(new Interval(100L, 200L, 0, 0));
        intervals.add(new Interval(150L, 250L, 1, 0));
        intervals.add(new Interval(200L, 250L, 2, 0));
        intervals.add(new Interval(300L, 400L, 2, 0));

        multiGatewayAnalysis.setCurrentIntervals(intervals);
        multiGatewayAnalysis.computeDevicesPOV();
        Reports.printAccessCSV(multiGatewayAnalysis.getAllAccesses());
















    }

    public static void example9() {

        System.out.println("Example 9 - Compute multi gateway access");

        var multiGatewayAnalysis = new MultiGateway("devices.csv", "satellites.csv",
                "2020-03-20T11:00:00.000", "2020-03-21T11:00:00.000", 60D, 5);

        multiGatewayAnalysis.computeDevicesPOV();
        Reports.printAccessCSV(multiGatewayAnalysis.getAllAccesses());

    }

    public static void example8() {

        var simulation = new Simulation("2020-03-20T11:00:00.000", "2020-03-21T11:00:00.000", 1D, 5);

        List<Device> deviceList = Utils.devicesFromFile("devices.csv");
        List<Satellite> satellitesList = Utils.satellitesFromFile("satellites.csv");

        for (Satellite satellite : satellitesList) {
            simulation.setSatellite(satellite);

            for (Device device : deviceList) {
                simulation.setDevice(device);

//                System.out.println("Computing access intervals for satellite " + satellite.getId() + " to device " + device.getId());
                simulation.computeAccess();

                List<Interval> intervals = new ArrayList<>(simulation.getIntervals());
                List<Ephemeris> ephemerisList = new ArrayList<>();
                for (Interval interval : intervals) {   // TODO fix this
//                    System.out.println("Computing ephemeris for interval: " + Utils.unix2stamp(interval.getStart()) + " -> " + Utils.unix2stamp(interval.getEnd()));
                    simulation.computePVDBetween(interval.getStart(), interval.getEnd());
                    ephemerisList.addAll(simulation.getEphemerisList());
                }
                Reports.saveEphemerisCSV(ephemerisList, "C:\\Users\\Santi\\Desktop\\STARS\\MatlabFiles\\ephemeris.csv");
            }
        }
    }

    public static void example7() {

        var asset = new Device(25.0, 45.0, 788.0);
        var satellite = new Satellite("2020-03-20T02:26:56.031", 7000672.074930292,
                1.478E-4, 97.8877, 267.4896, 80.1454, 279.9910,
                -0.12340e-4, -0.00000150 * 3.141592653589793D / 1.86624E9D,
                0);

        var simulation = new Simulation("2020-03-20T11:00:00.000", "2020-03-21T11:00:00.000", asset, satellite, 60D, 5);

        simulation.computeAccess();
        Reports.printAccessReport(simulation.getIntervals());
        List<Interval> intervals = new ArrayList<>(simulation.getIntervals());

        for (Interval interval : intervals) {
            System.out.println("Computing ephemeris for interval: " + Utils.unix2stamp(interval.getStart()) + " -> " + Utils.unix2stamp(interval.getEnd()));
            simulation.computePVDBetween(interval.getStart(), interval.getEnd());
            Reports.printEphemerisReport(simulation.getEphemerisList());
        }

    }

    public static void example6() {

        var simulation = new Simulation("2020-03-20T11:00:00.000", "2020-03-21T11:00:00.000", 60D, 5);

        List<Device> deviceList = Utils.devicesFromFile("devices.csv");
        List<Satellite> satellitesList = Utils.satellitesFromFile("satellites.csv");

        for (Satellite satellite : satellitesList) {
            simulation.setSatellite(satellite);
            for (Device device : deviceList) {
                simulation.setDevice(device);
                simulation.computeAccess();
                Reports.printAccessCSV(simulation.getIntervals());
            }
        }

    }

    public static void example5() {

        List<Satellite> assetList = Utils.satellitesFromFile("satellites.csv");

        for (Asset asset : assetList) {
            Satellite sat = (Satellite) asset;
            OrbitalElements elements = sat.getElements();
            System.out.println(assetList.indexOf(asset) + " - " + elements.getSemiMajorAxis() + ","
                    + elements.getEccentricity() + "," + elements.getInclination()
                    + "," + elements.getRightAscension() + "," + elements.getArgOfPerigee() + ","
                    + elements.getAnomaly());
        }

    }

    public static void example4() {

        List<Device> assetList = Utils.devicesFromFile("devices.csv");

        for (Asset asset : assetList) {
            System.out.println(assetList.indexOf(asset) + " - " + asset.getLat() + "," + asset.getLon() + "," + asset.getHeight());
        }

    }

    public static void example1() {

        System.out.println("Example 1 - Compute access, satellite from TLE");

        var card1 = "1 43641U 18076A   20080.10203740 -.00000150  00000-0 -12340-4 0  9999";
        var card2 = "2 43641  97.8877 267.4896 0001478  80.1454 279.9910 14.82153417 78367";

        var satellite = new Satellite(card1, card2);
        var asset = new Device(25.0, 45.0, 788.0);
        var simulation = new Simulation("2020-03-20T11:00:00.000", "2020-03-21T11:00:00.000", asset, satellite, 60D, 5);

        simulation.computeAccess();

        Reports.printSatelliteInfo(satellite);
//        Reports.printAccessReport(simulation);
        Reports.printAccessCSV(simulation.getIntervals());

    }

    public static void example2() {

        System.out.println("Example 2 - Compute access, satellite from orbital elements");

        var asset = new Device(25.0, 45.0, 788.0);
        var satellite = new Satellite("2020-03-20T02:26:56.031", 7000672.074930292,
                1.478E-4, 97.8877, 267.4896, 80.1454, 279.9910,
                -0.12340e-4, -0.00000150 * 3.141592653589793D / 1.86624E9D,
                0);

        // This info is only relevant to generate the TLE
        satellite.setData(0, 43641, 2018, 76, "A", 'U', 7836, 999);

        var simulation = new Simulation("2020-03-20T11:00:00.000", "2020-03-21T11:00:00.000", asset, satellite, 60D, 5);

        simulation.computeAccess();

        Reports.printSatelliteInfo(satellite);
        Reports.printAccessReport(simulation.getIntervals());

    }

    public static void example3() {

        System.out.println("Example 3 - get position, velocity and doppler shift");

        var asset = new Device(25.0, 45.0, 788.0);
        var satellite = new Satellite("2020-03-20T02:26:56.031", 7000672.074930292,
                1.478E-4, 97.8877, 267.4896, 80.1454, 279.9910,
                -0.12340e-4, -0.00000150 * 3.141592653589793D / 1.86624E9D,
                0);

        var simulation = new Simulation("2020-03-20T11:00:00.000", "2020-03-20T12:00:00.000", asset, satellite, 60D, 5);

        simulation.computePVD();
        Reports.printEphemerisReport(simulation.getEphemerisList());

    }

}
