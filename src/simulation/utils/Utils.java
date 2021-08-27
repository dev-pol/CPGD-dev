package simulation.utils;

import org.orekit.data.DataContext;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import simulation.assets.Asset;
import simulation.assets.objects.Device;
import simulation.assets.objects.Satellite;
import simulation.exceptions.SatElsetException;
import simulation.structures.Ephemeris;
import simulation.structures.OrbitalElements;
import simulation.structures.SatElset;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {

    public static final double EARTH_RADIUS = 6378135;    // Radius Earth [m]; WGS-84 (semi-major axis, a) (Equatorial Radius)
    public static final double ECCENTRICITY = 8.1819190842622e-2;    // Ellipsoid constants: eccentricity; WGS84
    public static final double MU = 3.986004418e+14; // Gravitation coefficient

    private Utils() {

    }

    /**
     * Reads a properties file and loads it into a Properties class
     *
     * @return Properties containing key-values for configurations
     */
    public static Properties loadProperties() {

        Properties prop = new Properties();

        try (InputStream input = Utils.class.getClassLoader().getResourceAsStream("resources/config.properties")) {

            if (input == null) {
                System.out.println("Unable to find config.properties");
                throw new FileNotFoundException();
            }

            prop.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return prop;

    }

    /**
     * Reads a properties file and loads it in a hashmap
     *
     * @return Map<String, String> a Map containing key-values for configurations
     */
//    public static Map<String, String> loadProperties(String file) throws IOException {
//
//        LinkedHashMap<String, String> hashMap;
//
//        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
//
//            String temp;
//            hashMap = new LinkedHashMap<>();
//            temp = br.readLine();
//
//            while (temp != null) {
//                temp = temp.replace(" ", "").replace("\\t", "");
//
//                if (!(temp.startsWith("#") || temp.isEmpty()) && temp.contains("=")) {
//                    while (temp.contains("0x")) {
//                        char a = (char) Integer.parseInt(temp.substring(temp.indexOf("0x") + 2, temp.indexOf("0x") + 4), 16);
//                        temp = temp.substring(0, temp.indexOf("0x") + 2) + temp.substring(temp.indexOf("0x") + 4);
//                        temp = temp.replaceFirst("0x", String.valueOf(a));
//                    }
//                    hashMap.put(temp.substring(0, temp.indexOf("=")), temp.substring(temp.indexOf("=") + 1));
//                }
//                temp = br.readLine();
//            }
//        }
//
//        return hashMap;
//    }

    /**
     * Reads a file containing asset(s) parameter(s) and returns a list of objects accordingly
     *
     * @return List<Asset>
     */
    public static List<Device> devicesFromFile(String fileName) {

        List<Device> assetList = new ArrayList<>();
        var file = new File(fileName);
        try (var fr = new FileReader(file); var br = new BufferedReader(fr)) {
            String line;
            int id = 0;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("//") && line.length() > 0) {
                    var data = line.split(",");
                    assetList.add(new Device(id++, data[0], Double.parseDouble(data[1]), Double.parseDouble(data[2]), Double.parseDouble(data[3])*1000));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return assetList;
    }

    /**
     * Reads a file containing asset(s) parameter(s) and returns a list of objects accordingly
     *
     * @param fileName The path of the file
     *
     * @return List<Asset>
     */
    public static List<Satellite> satellitesFromFile(String fileName) {

        List<Satellite> satelliteList = new ArrayList<>();
        var file = new File(fileName);
        try (var fr = new FileReader(file); var br = new BufferedReader(fr)) {
            String line;
            int id = 0;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("//") && line.length() > 0) {
                    var data = line.split(",");
                    satelliteList.add(new Satellite(id++, data[0], Double.parseDouble(data[1]), Double.parseDouble(data[2]), Double.parseDouble(data[3])
                            , Double.parseDouble(data[4]), Double.parseDouble(data[5]), Double.parseDouble(data[6])));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return satelliteList;
    }

    /**
     * Get the Satellite's period in minutes
     *
     * @return double
     */
    public static double getSatellitePeriodMinutes(Satellite satellite) {
        var tle = satellite2tle(satellite);
        return (24 * 60) / (tle.getMeanMotion());
    }

    /**
     * Get the Satellite's period in seconds, from the semi major axis
     *
     * @return double
     */
    public static double computePeriodSeconds(double a) {
        return 2 * Math.PI * Math.sqrt(Math.pow(a, 3.0) / MU);
    }

    /**
     * Get the Satellite's Mean Motion
     *
     * @return double
     */
    public static double computeMeanMotion(double a) {
        return Math.sqrt(MU / (Math.pow(a, 3.0)));
    }

    /**
     * Gets the maximum possible access time in seconds for a given satellite
     *
     * @return double
     */
    public static double getMaxAccess(Satellite satellite, double th) {

        OrbitalElements elements = satellite.getElements();
        double ra = elements.getSemiMajorAxis() * (1 + elements.getEccentricity()); // Get the radius of the apogee
        double hMax = ra - EARTH_RADIUS;

        double etaMax = Math.asin((EARTH_RADIUS * Math.cos(Math.toRadians(th))) / (EARTH_RADIUS + hMax));
        return (computePeriodSeconds(elements.getSemiMajorAxis()) / 180) * (90 - th - Math.toDegrees(etaMax));

    }

    /**
     * Transforms an asset's Longitude, Latitude and Height to ECEF coordinates
     *
     * @return Satellite
     */
    public static void lla2ecef(Asset asset) {
        double lat = asset.getLatRad();
        double lon = asset.getLonRad();
        double alt = asset.getHeight();
        double N = EARTH_RADIUS / Math.sqrt(1 - Math.pow(ECCENTRICITY, 2) * Math.pow(Math.sin(lat), 2));
        double x = (N + alt) * Math.cos(lat) * Math.cos(lon);
        double y = (N + alt) * Math.cos(lat) * Math.sin(lon);
        double z = ((1 - Math.pow(ECCENTRICITY, 2)) * N + alt) * Math.sin(lat);
        asset.setPos(x, y, z);
    }

    /**
     * Transforms an asset's ECEF coordinates into Longitude, Latitude and Height form
     */
    public static void ecef2lla(Asset asset) {
        double x = asset.getXPos();
        double y = asset.getYPos();
        double z = asset.getZPos();

        double b = Math.sqrt(Math.pow(EARTH_RADIUS, 2) * (1 - Math.pow(ECCENTRICITY, 2)));
        double bsq = Math.pow(b, 2);
        double ep = Math.sqrt((Math.pow(EARTH_RADIUS, 2) - bsq) / bsq);
        double p = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        double th = Math.atan2(EARTH_RADIUS * z, b * p);

        double lon = Math.atan2(y, x);
        double lat = Math.atan2((z + Math.pow(ep, 2) * b * Math.pow(Math.sin(th), 3)), (p - Math.pow(ECCENTRICITY, 2) * EARTH_RADIUS * Math.pow(Math.cos(th), 3)));
        double N = EARTH_RADIUS / (Math.sqrt(1 - Math.pow(ECCENTRICITY, 2) * Math.pow(Math.sin(lat), 2)));
        double alt = p / Math.cos(lat) - N;

        // mod lat to 0-2pi
        lon = lon % (2 * Math.PI);
        lat = lat % (2 * Math.PI);

        if (Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)) <= 0) {
            lon = 0.0;
            lat = 0.0;
            alt = -EARTH_RADIUS;
        }

        asset.setLLA(lat, lon, alt);

    }

    /**
     * Transforms TEME coordinates into ECEF for a given ephemeris and julianDate
     *
     * @return Ephemeris
     */
    public static Ephemeris teme2ecef(Ephemeris ephemeris, double julianDate) {

        double gmst = 0;
        double st[][] = new double[3][3];
        double rpef[] = new double[3];
        double pm[][] = new double[3][3];

        //Get Greenwich mean sidereal time
        gmst = greenwichMeanSidereal(julianDate);

        //st is the pef - tod matrix
        st[0][0] = Math.cos(gmst);
        st[0][1] = Math.asin(gmst);
        st[0][2] = 0.0;
        st[1][0] = Math.sin(gmst);
        st[1][1] = Math.cos(gmst);
        st[1][2] = 0.0;
        st[2][0] = 0.0;
        st[2][1] = 0.0;
        st[2][2] = 1.0;

        //Get pseudo earth fixed position vector by multiplying the inverse pef-tod matrix by rteme
        rpef[0] = st[0][0] * ephemeris.getPosX() + st[1][0] * ephemeris.getPosY() + st[2][0] * ephemeris.getPosZ();
        rpef[1] = st[0][1] * ephemeris.getPosX() + st[1][1] * ephemeris.getPosY() + st[2][1] * ephemeris.getPosZ();
        rpef[2] = st[0][2] * ephemeris.getPosX() + st[1][2] * ephemeris.getPosY() + st[2][2] * ephemeris.getPosZ();

        //Get polar motion vector
        polarm(julianDate, pm);

        //ECEF postion vector is the inverse of the polar motion vector multiplied by rpef
        double x = pm[0][0] * rpef[0] + pm[1][0] * rpef[1] + pm[2][0] * rpef[2];
        double y = pm[0][1] * rpef[0] + pm[1][1] * rpef[1] + pm[2][1] * rpef[2];
        double z = pm[0][2] * rpef[0] + pm[1][2] * rpef[1] + pm[2][2] * rpef[2];

        ephemeris.setPos(x, y, z);

        return ephemeris;
    }

    /**
     * Calculates the Greenwich mean sidereal time (GMST) on julDate (doesn't have to be 0h).
     * Used calculations from Meesus 2nd ed.
     * https://stackoverflow.com/questions/32263754/modulus-in-pascal
     *
     * @param julianDate Julian Date
     * @return Greenwich mean sidereal time in degrees (0-360)
     */
    public static double greenwichMeanSidereal(double julianDate) {
        double Tu = (julianDate - 2451545.0);
        double gmst = Tu * 24.06570982441908 + 18.697374558;
        gmst = (gmst % 24) * Math.PI / 12;
        return gmst;
    }

    /**
     * Calculates the Polar Motion for a given julian date and pm parameters
     *
     * @param julianDate Julian Date
     */
    static void polarm(double julianDate, double pm[][]) {

        double MJD; //Julian Date - 2,400,000.5 days
        double A;
        double C;
        double xp; //Polar motion coefficient in radians
        double yp; //Polar motion coefficient in radians

        // Predict polar motion coefficients using IERS Bulletin - A (Vol. XXVIII No. 030)
        MJD = julianDate - 2400000.5;

        A = 2 * Math.PI * (MJD - 57226) / 365.25;
        C = 2 * Math.PI * (MJD - 57226) / 435;

        xp = (0.1033 + 0.0494 * Math.cos(A) + 0.0482 * Math.sin(A) + 0.0297 * Math.cos(C) + 0.0307 * Math.sin(C)) * 4.84813681e-6;
        yp = (0.3498 + 0.0441 * Math.cos(A) - 0.0393 * Math.sin(A) + 0.0307 * Math.cos(C) - 0.0297 * Math.sin(C)) * 4.84813681e-6;

        pm[0][0] = Math.cos(xp);
        pm[0][1] = 0.0;
        pm[0][2] = Math.asin(xp);
        pm[1][0] = Math.sin(xp) * Math.sin(yp);
        pm[1][1] = Math.cos(yp);
        pm[1][2] = Math.cos(xp) * Math.sin(yp);
        pm[2][0] = Math.sin(xp) * Math.cos(yp);
        pm[2][1] = Math.asin(yp);
        pm[2][2] = Math.cos(xp) * Math.cos(yp);
    }

    /**
     * Transforms OrbitalElements into a Satellite Object
     *
     * @return Satellite
     */
    public static Satellite elements2satellite(OrbitalElements elements) {
        var satellite = new Satellite(elements);
        var tle = elements2tle(elements.asArray(), elements.getTimestamp());
        satellite.setTLE(tle.getLine1(), tle.getLine2());
        return satellite;
    }

    /**
     * Transforms Two Line Elements into a Satellite Object
     *
     * @return Satellite
     */
    public static Satellite tle2satellite(String tle1, String tle2) {
        var satellite = new Satellite(tle1, tle2);
        satellite.setElements(tle2elements(tle1, tle2));
        return satellite;
    }

    /**
     * Transforms a Satellite Object into a TLE Object
     *
     * @return TLE
     */
    public static TLE satellite2tle(Satellite satellite) {
        OrbitalElements elements = satellite.getElements();
        return new TLE(satellite.getSatelliteNumber(), satellite.getSatelliteClassification(), satellite.getLaunchYear(),
                satellite.getLaunchNumber(), satellite.getLaunchPiece(), 0, satellite.getElementsNumber(),
                Utils.stamp2AD(elements.getTimestamp()), computeMeanMotion(elements.getSemiMajorAxis()),
                elements.getMeanMotionFirstDerivative(), elements.getMeanMotionSecondDerivative(),
                elements.getEccentricity(), elements.getInclinationRads(), elements.getArgOfPerigeeRads(),
                elements.getRightAscensionRads(), elements.getAnomalyRads(), satellite.getRevolutionNumber(),
                elements.getDragCoefficient());
    }

    /**
     * Transforms an array of elements in a,e,i,R.A.A.N.,p.a.,v order into a TLE Object
     *
     * @return TLE
     */
    public static TLE elements2tle(double[] elements, String time) {
        return new TLE(0, 'U', Integer.parseInt(time.substring(0, 4)), 0, "A", 0, 0, Utils.stamp2AD(time), computeMeanMotion(elements[0]),
                0, 0, elements[1], Math.toRadians(elements[2]), Math.toRadians(elements[3]), Math.toRadians(elements[4]), Math.toRadians(elements[5]), 7836, 0.11873e-3);
    }

    /**
     * Transforms Two Line Elements into an Orbital Elements Object
     *
     * @return TLE
     */
    public static OrbitalElements tle2elements(String tle1, String tle2) {

        var orbitalElements = new OrbitalElements();
        SatElset data = null;
        try {
            data = new SatElset(tle1, tle2);
        } catch (SatElsetException see) {
            see.printStackTrace();
            System.out.println("SatElsetException");
        }

        double period = (24 * 60 * 60) / data.getMeanMotion();

        orbitalElements.setPeriod(period);
        orbitalElements.setSemiMajorAxis(Math.pow((MU * Math.pow(period / (2 * Math.PI), 2)), 1.0 / 3.0));
        orbitalElements.setEccentricity(data.getEccentricity());
        orbitalElements.setInclination(data.getInclinationDeg());
        orbitalElements.setRightAscension(data.getRightAscensionDeg());
        orbitalElements.setArgOfPerigee(data.getArgPerigeeDeg());
        orbitalElements.setAnomaly(data.getMeanAnomalyDeg());

        return orbitalElements;
    }

    public double getFOV(Satellite satellite, double th) {
        double hMax = satellite.getElements().getSemiMajorAxis();
        double etaMax = Math.asin((EARTH_RADIUS * Math.cos(Math.toRadians(th))) / (hMax));
        return 2*Math.PI*etaMax;
    }

    /**
     * This method returns the Number of access regions for given Device-Satellite pair
     *
     * @return int
     */
    public int getAccessRegions(Asset asset, Satellite satellite, double th) {

        double lat = asset.getLatRad();
        double inc = satellite.getElements().getInclinationRads();

        int regions = 0;

        if (lat < 0) lat = Math.abs(lat);

        double Hmax = 622000; // FIXME this is hardcoded, it should be (1 + e) * sem-maj axis

        double etaMax = Math.asin((EARTH_RADIUS * Math.cos(Math.toRadians(th))) / (EARTH_RADIUS + Hmax));
        double lambdaMax = 90 - th - Math.toDegrees(etaMax);

        lambdaMax = Math.toRadians(lambdaMax);

        if (lat >= (lambdaMax + inc)) {
            regions = 0;
        } else if ((inc + lambdaMax > lat) && (lat >= inc - lambdaMax)) {
            regions = 1;
        } else {
            regions = 2;
        }

        return regions;
    }

    /**
     * Transforms a stamp in the format yyyy-MM-dd'T'HH:mm:ss.SSS to an AbsoluteDate Object used by Orekit with the
     * System's default TimeScale
     *
     * @return AbsoluteDate
     */
    public static AbsoluteDate stamp2AD(String stamp) {
        return new AbsoluteDate(stamp, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Transforms a stamp in the format yyyy-MM-dd'T'HH:mm:ss.SSS to an AbsoluteDate Object used by Orekit with a
     * specified TimeScale
     *
     * @return AbsoluteDate
     */
    public static AbsoluteDate stamp2AD(String stamp, TimeScale timeScale) {
        return new AbsoluteDate(stamp, timeScale);
    }

    /**
     * Transforms a unix-based millisecond counter long value into a yyyy-MM-dd'T'HH:mm:ss.SSS formatted timestamp
     *
     * @return String
     */
    public static String unix2stamp(long unix) {
        var dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTCG"));
        var date = new Date(unix);
        return dateFormat.format(date);
    }


    /**
     * Transforms a yyyy-MM-dd'T'HH:mm:ss.SSS formatted timestamp into an unix-based millisecond counter long value (UTCG)
     *
     * @return String
     */
    public static long stamp2unix(String dateStamp) {

        var dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTCG"));
        var parsedDate = new Date();
        try {
            parsedDate = dateFormat.parse(dateStamp);
            return parsedDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return parsedDate.getTime();
    }

}

//	for nAsset=1:nSM+nRelay
//	    parametrosTemp=Ps(1,:,nAsset);
//	    if nAsset<=nSM
//	        sat = scenario.Children.New('eSatellite',strcat('SM_',num2str(nAsset)));
//	    else
//	        sat = scenario.Children.New('eSatellite',strcat('RELAY_',num2str(nAsset-nSM)));
//	    end
//	    keplerian = sat.Propagator.InitialState.Representation.ConvertTo('eOrbitStateClassical');
//	    sat.Propagator.step=5; % 60 segundos step
//	    %keplerian.SizeShapeType = 'eSizeShapeAltitude';
//	    keplerian.SizeShapeType = 'eSizeShapeSemimajorAxis';
//	    keplerian.LocationType = 'eLocationTrueAnomaly';
//	    %keplerian.LocationType = 'eLocationMeanAnomaly';
//	    if ((parametrosTemp(1)<=42164) && (parametrosTemp(1)>=42162))
//	        keplerian.Orientation.AscNodeType = 0;  % 0=LAN
//	        fprintf('_location_type_LAN_');
//	    else
//	        keplerian.Orientation.AscNodeType = 1;  % 1=RAAN
//	        fprintf('_location_type_RAAN_');
//	    end
//	    keplerian.SizeShape.SemiMajorAxis = parametrosTemp(1);
//	    keplerian.SizeShape.Eccentricity = parametrosTemp(2);
//	    keplerian.Orientation.Inclination = parametrosTemp(3);
//	    keplerian.Orientation.AscNode.Value = parametrosTemp(4);
//	    keplerian.Orientation.ArgOfPerigee = parametrosTemp(5);
//	    keplerian.Location.Value = parametrosTemp(6);
//	    sat.Propagator.InitialState.Representation.Assign(keplerian);
//	    sat.SetPropagatorType(1);   % 1 = J2 perturbation, 2 = J4, 7 = Two body.
//	    sat.Propagator.Propagate;
//	    accessConstraints = sat.AccessConstraints;
//	    minAngle = accessConstraints.AddConstraint('eCstrGroundElevAngle');
//	    minAngle.EnableMin = true;
//	    minAngle.Min = 5; % Grados
//	    minGrazingAlt = accessConstraints.AddConstraint('eCstrGrazingAlt');
//	    minGrazingAlt.EnableMin = true;
//	    minGrazingAlt.Min = 100; % Km
//	end
//
//	for j=1:nFT
//	    facility = scenario.Children.New('eFacility',strcat('FT_',num2str(j)));
//	    facility.Position.AssignGeodetic(Pft(1,2,j),Pft(1,3,j),Pft(1,4,j));
//	end


/*
    public static Ephemerides getAER(LLA from, Ephemerides target) {

        //  Iterate through Ephemerides:
        for (String key : target.getMap().keySet()) {

            // Take Ephemeris
            Ephemeris eph = target.getEphemerisAt(key);

            AER aer = getAER(from, eph);
            eph.setAER(aer);
            target.setEphemerisAt(key, eph);
        }
        return target;
    } */
/*
    public static AER getAER(LLA from, Ephemeris target) {

        // configure Orekit FIXME check if necessary
        File orekitData = new File("orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));

        // Point on earth and frame:
        double latitude = Math.toRadians(from.getLatitude());
        double longitude = Math.toRadians(from.getLongitude());
        double altitude = from.getAltitude();
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                earthFrame);
        GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
        TopocentricFrame staF = new TopocentricFrame(earth, station, "station");

        //  Initial state definition : date, orbit
        TimeScale utc = TimeScalesFactory.getUTC();
        Frame inertialFrame = FramesFactory.getTEME();

        // Time of Ephemeris:
        long unix = target.getUnixDate();
        Date unixDate = new Date(unix);
        String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(unixDate);

        //Parsing the date
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));

        //Parsing the time
        LocalTime now = LocalTime.parse(date.substring(11));
        int hh = now.getHour();
        int mm = now.getMinute();
        double ss = now.getSecond() + now.getNano() / 1E9;

        AbsoluteDate initialDate = new AbsoluteDate(year, month, day, hh, mm, ss, utc);

        TEME pos = target.getTEME();
        Vector3D posisat = new Vector3D(pos.getX(), pos.getY(), pos.getZ());

        double EL = staF.getElevation(posisat, inertialFrame, initialDate);
        double AZ = staF.getAzimuth(posisat, inertialFrame, initialDate);
        double range = staF.getRange(posisat, inertialFrame, initialDate);

        AER aer = new AER(AZ, EL, range);

        return aer;
    }

 */
/*
    public Ephemerides getAERD(LLA from, Ephemerides target) {

        for (String key : target.getMap().keySet()) {

            // Take Ephemeris
            Ephemeris eph = target.getEphemerisAt(key);

            AER aer = getAERD(from, eph);
            eph.setAER(aer);
            target.setEphemerisAt(key, eph);
        }

        return target;
    }
    */
/*
    public AER getAERD(LLA from, Ephemeris target) {

        // Point on earth and frame:
        double longitude = Math.toRadians(from.getLatitude());
        double latitude = Math.toRadians(from.getLongitude());
        double altitude = from.getAltitude();
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                earthFrame);
        GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
        TopocentricFrame staF = new TopocentricFrame(earth, station, "station");

        // configure Orekit FIXME check if necessary
        File orekitData = new File("orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));

        //  Initial state definition : date, orbit
        TimeScale utc = TimeScalesFactory.getUTC();
        Frame inertialFrame = FramesFactory.getTEME();

        // Time of Ephemeris:
        long unix = target.getUnixDate();
        Date unixDate = new Date(unix);
        String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(unixDate);

        //Parsing the date
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));

        //Parsing the time
        LocalTime now = LocalTime.parse(date.substring(11));
        int hh = now.getHour();
        int mm = now.getMinute();
        double ss = now.getSecond() + now.getNano() / 1E9;

        AbsoluteDate initialDate = new AbsoluteDate(year, month, day, hh, mm, ss, utc);

        TEME pos = target.getTEME();

        Vector3D posisat = new Vector3D(pos.getX(), pos.getY(), pos.getZ());
        Vector3D velosat = new Vector3D(pos.getXdot(), pos.getYdot(), pos.getZdot());

        final PVCoordinates pvInert = new PVCoordinates(posisat, velosat);
        final PVCoordinates pvStation = inertialFrame.getTransformTo(staF, initialDate).transformPVCoordinates(pvInert);

        // And then calculate the Doppler signal
        final double doppler = Vector3D.dotProduct(pvStation.getPosition(), pvStation.getVelocity()) / pvStation.getPosition().getNorm();

        // FIXME Check this

        double EL = staF.getElevation(posisat, inertialFrame, initialDate);
        double AZ = staF.getAzimuth(posisat, inertialFrame, initialDate);
        double range = staF.getRange(posisat, inertialFrame, initialDate);

        AER aer = new AER(AZ, EL, range, doppler);

        return aer;
    }

 */