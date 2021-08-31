package simulation.utils;

import org.orekit.propagation.analytical.tle.TLE;
import simulation.Simulation;
import simulation.assets.objects.Device;
import simulation.assets.objects.Satellite;
import simulation.structures.Ephemeris;
import simulation.structures.Event;
import simulation.structures.Interval;
import simulation.structures.Solution;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Reports {

    public static String SEPARATOR_HALF = "======================================================================";

    /**
     * Prints the access report for a given simulation
     */
    public static void printAccessReport(List<Interval> intervals) {

        System.out.println("N " + ' ' + "From" + ' ' + "To" + ' ' + "Start" + '\t' + "End" + '\t' + "Duration");

        for (Interval interval : intervals) {
            System.out.println((intervals.indexOf(interval)  + 1) + " " + interval.getFromAssets() + " "
                    + interval.getToAssets() + " " + Utils.unix2stamp(interval.getStart()) + '\t'
                    + Utils.unix2stamp(interval.getEnd())
                    + '\t' + interval.getDuration());
        }

    }

    public static void saveAccessCSV(List<Interval> intervals, String path) {
        try (FileWriter writer = new FileWriter(path)) {
            for (Interval interval : intervals) {
                writer.write(interval.getFromAssets() + "," + interval.getToAssets() + "," + interval.getStart() + "," + interval.getEnd() + '\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveNOfAssetsInContactCSV(List<Interval> intervals, String path) {
        try (FileWriter writer = new FileWriter(path)) {
            for (Interval interval : intervals) {
                writer.write(interval.getFirstFrom() + "," + interval.getToAssets().size() + "," + interval.getStart() + "," + interval.getEnd() + '\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the access report for a given interval list in csv form
     */
    public static void printDevicesInfo(List<Device> devices) {
        for (Device device : devices) {
            System.out.println(device.getId() + "," + device.getLat() + "," + device.getLon()
                    + "," + device.getHeight());
        }
    }

    /**
     * Saves the id,lat,long,height data for a list of devices onto a CSV file
     */
    public static void saveDevicesInfo(List<Device> devices, String path) {
        try (FileWriter writer = new FileWriter(path)) {
            for (Device device : devices) {
                writer.write(device.getId() + "," + device.getLat() + "," + device.getLon()
                        + "," + device.getHeight() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the access report for a given interval list in csv form
     */
    public static void printAccessCSV(List<Interval> intervals) {
        for (Interval interval : intervals) {
            System.out.println(interval.getFromAssets() + "," + interval.getToAssets() + "," + interval.getStart()
                    + "," + interval.getEnd() + "," + interval.getDuration());
        }
    }

    /**
     * Prints the access report for a given interval list in csv form
     */
    public static void printMetricCSV(List<Interval> intervals) {
        for (Interval interval : intervals) {
            System.out.println(interval.getStart() + "," + interval.getEnd() + "," + interval.getMetric());
        }
    }

    public static void saveMetricCSV(List<Interval> intervals) {
        try (FileWriter writer = new FileWriter("C:\\Users\\Santi\\Desktop\\STARS\\MatlabFiles\\data.csv")) {
            for (Interval interval : intervals) {
                writer.write(interval.getStart() + "," + interval.getEnd() + "," + interval.getMetric() + '\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the access report for a given interval list in csv form
     */
    public static void printEventReport(List<Event> events) {
        for (Event event : events) {
            System.out.println(event.getWho() + " at " + event.getTime());
        }
    }

    /**
     * Prints an ephemeris report for the given Ephemeris List
     */
    public static void printEphemerisReport(List<Ephemeris> ephemerisList) {

        for (Ephemeris ephemeris : ephemerisList) {
            System.out.println(Utils.unix2stamp(ephemeris.getTime()) + "," + ephemeris.getPosX() + "," + ephemeris.getPosY() + "," + ephemeris.getPosZ() + "," + ephemeris.getRange() + "," + ephemeris.getDopplerShift());
        }

    }

    /**
     * Prints the an ephemeris report in csv format
     */
    public static void printEphemerisCSV(Simulation simulation) {

        List<Ephemeris> ephemerisList = simulation.getEphemerisList();

        for (Ephemeris ephemeris : ephemerisList) {
            System.out.println(Utils.unix2stamp(ephemeris.getTime()) + "," + ephemeris.getPosX() + "," + ephemeris.getPosY() + "," + ephemeris.getPosZ() + "," + ephemeris.getRange() + "," + ephemeris.getDopplerShift());
        }

    }

    public static void saveEphemerisCSV(List<Ephemeris> ephemerisList, String path) {
        try (FileWriter writer = new FileWriter(path)) {
            for (Ephemeris ephemeris : ephemerisList) {
                writer.write(ephemeris.getTime() + "," + ephemeris.getPosX() + "," + ephemeris.getPosY() + "," + ephemeris.getPosZ() + "," + ephemeris.getRange() + "," + ephemeris.getDopplerShift() + '\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the elements of each satellite in a Satellite List
     */
    public static void printSatellitesElements(List<Satellite> satelliteList) {
        for (Satellite satellite : satelliteList) {
            var orbitalElements = satellite.getElements();
            if (orbitalElements == null) {
                orbitalElements = Utils.tle2elements(satellite.getTLE1(), satellite.getTLE2());
                satellite.setElements(orbitalElements);
            }
            System.out.println("a: " + orbitalElements.getSemiMajorAxis() + " , e: " + orbitalElements.getEccentricity() + " , i: " + orbitalElements.getInclination()
                    + " , RAAN: " + orbitalElements.getRightAscension() + ", pa: " + orbitalElements.getArgOfPerigee() + " , v: " + orbitalElements.getAnomaly());
        }
    }

    /**
     * Prints detailed info of each satellite in a Satellite List
     */
    public static void printSatellitesInfo(List<Satellite> satelliteList) {
        for (Satellite satellite : satelliteList) {
            printSatelliteInfo(satellite);
        }
    }

    /**
     * Prints a Satellite's object info
     */
    public static void printSatelliteInfo(Satellite satellite) {
        TLE tle;
        if (satellite.getTLE1().isEmpty() || satellite.getTLE2().isEmpty()) {
            tle = Utils.satellite2tle(satellite);
            satellite.setTLE(tle.getLine1(), tle.getLine2());
        } else {
            tle = new TLE(satellite.getTLE1(), satellite.getTLE2());
        }
        System.out.println("Elements date: " + tle.getDate());

        System.out.println("TLE1: " + satellite.getTLE1());
        System.out.println("TLE2: " + satellite.getTLE2());
        var orbitalElements = satellite.getElements();
        if (orbitalElements == null) {
            orbitalElements = Utils.tle2elements(satellite.getTLE1(), satellite.getTLE2());
            satellite.setElements(orbitalElements);
        }
        System.out.println("a: " + orbitalElements.getSemiMajorAxis() + " , e: " + orbitalElements.getEccentricity() + " , i: " + orbitalElements.getInclination()
                + " , RAAN: " + orbitalElements.getRightAscension() + ", pa: " + orbitalElements.getArgOfPerigee() + " , v: " + orbitalElements.getAnomaly());
    }

    /**
     * Prints the solutions report for a given simulation
     */
    public static void printSolutionReport(List<Solution> solutions) {

        System.out.println("Solutions report");

        for (Solution solution : solutions) {
            System.out.println(solution.toString());
        }

    }

    /**
     * Saves the solutions report for a given simulation
     */
    public static void saveSolutionCSV(List<Solution> solutions, String path) {

        try (FileWriter writer = new FileWriter(path)) {
            writer.write("Planes,SatsPerPlane,inclination,MCG,Rejected0,Rejected1,Rejected2,Rejected3,Rejected4" + "\n");
            for (Solution solution : solutions) {
                writer.write(solution.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Saves a String List to a new file, each entry as a line
     */
    public static void saveLog(List<String> log, String path) {
        try (FileWriter writer = new FileWriter(path)) {
            for (String entry : log) {
                writer.write(entry + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Appends a String List to a file, each entry as a line
     */
    public static void appendLog(List<String> log, String path) {
        try (FileWriter writer = new FileWriter(path,true)) {
            for (String entry : log) {
                writer.write(entry + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
