/**
 * The properties object is used as a container for additional information within the GeoJSON format.
 */
public class Properties {

    private String name;

    /**
     * Gets the name of the property. This is typically extra words found in the input which do not correlate to a
     * coordinate.
     * @return a string representation of additional words from the input.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the property. This is typically extra words found in the input which do not correlate to a
     * coordinate.
     * @param name extra words identified from the input.
     */
    public void setName(String name) {
        this.name = name;
    }
}
