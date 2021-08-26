package simulation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import simulation.assets.Asset;
import simulation.assets.objects.Device;
import simulation.assets.objects.Satellite;
import simulation.structures.Ephemeris;
import simulation.structures.Interval;
import simulation.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Simulation implements Runnable {

    private List<Interval> intervalList;
    private List<Ephemeris> ephemerisList = new ArrayList<>();
    private String time1 = "2020-03-20T11:00:00.000";
    private String time2 = "2020-03-30T11:00:00.000";

    private Satellite satellite;
    private Device device;

    private double step = 60D;
    private double th;
    private Frame inertialFrame;
    private BodyShape earth;
    private GeodeticPoint geodeticPoint;
    private TopocentricFrame topocentricFrame;
    private TLEPropagator tlePropagator;
    private final double TH_DETECTION = 0.001; // 1 ms default
    private Date contact = new Date();
    private double lastSimTime = 0;

    public Simulation() {
        init();
    }

    public Simulation(String timeStart, String timeEnd, Device device, Satellite satellite, double step, double th) {
        init();
        this.time1 = timeStart;
        this.time2 = timeEnd;
        setSatellite(satellite);
        setDevice(device);
        this.step = step;
        this.th = Math.toRadians(th);
    }

    public Simulation(String timeStart, String timeEnd, double step, double th) {
        init();
        this.time1 = timeStart;
        this.time2 = timeEnd;
        this.step = step;
        this.th = Math.toRadians(th);
    }

    private void init() {

        // configure Orekit
        var orekitData = new File("orekit-data");
        if (!orekitData.exists()) {
            System.err.format(Locale.US, "Failed to find %s folder%n",
                    orekitData.getAbsolutePath());
            System.exit(1);
        }

        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));

        // configure Earth frame:
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        this.earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                earthFrame);
        this.inertialFrame = FramesFactory.getEME2000();
        this.intervalList = new ArrayList<>();

    }

    public void setParams(String timeStart, String timeEnd, double step, double th) {
        this.time1 = timeStart;
        this.time2 = timeEnd;
        this.step = step;
        this.th = Math.toRadians(th);
    }

    public String getStartTime() {
        return this.time1;
    }

    public String getEndTime() {
        return this.time2;
    }

    public long getStartTimeUnix() {
        return Utils.stamp2unix(this.time1);
    }

    public long getEndTimeUnix() {
        return Utils.stamp2unix(this.time2);
    }

    public long getTimeSpan() {
        return (Utils.stamp2unix(this.time1) - Utils.stamp2unix(this.time2));
    }

    public Satellite getSatellite() {
        return satellite;
    }

    public Device getDevice() {
        return device;
    }

    public List<Interval> getIntervals() {
        return intervalList;
    }

    public void setAssets(Device device, Satellite satellite) {
        setDevice(device);
        setSatellite(satellite);
    }

    public void setAsset(Asset asset) {
        if (asset.getClass().isAssignableFrom(Device.class)) {
            setDevice((Device) asset);
        } else if (asset.getClass().isAssignableFrom(Satellite.class)) {
            setSatellite((Satellite) asset);
        }
    }

    public void setDevice(Device device) {
        this.device = device;
        this.geodeticPoint = new GeodeticPoint(device.getLatRad(), device.getLonRad(), device.getHeight());
        this.topocentricFrame = new TopocentricFrame(earth, geodeticPoint, device.getName());
    }

    public void setSatellite(Satellite satellite) {
        this.satellite = satellite;
        TLE tle;
        if (satellite.getTLE1().isEmpty() || satellite.getTLE2().isEmpty()) {
            tle = Utils.satellite2tle(satellite);
        } else {
            tle = new TLE(satellite.getTLE1(), satellite.getTLE2());
        }
        this.tlePropagator = TLEPropagator.selectExtrapolator(tle);
    }

    public double getTotalAccess() {
        double sum = 0;
        for (Interval interval : intervalList) {
            sum = sum + interval.getDuration();
        }
        return sum;
    }

    public void computeAccess() {

        long t0 = System.currentTimeMillis();

        intervalList.clear();

        contact.setTime(Utils.stamp2unix(time1));
        EventDetector elevDetector = new ElevationDetector(step, TH_DETECTION, topocentricFrame).
                withConstantElevation(th).
                withHandler(
                        (s, detector, increasing) -> {
                            addInterval(s, detector, increasing);
                            return Action.CONTINUE;
                        });

        this.tlePropagator.addEventDetector(elevDetector);
        accessBetweenDates(Utils.stamp2AD(time1), Utils.stamp2AD(time2));
        lastSimTime = System.currentTimeMillis() - t0;

    }

    private void accessBetweenDates(AbsoluteDate time1, AbsoluteDate time2) {
        double scenarioTime = time2.durationFrom(time1);
        tlePropagator.propagate(time1, time1.shiftedBy(scenarioTime));
    }

    private void addInterval(SpacecraftState s, ElevationDetector detector, boolean dir) { // TODO remove detector and add last contact within simulation end case
        try {
            if (dir) {
                contact = s.getDate().toDate(TimeScalesFactory.getUTC());
            } else {
                intervalList.add(new Interval(contact.getTime(), s.getDate().toDate(TimeScalesFactory.getUTC()).getTime(), this.device.getId(), this.satellite.getId()));
            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    // * Generate TEMEOfDate Position - Velocity vectors * //
    public void computePVD() {
        propagateAndGetPVD(Utils.stamp2AD(time1), Utils.stamp2AD(time2), this.step);
    }

    public void computePVDBetween(long startTime, long endTime) {
        computePVDBetween(startTime, endTime, this.step);
    }

    public void computePVDBetween(long startTime, long endTime, double stepInSeconds) {
        computePVDBetween(Utils.unix2stamp(startTime), Utils.unix2stamp(endTime), stepInSeconds);   // FIXME needs rework
    }

    public void computePVDBetween(String startTime, String endTime) {
        computePVDBetween(startTime, endTime, this.step);
    }

    public void computePVDBetween(String startTime, String endTime, double stepInSeconds) {
        propagateAndGetPVD(Utils.stamp2AD(startTime), Utils.stamp2AD(endTime), stepInSeconds);
    }

    private void propagateAndGetPVD(AbsoluteDate startDate, AbsoluteDate endDate, double step) {

        long t0 = System.currentTimeMillis();
        ephemerisList.clear();
        var lastPoint = false;
        AbsoluteDate pointerDate = startDate;
        while (pointerDate.compareTo(endDate) <= 0) {
            // Get the position and velocity of spacecraft in station frame at any time
            PVCoordinates pvInert = tlePropagator.propagate(pointerDate).getPVCoordinates();
            var pvDevice = inertialFrame.getTransformTo(topocentricFrame, pointerDate).transformPVCoordinates(pvInert);

            addEphemeris(pointerDate.toDate(TimeScalesFactory.getUTC()), pvDevice);
            pointerDate = pointerDate.shiftedBy(step);

            if (pointerDate.compareTo(endDate) > 0 && !lastPoint) {
                pointerDate = endDate;
                lastPoint = true;
            }

        }
        lastSimTime = System.currentTimeMillis() - t0;
    }

    private void addEphemeris(Date date, PVCoordinates pvDevice) {

        // Get the satellite's position and velocity in reference to the station
        Vector3D pos = pvDevice.getPosition();
        Vector3D vel = pvDevice.getVelocity();

        // Calculate Range
        double range = pvDevice.getPosition().getNorm();

        // Calculate the doppler signal
        double doppler = Vector3D.dotProduct(pvDevice.getPosition(), pvDevice.getVelocity()) / range;

        var ephemeris = new Ephemeris(date.getTime(), pos.getX(), pos.getY(), pos.getZ(), vel.getX(), vel.getY(), vel.getZ(), range, doppler);
        ephemerisList.add(ephemeris);
    }

    public List<Ephemeris> getEphemerisList() {
        return ephemerisList;
    }

    public double getLastSimTime() {
        return this.lastSimTime;
    }

    @Override
    public void run() {
        computeAccess();
    }

}
	


