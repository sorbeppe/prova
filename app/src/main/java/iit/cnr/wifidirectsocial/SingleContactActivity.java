package iit.cnr.wifidirectsocial;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by sorbeppe84 on 04/03/2016.
 */
public class SingleContactActivity extends Activity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.list_item);

        TextView name_textView = (TextView) findViewById(R.id.name);
        TextView email_textView = (TextView) findViewById(R.id.email);
        TextView phone_mobile_textView = (TextView) findViewById(R.id.mobile);

        Intent i = getIntent();
        // getting attached intent data
        String name = i.getStringExtra("name");
        String email = i.getStringExtra("email");
        String phone_mobile = i.getStringExtra("mobile");
        // displaying
        name_textView.setText(name);
        email_textView.setText(email);
        phone_mobile_textView.setText(phone_mobile);

    }


}
