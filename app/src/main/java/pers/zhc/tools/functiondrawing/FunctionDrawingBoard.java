package pers.zhc.tools.functiondrawing;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import pers.zhc.u.MathFloatFunctionInterface;

public class FunctionDrawingBoard extends AppCompatActivity {
    static MathFloatFunctionInterface functionInterface;
    private FunctionDrawingBoardView functionDrawingBoardView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        functionDrawingBoardView = new FunctionDrawingBoardView(this);
        setContentView(functionDrawingBoardView);
    }

    @Override
    public void onBackPressed() {
        FunctionDrawingBoard.functionInterface = functionDrawingBoardView.getFunction();
        startActivity(new Intent(this, FunctionDrawing.class));
    }
}
