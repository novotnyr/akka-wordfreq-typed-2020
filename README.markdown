# Akka Demo

- Akka 2.6
- Scala 2.12
- Typed Akka
- syntax Java 8
- OOP styl cez `AbstractBehavior`

# Koncepty

- dva aktory: koordinátor a worker
- deklarácia typovaného protokolu cez _commandy_ a _eventy_
- rozhadzovanie roboty cez round-robin router
- agregovanie výsledkov a demonštrácia stavu v aktorovi
- adaptovanie udalosti _event_ na príkazy _command
- death-watch: sledovanie podriadených aktorov a reakcia na ukončenie
- demonštrácia synchrónneho volania cez _Ask_ 