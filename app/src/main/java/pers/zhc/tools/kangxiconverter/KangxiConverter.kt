package pers.zhc.tools.kangxiconverter;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KangxiConverter {
    private static final String kangxiRadicals = "⼀⼁⼂⼃⼄⼅⼆⼇⼈⼉⼊⼋⼌⼍⼎⼏⼐⼑⼒⼓⼔⼕⼖⼗⼘⼙⼚⼛⼜⼝⼞⼟⼠⼡⼢⼣⼤⼥⼦⼧⼨⼩⼪⼫⼬⼭⼮⼯⼰⼱⼲⼳⼴⼵⼶⼷⼸⼹⼺⼻⼼⼽⼾⼿⽀⽁⽂⽃⽄⽅⽆⽇⽈⽉⽊⽋⽌⽍⽎⽏⽐⽑⽒⽓⽔⽕⽖⽗⽘⽙⽚⽛⽜⽝⽞⽟⽠⽡⽢⽣⽤⽥⽦⽧⽨⽩⽪⽫⽬⽭⽮⽯⽰⽱⽲⽳⽴⽵⽶⽷⽸⽹⽺⽻⽼⽽⽾⽿⾀⾁⾂⾃⾄⾅⾆⾇⾈⾉⾊⾋⾌⾍⾎⾏⾐⾑⾒⾓⾔⾕⾖⾗⾘⾙⾚⾛⾜⾝⾞⾟⾠⾡⾢⾣⾤⾥⾦⾧⾨⾩⾪⾫⾬⾭⾮⾯⾰⾱⾲⾳⾴⾵⾶⾷⾸⾹⾺⾻⾼⾽⾾⾿⿀⿁⿂⿃⿄⿅⿆⿇⿈⿉⿊⿋⿌⿍⿎⿏⿐⿑⿒⿓⿔⿕";
    private static final String[] kangxiRadicalsArr = kangxiRadicals.split("");
    private static final String normalHans = "一丨丶丿乙亅二亠人儿入八冂冖冫几凵刀力勹匕匚匸十卜卩厂厶又口口土士夂夊夕大女子宀寸小尢尸屮山巛工己巾干幺广廴廾弋弓彐彡彳心戈户手支攴文斗斤方无日曰月木欠止歹殳毋比毛氏气水火爪父爻爿片牙牛犬玄玉瓜瓦甘生用田疋疒癶白皮皿目矛矢石示禸禾穴立竹米糸缶网羊羽老而耒耳聿肉臣自至臼舌舛舟艮色艸虍虫血行衣襾見角言谷豆豕豸貝赤走足身車辛辰辵邑酉采里金長門阜隶隹雨青非面革韋韭音頁風飛食首香馬骨高髟鬥鬯鬲鬼魚鳥鹵鹿麥麻黃黍黑黹黽鼎鼓鼠鼻齊齒龍龜龠";
    private static final String[] normalHansArr = normalHans.split("");

    public static Boolean hasKangxiRadicals(String str) {
        return str.matches(".*[" + kangxiRadicals + "].*");
    }

    public static Boolean hasNormalKangXiChars(String str) {
        return str.matches(".*[" + normalHans + "].*");
    }

    public static String KangXi2Normal(String str) {
        if (hasKangxiRadicals(str)) {
            int i = -1;
            while (i < kangxiRadicals.length() - 1) {
                i++;
                str = str.replace(kangxiRadicalsArr[i], normalHansArr[i]);
            }
        }
        return str;
    }

    public static String normal2KangXi(String str) {
        if (hasNormalKangXiChars(str)) {
            int i = -1;
            while (i < kangxiRadicals.length() - 1) {
                i++;
                str = str.replace(normalHansArr[i], kangxiRadicalsArr[i]);
            }
        }
        return str;
    }

    public static void markKangxiRadicalsEditText(EditText et) {
        String inputText = et.getText().toString();
        if(!hasKangxiRadicals(inputText)) return;
        SpannableString spannableString = new SpannableString(inputText);

        String regex = "[" + kangxiRadicals + "]"; // 你的正则表达式

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputText);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED); // 高亮颜色
            spannableString.setSpan(colorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        et.setText(spannableString);
        et.setSelection(inputText.length()); // 将光标移至末尾
    }

    public static void markNormalHansEditText(EditText et) {
        String inputText = et.getText().toString();
        if(!hasNormalKangXiChars(inputText)) return;
        SpannableString spannableString = new SpannableString(inputText);

        String regex = "[" + normalHans + "]"; // 你的正则表达式

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputText);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.GREEN); // 高亮颜色
            spannableString.setSpan(colorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        et.setText(spannableString);
        et.setSelection(inputText.length()); // 将光标移至末尾
    }
}
