package pers.zhc.tools.inputmethod;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ToastUtils;

public class MyIME extends InputMethodService {
    @Override
    public View onCreateInputView() {
        View candidateView = View.inflate(this, R.layout.wubi_input_method_candidate_view, null);
        setInputView(candidateView);
        setCandidatesViewShown(true);
        setCandidatesView(new Button(this));
        return super.onCreateInputView();
    }

    private boolean checkAcceptedKeyCodeRange(int keyCode) {
        return (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z)
                | (keyCode == KeyEvent.KEYCODE_COMMA || keyCode == KeyEvent.KEYCODE_PERIOD);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        InputConnection ic = getCurrentInputConnection();
        boolean accept = checkAcceptedKeyCodeRange(keyCode);
        if (accept) {
            if (!ic.commitText(String.valueOf(keyCode), 0)) ToastUtils.show(this, R.string.something_went_wrong);
        }
        return accept;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return checkAcceptedKeyCodeRange(keyCode);
    }
}
