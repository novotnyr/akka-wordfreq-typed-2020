package sk.upjs.ics.kopr.actor.wordcount;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Map;

public class Coordinator extends AbstractBehavior<Coordinator.Command> {
    private ActorRef<SentenceFrequencyCounter.Sentence> worker;

    private ActorRef<SentenceFrequencyCounter.Frequencies> messageAdapter;

    public static Behavior<Coordinator.Command> create() {
        return Behaviors.setup(Coordinator::new);
    }

    private Coordinator(ActorContext<Coordinator.Command> context) {
        super(context);
        this.worker = context.spawn(SentenceFrequencyCounter.create(), "frequency-counter");
        this.messageAdapter = context.messageAdapter(SentenceFrequencyCounter.Frequencies.class, frequencies -> new AggregateFrequencies(frequencies.getFrequencies()));
    }

    @Override
    public Receive<Coordinator.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CalculateFrequencies.class, this::calculateFrequencies)
                .onMessage(AggregateFrequencies.class, this::aggregateFrequencies)
                .build();
    }

    private Behavior<Command> aggregateFrequencies(AggregateFrequencies command) {
        System.out.println(command.getFrequencies());
        return Behaviors.same();
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
        ActorSystem<Coordinator.Command> system = ActorSystem.create(Coordinator.create(), "system");
        system.tell(new CalculateFrequencies("zlom dobro zlom"));
        system.tell(new CalculateFrequencies("dobro zvitazi nad zlom"));
    }

}
