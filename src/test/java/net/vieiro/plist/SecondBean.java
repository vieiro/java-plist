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

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class SecondBean {

    public enum EnumValue {
        A,
        B,
        C
    }

    private int integerNumber;
    private long longNumber;
    private Set<String> set;
    private EnumValue value;

    public SecondBean() {
        set = new HashSet<>();
        value = EnumValue.B;
    }

    public int getIntegerNumber() {
        return integerNumber;
    }

    public void setIntegerNumber(int integerNumber) {
        this.integerNumber = integerNumber;
    }

    public long getLongNumber() {
        return longNumber;
    }

    public void setLongNumber(long longNumber) {
        this.longNumber = longNumber;
    }

    public Set<String> getSet() {
        return set;
    }

    public void setSet(Set<String> set) {
        this.set = set;
    }

    public void setEnumValue(EnumValue value) {
        this.value = value;
    }

    public EnumValue getEnumValue() {
        return this.value;
    }
    
}
