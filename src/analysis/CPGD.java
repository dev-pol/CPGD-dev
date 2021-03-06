package analysis;

import simulation.ConstellationAccess;
import simulation.assets.objects.Device;
import simulation.assets.objects.Satellite;
import simulation.structures.Solution;
import simulation.utils.Reports;
import simulation.utils.Utils;
import simulation.utils.CoverageMesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// import org.apache.commons.lang3.ObjectUtils.Null;

public class CPGD {
    // Load configuration file
    static final String RUN_DATE = Utils.unix2stamp(System.currentTimeMillis()).replace(":", "-");
    static final Properties properties = Utils.loadProperties();
    static final String OUTPUT_PATH = (String) properties.get("output_path");
    static final String START_DATE = (String) properties.get("start_date");
    static final String END_DATE = (String) properties.get("end_date");
    static final String VERIFY_START_DATE = (String) properties.get("verify_start_date");
    static final String VERIFY_END_DATE = (String) properties.get("verify_end_date");
    static final String SEARCH_DATE = (String) properties.get("search_date");
    static final double TIME_STEP = Double.parseDouble((String) properties.get("time_step"));
    static final double VISIBILITY_THRESHOLD = Double.parseDouble((String) properties.get("visibility_threshold"));
    static final double MAX_MCG = Double.parseDouble((String) properties.get("max_mcg"));
    static final int MIN_PLANES = Integer.parseInt((String) properties.get("min_planes"));
    static final int MAX_PLANES = Integer.parseInt((String) properties.get("max_planes"));
    static final int MIN_SATS_IN_PLANE = Integer.parseInt((String) properties.get("min_sats_in_plane"));
    static final int MAX_SATS_IN_PLANE = Integer.parseInt((String) properties.get("max_sats_in_plane"));
    static final double MAX_INCLINATION = Double.parseDouble((String) properties.get("max_inclination"));
    static final double MIN_INCLINATION = Double.parseDouble((String) properties.get("min_inclination"));
    static final double INCLINATION_STEP = Double.parseDouble((String) properties.get("inclination_step"));
    static final int COMPLEXITY_LEVELS = Integer.parseInt((String) properties.get("complexity_levels"));
    static final double SEMI_MAJOR_AXIS = Double.parseDouble((String) properties.get("semi_major_axis"));
    static final double ECCENTRICITY = Double.parseDouble((String) properties.get("eccentricity"));
    static final double PERIGEE_ARGUMENT = Double.parseDouble((String) properties.get("perigee_argument"));
    static final double DEVICES_HEIGHT = Double.parseDouble((String) properties.get("devices_height"));
    static final String ConstellationConfiguration = (String) properties.get("ConstellationConfiguration");
    static final int MIN_PETALS = Integer.parseInt((String) properties.get("min_petals"));
    static final int MAX_PETALS = Integer.parseInt((String) properties.get("max_petals"));
    static final int MIN_DAYS = Integer.parseInt((String) properties.get("min_days"));
    static final int MAX_DAYS = Integer.parseInt((String) properties.get("max_days"));
    static final int MIN_PD = Integer.parseInt((String) properties.get("min_plane_phasing_d"));
    static final int MAX_PD = Integer.parseInt((String) properties.get("max_plane_phasing_d"));
    static final int MIN_PN = Integer.parseInt((String) properties.get("min_plane_phasing_n"));
    static final int MAX_PN = Integer.parseInt((String) properties.get("max_plane_phasing_n"));
    static final int MIN_SATS = Integer.parseInt((String) properties.get("min_sats"));
    static final int MAX_SATS = Integer.parseInt((String) properties.get("max_sats"));
    static final double raan_spread = Double.parseDouble((String) properties.get("raan_spread"));

    // useConstellation=1 provides an initial guess

    static final double StartingMeshGridResolution = Double
            .parseDouble((String) properties.get("StartingMeshGridResolution"));
    static final String CoverageGrid = (String) properties.get("CoverageGrid"); // ['Global', '*.csv', '*.shp']
    static final String CheckDevices = (String) properties.get("CheckDevices"); // ['all', 'progressive-rect',
                                                                                // 'progressive-mesh']
    static final String CSV_EXTENSION = ".csv";
    static final String LOG_EXTENSION = ".log";
    static final String LOG_FILE_PATH = OUTPUT_PATH + RUN_DATE + LOG_EXTENSION;
    // Check if we are running in debug mode
    static final boolean DEBUG_MODE = Boolean.parseBoolean((String) properties.get("debug_mode"));

    static List<String> pendingLog = new ArrayList<>();
    static double minInclination;
    static long lastComputeTime;

    public static void main(String[] args) {

        int cases = 0; // The cases above define which algorithm is run inside the while loop
        int[] discarded = new int[COMPLEXITY_LEVELS]; // Amount of discarded solutions at each complexity step
        String shpFile = null;

        // Initial solution
        int complexity = 0;
        int currentPlanes = MIN_PLANES;
        int currentSatsInPlane = MIN_SATS_IN_PLANE;
        int currentPetals = MIN_PETALS;
        int currentPD = MIN_PD;
        int currentPN = MIN_PN;
        int currentDays = MIN_DAYS;
        int currentSats = MIN_SATS;

        double MAX_LAT = 0, MIN_LAT = 0, MAX_LON = 0, MIN_LON = 0;
        double currentInclination = 0;
        double longitudeResolution = MAX_MCG * 0.1; // grid resolution longitude
        double meshResolution = StartingMeshGridResolution; // degrees
        double candidateMCG = -Double.MAX_VALUE;

        boolean allCandidatesTested = false;
        boolean exceededMCG = false;
        boolean solutionFound = false;

        List<Solution> solutions = new ArrayList<>(); // Solutions list
        List<Device> devices = new ArrayList<>(); // Devices list
        List<Satellite> satellites = new ArrayList<>(); // Satellites list

        // Parse scenario parameters and initialise values
        if (CoverageGrid.equals("global")) { // Global simulation
            System.out.println("Initialising Global Coverage Analysis:");
            MIN_LAT = 0; // Hemispherical symmetry
            MAX_LAT = Double.parseDouble((String) properties.get("max_lat"));
            MIN_LON = 0;
            MAX_LON = 360;
            cases = 0;
        } else if (CoverageGrid.endsWith(".csv")) { // CSV file provided
            System.out.println("Initialising Grid Coverage Analysis:");

            devices = Utils.devicesFromFile(CoverageGrid); // load grid
            if (devices.isEmpty()) {
                System.out.println("Empty device list. Check file name.");
                System.exit(-1);
            }
            if (CheckDevices.equals("progressive-rect")) {
                // Progressive complexity, search inside grid bounded by
                // [max(lat), min(lon)], [max(lat), max(lon)], [min(lat), min(lon)], [min(lat),
                // min(lon)]
                System.out.println("Progressive search using rectangular grid:");
                cases = 0;
                int size = devices.size();
                double current_lat, current_lon;
                Device current_device;
                // Find grid's maximum and minimum values
                MAX_LAT = -Double.MAX_VALUE;
                MIN_LAT = Double.MAX_VALUE;
                MIN_LON = Double.MAX_VALUE;
                MAX_LON = -Double.MAX_VALUE;
                for (int device_id = 0; device_id < size; device_id++) {
                    current_device = devices.get(device_id);
                    current_lat = current_device.getLat();
                    current_lon = current_device.getLon();
                    if (current_lat > MAX_LAT) {
                        MAX_LAT = current_lat;
                    }
                    if (current_lat < MIN_LAT) {
                        MIN_LAT = current_lat;
                    }
                    if (current_lon > MAX_LON) {
                        MAX_LON = current_lon;
                    }
                    if (current_lon < MIN_LON) {
                        MIN_LON = current_lon;
                    }
                }
                System.out.println("\n MAX_LAT: " + MAX_LAT + "\n MIN_LAT: " + MIN_LAT + "\n MAX_LON: " + MAX_LON
                        + "\n MIN_LON: " + MIN_LON);
            } else if (CheckDevices.equals("all")) {
                System.out.println("All grid points will be evaluated.");
                cases = 1;
            } else if (CheckDevices.equals("progressive-mesh")) {
                System.out.println("Grid-based progressive meshing TBD. Exiting.");
                cases = 2;
                System.exit(0);
            } else {
                System.out.println(
                        "Missing or invalid CheckDevices parameter. Valid values: 'all', 'progressive-rect', 'progressive-mesh'.");
                System.exit(-1);
            }
        } else if (CoverageGrid.endsWith(".shp")) { // Load shape file
            System.out.println("Initialising Regional Coverage Analysis:");
            shpFile = CoverageGrid;
            cases = 2;
        } else {
            System.out.println("CoverageGrid Parameter:" + CoverageGrid);
            System.out.println("Missing or invalid CoverageGrid parameter.");
            System.exit(-1);
        }

        var constellationAccess = new ConstellationAccess(START_DATE, SEARCH_DATE, TIME_STEP, VISIBILITY_THRESHOLD);
        ConstellationAccess.setDebugMode(DEBUG_MODE);
        constellationAccess.setIncludeCoverageGaps(true);

        tic();

        if ((cases == 0) || (cases == 1)) {
            // If the maximum device latitude is known (global or progressive-rect methods),
            // start from
            // a minimum inclination
            if (MIN_INCLINATION == 0) {
                // If we do not have a specific min inclination in mind
                minInclination = getInclination(SEMI_MAJOR_AXIS, ECCENTRICITY, VISIBILITY_THRESHOLD, MAX_LAT);
                currentInclination = minInclination;
            } else {
                minInclination = MIN_INCLINATION;
                currentInclination = minInclination;
            }

        }

        startLog(MAX_LAT);

        while (!allCandidatesTested) { // run for all candidates

            if (DEBUG_MODE)
                System.out
                        .println("Performing: " + currentPlanes + "-" + currentSatsInPlane + "-" + currentInclination);

            // Set initial (short-term / "first-look") scenario time
            constellationAccess.setScenarioParams(START_DATE, SEARCH_DATE, TIME_STEP, VISIBILITY_THRESHOLD);

            // Populate the candidate constellation
            populateConstellation(satellites, currentPlanes, currentSatsInPlane, currentInclination,
                    ConstellationConfiguration, currentSats, currentPetals, currentDays, currentPD, currentPN,
                    raan_spread);
            constellationAccess.setSatellites(satellites);

            if (cases == 0) { // global || *.csv + progressive-rect
                complexity = 0;
                exceededMCG = false;
                candidateMCG = -Double.MAX_VALUE;
                while ((!exceededMCG) && (complexity < COMPLEXITY_LEVELS)) {
                    if (complexity > 1) { // If the first candidate did not fail, increase scenario time
                        constellationAccess.setScenarioParams(START_DATE, END_DATE, TIME_STEP, VISIBILITY_THRESHOLD);
                    }

                    populateDeviceList(devices, longitudeResolution, complexity, MIN_LAT, MAX_LAT, MIN_LON, MAX_LON);

                    // Set the list of devices in the analyzer, compute accesses and MCG
                    constellationAccess.setDevices(devices);
                    constellationAccess.computeDevicesPOV();
                    constellationAccess.computeMaxMCG();
                    candidateMCG = constellationAccess.getMaxMCGMinutes();

                    if (candidateMCG > MAX_MCG) {
                        exceededMCG = true;
                    }
                    if (ConstellationConfiguration.equals("flower")) {
                        logProgressFlower(currentSats, currentDays, currentPetals, currentPD, currentPN,
                                currentInclination, complexity, candidateMCG, constellationAccess.getLastSimTime());
                    } else {
                        logProgress(currentPlanes, currentSatsInPlane, currentInclination, complexity, candidateMCG,
                                constellationAccess.getLastSimTime());
                    }
                    complexity = complexity + 1;

                }
                // Exceeded MCG or complexity
                if (exceededMCG) { // Exceeded MCG at any complexity > log failed attempt
                    discarded[complexity - 1] += 1;
                    if (ConstellationConfiguration.equals("walker")) {
                        log("Discarded: " + currentPlanes + " planes with " + currentSatsInPlane + " satellites at "
                                + currentInclination + " degrees. Complexity level: " + (complexity - 1) + " > MCG: "
                                + candidateMCG);
                    }
                } else { // Satisfied MCG at maximum complexity
                    solutionFound = true;
                    if (ConstellationConfiguration.equals("walker")) {
                        solutions.add(new Solution(currentPlanes, currentSatsInPlane, currentInclination, candidateMCG,
                                devices, satellites, discarded));
                        log("SOLUTION!: " + currentPlanes + " planes with " + currentSatsInPlane + " satellites at "
                                + currentInclination + " degrees. MCG: " + candidateMCG);
                    } else {
                        log("SOLUTION!: NumSats " + currentSats + " NumDays " + currentDays + " NumPetals "
                                + currentPetals + " PD " + currentPD + " PN " + currentPN + " inclination:"
                                + currentInclination +
                                " MCG: " + candidateMCG);
                    }
                }

            } else if (cases == 1) { // *.csv + all
                // Set all devices, compute constellation accesses and MCG
                constellationAccess.setDevices(devices);
                constellationAccess.computeDevicesPOV();
                constellationAccess.computeMaxMCG();
                candidateMCG = constellationAccess.getMaxMCGMinutes();

                // If a solution is found, log it
                if (candidateMCG <= MAX_MCG) {
                    solutionFound = true;
                    solutions.add(new Solution(currentPlanes, currentSatsInPlane, currentInclination, candidateMCG,
                            devices, satellites, discarded));
                    log("SOLUTION!: " + currentPlanes + " planes with " + currentSatsInPlane + " satellites at "
                            + currentInclination + " degrees. MCG: " + candidateMCG);
                } else {
                    log("Discarded: " + currentPlanes + " planes with " + currentSatsInPlane + " satellites at "
                            + currentInclination + " degrees. Complexity level: N/A" + " > MCG: "
                            + candidateMCG);
                }
            } else if (cases == 2) { // *.csv||*.shp + progressive-mesh
                complexity = 0;
                exceededMCG = false;
                candidateMCG = -Double.MAX_VALUE;
                meshResolution = StartingMeshGridResolution;
                while ((!exceededMCG) && (complexity < COMPLEXITY_LEVELS)) {
                    if (complexity > 1) { // If the MCG requirement is met at complexity < 1, increase scenario time
                        constellationAccess.setScenarioParams(START_DATE, END_DATE, TIME_STEP, VISIBILITY_THRESHOLD);
                    }

                    try {
                        // Generate initial coverage mesh (hexagonal grid over region shape) and compute
                        // centroids
                        List<Double> MeshGrid = new ArrayList<>(); // Even indexes contain lat-coordinates, odd indexes
                                                                   // contain
                                                                   // lon-coordinates
                        MeshGrid = CoverageMesh.buildMesh(shpFile, meshResolution);
                        int numCoordinates = MeshGrid.size();
                        if (DEBUG_MODE) {
                            for (int i = 0; i < numCoordinates / 2; i = i + 1) {
                                System.out.println("Coordinate Pairs (Lat, Lon): (" + MeshGrid.get(2 * i) + ","
                                        + MeshGrid.get(2 * i + 1) + ")");
                            }
                        }
                        // Clear device list before populating it
                        devices.clear();
                        // Populate device list
                        for (int i = 0; i < numCoordinates / 2; i = i + 1) {
                            // id, lat, lon, altitude
                            devices.add(new Device(i, MeshGrid.get(2 * i), MeshGrid.get(2 * i + 1), DEVICES_HEIGHT));
                        }
                    } catch (Exception e) {
                        System.out.println(
                                "Failed to build mesh. Please ensure both .shp and .shx files are present in the same directory."
                                        + " If so, try to reduce the initial grid resolution to be compatible with your .shp file.");
                        System.exit(-1);
                    }
                    // Compute constellation accesses
                    constellationAccess.setDevices(devices);
                    constellationAccess.computeDevicesPOV();
                    constellationAccess.computeMaxMCG();
                    candidateMCG = constellationAccess.getMaxMCGMinutes();

                    // Log Progress
                    logProgress(currentPlanes, currentSatsInPlane, currentInclination, complexity, candidateMCG,
                            constellationAccess.getLastSimTime());

                    // Break early if solution not satisfied
                    if (candidateMCG > MAX_MCG) {
                        exceededMCG = true;
                    }

                    // Adapt mesh resolution (exponential rule, double the resolution every
                    // iteration)

                    complexity = complexity + 1;
                    meshResolution = meshResolution / 2;

                }
                // Exceeded MCG or complexity
                if (exceededMCG) { // Exceeded MCG at any complexity > log failed attempt
                    discarded[complexity - 1] += 1;
                    log("Discarded: " + currentPlanes + " planes with " + currentSatsInPlane + " satellites at "
                            + currentInclination + " degrees. Complexity level: " + (complexity - 1) + " > MCG: "
                            + candidateMCG);
                } else { // Satisfied MCG at maximum complexity
                    /* If a solution is found for the initial time span, we look at the */
                    solutionFound = true;
                    solutions.add(new Solution(currentPlanes, currentSatsInPlane, currentInclination, candidateMCG,
                            devices, satellites, discarded));
                    log("SOLUTION!: " + currentPlanes + " planes with " + currentSatsInPlane + " satellites at "
                            + currentInclination + " degrees. MCG: " + candidateMCG);
                }
            }
            // Move to next constellation candidate
            currentInclination += INCLINATION_STEP;

            if ((currentInclination > MAX_INCLINATION) || (solutionFound)) {
                // First inclination
                solutionFound = false;
                currentInclination = minInclination;

                if (ConstellationConfiguration.equals("walker")) {
                    currentSatsInPlane++;

                    if (currentSatsInPlane > MAX_SATS_IN_PLANE) {
                        currentSatsInPlane = MIN_SATS_IN_PLANE;
                        currentPlanes++;
                    }

                    if (currentPlanes > MAX_PLANES) {
                        allCandidatesTested = true;
                    }
                } else if (ConstellationConfiguration.equals("flower")) {

                    // Then sweep PD/PN
                    currentPD = currentPD + 1;
                    // Symmetry condition
                    currentPN = currentPD;
                    if ((currentPD > MAX_PD) || (currentPD > currentSats)){ // Symmetry condition
                        currentPD = MIN_PD;
                        currentPN = currentPD; // Symmetry condition
                        //currentPN = currentPN + 1;
                        int max_assets = currentPD * currentDays; // maximum assets in the flower design
                        if (max_assets < currentSats) {
                            currentSats = max_assets;
                        }
                        //if (currentPN > MAX_PN) {
                            // Then increase petals
                            //currentPN = MIN_PN;
                            currentPetals = currentPetals + 1;
                            if (currentPetals > MAX_PETALS) {
                                currentPetals = MIN_PETALS;
                                currentDays = currentDays + 1;
                                // Then Days
                                if (currentDays > MAX_DAYS) {
                                    // Then sats

                                    currentDays = MIN_DAYS;
                                    currentSats = currentSats + 1;
                                    // If we exceed the maximum number of assets predicted by the design or the
                                    // limit of satellites
                                    if ((currentSats > MAX_SATS) || (currentSats > max_assets)) {
                                        allCandidatesTested = true;
                                    }
                                }
                            }
                        }
                    }

                }
            }

        }
        // Produce report with constellation candidates that satisfy requirements
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
     * This method populates the constellation with satellites, according to the
     * number of planes, sats per plane, and plane inclination
     **/
    private static void populateConstellation(List<Satellite> satellites, int planes, int satsPerPlane,
            double inclination, String ConstellationConfiguration, int num_assets, int num_petals, int num_days,
            int plane_phasing_d,
            int plane_phasing_n,
            double raan_spread) {

        satellites.clear();
        if (ConstellationConfiguration.equals("walker")) {
            double planePhase = 360.0 / planes;
            double satsPhase = 360.0 / satsPerPlane;

            // Generate the constellation
            int satId = 0;
            for (int plane = 0; plane < planes; plane++) { // Plane
                for (int sat = 0; sat < satsPerPlane; sat++) { // Satellites
                    satellites.add(new Satellite(satId++, START_DATE, SEMI_MAJOR_AXIS, ECCENTRICITY, inclination,
                            plane * planePhase, PERIGEE_ARGUMENT, sat * satsPhase)); // - plane * (planePhase /
                                                                                     // currentSatsInPlane)
                }
            }
        } else if (ConstellationConfiguration.equals("flower")) {
            /*
             * Class for describing and holding a flower constellation of satellites
             * 
             * :param params: A dict with the following arameters
             * - num_petals: Number of petals formed when viewing the relative orbits
             * - num_days: The number of days for the constellation to completely repeat its
             * orbit
             * - num_assets: The desired number of satellites involved in the constellation
             * - phasing_n: Phasing parameter n, effects allowable satellite positions
             * - inclination: Inclination of orbit relative to equatorial plane [degrees]
             * - raan_spread: Interval where the orbital planes are distributed (180 for
             * Star, 360 for Delta)
             * - perigee_argument: Argument of perigee for satellite orbits [degrees]
             */
            /*
             * https://www.esa.int/gsp/ACT/doc/ACTAFUTURA/AF02/papers/AF02.2006.7.pdf
             * Ns = Nso Fd ??? Nd Fd
             * after Np orbital periods
             * the rotating reference frame has performed Nd complete
             * rotations and, consequently, the satellite and the rotating
             * reference frame come back to their initial positions.
             */
            /*
             * theory the two integers, Np
             * and Nd, are identified as the Number of Petals and the
             * Number of Days, respectively.
             * en Nd really represents the
             * number of days to repeat the relative trajectory, while the
             * Number of Petals Np, that actually represent the number of
             * orbit revolutions, finds its origin because of the petal-like
             * shape of the relative trajectory
             * The number of admissible locations per orbit in
             * Flower Constellation is Nd
             */

            int max_assets = plane_phasing_d * num_days; // maximum assets
            if (max_assets < num_assets) {
                num_assets = max_assets;
            }
            double raan_spacing = -raan_spread * plane_phasing_n / plane_phasing_d;

            double mean_anomaly_spacing = -1 * raan_spacing * num_petals / num_days;

            if (Math.abs(mean_anomaly_spacing) > 360) {
                mean_anomaly_spacing = mean_anomaly_spacing % 360;
            }

            if (Math.abs(raan_spacing) > 360) {
                raan_spacing = raan_spacing % 360;
            }

            int satId = 0;
            double raan_i = 0;
            double M_i = 0;
            double V_i = 0; // true anomaly
            for (int idx = 1; idx < Math.min(max_assets, num_assets); idx++) {
                raan_i = raan_i + raan_spacing;

                if (raan_i < 0) {
                    raan_i = 360 + raan_i;
                }

                M_i = M_i + mean_anomaly_spacing;
                if (Math.abs(M_i) > 360) {
                    M_i = M_i % 360;
                }

                // True anomaly
                V_i = M_i + (2 * ECCENTRICITY - 0.25 * Math.pow(ECCENTRICITY, 3) * Math.sin(M_i * 180 / Math.PI)); // in
                                                                                                                   // Radians
                satellites.add(new Satellite(satId++, START_DATE, SEMI_MAJOR_AXIS, ECCENTRICITY, inclination,
                        raan_i, PERIGEE_ARGUMENT, V_i)); // - plane * (planePhase /
                                                         // currentSatsInPlane)
            }
        }
    }

    /**
     * This method populates the list of devices passed as a reference according to
     * the indicated complexity and the algorithm variables
     **/
    private static void populateDeviceList(List<Device> devices, double longitudeResolution, int complexity,
            double MIN_LAT, double MAX_LAT, double MIN_LON, double MAX_LON) {

        devices.clear();
        // Compute grid extremes
        int nFacilities = (int) Math.round((MAX_LON - MIN_LON) / longitudeResolution); // Number of grid points
                                                                                       // according to the grid
                                                                                       // resolution
        double latitudeResolution = Math.pow(2, complexity + 1);
        double step = (MAX_LAT - MIN_LAT) / latitudeResolution;

        // Generate list of devices
        int facId = 0;
        for (double lat = MIN_LAT; lat <= MAX_LAT; lat += step) {
            for (int fac = 0; fac < nFacilities; fac++) {
                devices.add(new Device(facId++, lat, MIN_LON + fac * longitudeResolution, DEVICES_HEIGHT)); // Complexity
                                                                                                            // reduction:
                                                                                                            // all
                                                                                                            // devices
                                                                                                            // at the
                                                                                                            // same
                                                                                                            // height
            }
        }
    }

    /**
     * This method starts the log file. It logs important run configurations in a
     * header.
     **/
    private static void startLog(double MAX_LAT) {

        pendingLog.add("Starting analysis at " + RUN_DATE);
        pendingLog.add("Scenario start: " + START_DATE + " - Scenario end: " + END_DATE);
        pendingLog.add("Target MCG: " + MAX_MCG + " - Maximum latitude band: " + MAX_LAT
                + " Degrees - complexity 0 - Search date " + SEARCH_DATE);
        pendingLog.add("Minimum number of planes: " + MIN_PLANES + " - Maximum number of planes: " + MAX_PLANES);
        pendingLog.add(
                "Minimum sats per plane: " + MIN_SATS_IN_PLANE + " - Maximum sats per planes: " + MAX_SATS_IN_PLANE);
        pendingLog.add("Minimum inclination: " + minInclination + " - Maximum inclination: " + MAX_INCLINATION);
        pendingLog.add("Inclination step: " + INCLINATION_STEP + " Degrees");
        pendingLog.add(Reports.SEPARATOR_HALF + " PROGRESS " + Reports.SEPARATOR_HALF);

        Reports.saveLog(pendingLog, LOG_FILE_PATH);
        pendingLog.clear();
    }

    /**
     * This method ends the log file. It logs the solutions.
     **/
    private static void endLog(List<Solution> solutions) {

        pendingLog.add(Reports.SEPARATOR_HALF + " STATISTICS " + Reports.SEPARATOR_HALF);
        pendingLog.add("Total compute time: " + toc() + " ms.");
        pendingLog.add(solutions.size() + " Solutions found");

        if (!solutions.isEmpty()) {
            StringBuilder sb = new StringBuilder("Solutions rejected at each complexity step: / ");
            int complexity = 0;
            for (int rejected : solutions.get(solutions.size() - 1).getDiscardedSolutions()) {
                sb.append(complexity++).append(": ").append(rejected).append(" / ");
            }
            pendingLog.add(sb.toString());
            pendingLog.add(Reports.SEPARATOR_HALF + " SOLUTIONS " + Reports.SEPARATOR_HALF);

            for (Solution solution : solutions) {
                pendingLog.add(solution.getnOfPlanes() + " planes with " + solution.getnOfSatsPerPlane()
                        + " satellites each, at " + solution.getInclination() + " degrees of inclination. MCG: "
                        + solution.getMcg());
            }
        }

        updateLog();
    }

    /**
     * This method logs an iteration of the algorithm together with the relevant
     * data
     **/
    private static void logProgress(int currentPlanes, int currentSatsInPlane, double currentInclination,
            int complexity, double mcg, double simTime) {
        // Get runtime memory

        log("Analyzing: " + currentPlanes + " planes with " + currentSatsInPlane + " satellites at "
                + currentInclination + " degrees. Complexity level: " + complexity + " > MCG: " + mcg
                + " - computation time: " + simTime + " ms, memory usage: "
                + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + " MB");
    }

    private static void logProgressFlower(int currentSats, int currentDays, int currentPetals,
            int currentPD, int currentPN, double currentInclination, int complexity, double mcg, double simTime) {
        // Get runtime memory

        log("Analyzing: Current sats: " + currentSats + " numDays  " + currentDays + " numPetals "
                + currentPetals + " PD " + currentPD + " PN " + currentPN + " inclination " + currentInclination +
                ". degrees. Complexity level: " + complexity + " > MCG: " + mcg
                + " - computation time: " + simTime + " ms, memory usage: "
                + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + " MB");
    }

    /**
     * This method appends a timestamp to a log entry and adds it to the List of
     * pending log statements
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
     * This method returns the maximum Lambda, which is defined as the maximum Earth
     * Central Angle or half of a satellite's "cone FOV" over the surface of the
     * Earth.
     **/
    public static double getLambdaMax(double semiMajorAxis, double eccentricity, double visibilityThreshold) {

        double hMax = ((1 + eccentricity) * semiMajorAxis) - Utils.EARTH_RADIUS;
        double etaMax = Math.asin(
                (Utils.EARTH_RADIUS * Math.cos(Math.toRadians(visibilityThreshold))) / (Utils.EARTH_RADIUS + hMax));
        return 90 - visibilityThreshold - Math.toDegrees(etaMax);

    }

    /**
     * This is a numerical method to obtain the inclination at which the percentage
     * of coverage at the maximum latitude equals the percentage of coverage at the
     * equator
     **/
    public static double getInclination(double semiMajorAxis, double eccentricity, double visibilityThreshold,
            double latMax) {

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
            pLm = Math.acos((-Math.sin(lam) + Math.cos(inc) * Math.sin(lat)) / (Math.sin(inc) * Math.cos(lat)))
                    / Math.PI;
            pLo = 1 - (2 / Math.PI) * Math.acos((Math.sin(lam)) / Math.sin(inc));

            if (Double.isNaN(pLo)) {
                pLo = 1;
            } else if (Double.isNaN(pLm)) {
                pLm = 0;
            }

            pOpt = pLm - pLo;

            if (pOpt == 0)
                break;

            if (pOpt < 0)
                inc0 = incx;
            else if (pOpt > 0)
                inc1 = incx;

            wdt++;
            if (wdt > 1000)
                break;

        }
        return Math.round(Math.toDegrees(inc) * 100.0) / 100.0;
    }

}
