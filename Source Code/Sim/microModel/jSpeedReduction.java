package microModel;

/**
 * A speed reduction allows a driver to adapt the speed before a certain 
 * location is reached. Speed limits at lanes are only considered as soon as the
 * vehicle is on that lane, which is not always sufficient. Speed reductions are 
 * typically usefull at the start of lanes where the speed limit is low due to 
 * curvature.
 */
public class jSpeedReduction extends jRSU {
    
    /**
     * Constructs a speed reduction. The reduced speed should be obtained from 
     * the lane.
     * @param lane Lane where the speed reduction is located.
     * @param x Location on the lane [m].
     */
    public jSpeedReduction(jLane lane, double x) {
        super(lane, x, false, true);
    }

    /** Empty, needs to be implemented. */
    public void init() {}

    /** Empty, needs to be implemented. */
    public void pass(jVehicle vehicle) {}

    /** Empty, needs to be implemented. */
    public void control() {}

    /** Empty, needs to be implemented. */
    public void noControl() {}
    
}