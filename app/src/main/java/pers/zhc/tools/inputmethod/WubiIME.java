package pers.zhc.tools.inputmethod;

import android.inputmethodservice.InputMethodService;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;
import pers.zhc.tools.test.wubiinput.WubiInput;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.sqlite.SQLite3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author bczhc
 */
public class WubiIME extends InputMethodService {
    /**
     * Chinese punctuation array
     */
    private final static String[] punctuationStrings = {
            "。",
            "，",
            "、",
            "【",
            "】",
            "/",
            "-",
            "=",
            "`"
    };

    /**
     * On-to-one correspondence with {@link WubiIME#punctuationStrings}
     */
    private final static int[] punctuationKeyCodes = {
            KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_COMMA,
            KeyEvent.KEYCODE_BACKSLASH,
            KeyEvent.KEYCODE_LEFT_BRACKET,
            KeyEvent.KEYCODE_RIGHT_BRACKET,
            KeyEvent.KEYCODE_SLASH,
            KeyEvent.KEYCODE_MINUS,
            KeyEvent.KEYCODE_EQUALS,
            KeyEvent.KEYCODE_GRAVE
    };
    /**
     * punctuations that needs to input with shift key
     */
    private final static String[] chinesePunctuationWithShiftStrings = {
            "《",
            "》",
            "？",
            "：",
            "！",
            "·",
            "#",
            "￥",
            "%",
            "……",
            "——",
            "*",
            "（",
            "）",
            "「",
            "」",
            "——",
            "+"
    };
    /**
     * On-to-one correspondence with {@link WubiIME#chinesePunctuationWithShiftStrings}
     */
    private final static int[] punctuationWithShiftKeyCodes = {
            KeyEvent.KEYCODE_COMMA,
            KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_SLASH,
            KeyEvent.KEYCODE_SEMICOLON,
            KeyEvent.KEYCODE_1,
            KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_7,
            KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9,
            KeyEvent.KEYCODE_0,
            KeyEvent.KEYCODE_LEFT_BRACKET,
            KeyEvent.KEYCODE_RIGHT_BRACKET,
            KeyEvent.KEYCODE_MINUS,
            KeyEvent.KEYCODE_EQUALS
    };
    /**
     * The keys only matter when composing, otherwise it'll be consumed by the next receiver.
     */
    private final static int[] keysMatterWhenComposing = {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_ENTER
    };
    private final StringBuilder wubiCodeSB = new StringBuilder();
    private final Quotation quotation = new Quotation();
    private final List<String> candidates = new ArrayList<>();
    private SQLite3 wubiDictDB = null;
    private TextView candidateTV, wubiCodeTV;
    private boolean alphabetMode = false;
    private InputConnection ic;
    private String lastWord;
    private TextToSpeech tts = null;
    private boolean tempEnglishMode = false;

    private boolean composing = false;
    private final KeyEventResolver keyEventResolver = new KeyEventResolver(new KeyEventResolverCallback() {
        @Override
        public boolean onKey(KeyEvent event) {
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            char c = (char) event.getUnicodeChar();

            if (keyCode == KeyEvent.KEYCODE_BACK && isInputViewShown()) {
                hideWindow();
                return true;
            }

            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (checkToSwitchTypingMode(event)) return true;
            }
            // some keys won't be consumed
            if (!checkIfConsumedKeys(event)) return false;
            InputRange inputRange = checkInputRange(keyCode);

            if (action == KeyEvent.ACTION_DOWN) {
                // number 0-9 keys
                if (inputRange == InputRange.NUM0_9 && !event.isShiftPressed() && !composing) {
                    return false;
                }
                if (event.isShiftPressed()) {
                    // on shift hold routines
                    for (int i = 0; i < punctuationWithShiftKeyCodes.length; i++) {
                        if (keyCode == punctuationWithShiftKeyCodes[i]) {
                            commitTheFirstCandidate();
                            commitText(chinesePunctuationWithShiftStrings[i]);
                            clear();
                            composing = false;
                            return true;
                        }
                    }
                    if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) {
                        commitTheFirstCandidate();
                        commitText(quotation.getDoubleQuotation());
                        clear();
                        composing = false;
                        return true;
                    }
                }
                if (!composing && inputRange == InputRange.A_Z) {
                    // start to input
                    composing = true;
                }
                if (!composing) {
                    // space
                    if (inputRange == InputRange.SPACE && !event.isShiftPressed()) {
                        commitText(" ");
                        return true;
                    }
                    // single quotation
                    if (inputRange == InputRange.APOSTROPHE) {
                        commitText(quotation.getSingleQuotation());
                    }
                    // check and commit Chinese punctuations
                    commitPunctuations(event);
                    if (inputRange == InputRange.SEMICOLON) {
                        // temporary English mode
                        wubiCodeTV.setText(R.string.temporary_english_mode);
                        tempEnglishMode = true;
                        composing = true;
                    }
                    return true;
                }
                // routines below are for composing
                if (!tempEnglishMode)
                    // Chinese input mode
                    switch (inputRange) {
                        case A_Z:
                            if (event.isShiftPressed()) {
                                // when capitalized, start temporary English input mode
                                tempEnglishMode = true;
                                composing = true;
                                candidateTV.setText(String.valueOf(c));
                                wubiCodeTV.setText(R.string.temporary_english_mode);
                                return true;
                            }
                            // on the fifth code
                            if (wubiCodeSB.length() == 4) {
                                commitTheFirstCandidate();
                                clear();
                            }
                            wubiCodeSB.append(c);
                            update();
                            // on the fourth code & have the only one candidate word
                            if (wubiCodeSB.length() == 4 && candidates.size() == 1) {
                                commitTheFirstCandidate();
                                clear();
                                composing = false;
                            }
                            break;
                        case PUNCTUATION:
                            commitTheFirstCandidate();
                            for (int i = 0; i < punctuationKeyCodes.length; i++) {
                                if (keyCode == punctuationKeyCodes[i]) {
                                    commitText(punctuationStrings[i]);
                                }
                            }
                            clear();
                            composing = false;
                            break;
                        case BACKSPACE:
                            Common.debugAssert(wubiCodeSB.length() != 0);
                            wubiCodeSB.deleteCharAt(wubiCodeSB.length() - 1);
                            update();
                            if (wubiCodeSB.length() == 0) composing = false;
                            break;
                        case SPACE:
                            commitTheFirstCandidate();
                            clear();
                            composing = false;
                            break;
                        // case SEMICOLON, APOSTROPHE: commit the second or the third candidate word
                        case SEMICOLON:
                            commitCandidate(1);
                            clear();
                            composing = false;
                            break;
                        case APOSTROPHE:
                            commitCandidate(2);
                            clear();
                            composing = false;
                            break;
                        case ENTER:
                            // clean wubi code
                            clear();
                            composing = false;
                            break;
                        case NUM0_9:
                            commitCandidate(keyCode - KeyEvent.KEYCODE_1);
                            clear();
                            composing = false;
                            break;
                        default:
                    }
                else {
                    // temporary English mode
                    switch (inputRange) {
                        case ENTER:
                            // temporary English mode, commit!
                            commitText(candidateTV.getText().toString());
                            clear();
                            composing = false;
                            tempEnglishMode = false;
                            break;
                        case BACKSPACE:
                            // delete the last character
                            CharSequence sequence = candidateTV.getText();
                            int length = sequence.length();
                            if (length == 0) {
                                // exit
                                tempEnglishMode = false;
                                composing = false;
                                clear();
                                return true;
                            }
                            CharSequence s = sequence.subSequence(0, length - 1);
                            candidateTV.setText(s);
                            break;
                        case SEMICOLON:
                            // commit Chinese semicolon and exit temporary English mode
                            if (candidateTV.getText().toString().isEmpty()) {
                                commitText("；");
                                composing = false;
                                tempEnglishMode = false;
                                clear();
                                return true;
                            }
                            // otherwise, input ";"
                        default:
                            // input alphabet
                            String text = candidateTV.getText().toString() + c;
                            candidateTV.setText(text);
                    }
                    return true;
                }
            }
            return true;
        }

        /**
         * Check if the keys should be consumed and handled.
         * @param event key event
         * @return {@code true} if should be consumed, {@code false} otherwise.
         */
        private boolean checkIfConsumedKeys(KeyEvent event) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_BACK) return false;

            // No key should be consumed by WubiIME when in alphabet mode, but SPACE should be consumed when having checked typing mode.
            if (alphabetMode) return false;
            if (!composing) {
                for (int code : keysMatterWhenComposing) {
                    if (keyCode == code) {
                        return false;
                    }
                }
            }

            InputRange inputRange = checkInputRange(keyCode);
            if (inputRange == InputRange.SHIFT) return false;
            return inputRange != InputRange.OTHERS;
        }

        @Override
        public void onShift(KeyEvent event) {
        }

        @Override
        public void onCtrl(KeyEvent event) {

        }
    });

    /**
     * Reset wubi code, candidate words, and input method text.
     */
    private void clear() {
        resetWubiCodeAndCandidates();
        clearInputMethodText();
    }

    /**
     * Check the key and switch typing mode.
     *
     * @param event key event
     * @return {@code true} if switched, {@code false} otherwise.
     */
    private boolean checkToSwitchTypingMode(@NotNull KeyEvent event) {
        if (event.isShiftPressed() && checkInputRange(event.getKeyCode()) == InputRange.SPACE) {
            alphabetMode = !alphabetMode;
            clearWubiCodeSB();
            clearInputMethodText();
            if (candidateTV != null) {
                candidateTV.setText(alphabetMode ? R.string.alphabet_mode : R.string.nul);
            }
            return true;
        }
        return false;
    }

    /**
     * Commit the punctuations ({@link WubiIME#punctuationStrings}) when not composing.
     *
     * @param event event
     */
    private void commitPunctuations(@NotNull KeyEvent event) {
        int keyCode = event.getKeyCode();
        for (int i = 0; i < punctuationKeyCodes.length; i++) {
            if (keyCode == punctuationKeyCodes[i]) {
                commitText(punctuationStrings[i]);
            }
        }
    }

    public enum InputRange {
        A_Z,
        PUNCTUATION,
        BACKSPACE,
        SPACE,
        SHIFT,
        SEMICOLON,
        APOSTROPHE,
        ENTER,
        NUM0_9,
        CTRL,
        SOME_MATTERS_WITH_SHIFT,
        OTHERS
    }

    /**
     * @param keyCode key code
     * @return {@link WubiIME.InputRange}
     */
    static InputRange checkInputRange(int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) return InputRange.A_Z;
        if (keyCode == KeyEvent.KEYCODE_DEL) return InputRange.BACKSPACE;
        if (keyCode == KeyEvent.KEYCODE_SPACE) return InputRange.SPACE;
        if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
            return InputRange.SHIFT;
        for (int punctuationKeyCode : punctuationKeyCodes) {
            if (punctuationKeyCode == keyCode) {
                return InputRange.PUNCTUATION;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_SEMICOLON) return InputRange.SEMICOLON;
        if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) return InputRange.APOSTROPHE;
        if (keyCode == KeyEvent.KEYCODE_ENTER) return InputRange.ENTER;
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) return InputRange.NUM0_9;
        if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT) return InputRange.CTRL;
        for (int code : punctuationWithShiftKeyCodes) {
            if (code == keyCode) return InputRange.SOME_MATTERS_WITH_SHIFT;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) return InputRange.SOME_MATTERS_WITH_SHIFT;
        return InputRange.OTHERS;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        ic = getCurrentInputConnection();
        quotation.reset();


        clear();
        if (alphabetMode) candidateTV.setText(R.string.alphabet_mode);
        tempEnglishMode = false;
        composing = false;
    }

    @Override
    public View onCreateInputView() {
        if (wubiDictDB == null) {
            wubiDictDB = WubiInput.getWubiDictDatabase(this);
        }
        return super.onCreateInputView();
    }

    @Override
    public View onCreateCandidatesView() {
        View candidateView = View.inflate(this, R.layout.wubi_input_method_candidate_view, null);
        candidateTV = candidateView.findViewById(R.id.candidates);
        wubiCodeTV = candidateView.findViewById(R.id.code);
        setCandidatesViewShown(true);
        return candidateView;
    }


    @Override
    public void onComputeInsets(InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (!isFullscreenMode()) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
        }
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        return true;
    }

    /**
     * Clear wubi code string and candidate word list
     */
    private void resetWubiCodeAndCandidates() {
        clearWubiCodeSB();
        candidates.clear();
    }

    /**
     * Clear the keyboard's candidate words and wubi code text
     */
    private void clearInputMethodText() {
        if (wubiCodeTV != null && candidateTV != null) {
            wubiCodeTV.setText(R.string.nul);
            candidateTV.setText(R.string.nul);
        }
    }

    /**
     * Set the keyboard candidate words and wubi code text
     */
    private void updateInputMethodText() {
        String wubiCodeStr = wubiCodeSB.toString();
        StringBuilder candidatesSB = new StringBuilder();
        for (int i = 0, candidatesSize = candidates.size() - 1; i < candidatesSize; i++) {
            String candidate = candidates.get(i);
            candidatesSB.append(candidate).append('|');
        }
        if (candidates.size() > 0) candidatesSB.append(candidates.get(candidates.size() - 1));

        if (wubiCodeTV != null && candidateTV != null) {
            wubiCodeTV.setText(wubiCodeStr);
            candidateTV.setText(candidatesSB.toString());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyEventResolver.onKeyDown(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return keyEventResolver.onKeyUp(event);
    }

    private void commitTheFirstCandidate() {
        if (ic != null) {
            if (candidates.size() != 0) commitCandidate(0);
        }
    }

    private void commitText(String s) {
        if (ic != null) {
            ic.commitText(s, 1/*a value that > 0*/);
            lastWord = s;
            if (WubiInputMethodTTSSettingActivity.isEnabledTTS()) {
                if (tts == null) {
                    tts = new TextToSpeech(this, null);
                }
                tts.speak(s, TextToSpeech.QUEUE_ADD, null, String.valueOf(System.currentTimeMillis()));
            } else tts = null;
        }
    }

    /**
     * Commit a candidate word
     *
     * @param pos candidate word index (start from 0)
     */
    private void commitCandidate(int pos) {
        if (pos >= candidates.size() || pos < 0) {
            return;
        }
        commitText(candidates.get(pos));
    }

    private void clearWubiCodeSB() {
        wubiCodeSB.delete(0, wubiCodeSB.length());
    }

    /**
     * Get candidate words and insert it to {@code candidates}.
     *
     * @param wubiCodeStr wubi code string
     */
    private void fetchCandidatesAndSetToField(@NotNull String wubiCodeStr) {
        candidates.clear();
        if (wubiCodeStr.isEmpty()) return;
        //Z key: repeat last word
        if (wubiCodeSB.toString().equals("z")) {
            candidates.add(lastWord);
        } else try {
            wubiDictDB.exec(String.format("SELECT word FROM wubi_code_%s WHERE code IS '%s'"
                    , wubiCodeStr.charAt(0), wubiCodeStr), contents -> {
                String[] split = contents[0].split("\\|");
                candidates.addAll(Arrays.asList(split));
                return 0;
            });
        } catch (Exception ignored) {
            //no such table xxx
        }
    }

    /**
     * Fetch candidates and update keyboard's display
     */
    private void update() {
        fetchCandidatesAndSetToField(wubiCodeSB.toString());
        updateInputMethodText();
    }

    private static class Quotation {
        private final String[] singleQuotation = {"‘", "’"};
        private final String[] doubleQuotation = {"“", "”"};
        private int singleQuotationIndex = 0, doubleQuotationIndex = 0;

        private String getSingleQuotation() {
            String r = singleQuotation[singleQuotationIndex];
            singleQuotationIndex = 1 - singleQuotationIndex;
            return r;
        }

        private String getDoubleQuotation() {
            String r = doubleQuotation[doubleQuotationIndex];
            doubleQuotationIndex = 1 - doubleQuotationIndex;
            return r;
        }

        private void reset() {
            singleQuotationIndex = 0;
            doubleQuotationIndex = 0;
        }
    }
}