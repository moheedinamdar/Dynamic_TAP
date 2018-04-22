package microModel;

/**
 * Single traffic light.
 */
public class jTrafficLight extends jRSU {

    /** Light color. */
    protected lightColor color = lightColor.GREEN;

    /**
     * Constructor that sets the traffic light as noticeable.
     * @param lane Lane where the traffic light is at.
     * @param position Position on the lane.
     */
    public jTrafficLight(jLane lane, double position) {
        super(lane, position, false, true);
    }
    
    /** Empty, needs to be implemented. */
    public void init() {}
    
    /** 
     * Empty, needs to be implemented. 
     * @param vehicle Passing vehicle.
     */
    public void pass(jVehicle vehicle) {}

    /** Empty, needs to be implemented. */
    public void control() {}
    
    /** 
     * Returns whether the traffic light is currently green.
     * @return Whether the traffic light is green.
     */
    public boolean isGreen() {
        return color==lightColor.GREEN;
    }
    
    /** 
     * Returns whether the traffic light is currently yellow. 
     * @return Whether the traffic light is yellow.
     */
    public boolean isYellow() {
        return color==lightColor.YELLOW;
    }
    
    /** 
     * Returns whether the traffic light is currently red. 
     * @return Whether the traffic light is red.
     */
    public boolean isRed() {
        return color==lightColor.RED;
    }
    
    /** Sets the traffic light to green. */
    public void setGreen() {
        color = lightColor.GREEN;
    }
    
    /** Sets the traffic light to yellow. */
    public void setYellow() {
        color = lightColor.YELLOW;
    }
    
    /** Sets the traffic light to red. */
    public void setRed() {
        color = lightColor.RED;
    }
    
    /**
     * Attaches a simple fixed-time controller to the traffic light. The phase 
     * of the cycle is set at the beginning.
     * @param tGreen Green duration [s].
     * @param tYellow Yellow duration [s].
     * @param tRed Red duration [s].
     */
    public void attachSimpleController(double tGreen, double tYellow, double tRed) {
        model.addController(new simpleController(model, tGreen, tYellow, tRed, 0));
    }
    
    /**
     * Attaches a simple fixed-time controller to the traffic light.
     * @param tGreen Green duration [s].
     * @param tYellow Yellow duration [s].
     * @param tRed Red duration [s].
     * @param tPhase Time within the cycle to start at [s].
     */
    public void attachSimpleController(double tGreen, double tYellow, double tRed, double tPhase) {
        model.addController(new simpleController(model, tGreen, tYellow, tRed, tPhase));
    }

    /** Empty, needs to be implemented. */
    public void noControl() {}
    
    /** Enumeration for traffic light colors. */
    protected enum lightColor {
        /** Light is red. */
        RED,
        /** Light is yellow (or orange). */
        YELLOW,
        /** Light is green. */
        GREEN
    }
    
    /**
     * Simple fixed-time controller to be attached to a traffic light.
     */
    private class simpleController extends jController {

        /** Green duration [s]. */
        private double tGreen;
        
        /** Yellow duration [s]. */
        private double tYellow;
        
        /** Red duration [s]. */
        private double tRed;
        
        /** Time of last state switch. */
        private double tSwitch;

        /**
         * Constructs a simple traffic light controller and add it to the model.
         * @param model model object.
         * @param tGreen Green duration [s].
         * @param tYellow Yellow duration [s].
         * @param tRed Red duration [s].
         * @param tPhase Time within the cycle to start at [s].
         */
        public simpleController(jModel model, double tGreen, double tYellow, double tRed, double tPhase) {
            super(model);
            // check whether input is valid
            if (tPhase<0 || tPhase>tGreen+tYellow+tRed) {
                throw new RuntimeException("Phase time "+tPhase+" outside of phase duration.");
            }
            if (tGreen<0 || tYellow<0 || tRed<0) {
                throw new RuntimeException("Negative time defined for traffic light.");
            }
            // Set phase
            if (tPhase<=tGreen) {
                setGreen();
                tSwitch = model.t() - tPhase;
            } else if (tPhase<=tGreen+tYellow) {
                setYellow();
                tSwitch = model.t() + tGreen - tPhase;
            } else if (tPhase<=tGreen+tYellow+tPhase) {
                setRed();
                tSwitch = model.t() + tGreen + tYellow - tPhase;
            }
            // Set times
            this.tGreen = tGreen;
            this.tYellow = tYellow;
            this.tRed = tRed;
        }

        /** Empty, needs to be implemented. */
        @Override
        public void init() {}

        /**
         * Changes the state of the traffic light according to present times.
         */
        @Override
        public void control() {
            if (isGreen() && model.t()-tSwitch>=tGreen) {
                setYellow();
                tSwitch = model.t();
            } else if (isYellow() && model.t()-tSwitch>=tYellow) {
                setRed();
                tSwitch = model.t();
            } else if (isRed() && model.t()-tSwitch>=tRed) {
                setGreen();
                tSwitch = model.t();
            }
        }

        /** Empty, needs to be implemented. */
        @Override
        public void noControl() {}
    }
}