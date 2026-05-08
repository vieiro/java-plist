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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import static net.vieiro.plist.impl.PListLiterals.ARRAY;
import static net.vieiro.plist.impl.PListLiterals.DATA;
import static net.vieiro.plist.impl.PListLiterals.DATE;
import static net.vieiro.plist.impl.PListLiterals.DICT;
import static net.vieiro.plist.impl.PListLiterals.FALSE;
import static net.vieiro.plist.impl.PListLiterals.INTEGER;
import static net.vieiro.plist.impl.PListLiterals.KEY;
import static net.vieiro.plist.impl.PListLiterals.PLIST;
import static net.vieiro.plist.impl.PListLiterals.PUBLIC_ID;
import static net.vieiro.plist.impl.PListLiterals.REAL;
import static net.vieiro.plist.impl.PListLiterals.STRING;
import static net.vieiro.plist.impl.PListLiterals.SYSTEM_ID;
import static net.vieiro.plist.impl.PListLiterals.TRUE;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import static net.vieiro.plist.impl.PListLiterals.KEY_PLIST_CLASS;
import static net.vieiro.plist.impl.PListLiterals.KEY_PLIST_ENUM;

/**
 *
 */
public class PListReader {

    private static final Logger LOG = Logger.getLogger(PListReader.class.getName());
    private static final Level DEBUG = Level.FINE;

    public static class PListParser extends DefaultHandler {

        private static final Object UNCLOSED_ARRAY = new Object();
        private static final Object UNCLOSED_DICT = new Object();

        private ArrayList<Object> stack;
        private StringBuilder characters = null;

        public PListParser() {

        }

        private void push(Object o) throws SAXException {
            if (LOG.isLoggable(DEBUG)) {
                LOG.log(DEBUG, String.format("Pushing object <%s>", Objects.toString(o)));
            }
            stack.add(o);
        }

        private Object pop() throws SAXException {
            if (stack.isEmpty()) {
                throw new SAXException("Empty stack");
            }
            Object pop = stack.remove(stack.size() - 1);
            if (LOG.isLoggable(DEBUG)) {
                LOG.log(DEBUG, "Popping <%s>", Objects.toString(pop));
            }
            return pop;
        }

        @Override
        public void startElement(String uri, String localName,
                String qName, Attributes attributes) throws SAXException {
            if (LOG.isLoggable(DEBUG)) {
                LOG.log(DEBUG, String.format("Start element <%s>", qName));
            }
            if (PLIST.equals(localName)) {
                if (!stack.isEmpty()) {
                    throw new SAXException("Incorrect <plist> element detected");
                }
                String version = attributes.getValue("version");
                if (!"1.0".equals(version)) {
                    throw new SAXException(String.format("Incorrect version '%s'", version));
                }
            }
            characters = new StringBuilder();
            switch (localName) {
                case ARRAY:
                    push(UNCLOSED_ARRAY);
                    break;
                case DICT:
                    push(UNCLOSED_DICT);
                    break;
            }
        }

        @SuppressWarnings("unchecked")
        private void collectAndPushArray() throws SAXException {
            if (stack.isEmpty()) {
                throw new SAXException("Empty stack");
            }
            int i = stack.size() - 1;
            for (; stack.get(i) != UNCLOSED_ARRAY; i--) {

            }
            ArrayList array = new ArrayList();
            int n = stack.size();
            for (int j = i + 1; j < n; j++) {
                array.add(pop());
            }
            Collections.reverse(array);
            pop(); // The sentinel
            push(array);
        }

        @SuppressWarnings("unchecked")
        private void collectAndPushDictionary() throws SAXException {
            int i = stack.size() - 1;
            for (; stack.get(i) != UNCLOSED_DICT; i--) {

            }
            HashMap map = new HashMap();
            int n = stack.size();
            for (int j = i + 1; j < n; j += 2) {
                Object value = pop();
                String key = (String) pop();
                map.put(key, value);
            }
            pop(); // The sentinel
            if (map.containsKey(KEY_PLIST_CLASS)) {
                try {
                    Object bean = createBeanFromDict(map);
                    push(bean);
                } catch (IOException ex) {
                    throw new SAXException(String.format("Could not deserialize object: %s (%s)",
                            ex.getMessage(),
                            ex.getClass().getName()), ex);
                }
            } else if (map.containsKey(KEY_PLIST_ENUM)) {
                try {
                    Object enumValue = createEnumFromDict(map);
                    push(enumValue);
                } catch (IOException ex) {
                    throw new SAXException(String.format("Could not deserialize enum: %s (%s)",
                            ex.getMessage(),
                            ex.getClass().getName()), ex);
                }
            } else {
                push(map);
            }
        }

        @Override
        public void endElement(String uri, String localName,
                String qName) throws SAXException {
            if (LOG.isLoggable(DEBUG)) {
                LOG.log(DEBUG, String.format("End element <%s>", qName));
            }
            String textContent = characters == null ? null : characters.toString();
            Object newValue = getObject(localName, textContent);
            if (DICT.equals(localName)) {
                collectAndPushDictionary();
            } else if (ARRAY.equals(localName)) {
                collectAndPushArray();
            } else {
                if (!PLIST.equals(localName)) {
                    push(newValue);
                }
            }
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            assert characters != null;
            characters.append(ch, start, length);
        }

        @Override
        public void startDocument() throws SAXException {
            stack = new ArrayList<>();
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
            if (PUBLIC_ID.equals(publicId) && SYSTEM_ID.equals(systemId)) {
                InputStream dtd = PListParser.class.getResourceAsStream("plist.dtd");
                assert dtd != null;
                return new InputSource(dtd);
            }
            throw new IOException(String.format("Incorrect publicID '%s' or systemID '%s'", publicId, systemId));
        }

        private static Object getObject(String localName, String textContent) throws SAXException {
            Object newValue = null;
            switch (localName) {
                case TRUE:
                    newValue = Boolean.TRUE;
                    break;
                case FALSE:
                    newValue = Boolean.FALSE;
                    break;
                case STRING:
                    if (textContent == null) {
                        throw new SAXException("String with null content");
                    }
                    newValue = textContent;
                    break;
                case REAL:
                    if (textContent == null) {
                        throw new SAXException("Real with null content");
                    }
                    newValue = Double.valueOf(textContent);
                    break;
                case INTEGER:
                    if (textContent == null) {
                        throw new SAXException("Integer with null content");
                    }
                    newValue = Long.valueOf(textContent);
                    break;
                case KEY:
                    if (textContent == null) {
                        throw new SAXException("key with null content");
                    }
                    newValue = textContent;
                    break;
                case DATA:
                    if (textContent == null) {
                        throw new SAXException("data with null content");
                    }
                    newValue = Base64.getDecoder().decode(textContent.trim());
                    break;
                case DATE:
                    if (textContent == null) {
                        throw new SAXException("date with null content");
                    }
                    newValue = Instant.parse(textContent);
                    break;
            }
            return newValue;
        }

        public Object getResult() throws SAXException {
            if (stack.isEmpty()) {
                return null;
            }
            if (stack.size() > 1) {
                String firstStackObjectType = Objects.toString(stack);
                String message = String.format("Configuration file corrupt. Stack contains %d elements. First is %s",
                        stack.size(), firstStackObjectType);
                LOG.log(Level.INFO, message);
                throw new SAXException(message);
            }
            return stack.get(0);
        }

    }

    public static Object parse(File file) throws IOException {
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
            return parse(input);
        }
    }

    public static Object parse(InputStream inputStream) throws IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            PListParser list = new PListParser();
            parser.parse(inputStream, list);
            return list.getResult();
        } catch (ParserConfigurationException ex) {
            throw new IOException(String.format("XML configuration exception %s (%s)", ex.getMessage(), ex.getClass().getName()), ex);
        } catch (SAXException ex) {
            throw new IOException(String.format("Corrupt configuration file %s (%s)", ex.getMessage(), ex.getClass().getName()), ex);
        }
    }

    /**
     * Given a map with a KEY_PLIST_ENUM pointing to a name, it tries to recover
     * the enum value
     *
     * @param properties The properties (including a KEY_PLIST_ENUM with a Enum
     * class and a "name" property with the enum value).
     * @return An enum value of the given type.
     * @throws IOException on I/O error.
     */
    private static Object createEnumFromDict(Map<String, Object> properties) throws IOException {
        String enumClassName = (String) properties.get(KEY_PLIST_ENUM);
        if (enumClassName == null) {
            throw new IOException("Dictionary is missing a '" + KEY_PLIST_ENUM + "' class name");
        }
        String enumValue = (String) properties.get("name"); // NOI18N
        if (enumValue == null) {
            throw new IOException("Dictionary contains a '" + KEY_PLIST_ENUM + "' but has not a 'name' with the value");
        }
        try {
            // TODO: Verify this works well with different classloaders
            Class enumClass = Thread.currentThread().getContextClassLoader().loadClass(enumClassName);
            Method nameMethod = Enum.class.getMethod("name");
            Object[] constants = enumClass.getEnumConstants();
            for (Object constant : constants) {
                String constantName = (String) nameMethod.invoke(constant);
                if (enumValue.equals(constantName)) {
                    return constant;
                }
            }
        } catch (Exception ex) {
            String message = String.format("Error writing enum: %s (%s)",
                    ex.getMessage(), ex.getClass().getName());
            throw new IOException(message, ex);
        }
        throw new IOException(String.format("Could not deserialize enum of type %s because it has not a constant with name '%s'", enumClassName, enumValue));
    }

    /**
     * Given a map with some property values and a KEY_PLIST_CLASS pointing to a
     * class name, this converts the map to the bean, populating the properties.
     *
     * @param properties The properties (including a KEY_PLIST_CLASS pointing to
     * a valid class name).
     * @return A bean of the given class, with the properties populated.
     * @throws IOException on I/O error.
     */
    @SuppressWarnings("unchecked")
    private static Object createBeanFromDict(Map<String, Object> properties) throws IOException {
        String propertyName = null;
        String beanClassName = (String) properties.get(KEY_PLIST_CLASS);
        if (beanClassName == null) {
            throw new IOException("Dictionary is missing a '" + KEY_PLIST_CLASS + "' class name");
        }
        try {
            // TODO: Verify this works well with different classloaders
            Class beanClass = Thread.currentThread().getContextClassLoader().loadClass(beanClassName);
            Constructor newInstance = null;
            try {
                newInstance = beanClass.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IOException(String.format("Java class %s is not a Java Bean: it's missing an empty constructor", beanClassName));
            }
            Object bean = newInstance.newInstance();
            BeanInfo info = Introspector.getBeanInfo(beanClass);
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                propertyName = pd.getName();
                Object value = properties.get(propertyName);
                if (value != null) {
                    Method setter = pd.getWriteMethod();
                    if (setter == null) {
                        continue;
                    }
                    // Coerce "long" in <integer> to appropriate "integer" or "short" variants
                    // Also coerce "real" in <real> to appropriate "float" or "double" variants
                    Object coercedValue = value;
                    Class propertyType = pd.getPropertyType();
                    if (Integer.class.equals(propertyType)
                            || int.class.equals(propertyType)) {
                        coercedValue = ((Long) value).intValue();
                    } else if (Short.class.equals(propertyType)
                            || short.class.equals(propertyType)) {
                        coercedValue = ((Long) value).shortValue();
                    } else if (Float.class.equals(propertyType)) {
                        coercedValue = ((Double) value).floatValue();
                    } else if (Class.class.equals(propertyType)) {
                        coercedValue = ((Class) value).getName();
                    } else if (propertyType.isAssignableFrom(Set.class) && (value instanceof List)) {
//                    } else if (Set.class.equals(propertyType) && (value instanceof List list)) {
                        // Coerce an 'Array' to a 'Set'
                        List list = (List) value;
                        coercedValue = list.stream().collect(Collectors.toSet());
                    } else if (File.class.equals(propertyType)) {
                        // Coerce Strings back to Files if required
                        coercedValue = new File((String) value);
                    } else if (Path.class.equals(propertyType)) {
                        coercedValue = Paths.get((String) value);
                    }
                    if (!Class.class.equals(propertyType)) {
                        setter.invoke(bean, coercedValue);
                    }
                }
            }
            return bean;
        } catch (Exception ex) {
            String message = null;
            if (propertyName == null) {
                message = String.format("Error writing object: %s (%s)",
                        ex.getMessage(), ex.getClass().getName());
            } else {
                message = String.format("Error writing property '%s' from object: %s (%s)",
                        propertyName,
                        ex.getMessage(), ex.getClass().getName());
            }
            throw new IOException(message, ex);
        }
    }

}
