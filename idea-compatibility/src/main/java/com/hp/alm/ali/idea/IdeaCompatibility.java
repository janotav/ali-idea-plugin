/*
 * Copyright 2013 Hewlett-Packard Development Company, L.P
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.alm.ali.idea;

/**
 * Allows to register/access APIs that are incompatible across Intellij versions
 */
public interface IdeaCompatibility {

    /**
     * Given implementation is registered if baseline of the runtime is greater or equal than given baseline and
     * no other implementation with higher baseline was registered.
     *
     * @param inf interface
     * @param impl actual implementation
     * @param baseline baseline compatible with the implementation
     * @return true if implementation was registered (possibly overriding previous registration)
     */
    boolean register(Class inf, Class impl, int baseline);

    /**
     * @param inf interface
     * @return implementation with highest registered baseline
     */
    <E> E getComponent(Class<E> inf);

}
