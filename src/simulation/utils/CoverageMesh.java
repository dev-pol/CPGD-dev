package simulation.utils;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Envelopes;
import org.geotools.grid.GridFeatureBuilder;
import org.geotools.grid.Grids;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.coordinate.Polygon;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CoverageMesh {

  //List[]
  public static List<Double> buildMesh(String ShpFile, double meshResolution) throws Exception {
    File file = null;
    // load shp
    try { 
       file = new File(ShpFile);
    }
    catch (NullPointerException e) {
      e.printStackTrace();
    }
    FileDataStore dataStore = FileDataStoreFinder.getDataStore(file);
    SimpleFeatureSource ozMapSource = dataStore.getFeatureSource();


    // Set the grid size (1 degree) and create a bounding envelope
    // that is neatly aligned with the grid size
    double sideLen = meshResolution;
    ReferencedEnvelope gridBounds = Envelopes.expandToInclude(ozMapSource.getBounds(), sideLen);

    // Create a feature type
    SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
    tb.setName("grid");
    tb.add(
        GridFeatureBuilder.DEFAULT_GEOMETRY_ATTRIBUTE_NAME,
        Polygon.class,
        gridBounds.getCoordinateReferenceSystem());
    tb.add("id", Integer.class);
    SimpleFeatureType TYPE = tb.buildFeatureType();
    // Build the grid the custom feature builder class
    GridFeatureBuilder builder = new IntersectionBuilder(TYPE, ozMapSource);
    SimpleFeatureSource grid = Grids.createHexagonalGrid(gridBounds, sideLen, -1, builder);
    SimpleFeatureCollection FeatureCollection = grid.getFeatures();
    
    // We need the centre of each polygon:
    // Iterate over feature list
    // Extract Geometry
    // Extract Centroid Point
    // Extract Coordinates of Centroid Point
    // Transform Coordinates into Doubles

    List<Double> PolygonCentroids = new ArrayList<>();
    //List<Double> PolygonCentroidLon = new ArrayList<>();
    SimpleFeatureIterator iterator = FeatureCollection.features();
      while (iterator.hasNext()){ // Un-nest the data until we get the coordinates
           SimpleFeature feature = iterator.next();
           Geometry geometry = (Geometry) feature.getDefaultGeometry();
           Point centroid = geometry.getCentroid();
           Coordinate coordinates = centroid.getCoordinate();
           PolygonCentroids.add(coordinates.y); // Lat
           PolygonCentroids.add(coordinates.x); // Lon
      }

    return PolygonCentroids;
  }
}