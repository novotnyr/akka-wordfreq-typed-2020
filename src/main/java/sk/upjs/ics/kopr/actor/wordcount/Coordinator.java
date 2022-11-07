package sk.upjs.ics.kopr.actor.wordcount;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
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
        Behavior<SentenceFrequencyCounter.Sentence> sentenceCounter = Behaviors
                    .supervise(SentenceFrequencyCounter.create())
                    .onFailure(SupervisorStrategy.restart());

        return Routers.pool(3, sentenceCounter);
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
            getContext().getLog().info("[{}] Frequencies: {}", getContext().getSelf(), this.allFrequencies);
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
        List<String> sentences = Arrays.asList(
                "Vienna (/viˈɛnə/ (About this soundlisten); German: Wien  (About this soundlisten)) is the national capital, largest city, and one of nine states of Austria.",
                "Vienna is Austria's most-populous city, with about 1.9 million inhabitants (2.6 million within the metropolitan area, nearly one third of the country's population), and its cultural, economic, and political center. ",
                "It is the 6th-largest city by population within city limits in the European Union.",
                "Until the beginning of the 20th century, Vienna was the largest German-speaking city in the world, and before the splitting of the Austro-Hungarian Empire in World War I, the city had 2 million inhabitants.",
                "Today, it is the second-largest German-speaking city after Berlin.",
                "Vienna is host to many major international organizations, including the United Nations, OPEC and the OSCE.",
                "The city is located in the eastern part of Austria and is close to the borders of the Czech Republic, Slovakia and Hungary.",
                "These regions work together in a European Centrope border region.",
                "Along with nearby Bratislava, Vienna forms a metropolitan region with 3 million inhabitants.",
                "In 2001, the city center was designated a UNESCO World Heritage Site.",
                "In July 2017 it was moved to the list of World Heritage in Danger.",
                "Additionally to being known as the City of Music due to its musical legacy, as many famous classical musicians such as Beethoven and Mozart who called Vienna home.",
                "Vienna is also said to be the City of Dreams, because of it being home to the world's first psychoanalyst Sigmund Freud.",
                "Vienna's ancestral roots lie in early Celtic and Roman settlements that transformed into a Medieval and Baroque city.",
                "It is well known for having played a pivotal role as a leading European music center, from the age of Viennese Classicism through the early part of the 20th century.",
                "The historic center of Vienna is rich in architectural ensembles, including Baroque palaces and gardens, and the late-19th-century Ringstraße lined with grand buildings, monuments and parks."
        );

        int remainingSentences = sentences.size();
        ActorSystem<Coordinator.Command> system = ActorSystem.create(Coordinator.create(remainingSentences), "system");

        for (String sentence : sentences) {
            system.tell(new CalculateFrequencies(sentence));
        }

    }

}
