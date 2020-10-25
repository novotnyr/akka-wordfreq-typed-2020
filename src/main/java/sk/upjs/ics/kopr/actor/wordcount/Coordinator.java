package sk.upjs.ics.kopr.actor.wordcount;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.PoolRouter;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Routers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        this.worker = context.spawn(workerPool(), "frequency-counter");
        this.messageAdapter = context.messageAdapter(SentenceFrequencyCounter.Frequencies.class, frequencies -> new AggregateFrequencies(frequencies.getFrequencies()));
        this.remainingSentences = remainingSentences;
    }

    private PoolRouter<SentenceFrequencyCounter.Sentence> workerPool() {
        return Routers.pool(3, SentenceFrequencyCounter.create());
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
        List<String> sentences = Arrays.asList("The quick brown fox tried to jump over the lazy dog and fell on the dog",
                "Dog is man's best friend",
                "Dog and Fox belong to the same family",
                "The dog was the first domesticated species",
                "The origin of the domestic dog is not clear");

        int remainingSentences = sentences.size();
        ActorSystem<Coordinator.Command> system = ActorSystem.create(Coordinator.create(remainingSentences), "system");

        for (String sentence : sentences) {
            system.tell(new CalculateFrequencies(sentence));
        }

    }

}
