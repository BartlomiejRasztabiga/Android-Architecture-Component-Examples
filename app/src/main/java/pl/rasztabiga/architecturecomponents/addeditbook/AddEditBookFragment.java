package pl.rasztabiga.architecturecomponents.addeditbook;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import pl.rasztabiga.architecturecomponents.R;
import pl.rasztabiga.architecturecomponents.SnackbarMessage;
import pl.rasztabiga.architecturecomponents.databinding.AddbookFragBinding;
import pl.rasztabiga.architecturecomponents.util.SnackbarUtils;

public class AddEditBookFragment extends Fragment {

    public static final String ARGUMENT_EDIT_BOOK_ID = "EDIT_BOOK_ID";

    private AddEditBookViewModel mViewModel;

    private AddbookFragBinding mViewDataBinding;

    public static AddEditBookFragment newInstance() {
        return new AddEditBookFragment();
    }

    public AddEditBookFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupFab();

        setupSnackbar();

        setupActionBar();

        loadData();
    }

    private void loadData() {
        // Add or edit an existing book?
        if (getArguments() != null) {
            Long editBookId = getArguments().getLong(ARGUMENT_EDIT_BOOK_ID);
            if (!editBookId.equals(0L)) {
                mViewModel.start(editBookId);
            }
        } else {
            mViewModel.start(null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.addbook_frag, container, false);
        if (mViewDataBinding == null) {
            mViewDataBinding = AddbookFragBinding.bind(root);
        }

        mViewModel = AddEditBookActivity.obtainViewModel(getActivity());

        mViewDataBinding.setViewmodel(mViewModel);

        setHasOptionsMenu(true);
        setRetainInstance(false);

        return mViewDataBinding.getRoot();
    }

    private void setupSnackbar() {
        mViewModel.getSnackbarMessage().observe(this, (SnackbarMessage.SnackbarObserver)
                snackbarMessageResourceId -> SnackbarUtils.showSnackbar(getView(),
                        getString(snackbarMessageResourceId)));
    }

    private void setupFab() {
        FloatingActionButton fab = getActivity().findViewById(R.id.fab_edit_book_done);
        fab.setImageResource(R.drawable.ic_done);
        fab.setOnClickListener(v -> mViewModel.saveBook());
    }

    private void setupActionBar() {
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        Long editBookId = getArguments().getLong(ARGUMENT_EDIT_BOOK_ID);
        if (getArguments() != null && !editBookId.equals(0L)) {
            actionBar.setTitle(R.string.edit_book);
        } else {
            actionBar.setTitle(R.string.add_book);
        }
    }
}
