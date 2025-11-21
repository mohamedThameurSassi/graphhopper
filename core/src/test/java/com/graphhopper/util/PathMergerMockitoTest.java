/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.graphhopper.util;

import com.graphhopper.ResponsePath;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Tests avec Mockito pour PathMerger
 * 
 * JUSTIFICATION DU CHOIX DE LA CLASSE TESTÉE:
 * ============================================
 * PathMerger est une classe appropriée pour les tests avec mocks car:
 * 1. Elle dépend de plusieurs collaborateurs complexes (Graph, Weighting, Path)
 * 2. Ces dépendances sont difficiles à instancier réellement dans les tests
 * 3. Elle a une logique métier claire qui mérite d'être testée
 * 4. Les mocks permettent d'isoler la logique de fusion sans se préoccuper de l'implémentation des dépendances
 * 
 * CHOIX DES CLASSES SIMULÉES:
 * ===========================
 * 1. Graph: Interface complexe représentant le graphe routier
 *    - Raison: Nécessite une infrastructure lourde (fichiers, base de données)
 *    - Le mock permet de contrôler les réponses sans créer un vrai graphe
 * 
 * 2. Weighting: Interface définissant les poids des arêtes
 *    - Raison: Dépend du profil de routage (voiture, vélo, etc.)
 *    - Le mock permet de simuler différents scénarios de pondération
 * 
 * 3. Path: Classe représentant un segment de chemin
 *    - Raison: Création manuelle complexe avec dépendances
 *    - Le mock permet de définir précisément les valeurs de test
 * 
 * 4. EncodedValueLookup: Interface pour accéder aux valeurs encodées
 *    - Raison: Système d'encodage complexe
 *    - Le mock simplifie la configuration du test
 * 
 * @author IFT3913 Student Team - Tests Mockito
 */
@ExtendWith(MockitoExtension.class)
class PathMergerMockitoTest {

    @Mock
    private Graph mockGraph;

    @Mock
    private Weighting mockWeighting;

    @Mock
    private Path mockPath1;

    @Mock
    private Path mockPath2;

    @Mock
    private EncodedValueLookup mockEncodedValueLookup;

    private PathMerger pathMerger;
    private Translation translation;

    @BeforeEach
    void setUp() {
        // Configuration du mock Graph pour retourner le Weighting mocké
        when(mockGraph.wrapWeighting(mockWeighting)).thenReturn(mockWeighting);
        
        // Initialisation de PathMerger avec les mocks
        pathMerger = new PathMerger(mockGraph, mockWeighting);
        
        // Translation pour les instructions (utilise l'implémentation réelle car elle est simple)
        translation = new TranslationMap().doImport().getWithFallBack(Helper.getLocale("en"));
    }

    /**
     * Test 1: Test de fusion de chemins avec des chemins trouvés
     * 
     * OBJECTIF DU TEST:
     * ================
     * Vérifier que PathMerger fusionne correctement deux chemins trouvés et calcule
     * les métriques totales (distance, temps, poids)
     * 
     * DÉFINITION DES MOCKS:
     * ====================
     * - mockPath1 et mockPath2: Simulés pour retourner des valeurs spécifiques
     * - Les valeurs choisies (100m/5s pour path1, 200m/10s pour path2) permettent
     *   de vérifier facilement l'addition des métriques
     * 
     * CHOIX DES VALEURS SIMULÉES:
     * ===========================
     * - isFound() = true: Les deux chemins sont trouvés (cas nominal)
     * - Distance path1 = 100.0m, path2 = 200.0m → Total attendu = 300.0m
     * - Temps path1 = 5000ms, path2 = 10000ms → Total attendu = 15000ms
     * - Poids path1 = 1.5, path2 = 2.5 → Total attendu = 4.0
     * - PointList vides: Simplifie le test en se concentrant sur les métriques
     * 
     * ORACLE:
     * =======
     * Le ResponsePath doit contenir:
     * - Distance totale = 300.0 (somme des distances)
     * - Temps total = 15000 (somme des temps)
     * - Poids total = 4.0 (somme des poids)
     */
    @Test
    void testDoWorkWithTwoFoundPaths() {
        
        // Path 1: 100 mètres, 5 secondes, poids 1.5
        lenient().when(mockPath1.isFound()).thenReturn(true);
        lenient().when(mockPath1.getDistance()).thenReturn(100.0);
        lenient().when(mockPath1.getTime()).thenReturn(5000L);
        lenient().when(mockPath1.getWeight()).thenReturn(1.5);
        lenient().when(mockPath1.getDescription()).thenReturn(Arrays.asList("Route A"));
        lenient().when(mockPath1.calcPoints()).thenReturn(new PointList(10, false));

        // Path 2: 200 mètres, 10 secondes, poids 2.5
        lenient().when(mockPath2.isFound()).thenReturn(true);
        lenient().when(mockPath2.getDistance()).thenReturn(200.0);
        lenient().when(mockPath2.getTime()).thenReturn(10000L);
        lenient().when(mockPath2.getWeight()).thenReturn(2.5);
        lenient().when(mockPath2.getDescription()).thenReturn(Arrays.asList("Route B"));
        lenient().when(mockPath2.calcPoints()).thenReturn(new PointList(10, false));

        List<Path> paths = Arrays.asList(mockPath1, mockPath2);
        PointList waypoints = new PointList(2, false);
        waypoints.add(49.0, 8.0);
        waypoints.add(49.1, 8.1);

        // Configuration de PathMerger pour ne pas générer d'instructions
        pathMerger.setEnableInstructions(false);
        pathMerger.setCalcPoints(false);

        // ACT: Exécution de la méthode testée
        ResponsePath result = pathMerger.doWork(waypoints, paths, mockEncodedValueLookup, translation);

        // ASSERT: Vérification des résultats
        assertNotNull(result, "Le ResponsePath ne doit pas être null");
        assertEquals(300.0, result.getDistance(), 0.001, 
            "La distance totale doit être la somme des distances des deux chemins (100 + 200)");
        assertEquals(15000L, result.getTime(), 
            "Le temps total doit être la somme des temps des deux chemins (5000 + 10000)");
        assertEquals(4.0, result.getRouteWeight(), 0.001, 
            "Le poids total doit être la somme des poids des deux chemins (1.5 + 2.5)");
        
        // Vérification que les méthodes des mocks ont été appelées
        // Note: isFound() peut être appelé plusieurs fois (vérifications internes)
        verify(mockPath1, atLeastOnce()).isFound();
        verify(mockPath1, atLeastOnce()).getDistance();
        verify(mockPath1, atLeastOnce()).getTime();
        verify(mockPath1, atLeastOnce()).getWeight();
        
        verify(mockPath2, atLeastOnce()).isFound();
        verify(mockPath2, atLeastOnce()).getDistance();
        verify(mockPath2, atLeastOnce()).getTime();
        verify(mockPath2, atLeastOnce()).getWeight();
    }

    /**
     * Test 2: Test avec un chemin non trouvé
     * 
     * OBJECTIF DU TEST:
     * ================
     * Vérifier que PathMerger gère correctement le cas où un des chemins n'est pas trouvé
     * et ajoute une erreur appropriée au ResponsePath
     * 
     * DÉFINITION DES MOCKS:
     * ====================
     * - mockPath1: Configuré avec isFound() = true (chemin trouvé)
     * - mockPath2: Configuré avec isFound() = false (chemin non trouvé)
     * 
     * CHOIX DES VALEURS SIMULÉES:
     * ===========================
     * - path1 trouvé avec des valeurs normales (150m, 7s, poids 2.0)
     * - path2 NON trouvé: simule une impossibilité de calcul du routage
     *   (ex: zone inaccessible, pas de route entre deux points)
     * 
     * JUSTIFICATION DU SCÉNARIO:
     * =========================
     * Ce test est crucial car dans une application de routage réelle:
     * - Un waypoint peut être dans une zone non accessible
     * - Des restrictions peuvent bloquer le passage
     * - Le graphe peut avoir des composantes déconnectées
     * 
     * ORACLE:
     * =======
     * Le ResponsePath doit:
     * - Contenir les erreurs (liste non vide)
     * - Avoir au moins 1 erreur de type ConnectionNotFoundException
     * - Calculer quand même les métriques pour le path1 trouvé
     */
    @Test
    void testDoWorkWithOnePathNotFound() {
        // ARRANGE: Configuration d'un scénario avec échec partiel
        
        // Path 1: Trouvé avec des valeurs normales
        lenient().when(mockPath1.isFound()).thenReturn(true);
        lenient().when(mockPath1.getDistance()).thenReturn(150.0);
        lenient().when(mockPath1.getTime()).thenReturn(7000L);
        lenient().when(mockPath1.getWeight()).thenReturn(2.0);
        lenient().when(mockPath1.getDescription()).thenReturn(Arrays.asList("Highway 1"));
        lenient().when(mockPath1.calcPoints()).thenReturn(new PointList(5, false));

        // Path 2: NON trouvé - simule un échec de routage
        // Scénario réaliste: destination inaccessible, zone privée, etc.
        lenient().when(mockPath2.isFound()).thenReturn(false);
        // Les autres méthodes ne seront pas appelées car le path n'est pas trouvé

        List<Path> paths = Arrays.asList(mockPath1, mockPath2);
        PointList waypoints = new PointList(2, false);
        waypoints.add(50.0, 9.0);
        waypoints.add(50.1, 9.1);

        pathMerger.setEnableInstructions(false);
        pathMerger.setCalcPoints(false);

        // ACT: Exécution avec un chemin manquant
        ResponsePath result = pathMerger.doWork(waypoints, paths, mockEncodedValueLookup, translation);

        // ASSERT: Vérification de la gestion d'erreur
        assertNotNull(result, "Le ResponsePath ne doit pas être null même en cas d'échec partiel");
        
        // Vérification qu'une erreur est présente (ResponsePath contient des erreurs quand un path n'est pas trouvé)
        assertTrue(result.hasErrors(), 
            "Des erreurs doivent être présentes quand un chemin n'est pas trouvé");
        assertFalse(result.getErrors().isEmpty(), 
            "La liste d'erreurs ne doit pas être vide");
        assertEquals(1, result.getErrors().size(), 
            "Il doit y avoir exactement 1 erreur (ConnectionNotFoundException)");
        
        // NOTE IMPORTANTE: On ne peut pas appeler getDistance(), getTime() etc. 
        // sur un ResponsePath qui contient des erreurs. C'est le comportement attendu
        // de l'API ResponsePath qui protège contre l'utilisation de données invalides.
        
        // Vérification des interactions avec les mocks
        verify(mockPath1, atLeastOnce()).isFound();
        verify(mockPath2, atLeastOnce()).isFound();
        
        // mockPath2 ne doit pas avoir ses méthodes de métriques appelées car il n'est pas trouvé
        verify(mockPath2, never()).getDistance();
        verify(mockPath2, never()).getTime();
        verify(mockPath2, never()).getWeight();
    }
}
