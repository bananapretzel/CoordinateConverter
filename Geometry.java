/**
 * The geometry class is used as a container for the coordinates and type of geometry to be represented in the
 * GeoJSON format.
 */
public class Geometry {
    /** Type defaults to point */
    private String type = "Point";
    private double[] coordinates;

    /**
     * Predefined constants which the geometry's type can be.
     */
    public enum typeConstant {
        POINT("Point"),
        LINESTRING("LineString"),
        POLYGON("Polygon"),
        MULTIPOINT("MultiPoint"),
        MULTILINESTRING("MultiLineString"),
        MULTIPOLYGON("MultiPolygon");

        public final String value;

        typeConstant(String point) {
            this.value = point;
        }
    }

    /**
     * Gets the coordinates set for the geometry object.
     * @return an array of doubles which represent coordinates
     */
    public double[] getCoordinates() {
        return coordinates;
    }

    /**
     * Sets the coordinates for this geometry object.
     * @param coordinates an array of doubles
     */
    public void setCoordinates(double[] coordinates) {

        this.coordinates = coordinates;
    }

    /**
     * Gets the geometry's type. The default is "point".
     * @return a string representing this geometry's type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the geometry's type. The default is "point".
     * @param cons a predefined constant which the geometry object can be.
     */
    public void setType(typeConstant cons) {
        this.type = cons.value;
    }
}
