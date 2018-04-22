package microModel;

/**
 * Respresents a dual-loop induction detector that registers vehicles including
 * their speeds.
 */
public class jDetector extends jRSU {

    /** Delayed flow measurement. */
    public jDelayed<Integer> q;

    /** Delayed speed measurement. */
    public jDelayed<Double> v;

    /** Vehicle count within the current period. */
    protected int qCur;

    /** Average vehicle speed within the current period. */
    protected double vCur;

    /** History of flow measurements. */
    public java.util.ArrayList<Integer> qHist = new java.util.ArrayList<Integer>();

    /** History of speed measuerments. */
    public java.util.ArrayList<Double> vHist = new java.util.ArrayList<Double>();

    /** History of measured period starting times. */
    public java.util.ArrayList<Double> tHist = new java.util.ArrayList<Double>();

    /** ID of real-life detector / user recognizable number. */
    protected int id;

    /**
     * Constructor that initializes the jDelayed measurements.
     * @param lane Lane where detector is at.
     * @param x Position [m] of detector at the lane.
     * @param period Aggregation period [s].
     * @param id User recognizable ID number.
     */
    public jDetector(jLane lane, double x, double period, int id) {
        super(lane, x, period, true, false);
        this.id = id;
        q = new jDelayed<Integer>(lane.model.settings.getDouble("detectorDelay"), lane.model.dt);
        v = new jDelayed<Double>(lane.model.settings.getDouble("detectorDelay"), lane.model.dt);
    }

    /** Empty, needs to be implemented. */
    public void init() {}
    
    /**
     * Performs the detector task. At the end of each aggregation period, a flow
     * count and average speed is added.
     */
    public void control() {
        // data is aggregated this time step
        q.put(qCur);
        v.put(vCur);
        if (lane.model.settings.getBoolean("storeDetectorData")) {
            qHist.add(qCur);
            vHist.add(vCur);
            tHist.add(lane.model.t);
        }
        // reset count
        qCur = 0;
        vCur = 0;
    }

    /**
     * No control puts 'null' into the jDelayed flow counts and speeds.
     */
    public void noControl() {
        q.put(null);
        v.put(null);
    }

    /**
     * Updates the current measurement with an additional vehicle.
     * @param veh Passing vehicle.
     */
    public void pass(jVehicle veh) {
        if (qCur == 0) {
            vCur = veh.v;
        } else {
            // add velocity to average
            vCur = ((vCur*qCur)+veh.v)/(qCur+1);
        }
        qCur++;
    }
    
    /**
     * Returns the ID of this detector.
     * @return ID of this detector.
     */
    public int id() {
        return id;
    }
}