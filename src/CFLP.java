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

    /**
     * Creates a new CFLP instance.
     *
     * @param  CFLPInstance instance
     * O(customer*facilities^2) because of the sort in this.setPreferencs()
     */
    public CFLP(CFLPInstance instance) {
        this.cflp = instance;

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
     * @param int   customerId
     * @param int[] solution
     * O(customers*facilities) worst case
     */
    public void branchAndBound(int customerId, int[] solution) {
        int upper = this.upperBound(solution.clone());
        int lower = this.lowerBound(solution);

        if (this.shouldBound(customerId, lower, upper)) { return; }

        if (customerId < this.cflp.getNumCustomers()){
            for (int i = 0; i < this.cflp.getNumFacilities(); i++) {
                int[] solutionClone = solution.clone();
                solutionClone[customerId] = this.preferences[customerId][i];
                this.branchAndBound(customerId + 1, solutionClone);
            }
        }
    }

    /**
     * Calculate a lower bound for the given solution. (doesnt have to be valid)
     *
     * @param int[] solution
     * O(n*?) depends on costs() method
     */
    public int lowerBound(int[] solution) {
        int costs = 0;
        int[] levels = new int[this.cflp.getNumFacilities()];
        int[] bandwidths = new int[this.cflp.getNumFacilities()];

        for (int i = 0; i < solution.length; i++) {
            if (solution[i] >= 0) costs += this.costs(solution, levels, bandwidths, i);
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
        int costs = 0;
        int[] levels = new int[this.cflp.getNumFacilities()];
        int[] bandwidths = new int[this.cflp.getNumFacilities()];

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
        this.preferences = new int[this.cflp.getNumCustomers()][this.cflp.getNumFacilities()];
        int temp = 0;

        for (int i = 0; i < this.cflp.getNumCustomers(); i++) {
            for (int j = 0; j < this.cflp.getNumFacilities(); j++) {
                this.preferences[i][j] = j;
            }
        }

        // bubble sort, yes could be easier but the input isn't too big
        for (int i = 0; i < this.cflp.getNumCustomers(); i++) {
            for (int j = 0; j < this.cflp.getNumFacilities(); j++) {
                for (int k = 1; k < this.cflp.getNumFacilities() - j; k++) {
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
     * @param  int customerId    [description]
     * @param  int lower         [description]
     * @param  int upper         [description]
     * O(1)
     */
    private boolean shouldBound(int customerId, int lower, int upper) {
        return (this.getBestSolution() != null && lower >= this.getBestSolution().getUpperBound())
                || upper == lower || customerId > this.cflp.getNumCustomers();
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

            bandwidths[solution[c]] += this.cflp.bandwidthOf(c);
            levels[solution[c]] = level;

            return this.cflp.distance(solution[c], c) * this.cflp.distanceCosts + facilityCost;
        } catch (Exception e) {
            return 0;
        }

    }
}
