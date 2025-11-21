# Documentation des Tests Mockito - GraphHopper

## Auteur
IFT3913 Student Team - Tests avec Mockito

## Date
Novembre 2025

---

## 1. Vue d'ensemble

Ce document présente l'implémentation de **4 cas de test** utilisant **Mockito** pour tester deux classes du projet GraphHopper. Les tests simulent au total **4 classes différentes** (Graph, Weighting, NodeAccess, EdgeIteratorState) en utilisant des mocks pilotés.

---

## 2. Classes testées

### 2.1 PathMerger (`com.graphhopper.util.PathMerger`)

**Fichier de test:** `PathMergerMockitoTest.java`

**Description:**
PathMerger est responsable de la fusion de plusieurs objets Path en un seul chemin continu pour générer une réponse de routage complète.

**Justification du choix:**
La classe est relativement simple a tester. Les objets Path ne demande pas énormément de valeurs pour être instancier et la classe reste cependant importante pour le reste du projet. On a donc décidé de commencer avec cette classe pour les test. De plus, comme elle doit faire le lien entre des Paths contenant différentes vitesse et distances, il peut y avoir des edges cases intéressants.

**Nombre de tests:** 2 cas de test


### 2.2 Path (`com.graphhopper.routing.Path`)

**Fichier de test:** `PathMockitoTest.java`

**Description:**
Path représente le résultat d'un calcul de plus court chemin. Elle contient la séquence d'arêtes et fournit des méthodes pour extraire des informations détaillées (nœuds, points GPS, etc.).

**Justification du choix:**
La classe est utilisées un peu partout dans le projet et dépend de plusieurs classes. Encore une fois les objets path sont facile a tester et instancier c'est pourquoi on continué avec cette classe.

**Nombre de tests:** 2 cas de test

---

## 3. Classes simulées avec Mockito

### 3.1 Graph (`com.graphhopper.storage.Graph`)

**Utilisé dans:** PathMergerMockitoTest, PathMockitoTest

**Raison de la simulation:**
Les données de OSM viennent souvent en Json qui peuvent attendre plusieurs centaines de mégabytes
J'ai déjà travailler sur un projet visant a diminuer et simplifier les graphes de OSM et faire ce travail ici serait trop lent et pénalisant en performance. C'est pourquoi on a décidé de simuler cette classe. Ainsi on ne dépend pas du temps pour charger 100MB de Jsons en mémoire

**Méthodes mockées:**
```java
when(mockGraph.getNodeAccess()).thenReturn(mockNodeAccess);
when(mockGraph.wrapWeighting(mockWeighting)).thenReturn(mockWeighting);
when(mockGraph.getEdgeIteratorState(edgeId, nodeId)).thenReturn(mockEdge);
```

---

### 3.2 Weighting (`com.graphhopper.routing.weighting.Weighting`)

**Utilisé dans:** PathMergerMockitoTest

**Raison de la simulation:**
Cette classe est nécessaire aux tests de nos autres classes. Cependant, elle nécessite d'avoir déjà configurer l'encodeur et dépend aussi d'autres classes plus complexe. Encore une fois on gagne en simplicité en la simulant

**Méthodes mockées:**
```java
when(mockGraph.wrapWeighting(mockWeighting)).thenReturn(mockWeighting);
```

---

### 3.3 NodeAccess (`com.graphhopper.storage.NodeAccess`)

**Utilisé dans:** PathMockitoTest

**Raison de la simulation:**
On ne voulait pas avoir à charger une base de donnée SQL pour faire nos tests

**Méthodes mockées:**
```java
when(mockNodeAccess.is3D()).thenReturn(false);
when(mockNodeAccess.getLat(nodeId)).thenReturn(latitude);
when(mockNodeAccess.getLon(nodeId)).thenReturn(longitude);
```

---

### 3.4 EdgeIteratorState (`com.graphhopper.util.EdgeIteratorState`)

**Utilisé dans:** PathMockitoTest

**Raison de la simulation:**


**Avantages du mock:**
- Simulation facile d'arêtes avec propriétés spécifiques
- Contrôle de la géométrie (waypoints) des routes
- Tests de différents types de routes (autoroute, rue, etc.)

**Méthodes mockées:**
```java
when(mockEdge.getBaseNode()).thenReturn(nodeId);
when(mockEdge.getAdjNode()).thenReturn(adjNodeId);
when(mockEdge.getEdge()).thenReturn(edgeId);
when(mockEdge.fetchWayGeometry(any())).thenReturn(pointList);
```

---

## 4. Description détaillée des tests

### 4.1 PathMergerMockitoTest

#### Test 1: `testDoWorkWithTwoFoundPaths()`

**Objectif:**
Vérifier que PathMerger fusionne correctement deux chemins trouvés et calcule les métriques totales.

**Mocks utilisés:**
- `mockGraph` : Graph
- `mockWeighting` : Weighting
- `mockPath1` : Path
- `mockPath2` : Path
- `mockEncodedValueLookup` : EncodedValueLookup

**Valeurs simulées et justification:**

| Mock | Méthode | Valeur | Justification |
|------|---------|--------|---------------|
| mockPath1 | isFound() | true | Chemin trouvé (cas nominal) |
| mockPath1 | getDistance() | 100.0 | Distance en mètres, facile à additionner |
| mockPath1 | getTime() | 5000 | Temps en ms (5 secondes) |
| mockPath1 | getWeight() | 1.5 | Poids arbitraire pour vérifier la somme |
| mockPath2 | isFound() | true | Deuxième chemin trouvé |
| mockPath2 | getDistance() | 200.0 | Double de path1 pour variation |
| mockPath2 | getTime() | 10000 | Double de path1 pour variation |
| mockPath2 | getWeight() | 2.5 | Complète path1 pour atteindre 4.0 |

**Valeurs attendues:**
- Distance totale: 300.0 m (100 + 200)
- Temps total: 15000 ms (5000 + 10000)
- Poids total: 4.0 (1.5 + 2.5)

**Oracle:**
Le ResponsePath doit contenir les sommes exactes des métriques.

---

### 4.2 PathMockitoTest

#### Test 1: `testCalcNodesWithTwoEdges()`

**Objectif:**
Vérifier que calcNodes() récupère correctement tous les nœuds d'un chemin.

**Mocks utilisés:**
- `mockGraph` : Graph
- `mockNodeAccess` : NodeAccess
- `mockEdge1` : EdgeIteratorState
- `mockEdge2` : EdgeIteratorState

**Valeurs simulées et justification:**

| Élément | Valeur | Justification |
|---------|--------|---------------|
| fromNode | 0 | Nœud de départ |
| Edge 10 | 0 → 1 | Première arête connectant nodes 0 et 1 |
| Edge 20 | 1 → 2 | Deuxième arête connectant nodes 1 et 2 |
| endNode | 2 | Nœud d'arrivée |

**Séquence attendue:** [0, 1, 2]

**Scénario réaliste:**
Route : Intersection A (node 0) → Rue X (edge 10) → Intersection B (node 1) → Rue Y (edge 20) → Intersection C (node 2)

**Oracle:**
La liste doit contenir exactement 3 nœuds dans l'ordre [0, 1, 2].

---


## 6. Exécution des tests

### 6.1 Installation de Mockito

La dépendance a été ajoutée au `core/pom.xml` :

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.14.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.14.2</version>
    <scope>test</scope>
</dependency>
```

### 6.2 Commandes d'exécution

**Exécuter tous les tests:**
```bash
mvn test
```

**Exécuter uniquement les tests Mockito:**
```bash
mvn test -Dtest=PathMergerMockitoTest,PathMockitoTest
```

**Exécuter un test spécifique:**
```bash
mvn test -Dtest=PathMergerMockitoTest#testDoWorkWithTwoFoundPaths
```

**Fichiers créés:**
- `core/src/test/java/com/graphhopper/util/PathMergerMockitoTest.java`
- `core/src/test/java/com/graphhopper/routing/PathMockitoTest.java`

**Modification:**
- `core/pom.xml` : Ajout des dépendances Mockito
