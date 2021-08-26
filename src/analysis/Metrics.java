package analysis;

import simulation.structures.Interval;
import simulation.utils.Reports;

import java.util.List;

public class Metrics {

    public static void main(String[] args) {

        var multiGatewayAnalysis = new MultiGateway("devices.csv", "satellites.csv",
                "2020-03-20T11:00:00.000", "2020-03-21T11:00:00.000", 60D, 5);

        multiGatewayAnalysis.setIncludeCoverageGaps(true);

        multiGatewayAnalysis.computeDevicesPOV();
//        multiGatewayAnalysis.computeSatellitesPOV();

        System.out.println("Overlapped intervals");
        Reports.printAccessCSV(multiGatewayAnalysis.getCombinedIntervals());
        System.out.println("Individual intervals");
        Reports.printAccessCSV(multiGatewayAnalysis.getCurrentIntervals());
        System.out.println("Maximum MCG: " + multiGatewayAnalysis.getMaxMCGMinutes());


    }

    public static List<Interval> getThroughput(List<Interval> intervals) {

        double frameRate = 0.3;
        double pNoInterference = 0.8;

        for (Interval interval : intervals) {
            double nOfGateways = interval.getFromAssets().size();
            if (nOfGateways == 0) {
                interval.setMetric(0);
            } else {
                interval.setMetric(nOfGateways*frameRate*Math.pow(pNoInterference, nOfGateways - 1.00000));
            }
        }

        return intervals;

    }

    // C:\Users\Santi\Desktop\STARS\MatlabFiles

}
