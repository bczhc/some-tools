package pers.zhc.tools.wubi;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import kotlin.Unit;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import pers.zhc.tools.R;
import pers.zhc.tools.utils.*;
import pers.zhc.tools.views.SmartHintEditText;
import pers.zhc.tools.wubi.SingleCharCodesChecker.CheckingRule;
import pers.zhc.util.Assertion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

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
     * punctuations that need to input with shift key
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
    @Nullable
    private TextView wubiCodeTV;
    @Nullable
    private FlexboxLayout candidateLayout = null;
    private TextView candidateHintTV;
    private boolean alphabetMode = false;
    private InputConnection ic;
    private String lastWord;
    private TextToSpeech tts = null;
    private boolean tempEnglishMode = false;
    private final TemporaryAlphabetModeManager tam = new TemporaryAlphabetModeManager();
    private int tvBackgroundColor, tvTextColor;
    /**
     * when is {@code null}, it's disabled
     */
    @Nullable
    private SingleCharCodesChecker singleCharCodesChecker = null;
    @Nullable
    private CheckingRule sccAlternativeRule = null;

    @Override
    public void onCreate() {
        super.onCreate();

        int backgroundColor = Color.WHITE;
        int textColor = Color.BLACK;

        final int nightMode = AppCompatDelegate.getDefaultNightMode();
        if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            backgroundColor = Color.BLACK;
            textColor = Color.WHITE;
        }

        tvBackgroundColor = backgroundColor;
        tvTextColor = textColor;

        candidateHintTV = View.inflate(this, R.layout.wubi_candidate_cell, null).findViewById(R.id.tv);
        candidateHintTV.setTextColor(tvTextColor);
        candidateHintTV.setBackgroundColor(tvBackgroundColor);
    }

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
                                    // open Wubi dictionary editing activity
                                    startWubiDictEditingActivity(wubiCodeTV.getText().toString());
                                    break;
                                case KeyEvent.KEYCODE_T:
                                    // tools
                                    showToolsDialog();
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
                        tam.start();
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
                                tam.start();
                                tam.setText(String.valueOf(c));
                                wubiCodeTV.setText(R.string.temporary_english_mode);
                                return true;
                            }
                            // on the fifth code
                            if (wubiCodeSB.length() == 4) {
                                sccAlternativeRule = CheckingRule.SHORTCUT_1_2;
                                commitTheFirstCandidate();
                                clear();
                            }
                            wubiCodeSB.append(c);
                            update();
                            // on the fourth code & have the only one candidate word
                            if (wubiCodeSB.length() == 4 && candidates.size() == 1) {
                                sccAlternativeRule = CheckingRule.SHORTCUT_1_2;
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
                            commitText(tam.getText());
                            clear();
                            tam.finish();
                            break;
                        case BACKSPACE:
                            // delete the last character
                            final int length = tam.getText().length();
                            if (length == 0) {
                                // exit
                                tam.finish();
                                clear();
                                return true;
                            }
                            CharSequence s = tam.getText().subSequence(0, length - 1);
                            tam.setText(s.toString());
                            break;
                        case SEMICOLON:
                            // commit Chinese semicolon and exit temporary English mode
                            if (event.isCtrlPressed()) {
                                commitText(tam.getText());
                                commitText("；");
                                tam.finish();
                                clear();
                            }
                            // otherwise, input ";"
                        default:
                            // input alphabet
                            if (c != 0) {
                                tam.setText(tam.getText() + c);
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

    private void startWubiDictEditingActivity(@NotNull String code) {
        final Intent intent = new Intent(WubiIME.this, WubiDatabaseEditActivity.class);
        intent.putExtra(WubiDatabaseEditActivity.EXTRA_WUBI_CODE, code);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showAddingNewWordsDialog() {
        if (candidateLayout != null) {
            ContextThemeWrapper themedContext = getThemedContext();
            CharSequence selectedText = ic.getSelectedText(0);
            Dialog dialog = createAddingNewWordsDialog(themedContext, selectedText != null ? selectedText.toString() : null);

            IBinder windowToken = candidateLayout.getWindowToken();
            setDialogAttrs(dialog, windowToken, MATCH_PARENT, WRAP_CONTENT);
            dialog.show();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void setDialogAttrs(@NotNull Dialog dialog, IBinder token, int width, int height) {
        final Window window = dialog.getWindow();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.token = token;
        lp.width = width;
        lp.height = height;
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;

        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    @Contract(" -> new")
    private @NotNull ContextThemeWrapper getThemedContext() {
        int theme = R.style.Theme_Application_NoActionBar;

        final int nightMode = AppCompatDelegate.getDefaultNightMode();
        if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            theme = R.style.Theme_Application_Dark_NoActionBar;
        }
        return new ContextThemeWrapper(this, theme);
    }

    static Dialog createAddingNewWordsDialog(Context context, @Nullable String defaultString) {
        final View inflate = View.inflate(context, R.layout.wubi_adding_new_words_dialog, null);

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
                ToastUtils.show(context, R.string.wubi_code_invalid_toast);
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
                ToastUtils.show(context, R.string.wubi_words_adding_word_already_exists_ignore_toast);
                return;
            }
            DictionaryDatabase.Companion.getDictDatabase().addRecord(word, code);
            ToastUtils.show(context, R.string.adding_succeeded);
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        Dialog dialog = builder.setView(inflate).setPositiveButton(R.string.confirm, (dialog1, which) -> {
            positiveButtonAction.onClick(dialog1, which);
            inverseDictDatabase.close();
        }).setNegativeButton(R.string.cancel, (dialog1, which) -> inverseDictDatabase.close()).create();

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
        if (defaultString != null) {
            wordET.setText(defaultString);
        }
        return dialog;
    }

    private void showToolsDialog() {
        if (candidateLayout == null) return;
        IBinder token = candidateLayout.getWindowToken();
        Context context = getThemedContext();

        final View inflate = View.inflate(context, R.layout.wubi_tools_dialog, null);
        RecyclerView rv = inflate.findViewById(R.id.recycler_view);

        List<String> data = Arrays.asList(context.getResources().getStringArray(R.array.wubi_tools_names));

        final RecyclerViewArrayAdapter<String> adapter = RecyclerViewUtils.Companion.buildSimpleItem1ListAdapter(
                context, data, true
        );

        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        final Dialog dialog = new Dialog(context);
        dialog.setContentView(inflate);
        setDialogAttrs(dialog, token, MATCH_PARENT, WRAP_CONTENT);
        dialog.show();

        final View.OnClickListener[] listeners = {
                v -> {
                    // single character codes records
                    showSingleCharCodesRecordingDialog();
                },
                v -> {
                    // wubi code inverse lookup
                    CharSequence selectedText = ic.getSelectedText(0);
                    handleInverseLookup(LangUtils.Companion.nullMap(selectedText, CharSequence::toString));
                }
        };

        adapter.setOnItemClickListener((position, view) -> {
            listeners[position].onClick(view);
            return Unit.INSTANCE;
        });
    }

    private void showSingleCharCodesRecordingDialog() {
        if (candidateLayout == null) {
            return;
        }

        Context context = getThemedContext();
        final View inflate = View.inflate(context, R.layout.wubi_single_char_codes_recording_dialog, null);
        MaterialSwitch materialSwitch = inflate.findViewById(R.id.switch_btn);
        Button emptyButton = inflate.findViewById(R.id.empty_button);
        Button copyButton = inflate.findViewById(R.id.copy_button);
        RecyclerView recyclerView = inflate.findViewById(R.id.recycler_view);
        LinearLayout optionLL = inflate.findViewById(R.id.option_ll);

        materialSwitch.setChecked(singleCharCodesChecker != null);
        if (singleCharCodesChecker == null) {
            optionLL.setVisibility(View.GONE);
        } else {
            optionLL.setVisibility(View.VISIBLE);
        }

        final AtomicReference<SingleCharCodesChecker.RecyclerViewAdapter> adapter = new AtomicReference<>(null);

        final Runnable setupRecyclerView = () -> {
            adapter.set(singleCharCodesChecker.getRecyclerViewAdapter(context));

            recyclerView.setAdapter(adapter.get());
            RecyclerViewUtilsKt.setLinearLayoutManager(recyclerView);
            RecyclerViewUtilsKt.addDividerLines(recyclerView);
        };

        materialSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                singleCharCodesChecker = new SingleCharCodesChecker(record -> {
                    ToastUtils.show(context, context.getString(
                            R.string.wubi_single_char_code_check_notify_toast,
                            record.getChar(),
                            record.getInputCode(),
                            record.getShortestCode()
                    ));
                    return Unit.INSTANCE;
                });
                optionLL.setVisibility(View.VISIBLE);
                if (adapter.get() == null) {
                    setupRecyclerView.run();
                }
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                singleCharCodesChecker = null;
                optionLL.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
            }
        });

        emptyButton.setOnClickListener(v -> {
            if (singleCharCodesChecker != null) {
                singleCharCodesChecker.clear();
                adapter.get().updateList();

            } else {
                recyclerView.setVisibility(View.GONE);
            }
        });

        copyButton.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();

            ArrayList<SingleCharCodesChecker.Record> records = singleCharCodesChecker.queryAll();
            for (SingleCharCodesChecker.Record record : records) {
                sb.append(context.getString(
                        R.string.wubi_single_char_code_check_notify_toast,
                        record.getChar(), record.getInputCode(), record.getShortestCode()
                ));
                sb.append("\n");
            }

            ClipboardUtils.Companion.putWithToast(context, sb.toString());
        });

        if (singleCharCodesChecker != null) {
            setupRecyclerView.run();
        }

        final Dialog dialog = new Dialog(context);
        dialog.setContentView(inflate);
        setDialogAttrs(dialog, candidateLayout.getWindowToken(), MATCH_PARENT, WRAP_CONTENT);
        dialog.show();
    }

    private void handleInverseLookup(@Nullable String text) {
        IBinder windowToken = getWindowToken();
        if (windowToken == null) return;

        ContextThemeWrapper context = getThemedContext();

        if (text == null) {
            showInverseDictLookupDialog(context, null, new ArrayList<>(), true, windowToken);
            return;
        }

        Dialog progressDialog = createProgressDialog(context, windowToken);

        progressDialog.show();
        new Thread(() -> {
            final List<String> queried = new ArrayList<>();
            WubiInverseDictManager.Companion.useDatabase(db -> {
                queried.addAll(Arrays.asList(db.query(text)));
                return Unit.INSTANCE;
            });

            Common.runOnUiThread(context, () -> {
                progressDialog.dismiss();
                showInverseDictLookupDialog(context, text, queried, false, windowToken);
            });
        }).start();
    }

    @NotNull
    private Dialog createProgressDialog(Context context, IBinder windowToken) {
        Dialog progressDialog = new Dialog(context);
        setDialogAttrs(progressDialog, windowToken, MATCH_PARENT, WRAP_CONTENT);

        View inflate = View.inflate(context, R.layout.progress_bar_indeterminate_compat, null);
        TextView titleTV = inflate.findViewById(R.id.title);
        titleTV.setText(getString(R.string.looking_up));
        progressDialog.setContentView(inflate);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    private void showInverseDictLookupDialog(Context context, @Nullable String text, List<String> queried, boolean noSelectionHint, IBinder windowToken) {
        Dialog dialog = new Dialog(context);
        View inflate = View.inflate(context, R.layout.wubi_inverse_dict_lookup_dialog, null);
        dialog.setContentView(inflate);
        setDialogAttrs(dialog, windowToken, MATCH_PARENT, WRAP_CONTENT);

        TextView headTV = inflate.findViewById(R.id.head_tv);
        if (noSelectionHint) {
            headTV.setText(getString(R.string.wubi_no_selected_text_hint));
        } else {
            headTV.setText(getString(R.string.wubi_inverse_lookup_dialog_head, text));
        }

        boolean noResults = queried.isEmpty();
        if (noResults && !noSelectionHint) {
            queried = Collections.singletonList(getString(R.string.none));
        }

        RecyclerView recyclerView = inflate.findViewById(R.id.recycler_view);
        RecyclerViewArrayAdapter<String> adapter = new RecyclerViewArrayAdapter<>(context, queried, android.R.layout.simple_list_item_1, (view, s) -> {
            TextView textView = (TextView) view;
            textView.setGravity(Gravity.CENTER);
            textView.setText(s);
            if (!noResults) {
                textView.setBackgroundResource(R.drawable.selectable_bg);
            }
            return Unit.INSTANCE;
        });

        List<String> finalQueried = queried;
        adapter.setOnItemClickListener((position, view) -> {
            if (!noResults) {
                String code = finalQueried.get(position);
                startWubiDictEditingActivity(code);
            }
            return Unit.INSTANCE;
        });

        recyclerView.setAdapter(adapter);
        RecyclerViewUtilsKt.setLinearLayoutManager(recyclerView);

        dialog.show();
    }

    /**
     * Reset wubi code, candidate words, and input method text.
     */
    private void clear() {
        resetWubiCodeAndCandidates();
        if (wubiCodeTV != null) {
            wubiCodeTV.setText(R.string.nul);
        }
        if (candidateLayout != null) {
            candidateLayout.removeAllViews();
        }
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

            if (candidateLayout != null) {
                candidateLayout.removeAllViews();
                candidateLayout.addView(candidateHintTV);
                candidateHintTV.setText(alphabetMode ? R.string.alphabet_mode : R.string.nul);
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
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)
            return InputRange.NUM0_9;
        if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT)
            return InputRange.CTRL;
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
        if (alphabetMode) candidateHintTV.setText(R.string.alphabet_mode);
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
        candidateLayout = candidateView.findViewById(R.id.candidate_fl);
        wubiCodeTV = candidateView.findViewById(R.id.code);

        // set minimal size
        final View view = View.inflate(this, R.layout.wubi_candidate_cell, null).findViewById(R.id.tv);
        Assertion.doAssertion(wubiCodeTV != null);
        wubiCodeTV.measure(0, 0);
        view.measure(0, 0);
        candidateView.setMinimumHeight(wubiCodeTV.getMeasuredHeight() + view.getMeasuredHeight());

        setCandidatesViewShown(true);
        return candidateView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        if (wubiCodeTV != null) {
            wubiCodeTV.setBackgroundColor(tvBackgroundColor);
            wubiCodeTV.setTextColor(tvTextColor);
        }
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
        if (candidateLayout == null) {
            return;
        }

        String wubiCodeStr = wubiCodeSB.toString();
        if (wubiCodeTV != null) {
            wubiCodeTV.setText(wubiCodeStr);
        }

        candidateLayout.removeAllViews();
        for (int i = 0; i < candidates.size(); i++) {
            String candidate = candidates.get(i);
            String cellStr = toSuperscriptNumberChar(i + 1) + candidate;

            TextView cellTV = View.inflate(this, R.layout.wubi_candidate_cell, null).findViewById(R.id.tv);
            cellTV.setBackgroundColor(tvBackgroundColor);
            cellTV.setTextColor(tvTextColor);
            cellTV.setText(cellStr);

            cellTV.setOnClickListener(v -> {
                commitText(candidate);
                clear();
            });

            candidateLayout.addView(cellTV);
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

            if (SingleCharCodesChecker.Companion.checkIfSingleChar(s) && singleCharCodesChecker != null) {
                CheckingRule rule;
                if (sccAlternativeRule != null) {
                    rule = sccAlternativeRule;
                    sccAlternativeRule = null;
                } else {
                    rule = CheckingRule.SHORTCUT_1_2_3;
                }
                singleCharCodesChecker.asyncCommit(s, wubiCodeSB.toString(), rule);
            }
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

    private class TemporaryAlphabetModeManager {
        // TAM: Temporary Alphabet Mode
        private void start() {
            if (candidateLayout != null && wubiCodeTV != null) {
                wubiCodeTV.setText(R.string.temporary_english_mode);
                composing = true;
                tempEnglishMode = true;
                candidateLayout.removeAllViews();
                candidateLayout.addView(candidateHintTV);
            }
        }

        private void finish() {
            if (candidateLayout != null) {
                composing = false;
                tempEnglishMode = false;
                candidateLayout.removeAllViews();
                candidateHintTV.setText(R.string.nul);
            }
        }

        private void setText(String text) {
            candidateHintTV.setText(text);
        }

        private @NotNull String getText() {
            return candidateHintTV.getText().toString();
        }
    }

    @Nullable
    private IBinder getWindowToken() {
        return LangUtils.Companion.nullMap(candidateLayout, View::getWindowToken);
    }
}
