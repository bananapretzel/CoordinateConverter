/**
 * The feature class is used as an object for the GeoJSON format. This class currently only represents an individual
 * point on a map.
 */
public class Feature {
    private String type = "Feature";
    private Geometry geometry = new Geometry();
    private Properties properties = new Properties();

    /**
     * Gets the geometry object currently held within this feature object.
     *
     * @return a geometry object which contains the coordinates and type of geometry for it.
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * Sets a geometry object for this feature.
     *
     * @param geometry a geometry object.
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Gets the properties object from this feature.
     *
     * @return the feature's property object.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the properties object for this feature.
     *
     * @param properties a propetry object.
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
