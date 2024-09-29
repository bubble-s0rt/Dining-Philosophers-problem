import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DiningPhilosophers {
    private static final int NUM_TABLES = 6;        // 5 regular tables + 1 sixth table
    private static final int NUM_PHILOSOPHERS = 5;  // Philosophers per table
    private static final int TOTAL_PHILOSOPHERS = NUM_PHILOSOPHERS * (NUM_TABLES - 1);  // 25 philosophers initially
    private static final Random random = new Random();
    
    //smaphor for each philosopher per table
    private static final Semaphore[][] forks = new Semaphore[NUM_TABLES][NUM_PHILOSOPHERS];
    
    // Tracks philosophers at each table (initially 25 philosophers across 5 tables)
    private static int[] philosophersAtTable = new int[NUM_TABLES];
    
    // Deadlock detection flags
    private static volatile boolean sixthTableDeadlocked = false;
    private static volatile String lastPhilosopherToMove = "";

    public static void main(String[] args) throws InterruptedException {
        // Initialize forks as semaphores (one per philosopher at each table)
        for (int i = 0; i < NUM_TABLES; i++) {
            for (int j = 0; j < NUM_PHILOSOPHERS; j++) {
                forks[i][j] = new Semaphore(1);  // Each fork can be held by 1 philosopher at a time
            }
        }
        
        // Initialize philosopher counts for the 5 tables
        for (int i = 0; i < NUM_TABLES - 1; i++) {
            philosophersAtTable[i] = NUM_PHILOSOPHERS;  // Each of the first 5 tables has 5 philosophers
        }

        // Thread pool philosopher
        ExecutorService executorService = Executors.newFixedThreadPool(TOTAL_PHILOSOPHERS);
        
       // Thread for managing philosopher
        char philosopherLabel = 'A';  // Philosophers are labelled 'A' to 'Y' (25 philosophers total)
        for (int table = 0; table < NUM_TABLES - 1; table++) {
            for (int philosopher = 0; philosopher < NUM_PHILOSOPHERS; philosopher++) {
                executorService.submit(new Phil(philosopherLabel++, table, philosopher));
            }
        }

        // Await the simulation to finish (i.e., sixth table enters deadlock)
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);  // Wait for simulation to finish

        System.out.println("The sixth table deadlocked. The last philosopher to move to the sixth table was: " + lastPhilosopherToMove);
    }

    // Phil class simulating behavior
    static class Phil implements Runnable {
        private final char label;
        private int currentTable;  // Current table philosopher is seated at
        private final int position; // Position of philosopher at the table (used to acquire forks)

        public Phil(char label, int currentTable, int position) {
            this.label = label;
            this.currentTable = currentTable;
            this.position = position;
        }

        @Override
        public void run() {
            try {
                while (!sixthTableDeadlocked) {
                    // Philosopher thinks for a random time between 0 to 10 seconds
                    think();

                    // Philosopher gets hungry and tries to eat
                    if (eat()) {
                        // After eating, continue thinking and eating at the same table
                        continue;
                    }

                    // Deadlock occurred, move philosopher to the sixth table if possible
                    moveToSixthTable();
                    
                    // If all philosophers are at the sixth table, declare deadlock
                    if (philosophersAtTable[5] == NUM_PHILOSOPHERS) {
                        sixthTableDeadlocked = true;
                        lastPhilosopherToMove = String.valueOf(label);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Philosopher thinking phase
        private void think() throws InterruptedException {
            System.out.println("Phil " + label + " is thinking at Table " + currentTable + "...");
            Thread.sleep(random.nextInt(10000));  // Think for 0-10 seconds
        }

        // Philosopher tries to pick up forks and eat
        private boolean eat() throws InterruptedException {
            int lfork = position;
            int rFork = (position + 1) % NUM_PHILOSOPHERS;

            // Try to pick up left fork
            forks[currentTable][lfork].acquire();
            System.out.println("Phil " + label + " picked up left fork " + lfork + " at Table " + currentTable + ".");
            
            // Wait 4 seconds, then try to pick up right fork
            Thread.sleep(4000);
            boolean hasRightFork = forks[currentTable][rFork].tryAcquire(4, TimeUnit.SECONDS);

            if (!hasRightFork) {
                // Failed to acquire right fork, release left fork and return false
                forks[currentTable][lfork].release();
                return false;
            }

            // If successful in acquiring both forks, eat
            System.out.println("Phil " + label + " picked up right fork " + rFork + " at Table " + currentTable + ".");
            eatMeal();
            return true;
        }

        // Philosopher eats for a random time between 0 to 5 seconds
        private void eatMeal() throws InterruptedException {
            System.out.println("Phil " + label + " is eating at Table " + currentTable + ".");
            Thread.sleep(random.nextInt(5000));  // Eat for 0-5 seconds

            // Put down both forks
            int lfork = position;
            int rFork = (position + 1) % NUM_PHILOSOPHERS;
            forks[currentTable][lfork].release();
            forks[currentTable][rFork].release();
            System.out.println("Phil " + label + " finished eating at Table " + currentTable + " and put down both forks.");
        }

        // Philosopher moves to the sixth table after deadlock occurs
        private void moveToSixthTable() throws InterruptedException {
            synchronized (philosophersAtTable) {
                if (philosophersAtTable[5] < NUM_PHILOSOPHERS) {
                    // Move philosopher to the sixth table
                    System.out.println("Phil " + label + " is moving to the sixth table.");
                    philosophersAtTable[currentTable]--;
                    currentTable = 5;  // Move to the sixth table
                    philosophersAtTable[5]++;
                }
            }
        }
    }
}
