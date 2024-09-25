public class DiningPhilosophers {

    public static void main(String[] args) {
        int numPhilosophers = 5;

        
        for (int i = 0; i < numPhilosophers; i++) {
            new Thread(new Philosopher(i)).start(); //implemented theads for philosophers
        }
    }

    static class Philosopher implements Runnable {
        private int id;

        public Philosopher(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    think();
                    eat();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void think() throws InterruptedException {
            System.out.println("Philosopher " + id + " is thinking...");
            Thread.sleep(1000); //thinks
        }

        private void eat() throws InterruptedException {
            System.out.println("Philosopher " + id + " picks up left fork.");
            Thread.sleep(1000);  //eat
            System.out.println("Philosopher " + id + " picks up right fork.");
            System.out.println("Philosopher " + id + " is eating...");
            Thread.sleep(1000);  //Eat
            System.out.println("Philosopher " + id + " puts down forks.");
        }
    }
}
