package sk.upjs.ics.kopr.actor.wordcount;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SentenceFrequencyCounter extends AbstractBehavior<SentenceFrequencyCounter.Sentence> {
    public static Behavior<Sentence> create() {
        return Behaviors.setup(SentenceFrequencyCounter::new);
    }

    private SentenceFrequencyCounter(ActorContext<Sentence> context) {
        super(context);
    }

    @Override
    public Receive<Sentence> createReceive() {
        return newReceiveBuilder().onMessage(Sentence.class, this::calculateFrequencies)
                .build();
    }

    private Behavior<Sentence> calculateFrequencies(Sentence sentence) {
        Map<String, Long> frequencies = Stream.of(sentence.getSentence().split("\\s"))
                .collect(Collectors.groupingBy(String::toString, Collectors.counting()));

        getContext().getLog().info("[{}] Worker Frequencies: {}", getContext().getSelf(), frequencies);

        sentence.replyTo.tell(new Frequencies(frequencies));

        return this;
    }

    public static class Sentence {
        private final String sentence;

        private final ActorRef<Frequencies> replyTo;

        public Sentence(String sentence, ActorRef<Frequencies> replyTo) {
            this.sentence = sentence;
            this.replyTo = replyTo;
        }

        public String getSentence() {
            return sentence;
        }

        public ActorRef<Frequencies> getReplyTo() {
            return replyTo;
        }
    }


    public static class Frequencies {
        private final Map<String, Long> frequencies;

        public Frequencies(Map<String, Long> frequencies) {
            this.frequencies = frequencies;
        }

        public Map<String, Long> getFrequencies() {
            return frequencies;
        }
    }

    // --------------------------------
    public static void main(String[] args) {
        ActorSystem<Sentence> system = ActorSystem.create(SentenceFrequencyCounter.create(), "system");
        AskPattern
                .ask(system, (ActorRef<Frequencies> replyTo) -> new Sentence("zlom dobro zlom", replyTo),
                        Duration.ofSeconds(5),
                        system.scheduler())
                .whenComplete((frequencies, throwable) -> {
                    System.out.println("[Main] " + frequencies.getFrequencies());
                });
    }

}
