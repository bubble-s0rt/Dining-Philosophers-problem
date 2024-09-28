import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DiningPhilosophers {
    private static final int NUM_TABLES = 6;       
    private static final int NUM_PHILOSOPHERS = 5; 
    private static final int TOTAL_PHILOSOPHERS = NUM_PHILOSOPHERS * (NUM_TABLES - 1); 
    private static final Random random = new Random();
    
    //smaphor for each philosopher per table
    private static final Semaphore[][] forks = new Semaphore[NUM_TABLES][NUM_PHILOSOPHERS];
    
    private static int[] philosophersAtTable = new int[NUM_TABLES];
    
    // Deadlock detection flags
    private static volatile boolean sixthTableDeadlocked = false;
    private static volatile String lastPhilosopherToMove = "";

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < NUM_TABLES; i++) {
            for (int j = 0; j < NUM_PHILOSOPHERS; j++) {
                forks[i][j] = new Semaphore(1);  
            }
        }
        
        // Initialize the philosopher counts for the tables
        for (int i = 0; i < NUM_TABLES - 1; i++) {
            philosophersAtTable[i] = NUM_PHILOSOPHERS;// Each table starts with 5 philosophers
        }

        // Thread for managing philosopher
        ExecutorService executorService = Executors.newFixedThreadPool(TOTAL_PHILOSOPHERS);
        
        
        char philosopherLabel = 'A';  
        for (int table = 0; table < NUM_TABLES - 1; table++) {
            for (int philosopher = 0; philosopher < NUM_PHILOSOPHERS; philosopher++) {
                executorService.submit(new Phil(philosopherLabel++, table, philosopher));
            }
        }

       
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS); 

        System.out.println("The sixth table deadlocked. The last philosopher to move to the sixth table was: " + lastPhilosopherToMove);
    }

    
    static class Phil implements Runnable {
        private final char label;
        private int currentTable;  
        private final int position; 

        public Phil(char label, int currentTable, int position) {
            this.label = label;
            this.currentTable = currentTable;
            this.position = position;
        }

        @Override
        public void run() {
            try {
                while (!sixthTableDeadlocked) {
                    think();

                    if (eat()) {
                       
                        continue;
                    }

                    moveToSixthTable();
                    
                    if (philosophersAtTable[5] == NUM_PHILOSOPHERS) {
                        sixthTableDeadlocked = true;
                        lastPhilosopherToMove = String.valueOf(label);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

       
        private void think() throws InterruptedException {
            System.out.println("Phil " + label + " is thinking at Table " + currentTable + "...");
            Thread.sleep(random.nextInt(10000));  
        }

        
        private boolean eat() throws InterruptedException {
            int lfork = position;
            int rFork = (position + 1) % NUM_PHILOSOPHERS;

          
            forks[currentTable][lfork].acquire();
            System.out.println("Phil " + label + " picked up left fork " + lfork + " at Table " + currentTable + ".");
            
            
            Thread.sleep(4000);
            boolean hasRightFork = forks[currentTable][rFork].tryAcquire(4, TimeUnit.SECONDS);

            if (!hasRightFork) {
                
                forks[currentTable][lfork].release();
                return false;
            }

          
            System.out.println("Phil " + label + " picked up right fork " + rFork + " at Table " + currentTable + ".");
            eatMeal();
            return true;
        }

       
        private void eatMeal() throws InterruptedException {
            System.out.println("Phil " + label + " is eating at Table " + currentTable + ".");
            Thread.sleep(random.nextInt(5000)); 

          
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
