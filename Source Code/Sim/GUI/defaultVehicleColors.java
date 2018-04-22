package GUI;

import microModel.jVehicle;

/**
 * Enumeration of vehicle color selections with additional functionality to
 * return the color and to set the color information (e.g. legend).
 */
public enum defaultVehicleColors implements vehicleColor {
    
    /** Default white. */
    NONE, 
    /** Velocity on continuous map. */
    VELOCITY, 
    /** Acceleration on continuous map. */
    ACCELERATION, 
    /** Destination. */
    DESTINATION,
    /** Desired headway on continuous map. */
    DESIRED_HEADWAY,
    /** Lane change preparation (yielding, synchronizing etc.). */
    LANE_CHANGE_PROCESS, 
    /** Desired speed on continuous map. */
    DESIRED_SPEED, 
    /** Class id. */
    CLASS_ID;

    /** Default acceleration values of the default color map. */
    private final double[] ACCELERATIONS = new double[] {-10, -5, -2, 0, 2};
    
    /** Default velocity values of the default color map. */
    private final double[] VELOCITIES = new double[] {0, 30, 60, 90, 120};
    
    /** Default headway values of the default color map. */
    private final double[] HEADWAYS = new double[] {.5, .75, 1, 1.25, 1.5};
    
    /** Default desired speed values of the default color map. */
    private final double[] DESIRED_SPEEDS = new double[] {.6, .75, .9, 1.05, 1.2}; 

    /**
     * Returns the value as a better looking string for the drop-down box.
     * @return Better looking string.
     */
    @Override
    public String toString() {
        return name().toLowerCase().replace("_", " ");
    }

    /**
     * Returns the color for the given vehicle based on a default color indication.
     * @param vehicle Vehicle to determine the color for.
     * @return Color for the vehicle.
     */
    public java.awt.Color getColor(jVehicle vehicle) {
        java.awt.Color c = new java.awt.Color(255, 255, 255);
        if (this==VELOCITY) {
            c = networkCanvas.fromDefaultMap(VELOCITIES, vehicle.v*3.6);
        } else if (this==DESTINATION) {
            c = networkCanvas.nToColor(vehicle.route.destinations()[0]);
        } else if (this==CLASS_ID) {
            c = networkCanvas.nToColor(vehicle.classID);
        } else if (this==LANE_CHANGE_PROCESS) {
            boolean indic = vehicle.leftIndicator || vehicle.rightIndicator;
            boolean sync = vehicle.getDriver().leftSync || vehicle.getDriver().rightSync;
            boolean yield = vehicle.getDriver().leftYield || vehicle.getDriver().rightYield;
            if (indic) {
                c = new java.awt.Color(255, 64, 0);
            } else if (sync) {
                c = new java.awt.Color(255, 204, 0);
            } else if (yield) {
                c = new java.awt.Color(0, 204, 0);
            } 
        } else if (this==ACCELERATION) {
            c = networkCanvas.fromDefaultMap(ACCELERATIONS, vehicle.a);
        } else if (this==DESIRED_HEADWAY) {
            c = networkCanvas.fromDefaultMap(HEADWAYS, vehicle.getDriver().T());
        } else if (this==DESIRED_SPEED) {
            c = networkCanvas.fromDefaultMap(DESIRED_SPEEDS, 
                    vehicle.getDriver().desiredVelocity()/vehicle.lane.getVLim());
        } else if (this==NONE) {
            // white
        }
        return c;
    }

    /** 
     * Fills the color information panel with the appropiate information/legend
     * depending on a default selection value.
     * @param panel Panel to be filled with the information.
     */
    public void setInfo(javax.swing.JPanel panel) {
        panel.setLayout(null);
        int h = (int) panel.getMaximumSize().getHeight();
        int w = (int) panel.getMaximumSize().getWidth();
        int b = 1;
        int width = (w-4*b)/3;
        int height = (h-3*b)/2;
        java.text.DecimalFormat df0 = new java.text.DecimalFormat("0");
        java.text.DecimalFormat df2 = new java.text.DecimalFormat("0.00");
        float fontSize = 9f;
        if (this==VELOCITY) {
            java.awt.Color[] colors = new java.awt.Color[5];
            String[] labels = new String[5];
            for (int k=0; k<5; k++) {
                colors[k] = networkCanvas.fromDefaultMap(VELOCITIES, VELOCITIES[k]);
                labels[k] = df0.format(VELOCITIES[k])+"km/h";
            }
            networkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==DESTINATION) {
            java.awt.Color[] colors = new java.awt.Color[6];
            String[] labels = new String[6];
            for (int k=0; k<6; k++) {
                colors[k] = networkCanvas.nToColor(k+1);
                labels[k] = "destination "+(k+1);
            }
            labels[5] = "etc.";
            networkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==CLASS_ID) {
            java.awt.Color[] colors = new java.awt.Color[6];
            String[] labels = new String[6];
            for (int k=0; k<6; k++) {
                colors[k] = networkCanvas.nToColor(k+1);
                labels[k] = "class "+(k+1);
            }
            labels[5] = "etc.";
            networkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==LANE_CHANGE_PROCESS) {
            java.awt.Color[] colors = {new java.awt.Color(255, 204, 0), new java.awt.Color(255, 64, 0),
                new java.awt.Color(0, 204, 0), new java.awt.Color(255, 255, 255)};
            String[] labels = {"synchronizing", "indicating", "yielding", "none"};
            networkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==ACCELERATION) {
            java.awt.Color[] colors = new java.awt.Color[5];
            String[] labels = new String[5];
            for (int k=0; k<5; k++) {
                colors[k] = networkCanvas.fromDefaultMap(ACCELERATIONS, ACCELERATIONS[k]);
                labels[k] = "<html>"+df0.format(ACCELERATIONS[k])+"m/s<sup>2</sup></html>";
            }
            networkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==DESIRED_HEADWAY) {
            java.awt.Color[] colors = new java.awt.Color[5];
            String[] labels = new String[5];
            for (int k=0; k<5; k++) {
                colors[k] = networkCanvas.fromDefaultMap(HEADWAYS, HEADWAYS[k]);
                labels[k] = df2.format(HEADWAYS[k])+"s";
            }
            networkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==DESIRED_SPEED) {
            java.awt.Color[] colors = new java.awt.Color[5];
            String[] labels = new String[5];
            for (int k=0; k<5; k++) {
                colors[k] = networkCanvas.fromDefaultMap(DESIRED_SPEEDS, DESIRED_SPEEDS[k]);
                labels[k] = df2.format(DESIRED_SPEEDS[k])+"x";
            }
            networkCanvas.defaultLegend(panel, colors, labels);
        } else if (this==NONE) {
            javax.swing.JLabel lab = new javax.swing.JLabel("Select vehicle color indication here.");
            lab.setBounds(b, h/2-height/2, w-2*b, height);
            lab.setFont(lab.getFont().deriveFont(fontSize).deriveFont(java.awt.Font.PLAIN));
            panel.add(lab);
        }
    }
}