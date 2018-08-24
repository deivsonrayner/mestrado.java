package academico.cne;

import java.awt.geom.Point2D;

import org.json.JSONArray;

/**
 * @author Christopher Fuhrman (christopher.fuhrman@gmail.com)
 * @version 2006-09-27
 */
public class PolygonUtilities {
	
	public static String converteCoordenadas(JSONArray json) {
		int coordSize = json.length();
		String polygon = ""; 
		for (int idx2 = 0; idx2 < coordSize; idx2++) {
			if (polygon.isEmpty()) {
				polygon = "POLYGON((";
			} else {
				polygon += ",";
			}
			JSONArray point = json.getJSONArray(idx2);
			String lat = "" + point.getDouble(1);
			String lng = "" + point.getDouble(0);
			polygon += lat + "%20" + lng;
		}
		polygon += "))";
		
		return polygon;
	}
	
	public static double[][] converteCoordenadasToArray(JSONArray json) {
		int coordSize = json.length();
		double[][] polygon = new double[coordSize][]; 
		for (int idx = 0; idx < coordSize; idx++) {
			
			JSONArray point = json.getJSONArray(idx);
			Double lat = point.getDouble(1);
			Double lng = point.getDouble(0);
			double[] item = new double[] {lng,lat};
			polygon[idx] = item;
		}
		
		return polygon;
	}
	
	public static Point2D[] converteCoordenadasToPoint(JSONArray json) {
		int coordSize = json.length();
		Point2D[] points = new Point2D[coordSize];
		for(int idx = 0; idx < coordSize; idx++) {
			JSONArray point = json.getJSONArray(idx);
			Double lat = point.getDouble(1);
			Double lng = point.getDouble(0);
			Point2D point2d = new Point2D.Double(lng,lat);
			points[idx] = point2d;
		}
		return points;
	}

	/**
	 * Function to calculate the area of a polygon, according to the algorithm
	 * defined at http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
	 * 
	 * @param polyPoints
	 *            array of points in the polygon
	 * @return area of the polygon defined by pgPoints
	 */
	public static double area(Point2D[] polyPoints) {
		int i, j, n = polyPoints.length;
		double area = 0;

		for (i = 0; i < n; i++) {
			j = (i + 1) % n;
			area += polyPoints[i].getX() * polyPoints[j].getY();
			area -= polyPoints[j].getX() * polyPoints[i].getY();
		}
		area /= 2.0;
		return (area);
	}

	/**
	 * Function to calculate the center of mass for a given polygon, according
	 * ot the algorithm defined at
	 * http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
	 * 
	 * @param polyPoints
	 *            array of points in the polygon
	 * @return point that is the center of mass
	 */
	public static double[] centerOfMass(Point2D[] polyPoints) {
		double cx = 0, cy = 0;
		double area = area(polyPoints);
		int i, j, n = polyPoints.length;

		double factor = 0;
		for (i = 0; i < n; i++) {
			j = (i + 1) % n;
			factor = (polyPoints[i].getX() * polyPoints[j].getY()
					- polyPoints[j].getX() * polyPoints[i].getY());
			cx += (polyPoints[i].getX() + polyPoints[j].getX()) * factor;
			cy += (polyPoints[i].getY() + polyPoints[j].getY()) * factor;
		}
		area *= 6.0f;
		factor = 1 / area;
		cx *= factor;
		cy *= factor;
		return new double[] {cx,cy};
	}

}