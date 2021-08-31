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

    static final String RUN_DATE = Utils.unix2stamp(System.currentTimeMillis()).replace(":","-");
    static final Properties properties = Utils.loadProperties();
    static final String OUTPUT_PATH = (String) properties.get("output_path");
    static final String START_DATE = (String) properties.get("start_date");
    static final String END_DATE = (String) properties.get("end_date");
    static final String SEARCH_DATE = (String) properties.get("search_date");
    static final double TIME_STEP = Double.parseDouble((String) properties.get("time_step"));
    static final double VISIBILITY_THRESHOLD = Double.parseDouble((String) properties.get("visibility_threshold"));
    static final double MAX_MCG = Double.parseDouble((String) properties.get("max_mcg"));
    static final double MAX_LAT = Double.parseDouble((String) properties.get("max_lat"));
    static final int MIN_PLANES = Integer.parseInt((String) properties.get("min_planes"));
    static final int MAX_PLANES = Integer.parseInt((String) properties.get("max_planes"));
    static final int MIN_SATS_IN_PLANE = Integer.parseInt((String) properties.get("min_sats_in_plane"));
    static final int MAX_SATS_IN_PLANE = Integer.parseInt((String) properties.get("max_sats_in_plane"));
    static final double MAX_INCLINATION = Double.parseDouble((String) properties.get("max_inclination"));
    static final double INCLINATION_STEP = Double.parseDouble((String) properties.get("inclination_step"));
    static final int COMPLEXITY_LEVELS = Integer.parseInt((String) properties.get("complexity_levels"));
    static final double SEMI_MAJOR_AXIS = Double.parseDouble((String) properties.get("semi_major_axis"));
    static final double ECCENTRICITY = Double.parseDouble((String) properties.get("eccentricity"));
    static final double PERIGEE_ARGUMENT = Double.parseDouble((String) properties.get("perigee_argument"));
    static final double DEVICES_HEIGHT = Double.parseDouble((String) properties.get("devices_height"));
    static final String CSV_EXTENSION = ".csv";
    static final String LOG_EXTENSION = ".log";
    static final String LOG_FILE_PATH = OUTPUT_PATH + RUN_DATE + LOG_EXTENSION;

    static List<String> pendingLog = new ArrayList<>();
    static double minInclination;
    static long lastComputeTime;

    public static void main(String[] args) {

        tic();

        var multiGatewayAnalysis = new MultiGateway(START_DATE, SEARCH_DATE, TIME_STEP, VISIBILITY_THRESHOLD);

        multiGatewayAnalysis.setIncludeCoverageGaps(true);

        minInclination = getInclination(SEMI_MAJOR_AXIS, ECCENTRICITY, VISIBILITY_THRESHOLD, MAX_LAT);

        startLog();

        // Amount of discarded solutions at each complexity step
        int[] discarded = new int[COMPLEXITY_LEVELS];

        // Initial solution
        int currentPlanes = MIN_PLANES;
        int currentSatsInPlane = MIN_SATS_IN_PLANE;
        double currentInclination = minInclination;

        boolean go = true;
        boolean solutionFound = false;

        List<Solution> solutions = new ArrayList<>(); // Solutions list
        List<Device> devices = new ArrayList<>(); // Devices list
        List<Satellite> satellites = new ArrayList<>(); // Satellites list

        double longitudeResolution = MAX_MCG * 0.25; // grid resolution longitude - wise
        int nFacilities = (int) Math.round(360 / longitudeResolution); // Number of grid points according to the grid resolution

        while (go) {

            System.out.println("Performing: " + currentPlanes + "-" + currentSatsInPlane + "-" + currentInclination);

            int complexity = 0;

            // Set "first look" scenario time
            multiGatewayAnalysis.setScenarioParams(START_DATE, SEARCH_DATE, TIME_STEP, VISIBILITY_THRESHOLD);

            // Populate the scenario
            populateConstellation(satellites, currentPlanes, currentSatsInPlane, currentInclination);
            populateDeviceList(devices, nFacilities, longitudeResolution, complexity);

            Reports.saveDevicesInfo(devices,OUTPUT_PATH + "devices_complexity_" + complexity + CSV_EXTENSION);

            // Simulate
            multiGatewayAnalysis.setAssets(devices, satellites); // Set assets (list of devices + constellation) in the analyzer
            multiGatewayAnalysis.computeDevicesPOV(); // Compute access intervals
            multiGatewayAnalysis.computeMaxMCG(); // Compute MCG

            logProgress(currentPlanes, currentSatsInPlane, currentInclination, complexity,
                    multiGatewayAnalysis.getMaxMCGMinutes(), multiGatewayAnalysis.getLastSimTime());

            if (multiGatewayAnalysis.getMaxMCGMinutes() <= MAX_MCG) { // If the MCG requirement is met at complexity 0, increase complexity

                // Increase scenario time
                multiGatewayAnalysis.setScenarioParams(START_DATE, END_DATE, TIME_STEP, VISIBILITY_THRESHOLD);

                for (complexity = 1; complexity < COMPLEXITY_LEVELS; complexity++) {

                    populateDeviceList(devices, nFacilities, longitudeResolution, complexity);
                    Reports.saveDevicesInfo(devices,OUTPUT_PATH + "devices_complexity_" + complexity + CSV_EXTENSION);

                    // Set the list of devices in the analyzer, compute accesses and MCG
                    multiGatewayAnalysis.setDevices(devices);
                    multiGatewayAnalysis.computeDevicesPOV();
                    multiGatewayAnalysis.computeMaxMCG();

                    logProgress(currentPlanes, currentSatsInPlane, currentInclination, complexity,
                            multiGatewayAnalysis.getMaxMCGMinutes(), multiGatewayAnalysis.getLastSimTime());

                    // If the requirement is not met after increasing complexity, break the loop
                    if (multiGatewayAnalysis.getMaxMCGMinutes() > MAX_MCG) {
                        break;
                    }
                }
            }

            // Add solution found
            if (multiGatewayAnalysis.getMaxMCGMinutes() <= MAX_MCG) {
                solutionFound = true;
                solutions.add(new Solution(currentPlanes, currentSatsInPlane, currentInclination,
                        multiGatewayAnalysis.getMaxMCGMinutes(), devices, satellites, discarded));
                log("SOLUTION!: " + currentPlanes + " planes with " + currentSatsInPlane
                        + " satellites at " + currentInclination + " degrees. MCG: " + multiGatewayAnalysis.getMaxMCGMinutes());

            } else {
                discarded[complexity] += 1;
                log("Discarded: " + currentPlanes + " planes with " + currentSatsInPlane
                        + " satellites at " + currentInclination + " degrees. Complexity level: " + complexity
                        + " > MCG: " + multiGatewayAnalysis.getMaxMCGMinutes());
            }

            // Here we perform the movement towards another solutions
            currentInclination += INCLINATION_STEP;

            if (currentInclination > MAX_INCLINATION || solutionFound) {

                solutionFound = false;
                currentInclination = minInclination;
                currentSatsInPlane++;

                if (currentSatsInPlane > MAX_SATS_IN_PLANE) {
                    currentSatsInPlane = MIN_SATS_IN_PLANE;
                    currentPlanes++;
                }

                if (currentPlanes > MAX_PLANES) {
                    go = false;
                }
            }
        }

        Reports.saveSolutionCSV(solutions, OUTPUT_PATH + RUN_DATE + CSV_EXTENSION);
        endLog(solutions);

    }

    /**
     * Starts a clock to measure compute time
     **/
    private static void tic() {
        lastComputeTime = System.currentTimeMillis();
    }

    /**
     * Stops the clock and returns the measured time
     **/
    private static long toc() {
        lastComputeTime = System.currentTimeMillis() - lastComputeTime;
        return lastComputeTime;
    }

    /**
     * This method populates the constellation with satellites, according to the number of planes, sats per plane,
     * and plane inclination
     **/
    private static void populateConstellation(List<Satellite> satellites, int planes, int satsPerPlane, double inclination) {

        satellites.clear();

        double planePhase = 360.0 / planes;
        double satsPhase = 360.0 / satsPerPlane;

        // Generate the constellation
        int satId = 0;
        for (int plane = 0; plane < planes; plane++) {   // Plane
            for (int sat = 0; sat < satsPerPlane; sat++) {    // Satellites
                satellites.add(new Satellite(satId++, START_DATE, SEMI_MAJOR_AXIS,
                        ECCENTRICITY, inclination, plane * planePhase, PERIGEE_ARGUMENT,
                        sat * satsPhase));  // - plane * (planePhase / currentSatsInPlane)
            }
        }
    }

    /**
     * This method populates the list of devices passed as a reference according to the indicated complexity and
     * the algorithm variables
     **/
    private static void populateDeviceList(List<Device> devices, int nFacilities, double longitudeResolution, int complexity) {

        devices.clear();

        double latitudeResolution = Math.pow(2, complexity);
        double step = MAX_LAT / latitudeResolution;
        double lastStep = MAX_LAT - step;
        double firstStep;

        if (complexity == 0 || complexity == 1) {
            firstStep = 0;
            step = MAX_LAT / 2;
            lastStep = MAX_LAT;
        } else {
            firstStep = step;
        }

        // Generate list of devices
        int facId = 0;
        for (double lat = firstStep; lat <= lastStep; lat += step) {
                for (int fac = 0; fac < nFacilities; fac++) {
                    devices.add(new Device(facId++, lat, fac * longitudeResolution, DEVICES_HEIGHT));
                }
        }
    }

    /**
     * This method starts the log file. It logs important run configurations in a header.
     **/
    private static void startLog() {

        pendingLog.add("Starting analysis at " + RUN_DATE);
        pendingLog.add("Scenario start: " + START_DATE + " - Scenario end: " + END_DATE);
        pendingLog.add("Target MCG: " + MAX_MCG + " - Maximum latitude band: " + MAX_LAT
                + " Degrees - complexity 0 - Search date " + SEARCH_DATE);
        pendingLog.add("Minimum number of planes: " + MIN_PLANES + " - Maximum number of planes: " + MAX_PLANES);
        pendingLog.add("Minimum sats per plane: " + MIN_SATS_IN_PLANE + " - Maximum sats per planes: " + MAX_SATS_IN_PLANE);
        pendingLog.add("Minimum inclination: " + minInclination + " - Maximum inclination: " + MAX_INCLINATION);
        pendingLog.add("Inclination step: " + INCLINATION_STEP + " Degrees");
        pendingLog.add( Reports.SEPARATOR_HALF + " PROGRESS " + Reports.SEPARATOR_HALF);

        Reports.saveLog(pendingLog, LOG_FILE_PATH);
        pendingLog.clear();
    }

    /**
     * This method ends the log file. It logs the solutions.
     **/
    private static void endLog(List<Solution> solutions) {

        pendingLog.add( Reports.SEPARATOR_HALF + " STATISTICS " + Reports.SEPARATOR_HALF);
        pendingLog.add("Total compute time: " + toc() + " ms.");
        pendingLog.add(solutions.size() + " Solutions found");
        StringBuilder sb = new StringBuilder("Solutions rejected at each complexity step: / ");
        int complexity = 0;
        for (int rejected : solutions.get(solutions.size() - 1).getDiscardedSolutions()) {
            sb.append(complexity++).append(": ").append(rejected).append(" / ");
        }
        pendingLog.add(sb.toString());
        pendingLog.add( Reports.SEPARATOR_HALF + " SOLUTIONS " + Reports.SEPARATOR_HALF);

        for (Solution solution : solutions) {
            pendingLog.add(solution.getnOfPlanes() + " planes with " + solution.getnOfSatsPerPlane() + " satellites each, at "
                    + solution.getInclination() + " degrees of inclination. MCG: " + solution.getMcg());
        }
        updateLog();
    }

    /**
     * This method logs an iteration of the algorithm together with the relevant data
     **/
    private static void logProgress(int currentPlanes, int currentSatsInPlane, double currentInclination,
                                    int complexity, double mcg, double simTime) {
        log("Analyzing: " + currentPlanes + " planes with " + currentSatsInPlane
                + " satellites at " + currentInclination + " degrees. Complexity level: " + complexity
                + " > MCG: " + mcg + " - computation time: "
                + simTime + " ms.");
    }

    /**
     * This method appends a timestamp to a log entry and adds it to the List of pending log statements
     **/
    private static void log(String entry) {
        pendingLog.add(Utils.unix2stamp(System.currentTimeMillis()) + " >> " + entry);
        updateLog();
    }

    /**
     * This method updates the log file with every pending log statement
     **/
    private static void updateLog() {
        Reports.appendLog(pendingLog, LOG_FILE_PATH);
        pendingLog.clear();
    }

    /**
     * This method returns the maximum Lambda, which is defined as the maximum Earth Central Angle or
     * half of a satellite's "cone FOV" over the surface of the Earth.
     **/
    public static double getLambdaMax(double semiMajorAxis, double eccentricity, double visibilityThreshold) {

        double hMax = ((1 + eccentricity) * semiMajorAxis) - Utils.EARTH_RADIUS;
        double etaMax = Math.asin((Utils.EARTH_RADIUS * Math.cos(Math.toRadians(visibilityThreshold))) / (Utils.EARTH_RADIUS + hMax));
        return 90 - visibilityThreshold - Math.toDegrees(etaMax);

    }

    /**
     * This is a numerical method to obtain the inclination at which the percentage of coverage at the maximum
     * latitude equals the percentage of coverage at the equator
     **/
    public static double getInclination(double semiMajorAxis, double eccentricity, double visibilityThreshold, double latMax) {

        double inc = 55, pLo, pLm; // inclination, percentage at zero latitude, percentage at maximum latitude

        double lam = Math.toRadians(getLambdaMax(semiMajorAxis, eccentricity, visibilityThreshold));
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
        return Math.round(Math.toDegrees(inc) * 100.0) / 100.0;
    }

}
