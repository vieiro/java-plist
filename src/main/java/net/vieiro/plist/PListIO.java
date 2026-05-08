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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.vieiro.plist.impl.PListReader;
import net.vieiro.plist.impl.PListWriter;

/**
 * Reads and writes <b>Java Beans</b> to permanent storage in Apple's PLIST
 * (ASCII) format. Objects <b>must be Java Beans</b> (empty constructor, use
 * getters/setters), and contain properties of the following types: Lists, Maps,
 * String, byte[], Short, Integer, Long, Double, Float and, of course, other
 * nested beans. Map keys <b>must</b> always be Strings.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Property_list">Property list</a>
 */
public final class PListIO {

    /**
     * This is a final utility class, no constructor.
     */
    private PListIO() {
    }

    /**
     * Reads a previously saved configuration from a "plist" file.
     *
     * @param input The plist file.
     * @return The persisted object.
     * @throws IOException On error.
     */
    public static Object read(InputStream input) throws IOException {
        return PListReader.parse(input);
    }

    /**
     * Reads a previously saved configuration from a "plist" file.
     *
     * @param file The plist file.
     * @return The persisted object.
     * @throws IOException On error.
     */
    public static Object read(File file) throws IOException {
        return PListReader.parse(file);
    }

    /**
     * Writes an object to a "plist" file. NOTE: This recursively serializes
     * nested objects.
     * TODO: Should this be synchronized/atomic?
     *
     * @param file The file.
     * @param data the data to persist.
     * @throws IOException On error.
     */
    public static void write(File file, Object data) throws IOException {
        PListWriter.write(file, data);
    }

    /**
     * Writes an object to a "plist" file.
     *
     * @param output the OutputStream.
     * @param data the data to persist.
     * @throws IOException On error.
     */
    public static void write(OutputStream output, Object data) throws IOException {
        PListWriter.write(output, data);
    }

}
