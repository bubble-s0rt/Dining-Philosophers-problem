import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DiningPhilosophers{
    private static final int NUM_PHILOSOPHERS = 5;
    private static final Random random = new Random();

     // semaphor for each fork
    private static final Semaphore[] forks = new Semaphore[NUM_PHILOSOPHERS];

    public static void main(String[] args) {
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            forks[i] = new Semaphore(1); //one philosopher at a time
        }

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_PHILOSOPHERS);
        
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            executorService.submit(new Philosopher(i));
        }
        executorService.shutdown();
    }


    static class Philosopher implements Runnable {
        private final int position;

        public Philosopher(int position) {
            this.position = position;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    think();
                    if (!eat()) {
                        System.out.println("Philosopher " + position + " couldn't acquire both forks. Retrying...");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        private void think() throws InterruptedException {
            System.out.println("Philosopher " + position + " is thinking...");
            Thread.sleep(random.nextInt(5000)); 
        }

        private boolean eat() throws InterruptedException {
            int leftFork = position;
            int rightFork = (position + 1) % NUM_PHILOSOPHERS;

            if (!forks[leftFork].tryAcquire(2, TimeUnit.SECONDS)) {
                return false; 
            }
            System.out.println("Philosopher " + position + " picked up left fork " + leftFork + ".");
            
            if (!forks[rightFork].tryAcquire(2, TimeUnit.SECONDS)) {
                forks[leftFork].release();
                return false; 
            }
            System.out.println("Philosopher " + position + " picked up right fork " + rightFork + ".");

            eatMeal();
            
            forks[leftFork].release();
            forks[rightFork].release();
            System.out.println("Philosopher " + position + " put down forks.");
            
            return true;
        }

        private void eatMeal() throws InterruptedException {
            System.out.println("Philosopher " + position + " is eating.");
            Thread.sleep(random.nextInt(3000)); 
    }
}
