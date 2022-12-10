import java.util.ArrayList;
import java.util.List;

/**
 * The FeatureCollection class is an object for the GEOJson format which will hold a list of features. This class is
 * designed to be the first object when converted to the JSON format.
 */
public class FeatureCollection {
    private String type = "FeatureCollection";
    private List<Feature> features = new ArrayList<>();

    /**
     * Returns all the current features held within the object.
     *
     * @return a list of features.
     */
    public List<Feature> getFeatures() {
        return features;
    }

    /**
     * This method can be used to add an already compiled list of features as opposed to incrementally adding features
     * using the "addFeature" method.
     *
     * @param features a list of features.
     */
    public void setFeatures(List<Feature> features) {
        this.features = features;
    }

    /**
     * Adds a feature to the list of features.
     *
     * @param feature an object representing a feature which
     */
    public void addFeature(Feature feature) {
        features.add(feature);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

//    @Override
//    public String toString() {
//        return "FeatureCollection{" +
//                "type='" + type + '\'' +
//                ", features=" + features +
//                '}';
//    }
}
