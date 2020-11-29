package pers.zhc.tools.inputmethod;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import pers.zhc.tools.R;
import pers.zhc.tools.test.wubiinput.WubiInput;
import pers.zhc.tools.utils.sqlite.MySQLite3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WubiIME extends InputMethodService {
    private final StringBuilder wubiCodeSB = new StringBuilder();
    private MySQLite3 wubiDictDB = null;
    private TextView candidateTV, wubiCodeTV;
    private boolean isAlphabetsMode = false;

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

    private boolean checkAcceptedKeyCodeRange(int keyCode) {
        return (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z)
                | (keyCode == KeyEvent.KEYCODE_COMMA || keyCode == KeyEvent.KEYCODE_PERIOD)
                | keyCode == KeyEvent.KEYCODE_DEL
                | keyCode == KeyEvent.KEYCODE_SPACE;
    }

    private final List<String> candidates = new ArrayList<>();
    private boolean switchTypingMode = true;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switchTypingMode = keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT;
        if (isAlphabetsMode) return false;
        InputConnection ic = getCurrentInputConnection();

        boolean accept = checkAcceptedKeyCodeRange(keyCode);
        if (accept) {
            if (wubiCodeSB.length() == 4 && (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z)) {
                commitTheFirstCandidate(ic);
                clearWubiCodeSB();
                candidates.clear();
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_SPACE:
                    if (wubiCodeSB.length() == 0) {
                        ic.commitText(" ", 0);
                        break;
                    }
                    commitTheFirstCandidate(ic);
                    candidates.clear();
                    clearWubiCodeSB();
                    break;
                case KeyEvent.KEYCODE_COMMA:
                    commitTheFirstCandidate(ic);
                    candidates.clear();
                    clearWubiCodeSB();
                    ic.commitText("，", 0);
                    break;
                case KeyEvent.KEYCODE_PERIOD:
                    commitTheFirstCandidate(ic);
                    candidates.clear();
                    clearWubiCodeSB();
                    ic.commitText("。", 0);
                    break;
                case KeyEvent.KEYCODE_DEL:
                    if (wubiCodeSB.length() == 0) {
                        ic.deleteSurroundingText(1, 0);
                    } else wubiCodeSB.deleteCharAt(wubiCodeSB.length() - 1);
                    break;
                default:
                    wubiCodeSB.append(((char) event.getUnicodeChar()));
                    break;
            }
            refresh();
            if (wubiCodeSB.length() == 4 && candidates.size() == 1) {
                commitTheFirstCandidate(ic);
                candidates.clear();
                clearWubiCodeSB();
                setTVs(getString(R.string.nul));
            }
        }
        return accept;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            if (switchTypingMode) {
                isAlphabetsMode = !isAlphabetsMode;
            }
            if (isAlphabetsMode) {
                candidateTV.setText(R.string.alphabet_mode);
            } else candidateTV.setText(R.string.nul);
        }
        return checkAcceptedKeyCodeRange(keyCode);
    }

    private void commitTheFirstCandidate(InputConnection ic) {
        if (candidates.size() != 0)
            ic.commitText(candidates.get(0), 0);
    }

    private void clearWubiCodeSB() {
        wubiCodeSB.delete(0, wubiCodeSB.length());
    }

    private void refresh() {
        String wubiCodeStr = wubiCodeSB.toString();
        setCandidatesField(wubiCodeStr);
        setTVs(wubiCodeStr);
    }

    private void setTVs(String wubiCodeStr) {
        String candidatesString = arrays2String(candidates.toArray());
        candidateTV.setText(candidatesString);
        wubiCodeTV.setText(wubiCodeStr);
    }

    private String arrays2String(Object[] a) {
        if (a == null)
            return "null";

        int iMax = a.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.toString();
            b.append(", ");
        }
    }

    private void setCandidatesField(String wubiCodeStr) {
        candidates.clear();
        if (wubiCodeStr.isEmpty()) return;
        //TODO 输一个码会索引再次的bug
        wubiDictDB.exec(String.format("SELECT word FROM wubi_code_%s WHERE code IS '%s'"
                , wubiCodeStr.charAt(0), wubiCodeStr), contents -> {
            String[] split = contents[0].split("\\|");
            candidates.addAll(Arrays.asList(split));
            return 0;
        });
    }
}