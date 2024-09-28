import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Comparator;
import java.util.PriorityQueue;

public class DiningPhilosophers {
    private static final int NUM_PHILOSOPHERS = 5;
    private static final int NUM_TABLES = 6;  // Sixth table for deadlock handling
    private static final Random random = new Random();
    private static final int MAX_ATTEMPTS = 3;  // Max failed attempts before moving to the sixth table

    //smaphor for each philosopher per table
    private static final Semaphore[][] forks = new Semaphore[NUM_TABLES][NUM_PHILOSOPHERS];

    private static final int[] philosophersAtTable = new int[NUM_TABLES];

    // Deadlock detection flags
    private static volatile boolean sixthTableDeadlocked = false;
    private static volatile String lastPhilosopherToMove = "";

    private static final int[] mealsCount = new int[NUM_PHILOSOPHERS * (NUM_TABLES - 1)];

    // Priority queue for fairness (least-fed philosopher first)
    private static final PriorityQueue<Philosopher> eatingQueue = new PriorityQueue<>(Comparator.comparingInt(Philosopher::getMealsEaten));

    public static void main(String[] args) throws InterruptedException {
        for (int table = 0; table < NUM_TABLES; table++) {
            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                forks[table][i] = new Semaphore(1, true);  
            }
        }

        for (int i = 0; i < NUM_TABLES - 1; i++) {
            philosophersAtTable[i] = NUM_PHILOSOPHERS;
        }

        // Thread pool philosopher
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PHILOSOPHERS * (NUM_TABLES - 1));

       
        char philosopherLabel = 'A'; 
        for (int table = 0; table < NUM_TABLES - 1; table++) {
            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                Philosopher philosopher = new Philosopher(philosopherLabel++, table, i);
                eatingQueue.add(philosopher);
                executorService.submit(philosopher);
            }
        }

       
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS); 

        System.out.println("The sixth table deadlocked. The last philosopher to move was: " + lastPhilosopherToMove);

        for (int i = 0; i < NUM_PHILOSOPHERS * (NUM_TABLES - 1); i++) {
            System.out.println("Philosopher " + (char) ('A' + i) + " ate " + mealsCount[i] + " times.");
        }
    }
    static class Philosopher implements Runnable {
        private final char label;
        private final int position;
        private int currentTable;
        private int failedAttempts = 0; 

        public Philosopher(char label, int currentTable, int position) {
            this.label = label;
            this.currentTable = currentTable;
            this.position = position;
        }

        public int getMealsEaten() {
            return mealsCount[label - 'A'];
        }

        @Override
        public void run() {
            try {
                while (!sixthTableDeadlocked) {
                    think();
                    synchronized (eatingQueue) {
                        Philosopher leastFedPhilosopher = eatingQueue.peek();
                        if (leastFedPhilosopher != this) {
                            eatingQueue.wait();  
                        }
                    }
                    if (!eat()) {
                        failedAttempts++;
                        if (failedAttempts >= MAX_ATTEMPTS) {
                            moveToSixthTable();
                            // sixth table deadlock checkk
                            if (philosophersAtTable[5] == NUM_PHILOSOPHERS) {
                                sixthTableDeadlocked = true;
                                lastPhilosopherToMove = String.valueOf(label);  // Track last philosopher
                            }
                        }
                    } else {
                        failedAttempts = 0;// Reset failed attempts on successful eating
                    }

                    // Notify the next philosopher in line
                    synchronized (eatingQueue) {
                        eatingQueue.remove(this); 
                        eatingQueue.notifyAll(); 
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void think() throws InterruptedException {
            System.out.println("Philosopher " + label + " is thinking at Table " + currentTable + "...");
            Thread.sleep(random.nextInt(3000));  
        }


        private boolean eat() throws InterruptedException {
            int leftFork = position;
            int rightFork = (position + 1) % NUM_PHILOSOPHERS;

            if (!forks[currentTable][leftFork].tryAcquire(500, TimeUnit.MILLISECONDS)) {
                return false; 
            }
            if (!forks[currentTable][rightFork].tryAcquire(500, TimeUnit.MILLISECONDS)) {
                forks[currentTable][leftFork].release();
                return false;
            }

            System.out.println("Philosopher " + label + " picked up both forks at Table " + currentTable + ".");

            eatMeal();

            mealsCount[label - 'A']++;

            forks[currentTable][leftFork].release();
            forks[currentTable][rightFork].release();
            System.out.println("Philosopher " + label + " finished eating at Table " + currentTable + " and put down forks.");
            return true;
        }
        private void eatMeal() throws InterruptedException {
            System.out.println("Philosopher " + label + " is eating at Table " + currentTable + ".");
            Thread.sleep(random.nextInt(2000)); 
        }

        // Move to the sixth table if multiple eating attempts fail
        private void moveToSixthTable() throws InterruptedException {
            synchronized (philosophersAtTable) {
                if (philosophersAtTable[5] < NUM_PHILOSOPHERS) { 
                    System.out.println("Philosopher " + label + " is moving to the sixth table.");
                    philosophersAtTable[currentTable]--;
                    currentTable = 5;  // Move to the sixth table
                    philosophersAtTable[5]++;
                }
            }
        }
    }
}
