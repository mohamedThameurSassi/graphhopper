# Documentation : Tests de Mutation avec PITest dans le Workflow CI/CD

## Vue d'ensemble

Ce document décrit les modifications apportées au workflow GitHub Actions pour intégrer les tests de mutation automatiques avec PITest à chaque commit, avec détection des régressions et notification ludique en cas de baisse du score de mutation.

---

## 📋 Table des matières

1. [Objectif](#objectif)
2. [Architecture de la solution](#architecture-de-la-solution)
3. [Modifications apportées](#modifications-apportées)
4. [Fonctionnement détaillé](#fonctionnement-détaillé)
5. [Justification des choix techniques](#justification-des-choix-techniques)
6. [Guide d'utilisation](#guide-dutilisation)
7. [Exemples de scénarios](#exemples-de-scénarios)

---

## 🎯 Objectif

Mettre en place un système de contrôle qualité automatique qui :
- Exécute les tests unitaires avec Maven
- Déclenche un rickroll ludique via GitHub Action en cas d'échec des tests
- Exécute les tests de mutation PITest sur les classes Java modifiées
- Compare le score de mutation entre le commit actuel et le commit précédent
- Détecte les régressions de qualité des tests
- Bloque le workflow en cas de baisse du score de mutation

---

## 🔧 Modifications apportées

### 1. Configuration Maven - PITest

**Fichier modifié** : `pom.xml` (racine du projet)

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.15.3</version>
    <dependencies>
        <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <version>1.2.0</version>
        </dependency>
    </dependencies>
</plugin>
```

**Justification** : 
- Version 1.15.3 de PITest pour compatibilité avec Java 24
- Plugin JUnit 5 pour support des tests modernes
- Configuration centralisée dans le POM parent

---

### 2. Nouveau Job CI/CD : `mutation-check`

**Fichier modifié** : `.github/workflows/build.yml`

#### Ajout du job `mutation-check` avec dépendance sur `build`

```yaml
mutation-check:
  needs: build  # S'exécute APRÈS le job build
  runs-on: ubuntu-latest
```

**Justification** :
- `needs: build` : Garantit que les tests unitaires passent avant d'exécuter les tests de mutation
- Économie de ressources : évite d'exécuter PITest si le build échoue
- Séparation des responsabilités : build/test vs. analyse qualité

---

## ⚙️ Fonctionnement détaillé


### Job 2 : Tests de Mutation PITest

#### Étape 2a : Détection des fichiers modifiés

```bash
CHANGED_FILES=$(git diff --name-only HEAD^ HEAD | grep "core/src/main/java/.*\.java$" || true)
```

**Fonctionnement** :
- `git diff --name-only HEAD^ HEAD` : Liste les fichiers modifiés entre l'avant-dernier commit et le dernier
- `grep "core/src/main/java/.*\.java$"` : Filtre uniquement les fichiers Java du module core
- `|| true` : Évite l'échec du script si aucun fichier ne correspond

**Pourquoi cibler uniquement `core/` ?**
- Module principal contenant la logique métier
- Autres modules (web-api, tools, etc.) moins critiques pour les tests de mutation
- Optimisation du temps d'exécution

---

#### Étape 2b : Conversion des chemins en noms de classes

```bash
for file in $CHANGED_FILES; do
   CLASS_NAME=$(echo $file | sed -E 's|.*core/src/main/java/||' | sed 's|/|.|g' | sed 's|.java$||')
   if [ -z "$CLASSES_TO_TEST" ]; then
     CLASSES_TO_TEST="$CLASS_NAME"
   else
     CLASSES_TO_TEST="$CLASSES_TO_TEST,$CLASS_NAME"
   fi
done
```

**Transformation** :
```
core/src/main/java/com/graphhopper/util/ArrayUtil.java
↓
com.graphhopper.util.ArrayUtil
```

**Résultat** : Liste séparée par des virgules pour PITest (ex: `com.graphhopper.util.ArrayUtil,com.graphhopper.util.PathMerger`)

---

#### Étape 2c : Exécution PITest sur le commit actuel

```bash
mvn -pl core org.pitest:pitest-maven:mutationCoverage \
    -DtargetClasses="$CLASSES_TO_TEST" \
    -DtargetTests="com.graphhopper.*" \
    -DoutputFormats=XML \
    -Dthreads=2 \
    -DfailWhenNoMutations=false \
    --quiet || true
```

#### Étape 2d : Extraction du score de mutation

```bash
if [ -f "core/target/pit-reports/mutations.xml" ]; then
    KILLED_CURRENT=$(grep -o 'status="KILLED"' core/target/pit-reports/mutations.xml | wc -l)
    TOTAL_CURRENT=$(grep -o '<mutation ' core/target/pit-reports/mutations.xml | wc -l)
    if [ "$TOTAL_CURRENT" -eq 0 ]; then 
        SCORE_CURRENT=0
    else 
        SCORE_CURRENT=$(echo "scale=2; $KILLED_CURRENT * 100 / $TOTAL_CURRENT" | bc)
    fi
fi
```

**Calcul du score** :
```
Score = (Mutations tuées / Mutations totales) × 100
```

#### Étape 2e : Checkout du commit précédent

```bash
git checkout HEAD^
```

**Objectif** : Obtenir le code de l'avant-dernier commit pour calculer le score de référence (baseline)

**Processus** :
1. Stash éventuel des changements (si nécessaire)
2. Checkout de `HEAD^` (commit parent)
3. Rebuild des dépendances
4. Exécution PITest sur le même ensemble de classes
5. Extraction du score baseline

---

#### Étape 2f : Comparaison et détection de régression

```bash
if (( $(echo "$SCORE_CURRENT < $SCORE_PREV" | bc -l) )); then
   echo "Mutation score dropped from $SCORE_PREV to $SCORE_CURRENT!"
   echo "Consider adding more comprehensive tests to maintain quality."
   exit 1
else
   echo "Mutation score maintained or improved."
fi
```

**Logique de décision** :

```
┌─────────────────────────────────────────────────────────┐
│  Score actuel < Score précédent ?                       │
├─────────────────────────────────────────────────────────┤
│  OUI → Régression détectée                              │
│        ├─ Affichage du message d'avertissement          │
│        └─ exit 1 (échec du workflow)                    │
│                                                          │
│  NON → Qualité maintenue ou améliorée                   │
│        └─ exit 0 (succès du workflow)                   │
└─────────────────────────────────────────────────────────┘
```

### 3. Pourquoi `targetTests="com.graphhopper.*"` au lieu d'une correspondance exacte ?

**Problème initial** : `-DtargetTests="${CLASSES_TO_TEST}*Test"` causait l'erreur "No mutations found"

**Raisons** :
1. **Nommage des tests non uniforme** :
   - `ArrayUtilTest.java` (correspondance directe)
   - `ArrayUtilExtendedTest.java` (tests supplémentaires)
   - Certaines classes n'ont pas de tests directs

2. **Logique de découverte PITest** :
   - PITest est strict avec les patterns de tests
   - Un pattern trop restrictif exclut des tests pertinents

**Solution retenue** : `com.graphhopper.*`

Cette approche présente un compromis :

**Avantages** :
- Capture tous les tests pertinents
- Évite les faux négatifs ("No mutations found")
- Fonctionne même avec des conventions de nommage variées

**Inconvénients** :
- Capture plus de code que nécessaire (over-inclusive)

**Justification** :
> Nous avons décidé qu'il est plus important (dans notre cas) de se concentrer sur avoir plus que pas assez.
> 
> Les tests et les classes non modifiées n'impactent pas le résultat final du test de mutation, donc le surplus de code analysé n'affecte que le temps d'exécution, pas la précision des résultats.

---


