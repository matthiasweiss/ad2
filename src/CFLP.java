package ad2.ss17.cflp;

import java.util.*;

public class CFLP extends AbstractCFLP {

    private CFLPInstance cflp;
    private int[] solution;

    public CFLP(CFLPInstance instance) {
        this.cflp = instance;
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
                solutionClone[customerId] = i;
                this.branchAndBound(customerId + 1, solutionClone);
            }
        }
    }

    /**
     * Decides whether or not to continue branching.
     *
     * O(1)
     */
    public boolean shouldContinue(int customerId, int lower, int upper) {
        return (this.getBestSolution() == null || this.getBestSolution().getUpperBound() > lower)
                && upper != lower && customerId < this.cflp.getNumCustomers();
    }

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
     * O(n?)
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

    private int costs(int[] solution, int[] levels, int[] bandwidths, int index) {
        int facilityCost;
        int requiredLevel = levels[solution[index]];

        // get the required facility level
        while (requiredLevel * this.cflp.maxBandwidthOf(solution[index]) < this.cflp.bandwidthOf(index) + bandwidths[solution[index]]){
            requiredLevel++;
        }

        // get the creation or upgrade cost for the facility
        facilityCost = this.cflp.factor(requiredLevel, this.cflp.baseOpeningCostsOf(solution[index]))
                - this.cflp.factor(levels[solution[index]], this.cflp.baseOpeningCostsOf(solution[index]));

        bandwidths[solution[index]] += this.cflp.bandwidthOf(index);

        levels[solution[index]] = requiredLevel;

        return this.cflp.distance(solution[index], index) * this.cflp.distanceCosts + facilityCost;
    }
}
