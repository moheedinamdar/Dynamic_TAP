package GUI;

import microModel.jVehicle;

/**
 * Interface for user defined vehicle colors. Sub classes should implement the
 * methods as described. To make the color indication selectable, add it to the
 * GUI by using <tt>jModelGUI.addVehicleColor(vehicleColor)</tt>.<br>
 * <br>
 * You can use the following static conveniance methods of the 
 * <tt>networkCanvas</tt> class to get colors and to generate a default legend.
 * <br><br>
 * - <tt>fromDefaultMap</tt> returns an interpolated color on the default color
 * map. The value extend of the map can be defined.<br>
 * - <tt>fromMap</tt> returns an interpolated color in a user defined map.<br>
 * - <tt>nToColor</tt> returns a distinct color for the values 1-18 and repeats
 * the colors for higher values.<br>
 * - <tt>defaultLegend</tt> creates a default legend in a panel with given 
 * colors and labels.
 */
public interface vehicleColor {
    
    /**
     * Returns the color for visualization of a vehicle.
     * @param vehicle The vehicle that will be given the returned color.
     * @return Color for the vehicle.
     */
    public java.awt.Color getColor(jVehicle vehicle);
    
    /**
     * Displays the color information (e.g. a legend) if the color is selected.
     * @param panel Panel within which the information is displayed.
     */
    public void setInfo(javax.swing.JPanel panel);
}