package pl.rasztabiga.architecturecomponents.addeditbook;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import pl.rasztabiga.architecturecomponents.R;
import pl.rasztabiga.architecturecomponents.ViewModelFactory;
import pl.rasztabiga.architecturecomponents.util.ActivityUtils;

/**
 * Displays an add book screen.
 */
public class AddEditBookActivity extends AppCompatActivity implements AddEditBookNavigator {

    public static final int REQUEST_CODE = 1;

    public static final int ADD_EDIT_RESULT_OK = RESULT_FIRST_USER + 1;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBookSaved() {
        setResult(ADD_EDIT_RESULT_OK);
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addbook_act);

        setupToolbar();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        AddEditBookFragment addEditTaskFragment = obtainViewFragment();

        ActivityUtils.replaceFragmentInActivity(getSupportFragmentManager(),
                addEditTaskFragment, R.id.contentFrame);

        subscribeToNavigationChanges();
    }

    private void subscribeToNavigationChanges() {
        AddEditBookViewModel viewModel = obtainViewModel(this);

        // The activity observes the navigation events in the ViewModel
        viewModel.getBookUpdatedEvent().observe(this, e -> AddEditBookActivity.this.onBookSaved());
    }

    public static AddEditBookViewModel obtainViewModel(FragmentActivity activity) {
        // Use a Factory to inject dependencies into the ViewModel
        ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());

        return ViewModelProviders.of(activity, factory).get(AddEditBookViewModel.class);
    }

    @NonNull
    private AddEditBookFragment obtainViewFragment() {
        // View Fragment
        AddEditBookFragment addEditBookFragment = (AddEditBookFragment) getSupportFragmentManager()
                .findFragmentById(R.id.contentFrame);

        if (addEditBookFragment == null) {
            addEditBookFragment = AddEditBookFragment.newInstance();

            // Send the task ID to the fragment
            Bundle bundle = new Bundle();
            bundle.putLong(AddEditBookFragment.ARGUMENT_EDIT_BOOK_ID,
                    getIntent().getLongExtra(AddEditBookFragment.ARGUMENT_EDIT_BOOK_ID, 0L));
            addEditBookFragment.setArguments(bundle);
        }
        return addEditBookFragment;
    }
}
