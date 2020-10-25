package sk.upjs.ics.kopr.actor.wordcount;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Map;

public class Coordinator extends AbstractBehavior<Coordinator.Command> {
    private ActorRef<SentenceFrequencyCounter.Sentence> worker;

    private ActorRef<SentenceFrequencyCounter.Frequencies> messageAdapter;

    private final Map<String, Long> allFrequencies = new HashMap<>();

    private int remainingSentences = 0;

    public static Behavior<Coordinator.Command> create(int remainingSentences) {
        return Behaviors.setup(context -> new Coordinator(context, remainingSentences));
    }

    private Coordinator(ActorContext<Command> context, int remainingSentences) {
        super(context);
        this.worker = context.spawn(SentenceFrequencyCounter.create(), "frequency-counter");
        this.messageAdapter = context.messageAdapter(SentenceFrequencyCounter.Frequencies.class, frequencies -> new AggregateFrequencies(frequencies.getFrequencies()));
        this.remainingSentences = remainingSentences;
    }

    @Override
    public Receive<Coordinator.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CalculateFrequencies.class, this::calculateFrequencies)
                .onMessage(AggregateFrequencies.class, this::aggregateFrequencies)
                .build();
    }

    private Behavior<Command> aggregateFrequencies(AggregateFrequencies command) {
        MapUtils.aggregateInto(this.allFrequencies, command.getFrequencies());

        this.remainingSentences--;
        if (this.remainingSentences == 0) {
            System.out.println(this.allFrequencies);
            return Behaviors.stopped();
        }
        return this;
    }

    private Behavior<Coordinator.Command> calculateFrequencies(CalculateFrequencies command) {
        SentenceFrequencyCounter.Sentence sentence = new SentenceFrequencyCounter.Sentence(command.getSentence(), messageAdapter);
        this.worker.tell(sentence);
        return this;
    }

    public interface Command {}

    public static class CalculateFrequencies implements Command {
        private final String sentence;

        public CalculateFrequencies(String sentence) {
            this.sentence = sentence;
        }

        public String getSentence() {
            return sentence;
        }
    }

    public static class AggregateFrequencies implements Command {
        private final Map<String, Long> frequencies;

        public AggregateFrequencies(Map<String, Long> frequencies) {
            this.frequencies = frequencies;
        }

        public Map<String, Long> getFrequencies() {
            return frequencies;
        }
    }

    // --------------------------------
    public static void main(String[] args) {
        int remainingSentences = 2;
        ActorSystem<Coordinator.Command> system = ActorSystem.create(Coordinator.create(remainingSentences), "system");
        system.tell(new CalculateFrequencies("zlom dobro zlom"));
        system.tell(new CalculateFrequencies("dobro zvitazi nad zlom"));
    }

}
