package sk.upjs.ics.kopr.actor.wordcount;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

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

        System.out.println(frequencies);

        return this;
    }

    public static class Sentence {
        private final String sentence;

        public Sentence(String sentence) {
            this.sentence = sentence;
        }

        public String getSentence() {
            return sentence;
        }
    }

    // --------------------------------
    public static void main(String[] args) {
        ActorSystem<Sentence> system = ActorSystem.create(SentenceFrequencyCounter.create(), "system");
        system.tell(new Sentence("zlom dobro zlom"));
    }

}
