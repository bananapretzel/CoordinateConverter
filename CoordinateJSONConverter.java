import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An application that can take a variety of different geographic coordinate protocols and converts them
 * to standard form. Each coordinate entered will be put into a feature collection and represented in JSON using
 * the GEOJSON standard framework.
 */
public class CoordinateJSONConverter {
    /**
     * Used to make coordinates 8 significant figures long.
     */
    private static final DecimalFormat df = new DecimalFormat("###.######");

    /**
     * Container for any extra text contained in the input.
     */
    private String optionalInfo = "";

    /**
     * Entry point for the application. Google's gson library is used to format the output into JSON.
     *
     * @param args none
     */
    public static void main(String[] args) throws IOException {
        CoordinateJSONConverter CoordinateJSONConverter = new CoordinateJSONConverter();
        Scanner scan = new Scanner(System.in);
        FeatureCollection collection = new FeatureCollection();
        FileWriter fw = new FileWriter("output.GeoJson");
        df.setRoundingMode(RoundingMode.CEILING);

        // Creating gson
        GsonBuilder builder = new GsonBuilder();
        // To make the output look nice
        Gson gson = builder.setPrettyPrinting().create();

        while (scan.hasNextLine()) {
            double[] factoryResult;
            CoordinateJSONConverter.optionalInfo = "";
            String input = scan.nextLine();
            if (input.equals("")) {
                continue;
            }
            Feature feature = new Feature();

            try {
                factoryResult = CoordinateJSONConverter.factory(input);
            } catch (IndexOutOfBoundsException e) {
                System.err.print("Unable to process: ");
                System.err.print(input + "\n");
                continue;
            }
            if (factoryResult == null) {
                System.err.print("Unable to process: ");
                System.err.print(input + "\n");
                continue;
            }
            for (int i = 0; i < factoryResult.length; i++) {
                factoryResult[i] = Double.parseDouble(df.format(factoryResult[i]));
            }

            // swapping around decimals since geoJSON takes first number as a longitude.
            double[] swap = factoryResult;
            double temp = swap[0];
            swap[0] = swap[1];
            swap[1] = temp;
            feature.getGeometry().setCoordinates(swap);
            feature.getGeometry().setCoordinates(factoryResult);
            feature.getProperties().setName(CoordinateJSONConverter.optionalInfo);
            collection.addFeature(feature);
        }
        fw.write(gson.toJson(collection));
        fw.close();
        scan.close();
        // also prints to console
        System.out.println(gson.toJson(collection));

    }

    /**
     * This is the main method of the program. It takes the user's input and does a variety of checks using regex to
     * estimate what type of format the user has written the coordinates in. Once estimated, it will then send the input
     * to the appropriate method for conversion.
     *
     * @param input the user's input
     * @return the user's input converted to decimal form.
     */
    private double[] factory(String input) {
        String pure = input;
        if (!input.matches(".*\\d.*")) {
            return null;
        }
        // check for words in input
        if (input.matches(".*[a-zA-Z]{2,}.*")) {
            input = extractInfo(input);
        }
        input = input.replaceAll("d", "°");
        input = input.replaceAll("m", "'");
        input = input.replaceAll("s", "\"");

        // if input doesn't contain any normal symbols to derive coordinate values then assume it is the dreaded space
        try {
            if (!(input.contains(".") || input.contains("\"") || input.contains("'") || input.contains("°"))) {
                int counter = 0;
                String[] dms = {"°", "'", "\"", "°", "'", "\""};
                String input2 = "";
                for (int i = 0; i < input.length(); i++) {
                    if (input.charAt(i) == ' ' && Character.isDigit(input.charAt(i - 1))) {
                        input2 += dms[counter];
                        counter++;
                    } else {
                        input2 += input.charAt(i);
                    }
                }
                input = input2;
            }
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        // If input doesn't contain any char a-z, assume it is a number (decimal degrees with no cardinal).
        if (!(input.matches(".*[NESWnesw].*"))) {
            // Check if degree decimal minutes.
            String ddmCheck = input.replaceAll(",", " ");
            ddmCheck = ddmCheck.replaceAll("°", " ");
            String[] ddmSplit = ddmCheck.split(" ");
            List<String> temp = new ArrayList<>();
            for (String s : ddmSplit) {
                if (!s.equals("")) {
                    temp.add(s);
                }
            }
            if (temp.size() == 4) {
                return ddmProcess(ddmSplit);
            } else {
                input = input.replaceAll("°", "");
                if (input.contains(":")) {
                    return AlternateDmsProcess(input);
                }

                return decDegProcess(input);
            }
            // If using an alternate symbol...
        } else if (input.contains(":") || input.contains("'") || input.contains("′")) {
            return AlternateDmsProcess(input);
        }

        // Decimal degrees with cardinal check.
        // Else assume there is a cardinal letter.
        else {
            List<String> coordsWithNum = new ArrayList<>();
            String[] splitContainer;
            String spacesRemoved = input.replaceAll(" ", "");
            // If there is a comma.
            if (spacesRemoved.contains(",")) {
                splitContainer = spacesRemoved.split("(?<=,)", 2);
                // Else if there is a letter at the start.
            } else if (Character.isLetter(spacesRemoved.charAt(0))) {
                splitContainer = spacesRemoved.split("(?<=[\"″]|[0-9])(?=[NESWnesw])", 2);
                // Else assume the letter is after the coordinate.
            } else {
                splitContainer = spacesRemoved.split("(?<=[NESWnesw])", 2);
            }
            Collections.addAll(coordsWithNum, splitContainer);

            // if both elements do not have a cardinal...
            if (!(coordsWithNum.get(0).matches(".*[NESWnesw].*") && coordsWithNum.get(1).matches(".*[NESWnesw].*"))) {
                for (String s : coordsWithNum) {
                    if (!(s.contains("N") && s.contains("n") && s.contains("S") && s.contains("s"))) {
                        coordsWithNum.remove(s);
                        String temp = coordsWithNum.get(0);
                        if (Double.parseDouble(temp) > 0) {
                            temp += "N";
                        } else {
                            temp += "S";
                        }
                        coordsWithNum.set(0, temp);
                        coordsWithNum.add(1, s);
                    }
                    break;
                }
            }
            return decimalCardinal(coordsWithNum);
        }
    }

    /**
     * This method is invoked when the factory estimates that the user's input contains a cardinal direction identifier.
     *
     * @param preprocessed the user's un-perverted input.
     * @return the final product of the conversion presented in a double array format for gson to use.
     */
    private double[] decimalCardinal(List<String> preprocessed) {
        List<String> coordsArrayList = new ArrayList<>();
        double[] coords = new double[2];
        char latCard;
        char longCard;

        // Check if there is only one element that represents a longitude and latitude. if there is more than one, fail.
        if (longLatAmount(preprocessed)) {
            return null;
        }

        // Putting north as first coordinate
        for (int i = 0; i < preprocessed.size(); i++) {
            String str = preprocessed.get(i).replaceAll(" ", "");
            if (str.contains("N") || str.contains("n") || str.contains("S") || str.contains("s")) {
                coordsArrayList.add(str);
                preprocessed.remove(i);
                break;
            }
        }
        // Adding the other coordinate
        coordsArrayList.add(preprocessed.get(0));

        if (coordsArrayList.get(0).contains("S") || coordsArrayList.get(0).contains("s")) {
            latCard = 's';
        } else {
            latCard = 'n';
        }

        if (coordsArrayList.get(1).contains("E") || coordsArrayList.get(1).contains("e")) {
            longCard = 'e';
        } else {
            longCard = 'w';
        }

        // Removing the letter in each element and parsing as double so the method can return a double[].
        for (int i = 0; i < coordsArrayList.size(); i++) {
            coords[i] = Double.parseDouble(coordsArrayList.get(i).replaceAll("[^\\d.-]", ""));
        }
        positiveOrNegative(latCard, longCard, coords);
        return coords;
    }

    /**
     * This method is used if the program identifies the user's input using typical symbols used for the degree, minute,
     * and seconds format. It is a long process with the majority of the code used for cleaning the input, so it can be
     * presented in a uniform format for the conversion to take place.
     *
     * @param preprocessed the user's input.
     * @return the final product in degree decimal format.
     */
    private double[] AlternateDmsProcess(String preprocessed) {
        double[] coords = new double[2];
        char latCard;
        char longCard;
        String[] splitContainer;
        Pattern p = Pattern.compile("[0-9.]+");
        Matcher m;
        String spacesRemoved = preprocessed.replaceAll(" ", "");
        if (spacesRemoved.contains(",")) {
            splitContainer = spacesRemoved.split("(?<=,)", 2);
        } else if (Character.isLetter(spacesRemoved.charAt(0))) {
            splitContainer = spacesRemoved.split("(?<=[0-9])(?=[NESWnesw])|(?<=[\"″])(?=[NESWnesw])", 2);
        } else {
            splitContainer = spacesRemoved.split("(?<=[NESWnesw])(?=[0-9])", 2);
        }
        List<String> coordsArrayList = new ArrayList<>(List.of(splitContainer));

        // Removes commas.
        for (int i = 0; i < coordsArrayList.size(); i++) {
            String str = coordsArrayList.get(i).replaceAll(",", "");
            coordsArrayList.remove(i);
            coordsArrayList.add(i, str);
        }
        // Checks if long and lat are at least one coordinate each.
        if (longLatAmount(coordsArrayList)) {
            return null;
        }

        // Put latitude as first coordinate for easier use.
        for (int i = 0; i < coordsArrayList.size(); i++) {
            if (coordsArrayList.get(i).contains("N") || coordsArrayList.get(i).contains("n") || coordsArrayList.get(i).contains("S") || coordsArrayList.get(i).contains("s")) {
                String str = coordsArrayList.get(i);
                coordsArrayList.remove(i);
                coordsArrayList.add(0, str);
                break;
            }
        }

        // remember what the cardinal letter is
        if (coordsArrayList.get(0).contains("S") || coordsArrayList.get(0).contains("s")) {
            latCard = 's';
        } else {
            latCard = 'n';
        }

        if (coordsArrayList.get(1).contains("E") || coordsArrayList.get(1).contains("e")) {
            longCard = 'e';
        } else {
            longCard = 'w';
        }

        // Removing cardinal letters
        for (int i = 0; i < coordsArrayList.size(); i++) {
            String str = coordsArrayList.get(i).replaceAll("[NESWnesw]", "");
            coordsArrayList.remove(i);
            coordsArrayList.add(i, str);
        }

        String latitude = coordsArrayList.get(0);
        String longitude = coordsArrayList.get(1);
        m = p.matcher(latitude);
        long count = m.results().count();

        // If there is three digits in the array
        if (count == 3) {
            splitContainer = latitude.split("[^0-9^.-]");
            coords[0] = dmsConverter(splitContainer);
            // else if there is two assume degree-minute format and process
        } else if (count == 2) {
            latitude = latitude.replaceAll("'", "");
            splitContainer = latitude.split("[^0-9^.-]");
            coords[0] = Double.parseDouble(splitContainer[0]) + (Double.parseDouble(splitContainer[1]) / 60);
        } else {
            splitContainer = latitude.split("[^0-9^.-]");
            for (int i = 0; i < splitContainer.length; i++) {
                if (splitContainer[i].contains("-")) {
                    splitContainer[i] = splitContainer[i].replaceAll("-", "");
                }
            }
            coords[0] = dmsConverter(splitContainer);
        }

        // Removes commas.
        for (int i = 0; i < splitContainer.length; i++) {
            splitContainer[i] = splitContainer[i].replaceAll(",", "");
        }

        m = p.matcher(longitude);
        count = m.results().count();
        // If there is three digits in the array
        if (count == 3) {
            splitContainer = longitude.split("[^0-9^.-]");
            coords[1] = dmsConverter(splitContainer);
            // else if there is two assume degree-minute format and process
        } else if (count == 2) {
            longitude = longitude.replaceAll("'", "");
            splitContainer = longitude.split("[^0-9^.-]");
            coords[1] = Double.parseDouble(splitContainer[0]) + (Double.parseDouble(splitContainer[1]) / 60);
        } else {
            splitContainer = longitude.split("[^0-9^.-]");
            for (int i = 0; i < splitContainer.length; i++) {
                if (splitContainer[i].contains("-")) {
                    splitContainer[i] = splitContainer[i].replaceAll("-", "");
                }
            }
            coords[1] = dmsConverter(splitContainer);
        }
        positiveOrNegative(latCard, longCard, coords);
        return coords;
    }

    /**
     * A simple check to see if two coordinates with cardinal letters are represented properly with either
     * N || S && E || W. If the input doesn't conform, it will output an error and kill the program.
     *
     * @param list the list to check for validity.
     */
    private boolean longLatAmount(List<String> list) {
        // Check if there is only one element that represents a longitude and latitude. if there is more than one, fail.
        int latitude = 0;
        int longitude = 0;
        for (String s : list) {
            if (s.contains("N") || s.contains("n") || s.contains("S") || s.contains("s")) {
                latitude++;
            } else if (s.contains("E") || s.contains("e") || s.contains("W") || s.contains("w")) {
                longitude++;
            }
        }
        if (!(longitude == 1 && latitude == 1)) {
            return true;
        }
        return false;
    }

    /**
     * This method takes a degree, minute, and second coordinate represented as a string list and converts it to the
     * degree-decimal format. The String should be represented as pure numbers without any symbols.
     *
     * @param preprocessed a list of three numbers represented in string format.
     * @return a double which has been converted using the dms conversion formula.
     */
    private double dmsConverter(String[] preprocessed) {
        double decimalDegree;
        double[] decimalDegreeArray = Arrays.stream(preprocessed).mapToDouble(Double::valueOf).toArray();
        if (preprocessed[0].contains("-")) {
            decimalDegree = decimalDegreeArray[0] - ((decimalDegreeArray[1] / 60) + (decimalDegreeArray[2] / 3600));
        } else {
            decimalDegree = decimalDegreeArray[0] + ((decimalDegreeArray[1] / 60) + (decimalDegreeArray[2] / 3600));
        }
        return decimalDegree;
    }

    /**
     * A method used to extract words from the user's input. The words extracted will be used as optional info in the
     * geo-JSON format.
     *
     * @param input the user's input.
     * @return extra words in the user's input.
     */
    private String extractInfo(String input) {
        Pattern p = Pattern.compile("([a-zA-Z]{2,})");
        Matcher m = p.matcher(input);
        while (m.find()) {
            for (int i = 0; i < m.groupCount(); i++) {
                optionalInfo += m.group(i) + " ";
                input = input.replace(m.group(i), "");
            }
        }
        return input;
    }

    /**
     * This method runs when the factory process estimates the user's input as being simple decimal-degrees. It will
     * scrub any commas from the input and split the input into a list of doubles.
     *
     * @param input the user's input.
     * @return a list of doubles representing decimal-degrees.
     */
    private double[] decDegProcess(String input) {
        double[] coords = new double[2];
        input = input.replaceAll(",", " ");
        String[] splitContainer = input.split(" ");
        List<String> splitted = new ArrayList<>(List.of(splitContainer));

        // Removing empty strings.
        while (splitted.contains("")) {
            splitted.remove("");
        }
        for (int i = 0; i < splitted.size(); i++) {
            coords[i] = Double.parseDouble(splitted.get(i));
        }
        return coords;
    }

    /**
     * A method that is invoked when the user's input is estimated to be in the decimal-degree and minutes format.
     * The method remove empty strings from the input, removes any symbols and then processes the cleansed list
     * into a converted double array.
     *
     * @param input the user's input.
     * @return a double array that has been converted to decimal degrees.
     */
    private double[] ddmProcess(String[] input) {

        double[] coords = new double[2];
        List<String> removedEmpties = new ArrayList<>();
        int count = 0;

        // Removing empty strings.
        for (int i = 0; i < input.length; i++) {
            if (input[i].equals("")) {
            } else {
                removedEmpties.add(input[i]);
            }
        }
        double[] atomicCoords = new double[removedEmpties.size()];
        try {
            for (String s : removedEmpties) {
                String[] split = s.split("[^0-9^.-]");
                for (String value : split) {
                    atomicCoords[count] = Double.parseDouble(value);
                    count++;
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (atomicCoords[0] >= 0) {
            coords[0] = atomicCoords[0] + atomicCoords[1] / 60.0;
        } else {
            coords[0] = atomicCoords[0] - atomicCoords[1] / 60.0;
        }
        if (atomicCoords[2] >= 0) {
            coords[1] = atomicCoords[2] + (atomicCoords[3] / 60.0);
        } else {
            coords[1] = atomicCoords[2] - (atomicCoords[3] / 60.0);
        }
        return coords;
    }

    /**
     * Business logic to check if the final result of the processed coordinate should be positive or negative depending
     * on the cardinal.
     *
     * @param latCard       latitude cardinal letter placeholder
     * @param longCard      longitude cardinal letter placeholder
     * @param longLatCoords the preprocessed coordinates
     * @return the converted result
     */
    private double[] positiveOrNegative(char latCard, char longCard, double[] longLatCoords) {
        if ((latCard == 's' && longLatCoords[0] > 0) || (longLatCoords[0] < 0 && latCard == 'n')) {
            longLatCoords[0] *= -1;
        }
        if ((longCard == 'w' && longLatCoords[1] > 0) || (longLatCoords[1] < 0 && longCard == 'e')) {
            longLatCoords[1] *= -1;
        }
        return longLatCoords;
    }
}