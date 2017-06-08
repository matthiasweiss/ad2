package ad2.ss17.cflp;

import java.util.*;

public class CFLP extends AbstractCFLP {

    private CFLPInstance cflp;
    private int[] solution;

    public CFLP(CFLPInstance instance) {
        this.cflp = instance;
        this.solution = new int[this.cflp.getNumCustomers()];
        // Arrays.fill(this.solution, -1);
    }

    @Override
    public void run() {
        this.branchAndBound(0, this.solution);
    }

    public void branchAndBound(int customerId, int[] solution) {
        int upper = this.upperBound(solution);
        int lower = this.lowerBound(solution);

        if (customerId < this.cflp.getNumCustomers()){
            for (int i = 0; i < this.cflp.getNumFacilities(); i++) {
                int[] solutionClone = solution.clone();
                solutionClone[customerId] = i;
                this.branchAndBound(customerId + 1, solutionClone);
            }
        }

        this.setSolution(this.upperBound(solution), solution);
    }

    public int lowerBound(int[] solution) {
        int costs = 0;
        int[] levels = new int[this.cflp.getNumFacilities()];
        int[] bandwidths = new int[this.cflp.getNumFacilities()];

        for (int i = 0; i < solution.length; i++) {
            int facilityCost;
            int requiredLevel = levels[solution[i]];

            // get the required facility level
            // O(????)
            while (requiredLevel * this.cflp.maxBandwidthOf(solution[i]) < this.cflp.bandwidthOf(i) + bandwidths[solution[i]]){
                requiredLevel++;
            }

            // get the creation or upgrade cost for the facility
            facilityCost = this.cflp.factor(requiredLevel, this.cflp.baseOpeningCostsOf(solution[i]))
                    - this.cflp.factor(levels[solution[i]], this.cflp.baseOpeningCostsOf(solution[i]));

            bandwidths[solution[i]] += this.cflp.bandwidthOf(i);

            levels[solution[i]] = requiredLevel;

            costs += this.cflp.distance(solution[i], i) * this.cflp.distanceCosts + facilityCost;
        }

        return costs;
    }

    public int upperBound(int[] solution) {
        int costs = 0;
        int[] levels = new int[this.cflp.getNumFacilities()];
        int[] bandwidths = new int[this.cflp.getNumFacilities()];

        for (int i = 0; i < solution.length; i++) {
            int facilityCost;
            int requiredLevel = levels[solution[i]];

            // get the required facility level
            // O(????)
            while (requiredLevel * this.cflp.maxBandwidthOf(solution[i]) < this.cflp.bandwidthOf(i) + bandwidths[solution[i]]){
                requiredLevel++;
            }

            // get the creation or upgrade cost for the facility
            facilityCost = this.cflp.factor(requiredLevel, this.cflp.baseOpeningCostsOf(solution[i]))
                    - this.cflp.factor(levels[solution[i]], this.cflp.baseOpeningCostsOf(solution[i]));

            bandwidths[solution[i]] += this.cflp.bandwidthOf(i);

            levels[solution[i]] = requiredLevel;

            costs += this.cflp.distance(solution[i], i) * this.cflp.distanceCosts + facilityCost;
        }

        return costs;
    }
}
