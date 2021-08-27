package analysis;

import simulation.MultiGateway;
import simulation.assets.objects.Device;
import simulation.assets.objects.Satellite;
import simulation.structures.Solution;
import simulation.utils.Reports;
import simulation.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CPGD {

    static final Properties properties = Utils.loadProperties();

    static final String FILE_PATH = (String) properties.get("output_path");
    static final String START_DATE = (String) properties.get("start_date");
    static final String END_DATE = (String) properties.get("end_date");
    static final String SEARCH_DATE = (String) properties.get("search_date");
    static final double TIME_STEP = Double.parseDouble((String) properties.get("time_step"));
    static final double VISIBILITY_THRESHOLD = Double.parseDouble((String) properties.get("visibility_threshold"));

    static final String CSV_EXTENSION = ".csv";
    static final String LOG_EXTENSION = ".log";

    static List<String> statsLogger = new ArrayList<>();

    public static void main(String[] args) {

        var multiGatewayAnalysis = new MultiGateway(START_DATE, SEARCH_DATE, TIME_STEP, VISIBILITY_THRESHOLD);

        multiGatewayAnalysis.setIncludeCoverageGaps(true);

        // Maximum Coverage Gap in minutes
        double maxMCG = 120;

        // Maximum coverage latitude
        double latMax = 80;

        // Minimum and maximum values for scenario parameters
        int minPlanes = 3;
        int maxPlanes = 4;
        int minSatsInPlane = 3;
        int maxSatsInPlane = 4;
        double minInclination = getInclination(getLambdaMax(new Satellite(START_DATE,
                7000672.074930292, 1.478E-4, 45,
                250, 80, 0), 5), latMax);
        minInclination = Math.round(minInclination * 100.0) / 100.0;
        double maxInclination = 90;
        double inclinationStep = 5;

        statsLogger.add("Starting analysis at " + Utils.unix2stamp(System.currentTimeMillis()));
        statsLogger.add("Scenario start: " + START_DATE + " - Scenario end: " + END_DATE);
        statsLogger.add("Target MCG: " + maxMCG + " - Maximum latitude band: " + latMax
                + " Degrees - complexity 0 search date " + SEARCH_DATE);
        statsLogger.add("Minimum number of planes: " + minPlanes + " - Maximum number of planes: " + maxPlanes);
        statsLogger.add("Minimum sats per plane: " + minSatsInPlane + " - Maximum sats per planes: " + maxSatsInPlane);
        statsLogger.add("Minimum inclination: " + minInclination + " - Maximum inclination: " + maxInclination);
        statsLogger.add("Inclination step: " + inclinationStep + " Degrees");
        statsLogger.add("====================================================================== PROGRESS " +
                "======================================================================");

        // Amount of discarded solutions at each complexity step
        int[] discarded = new int[5];

        // Initial solution
        int currentPlanes = minPlanes;
        int currentSatsInPlane = minSatsInPlane;
        double currentInclination = minInclination;

        boolean go = true;
        boolean solutionFound = false;

        // Solution list
        List<Solution> solutions = new ArrayList<>();

        // Devices list
        List<Device> devices = new ArrayList<>();

        // Satellites list
        List<Satellite> satellites = new ArrayList<>();

        // grid resolution longitude - wise
        double longitudeResolution = maxMCG * 0.25;

        // Number of grid points according to the grid resolution
        int nFacilities = (int) Math.round(360 / longitudeResolution);

        while (go) {

            System.out.println("Performing: " + currentPlanes + "-" + currentSatsInPlane + "-" + currentInclination);

            // Set "first look" scenario time
            multiGatewayAnalysis.setScenarioParams(START_DATE, SEARCH_DATE, TIME_STEP, VISIBILITY_THRESHOLD);

            devices.clear();

            // Generate list of devices on the equator, maximum latitude band and at half-way in-between
            int facId = 0;
            for (int fac = 0; fac < nFacilities; fac++) {
                devices.add(new Device(facId++, 0.0, fac * longitudeResolution, 0.0));
                devices.add(new Device(facId++, latMax / 2, fac * longitudeResolution, 0.0));
                devices.add(new Device(facId++, latMax, fac * longitudeResolution, 0.0));
            }

            double planePhase = 360.0 / currentPlanes;
            double satsPhase = 360.0 / currentSatsInPlane;

            satellites.clear();

            // Generate the constellation
            int satId = 0;
            for (int plane = 0; plane < currentPlanes; plane++) {   // Plane
                for (int sat = 0; sat < currentSatsInPlane; sat++) {    // Satellites
                    satellites.add(new Satellite(satId++, START_DATE, 7000000.0,
                            1.0E-4, currentInclination, plane * planePhase, 0,
                            sat * satsPhase));  // - plane * (planePhase / currentSatsInPlane)
                }
            }

            // Set assets (list of devices + constellation) in the analyzer
            multiGatewayAnalysis.setAssets(devices, satellites);

            // Compute access intervals
            multiGatewayAnalysis.computeDevicesPOV();

            // Compute MCG
            multiGatewayAnalysis.computeMaxMCG();

            // Complexity increase algorithm

            int complexity = 0;

            log("Analyzing: " + currentPlanes + " planes with " + currentSatsInPlane
                    + " satellites at " + currentInclination + " degrees. Complexity level: " + complexity
                    + " > MCG: " + multiGatewayAnalysis.getMaxMCGMinutes() + " - computation time: "
                    + multiGatewayAnalysis.getLastSimTime() + " ms.");

            List<Double> exploredLatitudes = new ArrayList<>();

            if (multiGatewayAnalysis.getMaxMCGMinutes() <= maxMCG) { // If the MCG requirement is met, increase complexity

                // Increase scenario time
                multiGatewayAnalysis.setScenarioParams(START_DATE, END_DATE, TIME_STEP, VISIBILITY_THRESHOLD);

                for (complexity = 1; complexity <= 4; complexity++) {

                    double latitudeResolution = Math.pow(2, complexity);
                    double step = latMax / latitudeResolution;
                    double lastStep = latMax - step;
                    double firstStep;

                    if (complexity == 1) {
                        firstStep = 0;
                        lastStep = latMax;
                    } else {
                        firstStep = step;
                    }

                    devices.clear();

                    // Generate list of devices
                    facId = 0;
                    for (double lat = firstStep; lat <= lastStep; lat += step) {
                        if (!exploredLatitudes.contains(lat)) {
                            exploredLatitudes.add(lat);
                            for (int fac = 0; fac < nFacilities; fac++) {
                                devices.add(new Device(facId++, lat, fac * longitudeResolution, 0.0));
                            }
                        }
                    }

                    // Set the list of devices in the analyzer
                    multiGatewayAnalysis.setDevices(devices);

                    // Compute Accesses
                    multiGatewayAnalysis.computeDevicesPOV();

                    // Compute MCG
                    multiGatewayAnalysis.computeMaxMCG();

                    log("Analyzing: " + currentPlanes + " planes with " + currentSatsInPlane
                            + " satellites at " + currentInclination + " degrees. Complexity level: " + complexity
                            + " > MCG: " + multiGatewayAnalysis.getMaxMCGMinutes() + " - computation time: "
                            + multiGatewayAnalysis.getLastSimTime() + " ms.");

                    // If the requirement is not met after increasing complexity, break the loop
                    if (multiGatewayAnalysis.getMaxMCGMinutes() > maxMCG) {
                        break;
                    }

                }
            }

            // Add solution found
            if (multiGatewayAnalysis.getMaxMCGMinutes() <= maxMCG) {
                log("SOLUTION!: " + currentPlanes + " planes with " + currentSatsInPlane
                        + " satellites at " + currentInclination + " degrees. MCG: " + multiGatewayAnalysis.getMaxMCGMinutes());

                solutionFound = true;
                solutions.add(new Solution(currentPlanes, currentSatsInPlane, currentInclination,
                        multiGatewayAnalysis.getMaxMCGMinutes(), devices, satellites, discarded));

            } else {
                log("Discarded: " + currentPlanes + " planes with " + currentSatsInPlane
                        + " satellites at " + currentInclination + " degrees. Complexity level: " + complexity
                        + " > MCG: " + multiGatewayAnalysis.getMaxMCGMinutes());
                discarded[complexity] += 1;
            }

            // Here we perform the movement towards another solutions
            currentInclination += inclinationStep;
            currentInclination = Math.round(currentInclination * 100.0) / 100.0;

            if (currentInclination > maxInclination || solutionFound) {

                solutionFound = false;
                currentInclination = minInclination;
                currentSatsInPlane++;

                if (currentSatsInPlane > maxSatsInPlane) {
                    currentSatsInPlane = minSatsInPlane;
                    currentPlanes++;
                }

                if (currentPlanes > maxPlanes) {
                    go = false;
                }

            }
        }

        statsLogger.add("====================================================================== SOLUTIONS " +
                "======================================================================");
        statsLogger.add("Planes,SatsPerPlane,inclination,MCG,Rejected0,Rejected1,Rejected2,Rejected3,Rejected4");

        for (Solution solution : solutions) {
            statsLogger.add(solution.toString());
        }

        String fileName = Utils.unix2stamp(System.currentTimeMillis()).replace(":","-");
        Reports.saveSolutionReport(solutions, FILE_PATH + fileName + CSV_EXTENSION);
        Reports.saveLog(statsLogger, FILE_PATH + fileName + LOG_EXTENSION);

    }

    private static void log(String entry) {
        statsLogger.add(Utils.unix2stamp(System.currentTimeMillis()) + " >> " + entry);
    }

    /**
     * This method returns the maximum Lambda, which is defined as the maximum Earth Central Angle or
     * half of a satellite's "cone FOV" over the surface of the Earth.
     **/
    public static double getLambdaMax(Satellite satellite, double th) {

        double hMax = ((1 + satellite.getElements().getEccentricity()) *
                satellite.getElements().getSemiMajorAxis()) - Utils.EARTH_RADIUS;
        double etaMax = Math.asin((Utils.EARTH_RADIUS * Math.cos(Math.toRadians(th))) / (Utils.EARTH_RADIUS + hMax));
        return 90 - th - Math.toDegrees(etaMax);

    }

    /**
     * This is a numerical method to obtain the inclination at which the percentage of coverage at the maximum
     * latitude equals the percentage of coverage at the equator
     **/
    public static double getInclination(double lambda, double latMax) {

        double inc = 55, pLo, pLm; // inclination, percentage at zero latitude, percentage at maximum latitude

        double lam = Math.toRadians(lambda);
        double lat = Math.toRadians(latMax);

        double inc0 = 1;
        double inc1 = 89;
        double incx;
        double pOpt;
        int wdt = 0;

        while (Math.abs(inc0 - inc1) >= 0.01) {

            incx = (inc1 + inc0) / 2;
            inc = Math.toRadians(incx);
            pLm = Math.acos((-Math.sin(lam) + Math.cos(inc) * Math.sin(lat)) / (Math.sin(inc) * Math.cos(lat))) / Math.PI;
            pLo = 1 - (2 / Math.PI) * Math.acos((Math.sin(lam)) / Math.sin(inc));

            if (Double.isNaN(pLo)) {
                pLo = 1;
            } else if (Double.isNaN(pLm)) {
                pLm = 0;
            }

            pOpt = pLm - pLo;

            if (pOpt == 0) break;

            if (pOpt < 0)
                inc0 = incx;
            else if (pOpt > 0)
                inc1 = incx;

            wdt++;
            if (wdt > 1000) break;

        }

        return Math.toDegrees(inc);
    }

}
