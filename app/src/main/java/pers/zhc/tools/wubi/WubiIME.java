package pers.zhc.tools.wubi;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.views.SmartHintEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author bczhc
 */
public class WubiIME extends InputMethodService {
    private View candidateView = null;

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
     * One-to-one correspondence with {@link WubiIME#punctuationStrings}
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
     * One-to-one correspondence with {@link WubiIME#chinesePunctuationWithShiftStrings}
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
                if (event.isShiftPressed() && !tempEnglishMode) {
                    // on shift held routines
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
                if (event.isCtrlPressed() && !tempEnglishMode) {
                    switch (inputRange) {
                        case SEMICOLON:
                            commitTheFirstCandidate();
                            clear();
                            commitText("；");
                            break;
                        case NUM0_9:
                            commitTheFirstCandidate();
                            clear();
                            commitText(String.valueOf(keyCode - KeyEvent.KEYCODE_0));
                            break;
                        case SPACE:
                            if (candidates.size() > 0) {
                                commitTheFirstCandidate();
                            }
                            clear();
                            break;
                        default:
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_N:
                                    // show dialog: add new words
                                    showAddingNewWordsDialog();
                                    break;
                                case KeyEvent.KEYCODE_O:
                                    final Intent intent = new Intent(WubiIME.this, WubiDatabaseEditActivity.class);
                                    intent.putExtra(WubiDatabaseEditActivity.EXTRA_WUBI_CODE, wubiCodeTV.getText().toString());
                                    startActivity(intent);
                                    break;
                                default:
                            }
                    }
                    return true;
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
                if (!tempEnglishMode) {
                    // Chinese input mode
                    switch (inputRange) {
                        case A_Z:
                            if (event.isShiftPressed()) {
                                // when capitalized, start temporary English input mode
                                commitTheFirstCandidate();
                                clear();
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
                            Common.doAssertion(wubiCodeSB.length() != 0);
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
                } else {
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
                            if (event.isCtrlPressed()) {
                                commitText(candidateTV.getText().toString());
                                commitText("；");
                                composing = false;
                                tempEnglishMode = false;
                                clear();
                            }
                            // otherwise, input ";"
                        default:
                            // input alphabet
                            if (c != 0) {
                                String text = candidateTV.getText().toString() + c;
                                candidateTV.setText(text);
                            }
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
        private boolean checkIfConsumedKeys(@NotNull KeyEvent event) {
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
    });

    private void showAddingNewWordsDialog() {
        if (candidateView != null) {
            int theme = R.style.Theme_Application_NoActionBar;

            final int nightMode = AppCompatDelegate.getDefaultNightMode();
            if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
                theme = R.style.Theme_Application_Dark_NoActionBar;
            }

            final ContextThemeWrapper themedContext = new ContextThemeWrapper(this, theme);
            final View inflate = View.inflate(themedContext, R.layout.wubi_adding_new_words_dialog, null);

            final EditText wordET = ((SmartHintEditText) inflate.findViewById(R.id.wubi_word_et)).getEditText();
            final EditText codeET = ((SmartHintEditText) inflate.findViewById(R.id.wubi_code_et)).getEditText();
            final TextView existAlertTV = inflate.findViewById(R.id.alert_tv);
            final TextView existWordCodeTV = inflate.findViewById(R.id.wubi_word_tv);

            AtomicBoolean alreadyExists = new AtomicBoolean(false);
            final WubiInverseDictDatabase inverseDictDatabase = WubiInverseDictManager.Companion.openDatabase();

            final DialogInterface.OnClickListener positiveButtonAction = (dialog1, which) -> {
                final String word = wordET.getText().toString();
                final String code = codeET.getText().toString();
                if (!checkCode(code)) {
                    ToastUtils.show(this, R.string.wubi_code_invalid_toast);
                    return;
                }
                final String[] query = inverseDictDatabase.query(word);

                boolean existInDict = false;
                if (query.length != 0) {
                    for (String s : query) {
                        if (s.equals(code)) {
                            existInDict = true;
                            break;
                        }
                    }
                }
                if (existInDict) {
                    ToastUtils.show(this, R.string.wubi_words_adding_word_already_exists_ignore_toast);
                    return;
                }
                DictionaryDatabase.Companion.getDictDatabase().addRecord(word, code);
                ToastUtils.show(this, R.string.adding_succeeded);
            };

            Dialog dialog = new AlertDialog.Builder(themedContext)
                    .setView(inflate)
                    .setPositiveButton(R.string.confirm, (dialog1, which) -> {
                        positiveButtonAction.onClick(dialog1, which);
                        inverseDictDatabase.close();
                    })
                    .setNegativeButton(R.string.cancel, (dialog1, which) -> inverseDictDatabase.close())
                    .create();

            final Window window = dialog.getWindow();
            final WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.token = candidateView.getWindowToken();
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            window.setAttributes(lp);
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

            dialog.show();

            wordET.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    final String word = wordET.getText().toString();
                    final String[] query = inverseDictDatabase.query(word);
                    if (query.length != 0) {
                        // the word already exists
                        existAlertTV.setVisibility(View.VISIBLE);
                        existWordCodeTV.setVisibility(View.VISIBLE);
                        StringBuilder sb = new StringBuilder();
                        for (String sbs : query) {
                            sb.append(sbs).append('\n');
                        }
                        sb.deleteCharAt(sb.length() - 1);
                        existWordCodeTV.setText(sb.toString());
                        codeET.setText("");
                        alreadyExists.set(true);
                    } else {
                        existAlertTV.setVisibility(View.GONE);
                        existWordCodeTV.setText("");
                        existWordCodeTV.setVisibility(View.GONE);

                        final String composedWubiCode = inverseDictDatabase.composeCodeFromWord(word);
                        if (composedWubiCode != null) {
                            codeET.setText(composedWubiCode);
                        } else {
                            codeET.setText("");
                        }
                        alreadyExists.set(false);
                    }
                }
            });
            wordET.setText(ic.getSelectedText(0));
        }
    }

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
    @Contract(pure = true)
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
    public void onFinishInput() {
        super.onFinishInput();
    }

    @Override
    public View onCreateCandidatesView() {
        View candidateView = View.inflate(this, R.layout.wubi_input_method_candidate_view, null);
        candidateTV = candidateView.findViewById(R.id.candidates);
        wubiCodeTV = candidateView.findViewById(R.id.code);

        setCandidatesViewShown(true);
        this.candidateView = candidateView;
        return candidateView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        int backgroundColor = Color.WHITE;
        int textColor = Color.BLACK;

        final int nightMode = AppCompatDelegate.getDefaultNightMode();
        if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            backgroundColor = Color.BLACK;
            textColor = Color.WHITE;
        }

        candidateTV.setBackgroundColor(backgroundColor);
        candidateTV.setTextColor(textColor);
        wubiCodeTV.setBackgroundColor(backgroundColor);
        wubiCodeTV.setTextColor(textColor);
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
        super.onEvaluateInputViewShown();
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
     * 0-9 number superscript modifier characters
     */
    private static final char[] numberSuperscriptModifier = {
            '\u2070',
            '\u00b9',
            '\u00b2',
            '\u00b3',
            '\u2074',
            '\u2075',
            '\u2076',
            '\u2077',
            '\u2078',
            '\u2079',
    };

    @NotNull
    @Contract("_ -> new")
    private String toSuperscriptNumberChar(int num) {
        if (num < 0) throw new IllegalArgumentException("number should be positive");
        String numStr = String.valueOf(num);
        byte[] bytes = numStr.getBytes();
        char[] r = new char[numStr.length()];
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            r[i] = numberSuperscriptModifier[b - '0'];
        }
        return new String(r);
    }

    /**
     * Set the keyboard candidate words and wubi code text
     */
    private void updateInputMethodText() {
        String wubiCodeStr = wubiCodeSB.toString();
        StringBuilder candidatesSB = new StringBuilder();
        for (int i = 0, candidatesSize = candidates.size() - 1; i < candidatesSize; i++) {
            String candidate = candidates.get(i);
            candidatesSB.append(toSuperscriptNumberChar(i + 1)).append(candidate).append(' ');
        }
        if (candidates.size() > 0)
            candidatesSB.append(toSuperscriptNumberChar(candidates.size())).append(candidates.get(candidates.size() - 1));

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
    public void fetchCandidatesAndSetToField(@NotNull String wubiCodeStr) {
        candidates.clear();
        if (wubiCodeStr.isEmpty()) return;
        //Z key: repeat last word
        if (wubiCodeSB.toString().equals("z")) {
            candidates.add(lastWord);
        } else try {
            String[] fetched = DictionaryDatabase.Companion.getDictDatabase().fetchCandidates(wubiCodeStr);
            if (fetched != null) {
                this.candidates.addAll(Arrays.asList(fetched));
            }
        } catch (RuntimeException ignored) {
            // no such table
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

        @Contract(mutates = "this")
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean checkCode(@NotNull String code) {
        if (code.isEmpty() || code.length() > 4) {
            return false;
        }
        for (byte b : code.getBytes()) {
            if (!('a' <= b && b <= 'z')) {
                return false;
            }
        }
        return true;
    }

    public static void checkCodeOrThrow(@NotNull String code) {
        if (!checkCode(code)) throw new IllegalArgumentException();
    }
}
