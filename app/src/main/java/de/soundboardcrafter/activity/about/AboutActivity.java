package de.soundboardcrafter.activity.about;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import de.soundboardcrafter.R;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

public class AboutActivity extends AppCompatActivity {
    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, AboutActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView aboutTextView = findViewById(R.id.aboutTextView);
        aboutTextView.setText(
                Html.fromHtml(getString(R.string.about_text_html), FROM_HTML_MODE_COMPACT));
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());
        //Linkify.addLinks(aboutTextView, Linkify.WEB_URLS);
    }
}
