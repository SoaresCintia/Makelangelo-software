# Rapport - Tâche #3: test sur divers environnements - Projet : Makelangelo-software

**Par Larry Fotso Guiffo et Cíntia Dalila Soares**

# Documentation et Justification des Changements dans la GitHub Action

Ce document détaille les modifications apportées à la GitHub Action pour tester notre application avec cinq configurations de la JVM (Java Virtual Machine) différentes. Chaque configuration utilise un "flag" (option) de la JVM qui affecte la performance, la gestion de la mémoire ou l'observabilité. En plus des modifications techniques, nous justifions le choix de chaque flag et expliquons l'impact attendu sur notre application.

---

## 1. Documentation des Changements

### Vue d'ensemble

Pour améliorer la robustesse et les performances de notre application, nous avons configuré un pipeline CI (intégration continue) qui exécute les tests avec différentes configurations de la JVM. Cette approche nous permet d'explorer les effets de plusieurs options de la JVM sur notre code dans un environnement de production simulé.

### Modifications du Workflow GitHub Action

- **Stratégie de Matrice** : La GitHub Action utilise une stratégie de matrice pour exécuter des tests et des builds avec cinq configurations de flags JVM distinctes. Chaque configuration est testée sur plusieurs systèmes d’exploitation.
- **Flags JVM Utilisés** : Les tests couvrent les options suivantes de la JVM :
  - `-XX:+UseG1GC`
  - `-XX:+UnlockExperimentalVMOptions`
  - `-XX:+UseStringDeduplication`
  - `-XX:+UseCompressedOops`
  - `-XX:+OptimizeStringConcat`
- **Logs** : Chaque exécution dans le pipeline génère des logs qui documentent clairement le flag utilisé, permettant ainsi de suivre précisément les conditions de chaque test.

---

## 2. Motivation et Justification des Flags JVM

Chaque flag JVM a été sélectionné pour son impact potentiel sur la qualité, la performance, ou l'observabilité de l'application. Voici une explication de chaque flag utilisé et de la raison de son inclusion.

- **`-XX:+UseG1GC`** : Ce flag active le Garbage-First Garbage Collector (G1GC). Ce ramasse-miettes est conçu pour réduire les temps de pause, en particulier pour les applications ayant de grands tas de mémoire. Cela permet d'améliorer la performance globale et la réactivité de l'application.

- **`-XX:+UnlockExperimentalVMOptions`** : Ce flag déverrouille les options expérimentales de la JVM, nous permettant de tester des fonctionnalités qui ne sont pas encore totalement prêtes pour la production. En activant ces options, nous pouvons évaluer des optimisations potentielles pour notre application, en termes de performance et d'efficacité.

- **`-XX:+UseStringDeduplication`** : Ce flag active la déduplication des chaînes de caractères en mémoire, ce qui peut réduire l'utilisation de la mémoire en supprimant les chaînes de caractères en double. Cette optimisation est utile pour des applications avec de nombreuses données de chaînes répétitives, améliorant ainsi la gestion de la mémoire.

- **`-XX:+UseCompressedOops`** : Cette option permet de compresser les pointeurs d’objets, réduisant ainsi l'empreinte mémoire sur les systèmes 64 bits. En optimisant l'utilisation de la mémoire, cette option contribue à une meilleure efficacité globale de l'application.

- **`-XX:+OptimizeStringConcat`** : Ce flag optimise les opérations de concaténation de chaînes, ce qui améliore les performances pour les applications qui manipulent de nombreuses chaînes de caractères. Cela peut être particulièrement bénéfique dans des contextes de traitement de texte ou d'opérations sur des chaînes répétitives.

Ces choix de flags nous permettent de tester la robustesse et l'efficacité de notre application sous différentes conditions et d'identifier les options qui apportent le plus de bénéfices en termes de performance et de gestion de la mémoire.

---

## 3. Mesure de la Qualité : Couverture des Tests

Pour chaque exécution avec un flag différent, le pipeline CI mesure le taux de couverture des tests, ce qui nous permet de vérifier la robustesse du code sous diverses configurations de la JVM.

### Outil de Couverture : JaCoCo

Nous avons intégré JaCoCo, un outil de couverture de code pour Maven, qui génère des rapports détaillés de couverture de test. Les taux de couverture sont calculés automatiquement pour chaque build et affichés dans les logs de la CI.

#### Exemple de Configuration dans `pom.xml`

Voici la configuration utilisée pour activer JaCoCo dans Maven, ce qui permet de générer des rapports de couverture lors de chaque build :

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.7</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>

## Ajout d'Éléments d'Humour dans le Workflow CI/CD

Dans le cadre de l'intégration continue, il est essentiel de maintenir un environnement de travail agréable, même dans des processus de développement automatisés. Pour égayer les rapports de CI/CD, nous avons ajouté quelques messages humoristiques dans les logs du workflow GitHub Actions. Ces touches d'humour ont pour but de rendre les tests et les vérifications de build plus agréables pour les développeurs.

### Blagues Ajoutées

- **Blague sur la JVM** : Après l'étape de build et de test avec Maven, nous avons intégré une blague qui fait sourire tout en faisant un clin d'œil aux développeurs Java :
  ```bash
  echo "Pourquoi la JVM est-elle allée en thérapie ? Elle avait trop de flags non résolus ! 😂"

- **Commits amusants** : Nous avons aussi tenté des messages de commits originaux, comme "Volta o cão arrependido", une référence à la série mexicaine *Chaves* populaire en Amérique Latine.






