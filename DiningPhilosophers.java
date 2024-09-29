import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DiningPhilosophers {
    private static final int NUM_PHILOSOPHERS = 25;  // Number of philosophers and forks
    private static final Random random = new Random();

    // Forks (25 forks shared by 25 philosophers)
    private static final Object[] forks = new Object[NUM_PHILOSOPHERS];

    // Philosopher names (A, B, C, ..., Y)
    private static final char[] philosopherNames = new char[NUM_PHILOSOPHERS];

    // Philosopher states to track thinking, eating, and waiting
    private enum State {THINKING, HUNGRY, EATING}
    private static final State[] philosopherStates = new State[NUM_PHILOSOPHERS];

    // Deadlock detection flag
    private static volatile boolean deadlockDetected = false;
    private static volatile char lastPhilosopherMoved = ' ';

    public static void main(String[] args) throws InterruptedException {
        // Initialize philosopher names and forks
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            forks[i] = new Object();  // Use the object as a lock (fork)
            philosopherStates[i] = State.THINKING;  // Initial state is THINKING
            philosopherNames[i] = (char) ('A' + i);  // Assign names A, B, C, ..., Y
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
        executorService.awaitTermination(2, TimeUnit.MINUTES);  // Adjusted to 2 minutes
    }

    // Philosopher class implementing Runnable
    static class Philosopher implements Runnable {
        private final char name;  // Philosopher name (A, B, C, ..., Y)
        private final int id;     // Philosopher ID (0 to 24 for accessing forks and states)

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

        // Philosopher thinks for a random time (0-2 seconds)
        private void think() throws InterruptedException {
            synchronized (philosopherStates) {
                philosopherStates[id] = State.THINKING;
            }
            System.out.println("Philosopher " + name + " is thinking...");
            Thread.sleep(random.nextInt(2000));  // Reduced from 10000 to 2000 ms
        }

        // Philosopher tries to pick up both forks (left and right)
        private void pickUpForks() throws InterruptedException {
            int leftFork = id;
            int rightFork = (id + 1) % NUM_PHILOSOPHERS;

            // Set the state to HUNGRY (waiting for forks)
            synchronized (philosopherStates) {
                philosopherStates[id] = State.HUNGRY;
            }

            // Try to pick up left and right forks in the right order
            synchronized (forks[leftFork]) {
                synchronized (forks[rightFork]) {
                    // Successfully acquired both forks, update state to EATING
                    synchronized (philosopherStates) {
                        philosopherStates[id] = State.EATING;
                    }
                }
            }
        }

        // Philosopher eats for a random time (0-1 second)
        private void eat() throws InterruptedException {
            System.out.println("Philosopher " + name + " is eating...");
            Thread.sleep(random.nextInt(1000));  // Reduced from 5000 to 1000 ms
        }

        // Philosopher puts down both forks
        private void putDownForks() {
            int leftFork = id;
            int rightFork = (id + 1) % NUM_PHILOSOPHERS;

            // Release both forks
            synchronized (forks[leftFork]) {
                synchronized (forks[rightFork]) {
                    // After putting down forks, return to THINKING state
                    synchronized (philosopherStates) {
                        philosopherStates[id] = State.THINKING;
                    }
                    System.out.println("Philosopher " + name + " put down forks " + leftFork + " and " + rightFork + ".");
                }
            }
        }
    }

    // DeadlockDetector class to periodically check for deadlock
    static class DeadlockDetector implements Runnable {
        private static final int CHECK_INTERVAL = 1000;  // Check every 1 second

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
                for (State state : philosopherStates) {
                    if (state != State.HUNGRY) {
                        allHungry = false;
                        break;
                    }
                }

                if (allHungry) {
                    deadlockDetected = true;
                    System.out.println("Deadlock detected! All philosophers are waiting for forks.");
                } else {
                    System.out.println("No deadlock detected.");
                }
            }
        }
    }
}
