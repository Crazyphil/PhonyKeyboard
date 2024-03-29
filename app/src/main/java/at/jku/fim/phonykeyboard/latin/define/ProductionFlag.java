/*
 * Copyright (C) 2012 The Android Open Source Project
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

package at.jku.fim.phonykeyboard.latin.define;

public final class ProductionFlag {
    private ProductionFlag() {
        // This class is not publicly instantiable.
    }

    public static final boolean USES_DEVELOPMENT_ONLY_DIAGNOSTICS = false;

    // When false, USES_DEVELOPMENT_ONLY_DIAGNOSTICS_DEBUG suggests that all guarded
    // class-private DEBUG flags should be false, and any privacy controls should be enforced.
    // USES_DEVELOPMENT_ONLY_DIAGNOSTICS must be false for any production build.
    public static final boolean USES_DEVELOPMENT_ONLY_DIAGNOSTICS_DEBUG = false;

    public static final boolean IS_HARDWARE_KEYBOARD_SUPPORTED = false;
}
