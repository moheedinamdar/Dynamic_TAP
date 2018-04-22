package GUI;

import microModel.jVehicle;

public class TTC implements vehicleColor {

    // Return the color of the given vehicle
    public java.awt.Color getColor(jVehicle vehicle) {
        // Calculate time to collision
        double ttc = Double.POSITIVE_INFINITY;
        if (vehicle.down!=null && vehicle.v>vehicle.down.v) {
            double s = vehicle.getHeadway(vehicle.down);
            ttc = s/(vehicle.v-vehicle.down.v);
        }
        // Select color
        if (ttc < 4) {
            return java.awt.Color.RED;
        } else {
            return java.awt.Color.WHITE;
        }
    }
    
    // Set legend info using default method
    public void setInfo(javax.swing.JPanel panel) {
        java.awt.Color[] colors = {java.awt.Color.RED, java.awt.Color.WHITE};
        String[] labels = {"Critical", "Non-critical"};
        networkCanvas.defaultLegend(panel, colors, labels);
    }
    
    // Return nice readable string
    // if this method is not overridden, it shows up like myPackage.TTC@1372a1a
    @Override
    public String toString() {
        return "time to collision";
    }
}