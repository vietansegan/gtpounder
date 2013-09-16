package util;

/**
 *
 * @author vietan
 */
public class ZNormalizer extends AbstractNormalizer {

    private double mean;
    private double stdev;

    public ZNormalizer(double[] data) {
        this.mean = mean(data);
        this.stdev = standardDeviation(data);
    }

    public ZNormalizer(double mean, double stdev) {
        this.mean = mean;
        this.stdev = stdev;
    }

    @Override
    public double normalize(double originalValue) {
        return (originalValue - mean) / stdev;
    }

    @Override
    public double denormalize(double normalizedValue) {
        return normalizedValue * stdev + mean;
    }

    public double[] normalize(double[] originalValues) {
        double[] normValues = new double[originalValues.length];
        for (int i = 0; i < normValues.length; i++) {
            normValues[i] = this.normalize(originalValues[i]);
        }
        return normValues;
    }

    public double[] denormalize(double[] normalizedValues) {
        double[] denormValues = new double[normalizedValues.length];
        for (int i = 0; i < denormValues.length; i++) {
            denormValues[i] = this.denormalize(normalizedValues[i]);
        }
        return denormValues;
    }

    public static double mean(double[] values) {
        return sum(values) / values.length;
    }

    public static double sum(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum;
    }

    public static double standardDeviation(double[] values) {
        if (values.length <= 1) {
            return 0.0;
        }

        double mean = mean(values);
        double ssd = 0.0;
        for (int i = 0; i < values.length; i++) {
            ssd += (values[i] - mean) * (values[i] - mean);
        }
        return Math.sqrt(ssd / (values.length - 1));
    }
}
