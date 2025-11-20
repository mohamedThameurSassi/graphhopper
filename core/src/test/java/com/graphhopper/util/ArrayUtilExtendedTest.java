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

import com.carrotsearch.hppc.IntArrayList;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.Test;



import static com.carrotsearch.hppc.IntArrayList.from;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests étendus pour la classe ArrayUtil afin d'améliorer la couverture de code et les scores de tests de mutation.
 * Ces tests se concentrent sur les cas limites, les conditions d'erreur et les valeurs frontières qui pourraient ne pas être
 * couverts par la suite de tests existante.
 * 
 * @author IFT3913 Student Team
 */
class ArrayUtilExtendedTest {

    private final Faker faker = new Faker();

    /**
     * Test 1: Teste que {@code removeConsecutiveDuplicates} lance une IllegalArgumentException pour un paramètre end négatif
     * <p>
     * Intention: Vérifier la validation appropriée du paramètre end dans la méthode removeConsecutiveDuplicates
     * Motivation: Les valeurs end négatives sont invalides et doivent être rejetées pour éviter les problèmes d'index de tableau
     * Oracle: IllegalArgumentException doit être lancée quand end < 0
     * </p>
     */
    //@Test
   /* public void testRemoveConsecutiveDuplicatesNegativeEnd() {
        int[] arr = {1, 2, 3, 4, 5};
        
        assertThrows(IllegalArgumentException.class, 
            () -> ArrayUtil.removeConsecutiveDuplicates(arr, -1),
            "Doit lancer IllegalArgumentException pour un paramètre end négatif");
    }
   */
    /**
     * Test 2: Teste que {@code calcSortOrder} lance une IllegalArgumentException quand les tableaux ont des tailles différentes
     * <p>
     * Intention: Vérifier que calcSortOrder valide correctement les tailles des tableaux d'entrée
     * Motivation: Les tableaux de tailles différentes ne peuvent pas être triés ensemble correctement
     * Oracle: IllegalArgumentException doit être lancée quand arr1.size() != arr2.size()
     * </p>
     */
    @Test
    public void testCalcSortOrderDifferentSizes() {
        IntArrayList arr1 = from(1, 2, 3);
        IntArrayList arr2 = from(4, 5);
        
        assertThrows(IllegalArgumentException.class, 
            () -> ArrayUtil.calcSortOrder(arr1, arr2),
            "Doit lancer IllegalArgumentException quand les tableaux ont des tailles différentes");
    }

    /**
     * Test 3: Teste la méthode {@code subList} en utilisant java-faker pour la génération de données aléatoires
     * <p>
     * Intention: Démontrer l'utilisation de java-faker tout en testant subList avec des données aléatoires réalistes
     * Motivation: Utiliser faker pour générer des données de test variées et vérifier que subList fonctionne correctement
     * Oracle: La sous-liste doit contenir les éléments attendus dans le bon ordre
     * </p>
     */
    @Test
    public void testSubListWithFakerData() {
        IntArrayList original = new IntArrayList();
        int size = faker.number().numberBetween(10, 20);
        
        for (int i = 0; i < size; i++) {
            original.add(faker.number().numberBetween(-1000, 1000));
        }
        
        int fromIndex = faker.number().numberBetween(0, size / 2);
        int toIndex = faker.number().numberBetween(fromIndex, size);
        
        IntArrayList sub = ArrayUtil.subList(original, fromIndex, toIndex);
        
        assertEquals(toIndex - fromIndex, sub.size(), 
            "La taille de la sous-liste doit correspondre à la différence de plage");
        
        for (int i = 0; i < sub.size(); i++) {
            assertEquals(original.get(fromIndex + i), sub.get(i), 
                "Les éléments de la sous-liste doivent correspondre aux éléments originaux aux indices corrects");
        }
    }
}