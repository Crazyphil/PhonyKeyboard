/*
 * Copyright (C) 2013 The Android Open Source Project
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

package at.jku.fim.phonykeyboard.latin.makedict;

import at.jku.fim.phonykeyboard.latin.makedict.FormatSpec.FormatOptions;
import at.jku.fim.phonykeyboard.latin.makedict.FusionDictionary.PtNode;

import java.io.IOException;

/**
 * An interface of binary dictionary encoder.
 */
public interface DictEncoder {
    public void writeDictionary(final FusionDictionary dict, final FormatOptions formatOptions)
            throws IOException, UnsupportedFormatException;

    public void setPosition(final int position);
    public int getPosition();
    public void writePtNodeCount(final int ptNodeCount);
    public void writeForwardLinkAddress(final int forwardLinkAddress);

    public void writePtNode(final PtNode ptNode, final int parentPosition,
            final FormatOptions formatOptions, final FusionDictionary dict);
}
