package de.soundboardcrafter.activity.init;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import de.soundboardcrafter.R;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * Shown at first start - shows something like "Building your soundboards..."
 */
public class InitActivity extends AppCompatActivity {
    /**
     * Builds  an {@link Intent}, suitable for starting this activity.
     */
    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, InitActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        TextView aboutTextView = findViewById(R.id.aboutTextView);
        aboutTextView.setText(
                Html.fromHtml(getString(R.string.about_text_html), FROM_HTML_MODE_COMPACT));
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
