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
package net.vieiro.plist.impl;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import static net.vieiro.plist.impl.PListLiterals.DATA;
import static net.vieiro.plist.impl.PListLiterals.DATE;
import static net.vieiro.plist.impl.PListLiterals.FALSE;
import static net.vieiro.plist.impl.PListLiterals.INTEGER;
import static net.vieiro.plist.impl.PListLiterals.PLIST;
import static net.vieiro.plist.impl.PListLiterals.REAL;
import static net.vieiro.plist.impl.PListLiterals.STRING;
import static net.vieiro.plist.impl.PListLiterals.TRUE;
import static net.vieiro.plist.impl.PListLiterals.KEY_PLIST_CLASS;
import static net.vieiro.plist.impl.PListLiterals.KEY_PLIST_ENUM;

/**
 *
 */
public final class PListWriter {

    private static final Logger LOG = Logger.getLogger(PListWriter.class.getName());

    public static void write(File file, Object configuration) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            write(bos, configuration);
        }
    }

    public static void write(OutputStream bos, Object configuration) throws IOException {
        try (Writer writer = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
            write(writer, configuration);
        }
    }

    public static void write(Writer writer, Object configuration) throws IOException {
        try {
            XMLOutputFactory factory = XMLOutputFactory.newFactory();
            XMLStreamWriter streamWriter = factory.createXMLStreamWriter(writer);
            write(streamWriter, configuration);
        } catch (XMLStreamException ex) {
            throw new IOException(String.format("Error writing configuration %s (%s)", ex.getMessage(), ex.getClass().getName()), ex);
        }
    }

    private static void write(XMLStreamWriter writer, Object configuration) throws IOException {
        try {
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeDTD("\n<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
            writer.writeStartElement(PLIST);
            writeObjectDetails(writer, configuration);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
        } catch (Exception ex) {
            String message = String.format("Error writing configuration %s (%s)", ex.getMessage(), ex.getClass().getName());
            LOG.log(Level.SEVERE, message, ex);
            throw new IOException(message);
        }
    }

    private static void writeObjectDetails(XMLStreamWriter writer, Object value) throws Exception {
        if (value instanceof Map) {
            Map map = (Map) value;
            writeMap(writer, map);
        } else if (value instanceof List) {
            List list = (List) value;
            writeArray(writer, list);
        } else if (value instanceof Set) {
            Set set = (Set) value;
            writeSet(writer, set);
        } else if (value instanceof Boolean) {
            Boolean b = (Boolean) value;
            writer.writeEmptyElement(b ? TRUE : FALSE);
        } else if (value instanceof String) {
            writer.writeStartElement(STRING);
            writer.writeCharacters(Objects.toString(value));
            writer.writeEndElement();
        } else if (value instanceof Long) {
            writer.writeStartElement(INTEGER);
            writer.writeCharacters(Objects.toString(value));
            writer.writeEndElement();
        } else if (value instanceof Integer) {
            writer.writeStartElement(INTEGER);
            writer.writeCharacters(Objects.toString(value));
            writer.writeEndElement();
        } else if (value instanceof Short) {
            writer.writeStartElement(INTEGER);
            writer.writeCharacters(Objects.toString(value));
            writer.writeEndElement();
        } else if (value instanceof Float) {
            writer.writeStartElement(REAL);
            writer.writeCharacters(Objects.toString(value));
            writer.writeEndElement();
        } else if (value instanceof Double) {
            Double d = (Double) value;
            writer.writeStartElement(REAL);
            writer.writeCharacters(Objects.toString(d));
            writer.writeEndElement();
        } else if (value instanceof Instant) {
            Instant i = (Instant) value;
            writer.writeStartElement(DATE);
            writer.writeCharacters(i.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
            writer.writeEndElement();
        } else if (value instanceof byte[]) {
            byte [] b = (byte[]) value;
            writer.writeStartElement(DATA);
            writer.writeCharacters(Base64.getEncoder().encodeToString(b));
            writer.writeEndElement();
        } else if (value instanceof Enum) {
            Enum e = (Enum) value;
            writeEnum(writer, e);
        } else {
            writeBean(writer, value);
        }
        // This newline is not a proper XML indent, but it's fast, makes
        // compact XML and is more readable than having the whole file in a single line.
        writer.writeCharacters("\n");
    }

    /**
     * Retrieves the properties of a Bean using introspection, and maps them to
     * appropriate plist values. Files and Paths are always mapped to string,
     * int and short to longs, etc.
     *
     * @param bean
     * @return
     * @throws IOException
     */
    private static Map<String, Object> getBeanPropertyValues(Object bean) throws IOException {
        String propertyName = null;
        try {
            BeanInfo info = Introspector.getBeanInfo(bean.getClass());
            HashMap<String, Object> properties = new HashMap<>();
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                propertyName = pd.getName();
                Method getter = pd.getReadMethod();
                Object value = getter.invoke(bean);
                if (value != null) {
                    Class propertyType = pd.getPropertyType();
                    if (Class.class.equals(propertyType)) {
                        continue;
                    }
                    Object coercedValue = value;
                    // Coerce integer and short to long
                    // Coerce Class to class propertyName
                    // Coerce Float do Double
                    if (Class.class.equals(propertyType)) {
                        coercedValue = ((Class) value).getName();
                    } else if (Integer.class.equals(propertyType) || int.class.equals(propertyType)) {
                        coercedValue = ((Integer) value).longValue();
                    } else if (Short.class.equals(propertyType) || short.class.equals(propertyType)) {
                        coercedValue = ((Short) value).longValue();
                    } else if (Float.class.equals(propertyType) || float.class.equals(propertyType)) {
                        coercedValue = ((Float) value).doubleValue();
                    } else if (File.class.equals(propertyType)) {
                        coercedValue = ((File) value).getAbsolutePath();
                    } else if (Path.class.equals(propertyType)) {
                        coercedValue = ((Path) value).toString();
                    }
                    properties.put(propertyName, coercedValue);
                }
            }
            properties.put(KEY_PLIST_CLASS, bean.getClass().getName());
            return properties;
        } catch (Exception ex) {
            String message = String.format("Error reading property '%s' from object: %s (%s)",
                    propertyName, ex.getMessage(), ex.getClass().getName());
            throw new IOException(message, ex);
        }
    }

    private static void writeEnum(XMLStreamWriter writer, Enum<?> e) throws Exception {
        Map<String, Object> enumProperties = new HashMap<>();
        enumProperties.put(KEY_PLIST_ENUM, e.getClass().getName());
        enumProperties.put("name", e.name());
        writeMap(writer, enumProperties);
    }

    private static void writeBean(XMLStreamWriter writer, Object object) throws Exception {
        Map<String, Object> beanProperties = getBeanPropertyValues(object);
        writeMap(writer, beanProperties);
    }

    private static void writeMap(XMLStreamWriter writer, Map map) throws Exception {
        writer.writeStartElement("dict");
        for (Object key : map.keySet()) {
            writer.writeStartElement("key");
            writer.writeCharacters(Objects.toString(key));
            writer.writeEndElement();
            writeObjectDetails(writer, map.get(key));
        }
        writer.writeEndElement();
    }

    private static void writeArray(XMLStreamWriter writer, List array) throws Exception {
        writer.writeStartElement("array");
        for (Object value : array) {
            writeObjectDetails(writer, value);
        }
        writer.writeEndElement();
    }

    private static void writeSet(XMLStreamWriter writer, Set set) throws Exception {
        writer.writeStartElement("array");
        for (Object value : set) {
            writeObjectDetails(writer, value);
        }
        writer.writeEndElement();
    }

}
