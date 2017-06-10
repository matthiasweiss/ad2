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
     * Shortest distance for every customer to some facility
     */
    private int[] shortestDistances;

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

        this.storeShortestDistances();

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
        int[][] facilityCosts = new int[this.gnf][3];

        int costs = 0;
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] < 0) {
                // if solution[i] < 0 then the customer has no facility yet, so we just add costs of connecting the closest facility
                costs += this.shortestDistances[i] * this.cflp.distanceCosts;
            } else {
                // if the customer has a facility assigned we calculate the costs and add them to the total
                costs += this.facilityCost(solution, levels, bandwidths, facilityCosts, i);
            }
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
        int[][] facilityCosts = new int[this.gnf][3];

        int costs = 0;
        for (int i = 0; i < solution.length; i++) {
            // if solution[i] is below zero it does not have a facility, so just assign the closest
            if (solution[i] < 0) solution[i] = this.preferences[i][0];

            // calculate the cost if the customer is connected to the given facility
            costs += this.facilityCost(solution, levels, bandwidths, facilityCosts, i);
        }

        // if costs < 0 then there was an integer overflow so dont set the solution
        if (costs > 0) {
            this.setSolution(costs, solution);
        }

        return costs;
    }

    /**
     * Stores the distance to the nearest facility for every customer.
     *
     * O(customer*facility)
     */
    private void storeShortestDistances() {
        this.shortestDistances = new int[this.gnc];
        for (int i = 0; i < this.cflp.distances.length; i++) {
            for (int j = 0; j < this.cflp.distances[i].length; j++) {
                if (this.shortestDistances[j] == 0 || this.shortestDistances[j] > this.cflp.distances[i][j]) {
                    this.shortestDistances[j] = this.cflp.distances[i][j];
                }
            }
        }
    }

    /**
     * Sort the preferences for each user via bubble sort.
     *
     * O(customers*facilities^2)
     */
    private void setPreferences() {
        // initialize 2D preferences array
        this.preferences = new int[this.gnc][this.gnf];

        // fill 2D array with indices [[0,1,2], [0,1,2]] for new int[2][3]
        for (int i = 0; i < this.gnc; i++) {
            for (int j = 0; j < this.gnf; j++) {
                this.preferences[i][j] = j;
            }
        }

        // bubble sort, yes could be easier but the input isn't too big
        for (int i = 0, temp; i < this.gnc; i++) {
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
     * Get the cost if the customer ist connected to the given facility.
     * @param  int[]   solution
     * @param  int[]   levels
     * @param  int[]   bandwidths
     * @param  int[][] facilityCosts
     * @param  int     customer
     */
    private int facilityCost(int[] solution, int[] levels, int[] bandwidths, int[][] facilityCosts, int customer) {
        int facility = solution[customer];
        bandwidths[facility] += this.cflp.bandwidthOf(customer);
        int currentCost = facilityCosts[facility][0];

        for(; levels[facility] * this.cflp.maxBandwidthOf(facility) < bandwidths[facility]; levels[facility]++);

        for (int i = facilityCosts[facility][2]+1; i <= levels[facility]; i++) {
            if (i == 1) {
                facilityCosts[facility][0] = this.cflp.baseOpeningCostsOf(facility);
            } else if (i == 2) {
                facilityCosts[facility][1] = facilityCosts[facility][0];
                facilityCosts[facility][0] = (int) Math.ceil(this.cflp.baseOpeningCostsOf(facility) * 1.5);
            } else {
                int newCost = facilityCosts[facility][0] + facilityCosts[facility][1] + (4 - i) * this.cflp.baseOpeningCostsOf(facility);
                facilityCosts[facility][1] = facilityCosts[facility][0];
                facilityCosts[facility][0] = newCost;
            }
            facilityCosts[facility][2]++;
        }

        int expansionCost = facilityCosts[facility][0] - currentCost;
        return this.cflp.distance(facility, customer) * this.cflp.distanceCosts + expansionCost;
    }
}
