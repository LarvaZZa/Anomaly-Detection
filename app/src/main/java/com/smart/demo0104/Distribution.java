package com.smart.demo0104;

public class Distribution {
    private double[] data;
    private double max;
    private double min;
    private double variance;
    private double average;

    public Distribution(double[] data){
        System.out.println("I have data" + data[7]);
        this.data = data;
        calculateStats();
    }

    public double getMean(){
        return average;
    }

    private void calculateStats() {
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        average = 0;
        variance = 0;
        for (double sample : data) {
            if (sample > max) max = sample;
            if (sample < min) min = sample;
            average += sample;
            variance += sample * sample;
        }

        average = average / data.length;
        variance = variance / data.length - average * average;
    }

    public double getStandardDeviation() {
        return Math.sqrt( variance );
    }
}
