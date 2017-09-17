package pl.rasztabiga.architecturecomponents.bookdetail;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import pl.rasztabiga.architecturecomponents.R;
import pl.rasztabiga.architecturecomponents.SnackbarMessage;
import pl.rasztabiga.architecturecomponents.addeditbook.BookDetailUserActionsListener;
import pl.rasztabiga.architecturecomponents.databinding.BookdetailFragBinding;
import pl.rasztabiga.architecturecomponents.util.SnackbarUtils;

public class BookDetailFragment extends Fragment {

    public static final String ARGUMENT_BOOK_ID = "BOOK_ID";

    public static final int REQUEST_EDIT_BOOK = 1;

    private BookDetailViewModel mViewModel;

    public static BookDetailFragment newInstance(Long bookId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARGUMENT_BOOK_ID, bookId);
        BookDetailFragment fragment = new BookDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupFab();

        setupSnackbar();
    }

    private void setupFab() {
        FloatingActionButton fab = getActivity().findViewById(R.id.fab_edit_book);

        fab.setOnClickListener(v -> mViewModel.editBook());
    }

    private void setupSnackbar() {
        mViewModel.getSnackbarMessage().observe(this,
                (SnackbarMessage.SnackbarObserver) snackbarMessageResourceId ->
                        SnackbarUtils.showSnackbar(getView(),
                                getString(snackbarMessageResourceId)));
    }

    @Override
    public void onResume() {
        super.onResume();
        Long bookId = getArguments().getLong(ARGUMENT_BOOK_ID);
        if (bookId != 0L) {
            mViewModel.start(getArguments().getLong(ARGUMENT_BOOK_ID));
        } // Else I don't know...
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bookdetail_frag, container, false);

        BookdetailFragBinding viewDataBinding = BookdetailFragBinding.bind(view);

        mViewModel = BookDetailActivity.obtainViewModel(getActivity());

        viewDataBinding.setViewmodel(mViewModel);

        BookDetailUserActionsListener actionsListener = getBookDetailUserActionsListener();

        viewDataBinding.setListener(actionsListener);

        setHasOptionsMenu(true);

        return view;
    }

    private BookDetailUserActionsListener getBookDetailUserActionsListener() {
        return v -> mViewModel.setCompleted(((CheckBox) v).isChecked());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bookdetail_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_delete) {
            mViewModel.deleteBook();
            return true;
        }
        return false;
    }
}
