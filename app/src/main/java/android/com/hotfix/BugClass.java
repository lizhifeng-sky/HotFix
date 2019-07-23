package android.com.hotfix;

import android.content.Context;
import android.widget.Toast;

public class BugClass {
    public BugClass(Context context){
        Toast.makeText(context,"这是一个bug！",Toast.LENGTH_SHORT).show();
    }
}
