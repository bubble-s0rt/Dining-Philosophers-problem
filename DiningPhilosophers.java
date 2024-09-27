import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DiningPhilosophersStep3 {
    private static final int NUM_PHILOSOPHERS = 5;
    private static final int NUM_TABLES = 6;
    private static final Random random = new Random();
    private static final int MAX_ATTEMPTS = 3;

    private static final Semaphore[][] forks = new Semaphore[NUM_TABLES][NUM_PHILOSOPHERS]; // Forkssfor each philosopher per table

    //for umber of philosophers at each table
    private static final int[] philosophersAtTable = new int[NUM_TABLES];

    public static void main(String[] args) {
        for (int table = 0; table < NUM_TABLES; table++) {
            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                forks[table][i] = new Semaphore(1);
            }
        }

        for (int i = 0; i < NUM_TABLES - 1; i++) {
            philosophersAtTable[i] = NUM_PHILOSOPHERS;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PHILOSOPHERS * (NUM_TABLES - 1));
        
        for (int table = 0; table < NUM_TABLES - 1; table++) {
            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                executorService.submit(new Philosopher(table, i)); //philosohearr threads
            }
        }

        executorService.shutdown();
    }

    static class Philosopher implements Runnable {
        private final int position;
        private int currentTable;
        private int failedAttempts = 0;

        public Philosopher(int currentTable, int position) {
            this.position = position;
            this.currentTable = currentTable;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    think();
                    if (!eat()) {
                        failedAttempts++;
                        System.out.println("Philosopher " + position + " at Table " + currentTable + " failed attempt " + failedAttempts);
                        if (failedAttempts >= MAX_ATTEMPTS) {
                            moveToSixthTable();
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
            System.out.println("Philosopher " + position + " is thinking at Table " + currentTable + "...");
            Thread.sleep(random.nextInt(5000));
        }

        private boolean eat() throws InterruptedException {
            int leftFork = position;
            int rightFork = (position + 1) % NUM_PHILOSOPHERS;

            if (!forks[currentTable][leftFork].tryAcquire(2, TimeUnit.SECONDS)) {
                return false;
            }
            System.out.println("Philosopher " + position + " picked up left fork " + leftFork + " at Table " + currentTable + ".");
            
            if (!forks[currentTable][rightFork].tryAcquire(2, TimeUnit.SECONDS)) {
                forks[currentTable][leftFork].release();
                return false;
            }
            System.out.println("Philosopher " + position + " picked up right fork " + rightFork + " at Table " + currentTable + ".");

            eatMeal();
            
            forks[currentTable][leftFork].release();
            forks[currentTable][rightFork].release();
            System.out.println("Philosopher " + position + " put down forks at Table " + currentTable + ".");
            
            return true;
        }
         // eating for a random time
        private void eatMeal() throws InterruptedException {
            System.out.println("Philosopher " + position + " is eating at Table " + currentTable + ".");
            Thread.sleep(random.nextInt(3000));
        }
        // Philosopher moves to the sixth table
        private void moveToSixthTable() throws InterruptedException {
            synchronized (philosophersAtTable) {
                if (philosophersAtTable[5] < NUM_PHILOSOPHERS) {
                    System.out.println("Philosopher " + position + " is moving to the sixth table.");
                    philosophersAtTable[currentTable]--;
                    currentTable = 5;
                    philosophersAtTable[5]++;
                }
            }
        }
    }
}
