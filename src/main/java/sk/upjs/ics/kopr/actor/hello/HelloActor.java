package sk.upjs.ics.kopr.actor.hello;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class HelloActor extends AbstractBehavior<String> {

    private HelloActor(ActorContext<String> context) {
        super(context);
    }

    public static Behavior<String> create() {
        return Behaviors.setup(HelloActor::new);
    }

    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessage(String.class, s -> sayHello())
                .build();
    }

    private Behavior<String> sayHello() {
        System.out.println("Hello");
        return this;
    }

    // --------------------------------
    public static void main(String[] args) {
        ActorSystem<String> system = ActorSystem.create(HelloActor.create(), "system");
        system.tell("Hello");
    }
}
