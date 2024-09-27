import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DiningPhilosophers{
    private static final int NUM_PHILOSOPHERS = 5;
    private static final int NUM_TABLES = 6; 
    private static final Random random = new Random();
    private static final int MAX_ATTEMPTS = 3;  

    // Forks for each philosopher 
    private static final Semaphore[][] forks = new Semaphore[NUM_TABLES][NUM_PHILOSOPHERS];

    private static final int[] philosophersAtTable = new int[NUM_TABLES];

    // Deadlock detection flags
    private static volatile boolean sixthTableDeadlocked = false;
    private static volatile String lastPhilosopherToMove = "";

    public static void main(String[] args) throws InterruptedException {
        for (int table = 0; table < NUM_TABLES; table++) {
            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                forks[table][i] = new Semaphore(1);  // Each fork is held by 1 philosopher at a time
            }
        }

        for (int i = 0; i < NUM_TABLES - 1; i++) {
            philosophersAtTable[i] = NUM_PHILOSOPHERS;  //5 philosophers
        }

        // Thread pool for philosopher 
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PHILOSOPHERS * (NUM_TABLES - 1));

        char philosopherLabel = 'A';  // Labeling philosophers A to Y
        for (int table = 0; table < NUM_TABLES - 1; table++) {
            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                executorService.submit(new Philosopher(philosopherLabel++, table, i));
            }
        }

        // Await simulation termination
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);  // Wait for the simulation to finish

        System.out.println("The sixth table deadlocked. The last philosopher to move was: " + lastPhilosopherToMove);
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

        @Override
        public void run() {
            try {
                while (!sixthTableDeadlocked) {
                    think();
                
                    if (!eat()) {
                        failedAttempts++;
                        System.out.println("Philosopher " + label + " at Table " + currentTable + " failed attempt " + failedAttempts);
                        if (failedAttempts >= MAX_ATTEMPTS) {
                            moveToSixthTable();
                            
                            if (philosophersAtTable[5] == NUM_PHILOSOPHERS) {
                                sixthTableDeadlocked = true;
                                lastPhilosopherToMove = String.valueOf(label);  
                            }
                        }
                    } else {
                        failedAttempts = 0;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

       
        private void think() throws InterruptedException {
            System.out.println("Philosopher " + label + " is thinking at Table " + currentTable + "...");
            Thread.sleep(random.nextInt(5000)); 
        }

    
        private boolean eat() throws InterruptedException {
            int leftFork = position;
            int rightFork = (position + 1) % NUM_PHILOSOPHERS;

            
            if (!forks[currentTable][leftFork].tryAcquire(2, TimeUnit.SECONDS)) {
                return false; 
            }
            System.out.println("Philosopher " + label + " picked up left fork at Table " + currentTable + ".");

            
            if (!forks[currentTable][rightFork].tryAcquire(2, TimeUnit.SECONDS)) {
                forks[currentTable][leftFork].release();  
                return false; 
            }
            System.out.println("Philosopher " + label + " picked up right fork at Table " + currentTable + ".");

           
            eatMeal();

            
            forks[currentTable][leftFork].release();
            forks[currentTable][rightFork].release();
            System.out.println("Philosopher " + label + " finished eating at Table " + currentTable + " and put down forks.");
            return true;
        }

        
        private void eatMeal() throws InterruptedException {
            System.out.println("Philosopher " + label + " is eating at Table " + currentTable + ".");
            Thread.sleep(random.nextInt(3000));  

        
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