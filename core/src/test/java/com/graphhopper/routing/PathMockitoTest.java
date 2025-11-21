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

package com.graphhopper.routing;

import com.carrotsearch.hppc.IntArrayList;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests avec Mockito pour la classe Path
 * 
 * JUSTIFICATION DU CHOIX DE LA CLASSE TESTÉE:
 * ============================================
 * Path est une classe centrale dans GraphHopper qui représente un chemin calculé.
 * Elle est idéale pour les tests avec mocks car:
 * 1. Elle dépend fortement de Graph et NodeAccess qui sont complexes à instancier
 * 2. Elle contient une logique métier importante (calcul de points, d'arêtes, etc.)
 * 3. Les mocks permettent de tester la logique sans infrastructure réelle
 * 4. C'est une classe fréquemment utilisée donc sa fiabilité est critique
 * 
 * CHOIX DES CLASSES SIMULÉES:
 * ===========================
 * 1. Graph: Interface principale du système de graphe
 *    - Raison: Représente toute la structure routière, très lourd à créer
 *    - Le mock permet de simuler un graphe minimal avec les arêtes nécessaires
 *    - Avantage: Contrôle total sur les retours sans fichiers OSM
 * 
 * 2. NodeAccess: Interface d'accès aux coordonnées des nœuds
 *    - Raison: Nécessite un stockage sous-jacent complexe
 *    - Le mock permet de définir des coordonnées de test précises
 *    - Avantage: Pas besoin de fichiers de données géographiques
 * 
 * 3. EdgeIteratorState: Représente une arête du graphe
 *    - Raison: Dépend du Graph et du système de stockage
 *    - Le mock permet de définir des arêtes avec des propriétés spécifiques
 *    - Avantage: Simulation facile de différents types de routes
 * 
 * @author IFT3913 Student Team - Tests Mockito
 */
@ExtendWith(MockitoExtension.class)
class PathMockitoTest {

    @Mock
    private Graph mockGraph;

    @Mock
    private NodeAccess mockNodeAccess;

    @Mock
    private EdgeIteratorState mockEdge1;

    @Mock
    private EdgeIteratorState mockEdge2;

    private Path path;

    @BeforeEach
    void setUp() {
        when(mockGraph.getNodeAccess()).thenReturn(mockNodeAccess);
        
        when(mockNodeAccess.is3D()).thenReturn(false);
        
        path = new Path(mockGraph);
    }


    @Test
    void testCalcNodesWithTwoEdges() {
        path.setFromNode(0);
        path.setEndNode(2);
        path.setFound(true);
        path.addEdge(10); 
        path.addEdge(20);
        
        when(mockGraph.getEdgeIteratorState(10, 0)).thenReturn(mockEdge1);
        when(mockEdge1.getBaseNode()).thenReturn(1);
        when(mockGraph.getEdgeIteratorState(10, 1)).thenReturn(mockEdge1);
        when(mockEdge1.getEdge()).thenReturn(10);
        when(mockEdge1.getAdjNode()).thenReturn(1);
        
        when(mockGraph.getEdgeIteratorState(20, 1)).thenReturn(mockEdge2);
        when(mockEdge2.getBaseNode()).thenReturn(2);
        when(mockGraph.getEdgeIteratorState(20, 2)).thenReturn(mockEdge2);
        when(mockEdge2.getEdge()).thenReturn(20);
        when(mockEdge2.getAdjNode()).thenReturn(2);

        IntArrayList nodes = (IntArrayList) path.calcNodes();

        assertNotNull(nodes);
        assertEquals(3, nodes.size());
        assertEquals(0, nodes.get(0));
        assertEquals(1, nodes.get(1));
        assertEquals(2, nodes.get(2));
        
        verify(mockGraph, times(2)).getEdgeIteratorState(anyInt(), anyInt());
        verify(mockEdge1, times(1)).getAdjNode();
        verify(mockEdge2, times(1)).getAdjNode();
    }

    @Test
    void testCalcPointsWithGeometry() {
        path.setFromNode(0);
        path.setEndNode(2);
        path.setFound(true);
        path.addEdge(10);
        path.addEdge(20);
        
        when(mockNodeAccess.getLat(0)).thenReturn(49.0);
        when(mockNodeAccess.getLon(0)).thenReturn(8.0);
        when(mockNodeAccess.getLat(1)).thenReturn(49.5);
        when(mockNodeAccess.getLon(1)).thenReturn(8.5);
        when(mockNodeAccess.getLat(2)).thenReturn(50.0);
        when(mockNodeAccess.getLon(2)).thenReturn(9.0);
        
        PointList edge1Points = new PointList(2, false);
        edge1Points.add(49.25, 8.25);
        edge1Points.add(49.5, 8.5);
        
        when(mockGraph.getEdgeIteratorState(10, 0)).thenReturn(mockEdge1);
        when(mockEdge1.getBaseNode()).thenReturn(1);
        when(mockGraph.getEdgeIteratorState(10, 1)).thenReturn(mockEdge1);
        when(mockEdge1.getEdge()).thenReturn(10);
        when(mockEdge1.fetchWayGeometry(any())).thenReturn(edge1Points);
        
        PointList edge2Points = new PointList(2, false);
        edge2Points.add(49.75, 8.75);
        edge2Points.add(50.0, 9.0);
        
        when(mockGraph.getEdgeIteratorState(20, 1)).thenReturn(mockEdge2);
        when(mockEdge2.getBaseNode()).thenReturn(2);
        when(mockGraph.getEdgeIteratorState(20, 2)).thenReturn(mockEdge2);
        when(mockEdge2.getEdge()).thenReturn(20);
        when(mockEdge2.fetchWayGeometry(any())).thenReturn(edge2Points);

        PointList points = path.calcPoints();

        assertNotNull(points);
        assertEquals(5, points.size());
        assertEquals(49.0, points.getLat(0), 0.001);
        assertEquals(8.0, points.getLon(0), 0.001);
        assertEquals(49.25, points.getLat(1), 0.001);
        assertEquals(8.25, points.getLon(1), 0.001);
        assertEquals(50.0, points.getLat(4), 0.001);
        assertEquals(9.0, points.getLon(4), 0.001);
        
        verify(mockNodeAccess, times(1)).getLat(0);
        verify(mockNodeAccess, times(1)).getLon(0);
        verify(mockEdge1, times(1)).fetchWayGeometry(any());
        verify(mockEdge2, times(1)).fetchWayGeometry(any());
    }
}
