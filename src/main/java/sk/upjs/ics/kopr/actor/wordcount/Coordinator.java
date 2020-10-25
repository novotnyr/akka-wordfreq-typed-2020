package sk.upjs.ics.kopr.actor.wordcount;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Map;

public class Coordinator extends AbstractBehavior<Coordinator.CalculateFrequencies> {
    public static Behavior<CalculateFrequencies> create() {
        return Behaviors.setup(Coordinator::new);
    }

    private Coordinator(ActorContext<CalculateFrequencies> context) {
        super(context);
    }

    @Override
    public Receive<CalculateFrequencies> createReceive() {
        return newReceiveBuilder()
                .onMessage(CalculateFrequencies.class, this::calculateFrequencies)
                .build();
    }

    private Behavior<CalculateFrequencies> calculateFrequencies(CalculateFrequencies frequencies) {
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
    }
}
