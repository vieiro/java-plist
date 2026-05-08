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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Antonio Vieiro <antonio@vieiro.net>
 */
public class BeanSerializationTests {

    public BeanSerializationTests() {
    }

    @Test
    public void testShouldPersistAPersistentSecondBean() throws Exception {
        System.out.println("testShouldPersistAPersistentSecondBean");
        String fileName = "target/testShouldPersistAPersistentSecondBean.plist";
        SecondBean b = new SecondBean();
        b.setIntegerNumber(10);
        b.setLongNumber(-20);

        PListIO.write(new File(fileName), b);

        SecondBean b2 = (SecondBean) PListIO.read(new File(fileName));

        assertEquals(b.getIntegerNumber(), b2.getIntegerNumber());
        assertEquals(b.getLongNumber(), b2.getLongNumber());
    }

    @Test
    public void testShouldPersistNestedBeans() throws Exception {
        System.out.println("testShouldPersistNestedBeans");

        String filename = "target/testShouldPersistNestedBeans.plist";

        SecondBean b = new SecondBean();
        b.setIntegerNumber(10);
        b.setLongNumber(-20);
        b.setSet(Stream.of("A", "B").collect(Collectors.toSet()));

        FirstBean a = new FirstBean();
        a.setSecond(b);
        a.setName("Hello");
        a.setNumber((short) 10);

        PListIO.write(new File(filename), a);

        FirstBean a2 = (FirstBean) PListIO.read(new File(filename));

        SecondBean b2 = a2.getSecond();

        assertTrue(b2.getSet().contains("A"));
        assertTrue(b2.getSet().contains("B"));

    }

    public static class FilePathBean {

        private File file;
        private Path path;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public Path getPath() {
            return path;
        }

        public void setPath(Path path) {
            this.path = path;
        }

    }

    @Test
    public void testShouldSerializeAndDeserializeFilesAndPaths() throws Exception {
        System.out.println("testShouldSerializeAndDeserializeFilesAndPaths");

        FilePathBean original = new FilePathBean();
        original.setFile(new File(System.getProperty("user.home")));
        original.setPath(Paths.get("A").relativize(Paths.get("A", "B", "C")));

        FilePathBean serializedAndDeserialized = null;
        boolean debug = false;

        try (ByteArrayOutputStream output = new ByteArrayOutputStream(16 * 1024)) {
            PListIO.write(output, original);
            String outputText = new String(output.toByteArray(), StandardCharsets.UTF_8);
            try (ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
                if (debug) {
                    System.err.println(new String(output.toByteArray(), StandardCharsets.UTF_8));
                }
                serializedAndDeserialized = (FilePathBean) PListIO.read(input);
            }
        }

        assertNotNull(serializedAndDeserialized);
        assertEquals(original.getFile(), serializedAndDeserialized.getFile());
        assertEquals(original.getPath(), serializedAndDeserialized.getPath());

    }

}
