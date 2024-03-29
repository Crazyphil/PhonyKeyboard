/*
 * Copyright (C) 2011 The Android Open Source Project
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

package at.jku.fim.phonykeyboard.latin.suggestions;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import at.jku.fim.phonykeyboard.keyboard.Keyboard;
import at.jku.fim.phonykeyboard.keyboard.MoreKeysKeyboardView;
import at.jku.fim.phonykeyboard.latin.R;
import at.jku.fim.phonykeyboard.latin.SuggestedWords;
import at.jku.fim.phonykeyboard.latin.suggestions.MoreSuggestions.MoreSuggestionsListener;

/**
 * A view that renders a virtual {@link MoreSuggestions}. It handles rendering of keys and detecting
 * key presses and touch movements.
 */
public final class MoreSuggestionsView extends MoreKeysKeyboardView {
    private static final String TAG = MoreSuggestionsView.class.getSimpleName();

    public MoreSuggestionsView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.moreKeysKeyboardViewStyle);
    }

    public MoreSuggestionsView(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected int getDefaultCoordX() {
        final MoreSuggestions pane = (MoreSuggestions)getKeyboard();
        return pane.mOccupiedWidth / 2;
    }

    public void updateKeyboardGeometry(final int keyHeight) {
        updateKeyDrawParams(keyHeight);
    }

    public void adjustVerticalCorrectionForModalMode() {
        // Set vertical correction to zero (Reset more keys keyboard sliding allowance
        // {@link R#dimen.more_keys_keyboard_slide_allowance}).
        mKeyDetector.setKeyboard(getKeyboard(), -getPaddingLeft(), -getPaddingTop());
    }

    @Override
    public void onCodeInput(final int code, final int x, final int y) {
        final Keyboard keyboard = getKeyboard();
        if (!(keyboard instanceof MoreSuggestions)) {
            Log.e(TAG, "Expected keyboard is MoreSuggestions, but found "
                    + keyboard.getClass().getName());
            return;
        }
        final SuggestedWords suggestedWords = ((MoreSuggestions)keyboard).mSuggestedWords;
        final int index = code - MoreSuggestions.SUGGESTION_CODE_BASE;
        if (index < 0 || index >= suggestedWords.size()) {
            Log.e(TAG, "Selected suggestion has an illegal index: " + index);
            return;
        }
        if (!(mListener instanceof MoreSuggestionsListener)) {
            Log.e(TAG, "Expected mListener is MoreSuggestionsListener, but found "
                    + mListener.getClass().getName());
            return;
        }
        ((MoreSuggestionsListener)mListener).onSuggestionSelected(
                index, suggestedWords.getInfo(index));
    }
}
