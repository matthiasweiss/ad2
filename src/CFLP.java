package ad2.ss17.cflp;

public class CFLP extends AbstractCFLP {

    private CFLPInstance cflp;
    int[] solution;

    public CFLP(CFLPInstance instance) {
        this.cflp = instance;
        this.solution = new int[this.cflp.getNumCustomers()];
    }

    @Override
    public void run() {
        this.branchAndBound(0, this.solution.clone());
    }

    public void branchAndBound(int customerId, int[] solution){
        if (customerId < this.cflp.getNumCustomers()){
            for (int i = 0; i < this.cflp.getNumFacilities(); i++) {
                int[] solutionClone = solution.clone();
                solutionClone[customerId] = i;
                this.branchAndBound(customerId + 1, solutionClone);
            }
        }

        this.setSolution(getCosts(solution), solution);
    }

    public int getCosts(int[] solution){
        int costs = 0;
        int[] levels = new int[this.cflp.getNumFacilities()];
        int[] bandwidths = new int[this.cflp.getNumFacilities()];

        for (int i = 0; i < solution.length; i++) {
            int facilityCost;
            int requiredLevel = levels[solution[i]];

            // get the required facility level
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
