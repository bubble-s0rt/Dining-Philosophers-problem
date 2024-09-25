import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class DiningPhilosophers {
    private static final int NUM_TABLES = 6;
    private static final int NUM_PHILOSOPHERS = 5; 
    private final ReentrantLock[][] forks = new ReentrantLock[NUM_TABLES][NUM_PHILOSOPHERS]; 
    private final boolean[][] seats = new boolean[NUM_TABLES][NUM_PHILOSOPHERS]; 
    

    public DiningPhilosophers() {
        for (int table = 0; table < NUM_TABLES; table++) {
            for (int seat = 0; seat < NUM_PHILOSOPHERS; seat++) {
                forks[table][seat] = new ReentrantLock();
                seats[table][seat] = false;
            }
        }
    }


    public void startDining() {
        Thread[] philosopherThreads = new Thread[NUM_TABLES * NUM_PHILOSOPHERS];

        // philosopher threads for tables
        for (int table = 0; table < NUM_TABLES - 1; table++) {
            for (int seat = 0; seat < NUM_PHILOSOPHERS; seat++) {
                char philosopherLabel = (char) ('A' + (table * NUM_PHILOSOPHERS) + seat);
                philosopherThreads[(table * NUM_PHILOSOPHERS) + seat] = new Thread(new Philosopher(table, seat, philosopherLabel));
                philosopherThreads[(table * NUM_PHILOSOPHERS) + seat].start();
                seats[table][seat] = true; 
            }
        

        System.out.println("Simulation donee");
        System.out.println("The last philosopher who moved to the sixth table was: " + lastMovedPhilosopher);
    }

   