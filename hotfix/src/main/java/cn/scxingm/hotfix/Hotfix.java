package cn.scxingm.hotfix;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by scxingm on 2018/4/20.
 */

public class Hotfix {

    public static void init(Context context){
        Toast.makeText(context, "最新SDK版本："+Constant.VERSION, Toast.LENGTH_LONG).show();
    }

}
