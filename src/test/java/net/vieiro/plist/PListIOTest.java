/*
 * Copyright 2025 Antonio Vieiro <antonio@vieiro.net>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.vieiro.plist;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Antonio Vieiro <antonio@vieiro.net>
 */
public class PListIOTest {

    public PListIOTest() {
    }

    @Test
    public void testShouldReadPListFile() throws Exception {
        System.out.println("testShouldReadPListFile");

        try (InputStream input = PListIOTest.class.getResourceAsStream("test.xml")) {
            assertNotNull(input);

            Object root = PListIO.read(input);

            assertTrue(root instanceof ArrayList);

            List list = (List) root;
            assertEquals(1L, list.get(0));

            list = (List) list.get(5);
            assertEquals("A", list.get(0));

        }
    }

    @Test
    public void testShouldReadAndWriteDocument() throws Exception {
        System.out.println("testShouldReadAndWriteDocument");

        Object root = null;

        try (InputStream input = PListIOTest.class.getResourceAsStream("test.xml")) {
            assertNotNull(input);
            root = PListIO.read(input);
            assertTrue(root instanceof ArrayList);
            List list = (List) root;
            assertEquals(1L, list.get(0));
            List innerList = (List) list.get(5);
            assertEquals("A", innerList.get(0));
            byte [] data = (byte[]) list.get(6);
            String hello = new String(data, StandardCharsets.US_ASCII);
            assertEquals("Hello", hello);
        }

        PListIO.write(new File("target/test-result.xml"), root);

        Object rootFromFile = null;
        try (InputStream input = PListIOTest.class.getResourceAsStream("test.xml")) {
            assertNotNull(input);
            rootFromFile = PListIO.read(input);
            assertTrue(rootFromFile instanceof ArrayList);
            List list = (List) rootFromFile;
            assertEquals(1L, list.get(0));
            List innerList = (List) list.get(5);
            assertEquals("A", innerList.get(0));
        }

        List r1 = (List) root;
        List r2 = (List) rootFromFile;

        List r11 = (List) r1.get(5);
        List r21 = (List) r2.get(5);

        Instant i1 = (Instant) r11.get(2);
        Instant i2 = (Instant) r21.get(2);

        assertEquals(i1, i2);
    }

    @Test
    public void testShouldReadComplexPListFile() throws Exception {
        System.out.println("testShouldReadComplexPListFile");

        try (InputStream input = PListIOTest.class.getResourceAsStream("test_1.xml")) {
            assertNotNull(input);

            Object root = PListIO.read(input);

            JexlEngine jexl = new JexlBuilder().create();
            assertEquals("plugin-type=1", jexl.getProperty(root, "ACPI.Add[0].Comment"));
            assertEquals("I2C GPIO Pinning", jexl.getProperty(root, "ACPI.Add[1].Comment"));

            assertEquals(0L, jexl.getProperty(root, "ACPI.Patch[0].Count"));
            assertEquals(4275159040L, jexl.getProperty(root, "Booter.MmioWhitelist[0].Address"));
            assertEquals(Boolean.TRUE, jexl.getProperty(root, "UEFI.ConnectDrivers"));

        }
    }

}
