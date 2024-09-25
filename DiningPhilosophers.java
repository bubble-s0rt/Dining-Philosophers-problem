public class DiningPhilosophers {

    public static void main(String[] args) {
        int numPhilosophers = 5;
        Phil[] philosophers = new Phil[numPhilosophers];
        Fork[] forks = new Fork[numPhilosophers];

        for (int i = 0; i < numPhilosophers; i++) {
            forks[i] = new Fork(i);
            philosophers[i] = new Phil(i, forks[i], forks[(i + 1) % numPhilosophers]); //philosopher and fgorks
        }

        
        for (int i = 0; i < numPhilosophers; i++) {
            philosophers[i].eat(); //in each iteration the will try to eat
        }
    }
}

class Phil {
    private int id;
    private Fork lFork;
    private Fork rFork;

    public Phil(int id, Fork lFork, Fork rFork) {
        this.id = id;
        this.lFork = lFork;
        this.rFork = rFork;
    }

    public void eat() {
        System.out.println("Philosopher " + id + " is trying to pick up forks.");
        System.out.println("Philosopher " + id + " picked up left fork " + lFork.getId());
        System.out.println("Philosopher " + id + " picked up right fork " + rFork.getId());
        System.out.println("Philosopher " + id + " is eating.");
        System.out.println("Philosopher " + id + " finished eating and put down the forks.");
    }
}

class Fork {
    private int id;

    public Fork(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
