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

import com.github.javafaker.Faker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link StopWatch} class to improve mutation testing coverage.
 * 
 * @author IFT3913 Student Team
 */
class StopWatchTest {

    private final Faker faker = new Faker();

    /**
     * Test 4: Tests static factory method started() with faker-generated name
     * <p>
     * Intention: Verify that started() creates a running StopWatch and toString includes name
     * Motivation: Factory method should provide pre-started instances with proper naming
     * Oracle: Created StopWatch should immediately show positive elapsed time and include name
     * </p>
     */
    @Test
    public void testStartedFactoryMethodWithName() throws InterruptedException {
        String testName = faker.company().name();
        StopWatch sw = StopWatch.started(testName);
        
        Thread.sleep(10);
        assertTrue(sw.getCurrentSeconds() > 0, "started() StopWatch should be running immediately");
        assertTrue(sw.toString().contains(testName), "toString should contain the stopwatch name");
        
        sw.stop();
        assertTrue(sw.getSeconds() > 0, "started() StopWatch should have measured time");
    }

    /**
     * Test 5: Tests getTimeString() method formatting with different time ranges
     * <p>
     * Intention: Verify that getTimeString formats time appropriately 
     * Motivation: Different time ranges should use appropriate units and formatting
     * Oracle: Format should be readable and contain proper time units
     * </p>
     */
    @Test
    public void testGetTimeStringFormatting() throws InterruptedException {
        StopWatch sw = new StopWatch();
        
        assertEquals("0ns", sw.getTimeString(), "Zero time should format as nanoseconds");
        
        sw.start();
        Thread.sleep(10);
        sw.stop();
        
        String timeString = sw.getTimeString();
        assertNotNull(timeString, "getTimeString should not return null");
        assertTrue(timeString.length() > 0, "getTimeString should not be empty");
        assertTrue(timeString.matches(".*(?:ns|μs|ms|s|min|h).*"), 
            "Time string should contain appropriate time unit");
    }

    /**
     * Test 6: Tests stop() method and verifies time doesn't change after stopping
     * <p>
     * Intention: Vérifier que stop() arrête correctement le chronomètre et fige le temps
     * Motivation: Une fois arrêté, le temps écoulé ne doit plus augmenter
     * Oracle: Le temps mesuré doit rester constant après stop() même si on attend
     * </p>
     */
    @Test
    public void testStopFreezesTime() throws InterruptedException {
        StopWatch sw = new StopWatch();
        sw.start();
        Thread.sleep(10);
        sw.stop();
        
        double timeAfterStop = sw.getSeconds();
        Thread.sleep(10); // Wait more
        double timeAfterWait = sw.getSeconds();
        
        assertEquals(timeAfterStop, timeAfterWait, 0.001,
            "Le temps ne doit pas changer après stop()");
        assertTrue(timeAfterStop > 0, "Le temps mesuré doit être positif");
    }

    /**
     * Test 7: Tests getMillis() method for time conversion
     * <p>
     * Intention: Vérifier que getMillis() retourne le temps en millisecondes correctement
     * Motivation: Tester la conversion de nanosecondes en millisecondes
     * Oracle: getMillis() doit retourner environ getSeconds() * 1000
     * </p>
     */
    @Test
    public void testGetMillis() throws InterruptedException {
        StopWatch sw = new StopWatch();
        sw.start();
        Thread.sleep(50); // 50ms delay
        sw.stop();
        
        long millis = sw.getMillis();
        double seconds = sw.getSeconds();
        
        assertTrue(millis > 0, "getMillis doit retourner un nombre positif");
        assertTrue(millis >= 40, "getMillis doit être au moins 40ms (avec marge)");
        
        // Verify conversion: millis ≈ seconds * 1000
        double expectedMillis = seconds * 1000;
        assertEquals(expectedMillis, (double) millis, 10.0,
            "getMillis doit correspondre à getSeconds() * 1000");
    }
}