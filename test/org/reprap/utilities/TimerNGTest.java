/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reprap.utilities;

import java.util.logging.Level;
import java.util.logging.Logger;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author spy
 */
public class TimerNGTest {
    /**
     * Test of stamp method, of class Timer.
     */
    @Test
    public void testStamp() {
        System.out.println("stamp");
        double expResult = 2;
        String now = Timer.stamp();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(TimerNGTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        double result = Timer.elapsed();
        assertEquals(result, expResult, 0.04);
    }
}
