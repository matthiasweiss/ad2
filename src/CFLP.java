package ad2.ss17.cflp;

import java.util.*;

public class CFLP extends AbstractCFLP {

    /**
     * CFLPInstance to solve.
     */
    private CFLPInstance cflp;

    /**
     * 2D-Array with the order of preferred Facilities for each customer.
     * (preferences[c][0] returns closest facility for customer c)
     */
    private int[][] preferences;

    /*
     * Quick and dirty access to this.cflp.getNumFacilities()
     */
    private int gnf;

    /*
     * Quick and dirty access to this.cflp.getNumCustomers()
     */
    private int gnc;

    /**
     * Creates a new CFLP instance.
     *
     * @param  CFLPInstance instance
     * O(customer*facilities^2) because of the sort in this.setPreferencs()
     */
    public CFLP(CFLPInstance instance) {
        this.cflp = instance;

        // quick access to the number of facilities and customers
        this.gnf = this.cflp.getNumFacilities();
        this.gnc = this.cflp.getNumCustomers();

        this.setPreferences();
    }

    /**
     * Runs the branch and bound algorithm to determine a solution.
     */
    @Override
    public void run() {
        // Default solution is that no customer is assigned to a facility
        int[] solution = new int[this.cflp.getNumCustomers()];
        Arrays.fill(solution, -1);

        this.branchAndBound(0, solution);
    }

    /**
     * Calculate lower + upper bound for the current solution.
     *
     * @param int   customer
     * @param int[] solution
     * O(customers*facilities) worst case
     */
    public void branchAndBound(int customer, int[] solution) {
        // calculate upper and lower bounds
        int upper = this.upperBound(solution.clone());
        int lower = this.lowerBound(solution);

        // if we can bound the subtree we do so
        if (this.shouldBound(customer, lower, upper)) { return; }

        for (int i = 0; i < this.gnf; i++) {
            int[] solutionClone = solution.clone();

            // go through the sorted facilities of each customer
            solutionClone[customer] = this.preferences[customer][i];

            // branch to the next customer, recursion means depth first
            this.branchAndBound(customer + 1, solutionClone);
        }
    }

    /**
     * Calculate a lower bound for the given solution. (doesnt have to be valid)
     *
     * @param int[] solution
     * O(n*?) depends on costs() method
     */
    public int lowerBound(int[] solution) {
        // levels and bandwidths of each facility
        int[] levels = new int[this.gnf];
        int[] bandwidths = new int[this.gnf];

        int costs = 0;
        for (int i = 0; i < solution.length; i++) {
            // if solution[i] = -1 then it isnt set yet so dont increase the costs (invalid solution)
            if (solution[i] >= 0) { costs += this.costs(solution, levels, bandwidths, i); }
        }

        return costs;
    }

    /**
     * Calculate a (valid) upper bound for the given solution.
     *
     * @param int[] solution
     * O(n*?) depends on costs() method
     */
    public int upperBound(int[] solution) {
        // levels and bandwidths of each facility
        int[] levels = new int[this.gnf];
        int[] bandwidths = new int[this.gnf];

        int costs = 0;
        for (int i = 0; i < solution.length; i++) {
            // if solution[i] is below zero it does not have a facility, so just assign the closest
            if (solution[i] < 0) solution[i] = this.preferences[i][0];

            costs += this.costs(solution, levels, bandwidths, i);
        }

        this.setSolution(costs, solution);

        return costs;
    }

    /**
     * Sort the preferences for each user via bubble sort.
     *
     * O(customers*facilities^2)
     */
    private void setPreferences() {
        // initialize 2D preferences array
        this.preferences = new int[this.gnc][this.gnf];
        int temp = 0;

        // fill 2D array with indices [[0,1,2], [0,1,2]] for new int[2][3]
        for (int i = 0; i < this.gnc; i++) {
            for (int j = 0; j < this.gnf; j++) {
                this.preferences[i][j] = j;
            }
        }

        // bubble sort, yes could be easier but the input isn't too big
        for (int i = 0; i < this.gnc; i++) {
            for (int j = 0; j < this.gnf; j++) {
                for (int k = 1; k < this.gnf - j; k++) {
                    if (this.cflp.distance(k - 1, i) > this.cflp.distance(k, i)) {
                        temp = this.preferences[i][k-1];
                        this.preferences[i][k-1] = this.preferences[i][k];
                        this.preferences[i][k] = temp;
                    }
                }
            }
        }
    }

    /**
     * Decides whether or not the subtree should be bounded.
     *
     * @param  int customer
     * @param  int lower
     * @param  int upper
     * O(1)
     */
    private boolean shouldBound(int customer, int lower, int upper) {
        // bound if calculated lower > global best or upper is lower or there are no more customers
        return (this.getBestSolution() != null && lower >= this.getBestSolution().getUpperBound())
                || upper == lower || customer > this.cflp.getNumCustomers();
    }

     /**
      * Calculate the cost for the given solution
      *
      * @param  int[] solution
      * @param  int[] levels
      * @param  int[] bandwidths
      * @param  int   c (Customer)
      * O(1) if the level calculation is constant (is it tho?)
      */
    private int costs(int[] solution, int[] levels, int[] bandwidths, int c) {
        // get the required facility level
        int level = levels[solution[c]];
        for (; level * this.cflp.maxBandwidthOf(solution[c]) < this.cflp.bandwidthOf(c) + bandwidths[solution[c]]; level++);

        try {
            // get the creation or upgrade cost for the facility
            int facilityCost = this.cflp.factor(level, this.cflp.baseOpeningCostsOf(solution[c]))
                    - this.cflp.factor(levels[solution[c]], this.cflp.baseOpeningCostsOf(solution[c]));

            // increase the bandwidth of the facility by the bandwidth of the user
            bandwidths[solution[c]] += this.cflp.bandwidthOf(c);

            // set the level auf the facility according to the calculated one
            levels[solution[c]] = level;

            // costs = facilitycost + distance * distancecosts
            return this.cflp.distance(solution[c], c) * this.cflp.distanceCosts + facilityCost;
        } catch (Exception e) {
            // if an integer overflow happens return the highest possible integer
            return Integer.MAX_VALUE;
        }

    }
}
