package pers.zhc.tools.inputmethod;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import pers.zhc.tools.R;
import pers.zhc.tools.test.wubiinput.WubiInput;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.sqlite.MySQLite3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WubiIME extends InputMethodService {
    private final StringBuilder wubiCodeSB = new StringBuilder();
    private MySQLite3 wubiDictDB = null;
    private TextView candidateTV, wubiCodeTV;
    private boolean isAlphabetsMode = false, switchTypingMode = false;
    private InputConnection ic;
    private int inputRangeCode;
    private String lastWord;

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        ic = getCurrentInputConnection();
    }

    @Override
    public View onCreateInputView() {
        if (wubiDictDB == null) {
            wubiDictDB = WubiInput.getWubiDictDatabase(this);
        }
        View candidateView = View.inflate(this, R.layout.wubi_input_method_candidate_view, null);
        candidateTV = candidateView.findViewById(R.id.candidates);
        wubiCodeTV = candidateView.findViewById(R.id.code);
        setCandidatesView(candidateView);
        setCandidatesViewShown(true);
        return super.onCreateInputView();
    }

    private final static String[] chinesePunctuationStrings = {
            "。",
            "，",
            "、"
    };

    /**
     * On-to-one correspondence with {@link WubiIME#chinesePunctuationStrings}
     */
    private final static int[] punctuationKeyCodes = {
            KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_COMMA,
            KeyEvent.KEYCODE_BACKSLASH
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
            "…",
            "—",
            "*",
            "（",
            "）"
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
    };

//    private static final String shiftWithNumberInChinese = "！·#￥%…—*（）—+";

    /**
     * The keys only matter when composing, otherwise it'll be consumed by the next receiver.
     */
    private final static int[] keysMatterWhenComposing = {
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_ENTER
    };


    /**
     * @param keyCode key code
     * @return <p>
     * 1: A-Z
     * 2: punctuation
     * 3: backspace
     * 4: space
     * 5: shift
     * 6: semicolon
     * 7: apostrophe
     * 9: enter
     * 10: 0-9
     * 8: someMattersWithShift, preventing returning 0 and will be consumed by the next receiver.
     * 0: others
     * </p>
     */
    static int checkInputRange(int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) return 1;
        if (keyCode == KeyEvent.KEYCODE_DEL) return 3;
        if (keyCode == KeyEvent.KEYCODE_SPACE) return 4;
        if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) return 5;
        for (int punctuationKeyCode : punctuationKeyCodes) {
            if (punctuationKeyCode == keyCode) {
                return 2;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_SEMICOLON) return 6;
        if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) return 7;
        if (keyCode == KeyEvent.KEYCODE_ENTER) return 9;
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) return 10;

        for (int code : punctuationWithShiftKeyCodes) {
            if (code == keyCode) return 8;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) return 8;
        return 0;
    }

    private final List<String> candidates = new ArrayList<>();
    //    private boolean switchTypingMode = true;
    private boolean composing = false;
    private final KeyEventResolver keyEventResolver = new KeyEventResolver(new KeyEventResolverCallback() {
        @Override
        public void onKeyDown(KeyEvent event) {
            char c = (char) event.getUnicodeChar();
            int keyCode = event.getKeyCode();
            if (!composing) {
                //start to input
                if (inputRangeCode == 1/*A-Z*/) composing = true;
                resetWubiCodeAndCandidates();
            }
            switch (inputRangeCode) {
                case 1:
                    //A-Z
                    //on the fifth code
                    if (wubiCodeSB.length() == 4) {
                        commitTheFirstCandidate();
                        resetWubiCodeAndCandidates();
                    }
                    wubiCodeSB.append(c);
                    update();
                    //on the fourth code & have the only one candidate word
                    if (wubiCodeSB.length() == 4 && candidates.size() == 1) {
                        commitTheFirstCandidate();
                        clearInputMethodText();
                        composing = false;
                    }
                    break;
                case 2:
                    //punctuation
                    if (keyEventResolver.isHoldShift()) break;
                    commitTheFirstCandidate();
                    for (int i = 0; i < punctuationKeyCodes.length; i++) {
                        if (keyCode == punctuationKeyCodes[i]) {
                            commitText(chinesePunctuationStrings[i]);
                        }
                    }
                    clearInputMethodText();
                    composing = false;
                    break;
                case 3:
                    //backspace
                    Common.debugAssert(composing);
                    wubiCodeSB.deleteCharAt(wubiCodeSB.length() - 1);
                    update();
                    if (wubiCodeSB.length() == 0) composing = false;
                    break;
                case 4:
                    //space
                    commitTheFirstCandidate();
                    clearInputMethodText();
                    composing = false;
                    break;
                //case 6, 7: commit the second or the third candidate word
                case 6:
                    if (composing) {
                        commitCandidate(1);
                        clearInputMethodText();
                        composing = false;
                    } else if (!keyEventResolver.isHoldShift()) commitText("；");
                    break;
                case 7:
                    if (composing) {
                        commitCandidate(2);
                        clearInputMethodText();
                        composing = false;
                    } else {
                        if (keyEventResolver.isHoldShift()) {
                            commitText("“”");
                        } else {
                            commitText("‘’");
                        }
                        moveLeft();
                    }
                    break;
                case 9:
                    //enter
                    //clean wubi code
                    clearInputMethodText();
                    composing = false;
                    break;
            }
        }

        @Override
        public void onKeyDownWithShift(KeyEvent event) {
            int keyCode = event.getKeyCode();
            for (int i = 0; i < punctuationWithShiftKeyCodes.length; i++) {
                if (keyCode == punctuationWithShiftKeyCodes[i]) {
                    commitTheFirstCandidate();
                    commitText(chinesePunctuationWithShiftStrings[i]);
                    clearInputMethodText();
                    composing = false;
                }
            }
        }

        @Override
        public void onkeyUp(KeyEvent event) {
        }

        @Override
        public void onShift(KeyEvent event) {
            int action = event.getAction();
            if (action == KeyEvent.ACTION_DOWN) {
                //down
                switchTypingMode = true;
            } else {
                if (switchTypingMode) {
                    isAlphabetsMode = !isAlphabetsMode;
                }
                //up
                candidateTV.setText(isAlphabetsMode ? R.string.alphabet_mode : R.string.nul);
            }
        }
    });

    private void moveLeft() {
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
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
        wubiCodeTV.setText(R.string.nul);
        candidateTV.setText(R.string.nul);
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

        wubiCodeTV.setText(wubiCodeStr);
        candidateTV.setText(candidatesSB.toString());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (checkInputRange(keyCode) != 5/*shift*/) {
            //cancel the switch of typing mode
            switchTypingMode = false;
        }
        if (checkInputRange(keyCode) != 5 && isAlphabetsMode) return false;
        if (!composing) {
            for (int code : keysMatterWhenComposing) {
                if (keyCode == code) {
                    return false;
                }
            }
        }
        inputRangeCode = checkInputRange(keyCode);
        keyEventResolver.onKeyDown(event);
        return inputRangeCode != 0;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        keyEventResolver.onKeyUp(event);
        return inputRangeCode != 0;
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
        }
    }

    /**
     * Commit a candidate word
     *
     * @param pos candidate word index (start from 0)
     */
    private void commitCandidate(int pos) {
        if (pos >= candidates.size()) {
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
    private void fetchCandidatesAndSetToField(String wubiCodeStr) {
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
}