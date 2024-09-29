import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DiningPhilosophers {
    private static final int NUM_PHILOSOPHERS = 25;  // Number of philosophers
    private static final int NUM_FORKS = 5;  // Only 5 forks per table
    private static final Random random = new Random();

    // Forks (5 forks shared by 5 philosophers at each table)
    private static final Object[] forks = new Object[NUM_FORKS];

    // Philosopher names (A to Y for 25 philosophers)
    private static final char[] philosopherNames = new char[NUM_PHILOSOPHERS];

    // Philosopher states to track thinking, eating, and waiting
    private enum State {THINKING, HUNGRY, EATING}
    private static final State[] philosopherStates = new State[NUM_PHILOSOPHERS];

    // Deadlock detection flag
    private static volatile boolean deadlockDetected = false;

    // Last philosopher moved to the sixth table
    private static volatile char sixthTablePhilosopher = '\0';

    public static void main(String[] args) throws InterruptedException {
        // Initialize forks and philosopher states
        for (int i = 0; i < NUM_FORKS; i++) {
            forks[i] = new Object();  // Use the object as a lock (fork)
        }

        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            philosopherNames[i] = (char) ('A' + i);  // Names A to Y
            philosopherStates[i] = State.THINKING;  // Initial state is THINKING
        }

        // Thread pool for philosophers
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PHILOSOPHERS);

        // Submit philosopher tasks
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            executorService.submit(new Philosopher(philosopherNames[i], i));
        }

        // Start the deadlock detection thread
        new Thread(new DeadlockDetector()).start();

        // Wait for the simulation to finish
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
    }

    // Philosopher class implementing Runnable
    static class Philosopher implements Runnable {
        private final char name;  // Philosopher name (A to Y)
        private final int id;     // Philosopher ID (0 to 24)

        public Philosopher(char name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                while (!deadlockDetected) {
                    think();
                    pickUpForks();
                    eat();
                    putDownForks();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Philosopher thinks for a random time (0-10 seconds)
        private void think() throws InterruptedException {
            synchronized (philosopherStates) {
                philosopherStates[id] = State.THINKING;
            }
            System.out.println("Philosopher " + name + " is thinking...");
            Thread.sleep(random.nextInt(10000));
        }

        // Philosopher tries to pick up both forks (left and right) at their respective table
        private void pickUpForks() throws InterruptedException {
            int tableId = id % NUM_FORKS;  // Each philosopher tries to access one of the 5 forks at the table

            // Set the state to HUNGRY (waiting for forks)
            synchronized (philosopherStates) {
                philosopherStates[id] = State.HUNGRY;
            }

            // Try to pick up left and right forks in the right order
            synchronized (forks[tableId]) {
                synchronized (forks[(tableId + 1) % NUM_FORKS]) {
                    // Successfully acquired both forks, update state to EATING
                    synchronized (philosopherStates) {
                        philosopherStates[id] = State.EATING;
                    }
                }
            }
        }

        // Philosopher eats for a random time (0-5 seconds)
        private void eat() throws InterruptedException {
            System.out.println("Philosopher " + name + " is eating...");
            Thread.sleep(random.nextInt(5000));
        }

        // Philosopher puts down both forks
        private void putDownForks() {
            int tableId = id % NUM_FORKS;  // Each philosopher puts down forks on their assigned table

            // Release both forks
            synchronized (forks[tableId]) {
                synchronized (forks[(tableId + 1) % NUM_FORKS]) {
                    // After putting down forks, return to THINKING state
                    synchronized (philosopherStates) {
                        philosopherStates[id] = State.THINKING;
                    }
                    System.out.println("Philosopher " + name + " put down forks.");
                }
            }
        }
    }

    // DeadlockDetector class to periodically check for deadlock
    static class DeadlockDetector implements Runnable {
        private static final int CHECK_INTERVAL = 5000;  // Check every 5 seconds

        @Override
        public void run() {
            while (!deadlockDetected) {
                try {
                    Thread.sleep(CHECK_INTERVAL);  // Wait between checks
                    checkForDeadlock();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Check if all philosophers are stuck in the HUNGRY (waiting) state
        private void checkForDeadlock() {
            synchronized (philosopherStates) {
                boolean allHungry = true;
                for (int i = 0; i < NUM_FORKS; i++) {
                    // Check if all philosophers at the current table are hungry
                    if (philosopherStates[i] != State.HUNGRY) {
                        allHungry = false;
                        break;
                    }
                }

                if (allHungry) {
                    System.out.println("Deadlock detected at a table! Moving a philosopher to the sixth table.");
                    movePhilosopherToSixthTable();
                } else {
                    System.out.println("No deadlock detected.");
                }
            }
        }

        // Randomly select a philosopher to move to the sixth table to break deadlock
        private void movePhilosopherToSixthTable() {
            int philosopherToMove = random.nextInt(NUM_PHILOSOPHERS);  // Randomly select a philosopher to move
            sixthTablePhilosopher = philosopherNames[philosopherToMove];  // Get the name of the philosopher

            System.out.println("Philosopher " + sixthTablePhilosopher + " is moved to the sixth table.");
            // Continue the simulation until deadlock occurs at the sixth table
            checkDeadlockAtSixthTable();
        }

        // Check if deadlock occurs at the sixth table (with the moved philosopher)
        private void checkDeadlockAtSixthTable() {
            synchronized (philosopherStates) {
                boolean deadlockAtSixthTable = true;

                for (State state : philosopherStates) {
                    // If any philosopher is not hungry, there's no deadlock yet
                    if (state != State.HUNGRY) {
                        deadlockAtSixthTable = false;
                        break;
                    }
                }

                if (deadlockAtSixthTable) {
                    deadlockDetected = true;  // Set flag to terminate simulation
                    System.out.println("Deadlock detected at the sixth table! Last philosopher: " + sixthTablePhilosopher);
                }
            }
        }
    }
}
