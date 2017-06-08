package ad2.ss17.cflp;

import java.util.*;

public class CFLP extends AbstractCFLP {

    private CFLPInstance cflp;
    private int[] solution;
    private int[][] preferences;

    public CFLP(CFLPInstance instance) {
        this.cflp = instance;

        this.preferences = new int[this.cflp.getNumCustomers()][this.cflp.getNumFacilities()];
        this.sortPreferences(this.preferences);

        this.solution = new int[this.cflp.getNumCustomers()];
        Arrays.fill(this.solution, -1);
    }

    @Override
    public void run() {
        this.branchAndBound(0, this.solution);
    }

    public void branchAndBound(int customerId, int[] solution) {
        int upper = this.upperBound(solution.clone());
        int lower = this.lowerBound(solution);

        if (! this.shouldContinue(customerId, lower, upper)) { return; }

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
     * O(n*?) depends on costs() method
     */
    public int lowerBound(int[] solution) {
        int costs = 0;
        int[] levels = new int[this.cflp.getNumFacilities()];
        int[] bandwidths = new int[this.cflp.getNumFacilities()];

        for (int i = 0; i < solution.length; i++) {
            if (solution[i] >= 0) {
                costs += costs(solution, levels, bandwidths, i);
            }
        }

        return costs;
    }

    /**
     * Calculate a (valid) upper bound for the given solution.
     *
     * O(n*?) depends on costs() method
     */
    public int upperBound(int[] solution) {
        int costs = 0;
        int[] levels = new int[this.cflp.getNumFacilities()];
        int[] bandwidths = new int[this.cflp.getNumFacilities()];

        for (int i = 0; i < solution.length; i++) {
            // if solution[i] is below zero it does not have a facility, so just assign the first one to it.
            if (solution[i] < 0) solution[i] = 0;

            costs += costs(solution, levels, bandwidths, i);
        }

        this.setSolution(costs, solution);

        return costs;
    }

    /**
     * Sort the preferences for each user.
     *
     * O(customers*facilities^2)
     */
    private void sortPreferences(int[][] preferences) {
        int temp = 0;

        for (int i = 0; i < this.cflp.getNumCustomers(); i++) {
            for (int j = 0; j < this.cflp.getNumFacilities(); j++) {
                preferences[i][j] = j;
            }
        }

        for (int i = 0; i < this.cflp.getNumCustomers(); i++) {
            for (int j = 0; j < this.cflp.getNumFacilities(); j++) {
                for (int k = 1; k < this.cflp.getNumFacilities() - j; k++) {
                    if (this.cflp.distance(k - 1, i) > this.cflp.distance(k, i)) {
                        temp = preferences[i][k-1];
                        preferences[i][k-1] = preferences[i][k];
                        preferences[i][k] = temp;   
                    }
                }
            }
        }
    }

    /**
     * Decides whether or not to continue branching.
     *
     * O(1)
     */
    private boolean shouldContinue(int customerId, int lower, int upper) {
        return (this.getBestSolution() == null || this.getBestSolution().getUpperBound() > lower)
                && upper != lower && customerId < this.cflp.getNumCustomers();
    }

    /**
     * Calculate the cost for the given solution
     *
     * O(1) or O(log(n)) if required level is some sort of log.
     */
    private int costs(int[] solution, int[] levels, int[] bandwidths, int index) {
        int requiredLevel = levels[solution[index]];

        // get the required facility level
        while (requiredLevel * this.cflp.maxBandwidthOf(solution[index]) < this.cflp.bandwidthOf(index) + bandwidths[solution[index]]){
            requiredLevel++;
        }

        try {
            // get the creation or upgrade cost for the facility
            int facilityCost = this.cflp.factor(requiredLevel, this.cflp.baseOpeningCostsOf(solution[index]))
                    - this.cflp.factor(levels[solution[index]], this.cflp.baseOpeningCostsOf(solution[index]));

            bandwidths[solution[index]] += this.cflp.bandwidthOf(index);
            levels[solution[index]] = requiredLevel;

            return this.cflp.distance(solution[index], index) * this.cflp.distanceCosts + facilityCost;
        } catch (Exception e) {
            return 0;
        }

    }
}
