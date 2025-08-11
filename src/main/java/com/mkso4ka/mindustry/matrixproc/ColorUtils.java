package com.mkso4ka.mindustry.matrixproc;

public class ColorUtils {

    public static double[] argbToLab(int argb) {
        int r = (argb >> 24) & 0xFF;
        int g = (argb >> 16) & 0xFF;
        int b = (argb >> 8) & 0xFF;
        return rgbToLab(r, g, b);
    }

    public static double[] rgbToLab(int r, int g, int b) {
        double R = r / 255.0;
        double G = g / 255.0;
        double B = b / 255.0;

        R = (R > 0.04045) ? Math.pow((R + 0.055) / 1.055, 2.4) : R / 12.92;
        G = (G > 0.04045) ? Math.pow((G + 0.055) / 1.055, 2.4) : G / 12.92;
        B = (B > 0.04045) ? Math.pow((B + 0.055) / 1.055, 2.4) : B / 12.92;

        double X = R * 0.4124564 + G * 0.3575761 + B * 0.1804375;
        double Y = R * 0.2126729 + G * 0.7151522 + B * 0.0721750;
        double Z = R * 0.0193339 + G * 0.1191920 + B * 0.9503041;

        X /= 0.95047;
        Y /= 1.00000;
        Z /= 1.08883;

        X = (X > 0.008856) ? Math.cbrt(X) : (7.787 * X) + (16.0 / 116.0);
        Y = (Y > 0.008856) ? Math.cbrt(Y) : (7.787 * Y) + (16.0 / 116.0);
        Z = (Z > 0.008856) ? Math.cbrt(Z) : (7.787 * Z) + (16.0 / 116.0);

        double L = (116.0 * Y) - 16.0;
        double a = 500.0 * (X - Y);
        double b_ = 200.0 * (Y - Z);

        return new double[]{L, a, b_};
    }

    public static double deltaE2000(double[] lab1, double[] lab2) {
        double L1 = lab1[0];
        double a1 = lab1[1];
        double b1 = lab1[2];
        double L2 = lab2[0];
        double a2 = lab2[1];
        double b2 = lab2[2];

        double C1 = Math.sqrt(a1 * a1 + b1 * b1);
        double C2 = Math.sqrt(a2 * a2 + b2 * b2);
        double avgC = (C1 + C2) / 2.0;

        double G = 0.5 * (1 - Math.sqrt(Math.pow(avgC, 7.0) / (Math.pow(avgC, 7.0) + Math.pow(25.0, 7.0))));

        double a1_prime = (1 + G) * a1;
        double a2_prime = (1 + G) * a2;

        double C1_prime = Math.sqrt(a1_prime * a1_prime + b1 * b1);
        double C2_prime = Math.sqrt(a2_prime * a2_prime + b2 * b2);

        double h1_prime = Math.toDegrees(Math.atan2(b1, a1_prime));
        if (h1_prime < 0) h1_prime += 360;
        double h2_prime = Math.toDegrees(Math.atan2(b2, a2_prime));
        if (h2_prime < 0) h2_prime += 360;

        double deltaL_prime = L2 - L1;
        double deltaC_prime = C2_prime - C1_prime;

        double deltah_prime;
        if (C1_prime * C2_prime == 0) {
            deltah_prime = 0;
        } else {
            double diff = h2_prime - h1_prime;
            if (Math.abs(diff) <= 180) {
                deltah_prime = diff;
            } else if (diff > 180) {
                deltah_prime = diff - 360;
            } else {
                deltah_prime = diff + 360;
            }
        }

        double deltaH_prime = 2 * Math.sqrt(C1_prime * C2_prime) * Math.sin(Math.toRadians(deltah_prime / 2.0));

        double avgL_prime = (L1 + L2) / 2.0;
        double avgC_prime = (C1_prime + C2_prime) / 2.0;

        double avgh_prime;
        if (C1_prime * C2_prime == 0) {
            avgh_prime = h1_prime + h2_prime;
        } else {
            double diff = Math.abs(h1_prime - h2_prime);
            if (diff <= 180) {
                avgh_prime = (h1_prime + h2_prime) / 2.0;
            } else {
                if ((h1_prime + h2_prime) < 360) {
                    avgh_prime = (h1_prime + h2_prime + 360) / 2.0;
                } else {
                    avgh_prime = (h1_prime + h2_prime - 360) / 2.0;
                }
            }
        }

        double T = 1 - 0.17 * Math.cos(Math.toRadians(avgh_prime - 30)) +
                0.24 * Math.cos(Math.toRadians(2 * avgh_prime)) +
                0.32 * Math.cos(Math.toRadians(3 * avgh_prime + 6)) -
                0.20 * Math.cos(Math.toRadians(4 * avgh_prime - 63));

        double SL = 1 + (0.015 * Math.pow(avgL_prime - 50, 2)) / Math.sqrt(20 + Math.pow(avgL_prime - 50, 2));
        double SC = 1 + 0.045 * avgC_prime;
        double SH = 1 + 0.015 * avgC_prime * T;

        double RT = -2 * Math.sqrt(Math.pow(avgC_prime, 7.0) / (Math.pow(avgC_prime, 7.0) + Math.pow(25.0, 7.0))) *
                Math.sin(Math.toRadians(60 * Math.exp(-Math.pow((avgh_prime - 275) / 25.0, 2))));

        double termL = deltaL_prime / SL;
        double termC = deltaC_prime / SC;
        double termH = deltaH_prime / SH;

        return Math.sqrt(termL * termL + termC * termC + termH * termH + RT * termC * termH);
    }
}
